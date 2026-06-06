package kingdom.smp.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Config;
import kingdom.smp.Ironhold;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Async chat client for OpenRouter's OpenAI-compatible Chat Completions API.
 * Used by Kangarude to talk to Claude Haiku (or whichever model is configured
 * via {@code kangarudeOpenrouterModel}).
 */
public final class OpenRouterClient {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Kangarude-OpenRouter");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .executor(EXECUTOR)
        .build();

    private static final Gson GSON = new Gson();
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    private OpenRouterClient() {}

    /** A single message in the chat history. */
    public record Message(String role, String content) {}

    /** Env var wins so the key doesn't have to live in the world toml. */
    private static String resolveApiKey() {
        String env = System.getenv("OPENROUTER_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        try {
            String cfg = Config.OPENROUTER_API_KEY.get();
            return cfg == null ? "" : cfg.trim();
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /**
     * Backwards-compat: uses Kangarude's configured model + max_tokens=100.
     */
    public static void chat(
            String systemPrompt,
            List<Message> history,
            String userMessage,
            Consumer<String> onResult) {
        chat(Config.KANGARUDE_OPENROUTER_MODEL.get(), 100, 0.85,
            systemPrompt, history, userMessage, "Kangarude", onResult);
    }

    /**
     * Fires an async chat request with explicit model + sampling overrides so each
     * NPC can run on a different brain. Invokes {@code onResult} on the HTTP executor
     * thread with the assistant text (or {@code null} on failure).
     *
     * @param model       OpenRouter model id (e.g. {@code "anthropic/claude-haiku-4.5"})
     * @param maxTokens   token cap for the reply — keep tight for snappy NPCs
     * @param temperature sampling temperature (0.0–1.2)
     * @param systemPrompt the system message for this NPC
     * @param history     prior chat turns (will be sent verbatim)
     * @param userMessage current user input
     * @param tag         short label for log lines (e.g. {@code "Halric"})
     * @param onResult    receives the assistant content string, or null on failure
     */
    public static void chat(
            String model,
            int maxTokens,
            double temperature,
            String systemPrompt,
            List<Message> history,
            String userMessage,
            String tag,
            Consumer<String> onResult) {
        // Build messages with a plain-string system prompt (no caching).
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        appendHistoryAndUser(messages, history, userMessage);
        dispatch(model, maxTokens, temperature, messages, tag, onResult);
    }

    /**
     * Like {@link #chat} but splits the system message into a cacheable
     * prefix and a dynamic suffix, using Anthropic's prompt-caching feature
     * via OpenRouter. Subsequent turns within the cache TTL (~5 min) skip
     * re-encoding the prefix — major speed + cost win when the same NPC
     * is having a multi-turn conversation.
     *
     * <p>Requirements for a cache HIT:
     * <ul>
     *   <li>{@code cacheableSystem} is byte-identical to a recent prior call</li>
     *   <li>Same model</li>
     *   <li>Prefix is ≥ ~1024 tokens (Anthropic min)</li>
     *   <li>Provider supports cache_control (Anthropic + a few others on OpenRouter)</li>
     * </ul>
     *
     * @param cacheableSystem the STATIC portion of the system prompt
     *                        (personality + lore) — marked with cache_control
     * @param dynamicSystem   the per-turn variable portion (player name, memory,
     *                        runtime alerts, etc.) — appended after the cache
     *                        breakpoint, not cached. May be empty/null.
     */
    public static void chatWithCache(
            String model,
            int maxTokens,
            double temperature,
            String cacheableSystem,
            String dynamicSystem,
            List<Message> history,
            String userMessage,
            String tag,
            Consumer<String> onResult) {

        // Build a structured-content system message with cache_control on the
        // static part. OpenRouter passes this through to Anthropic verbatim.
        List<Map<String, Object>> systemContent = new ArrayList<>();
        Map<String, Object> staticPart = new LinkedHashMap<>();
        staticPart.put("type", "text");
        staticPart.put("text", cacheableSystem == null ? "" : cacheableSystem);
        staticPart.put("cache_control", Map.of("type", "ephemeral"));
        systemContent.add(staticPart);

        if (dynamicSystem != null && !dynamicSystem.isBlank()) {
            Map<String, Object> dynamicPart = new LinkedHashMap<>();
            dynamicPart.put("type", "text");
            dynamicPart.put("text", dynamicSystem);
            systemContent.add(dynamicPart);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));
        appendHistoryAndUser(messages, history, userMessage);
        dispatch(model, maxTokens, temperature, messages, tag, onResult);
    }

    private static void appendHistoryAndUser(
            List<Map<String, Object>> messages,
            List<Message> history,
            String userMessage) {
        for (Message m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
    }

    /** Shared HTTP send + response parsing — used by both chat() and chatWithCache(). */
    private static void dispatch(
            String model,
            int maxTokens,
            double temperature,
            List<Map<String, Object>> messages,
            String tag,
            Consumer<String> onResult) {

        String apiKey = resolveApiKey();
        if (apiKey.isEmpty()) {
            Ironhold.LOGGER.warn("[{}] OpenRouter API key not configured.", tag);
            onResult.accept(null);
            return;
        }
        if (model == null || model.isBlank()) {
            Ironhold.LOGGER.warn("[{}] OpenRouter model not configured.", tag);
            onResult.accept(null);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("HTTP-Referer", "https://kingdomsmp.local/ironhold")
            .header("X-Title", "Ironhold / " + tag)
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                try {
                    if (response.statusCode() / 100 != 2) {
                        Ironhold.LOGGER.warn("[{}] OpenRouter HTTP {}: {}",
                            tag, response.statusCode(), AiLog.snippet(response.body()));
                        onResult.accept(null);
                        return;
                    }
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!root.has("choices") || root.getAsJsonArray("choices").isEmpty()) {
                        Ironhold.LOGGER.warn("[{}] Unexpected OpenRouter response: {}", tag, AiLog.snippet(response.body()));
                        onResult.accept(null);
                        return;
                    }
                    JsonObject msg = root.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message");
                    String content = msg.get("content").getAsString().trim();
                    logCacheUsage(tag, root);
                    onResult.accept(content);
                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[{}] Failed to parse OpenRouter response: {}", tag, e.getMessage());
                    onResult.accept(null);
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[{}] OpenRouter request failed: {}", tag, ex.getMessage());
                onResult.accept(null);
                return null;
            });
    }

    /** Debug-level log of cache write / hit token counts when the provider reports them. */
    private static void logCacheUsage(String tag, JsonObject root) {
        try {
            if (!root.has("usage")) return;
            JsonObject u = root.getAsJsonObject("usage");
            int promptTokens = u.has("prompt_tokens") ? u.get("prompt_tokens").getAsInt() : -1;
            int cacheCreate = u.has("cache_creation_input_tokens")
                ? u.get("cache_creation_input_tokens").getAsInt() : 0;
            int cacheRead = u.has("cache_read_input_tokens")
                ? u.get("cache_read_input_tokens").getAsInt() : 0;
            if (cacheCreate > 0 || cacheRead > 0) {
                Ironhold.LOGGER.info("[{}] OpenRouter usage: prompt={} cache_write={} cache_read={}",
                    tag, promptTokens, cacheCreate, cacheRead);
            }
        } catch (Throwable ignored) { /* usage stats are best-effort */ }
    }
}
