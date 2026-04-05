package kingdom.smp.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Async LLM client for Void Invoker combat AI, backed by OpenRouter.
 *
 * <p>Supports a fight-history context so the model can reason about what
 * worked or failed in previous decisions within the same encounter.
 */
public final class OllamaClient {

    // ─── HTTP infrastructure ─────────────────────────────────────────────────

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "VoidInvoker-LLM");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .executor(EXECUTOR)
        .build();

    private static final Gson GSON = new Gson();
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";

    // ─── Prompt ──────────────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
        You are the Void Invoker, an ancient and arrogant arcane boss in a medieval Minecraft RPG server called Kingdom SMP.
        You are in active combat with a player. Each round you choose a combat behavior and deliver a short taunt.

        Taunt style by player class (1-2 sentences, under 120 characters):
        - PEASANT: mock their lack of training and inexperience
        - KNIGHT: mock their heavy armor — your void magic bypasses iron and steel entirely
        - RANGER: mock their arrows — your blink makes you completely uncatchable
        - ROGUE: mock their stealth — you sense them through the void itself
        - WIZARD: acknowledge them as a peer, but assert your absolute superiority
        - CLERIC: mock their holy light — void energy devours and extinguishes it

        Behavior options and when to use them:
        - aggressive: close in hard with nova blasts — use when player HP is low or you are healthy
        - kite: maintain distance, reposition with blink, poke with hex orbs — use when evenly matched
        - flee: sprint away and blink repeatedly — only use when your HP is critically low (<25%)
        - hex: stop moving and spam focused orb barrages — use when player is at medium range and struggling

        LEARNING: You will be given a history of your previous decisions and their outcomes (HP changes).
        Use this to adapt — if a strategy caused you to take heavy damage, switch tactics.
        If a strategy is working (player losing HP, you holding steady), continue or escalate it.

        You MUST respond ONLY with valid JSON and nothing else. No markdown, no explanation:
        {"taunt": "<your taunt here>", "behavior": "<aggressive|kite|flee|hex>"}
        """;

    private OllamaClient() {}

    // ─── Response type ───────────────────────────────────────────────────────

    /**
     * Result from a successful (or fallback) LLM call.
     *
     * @param taunt    Short taunting line to broadcast to nearby players.
     * @param behavior One of: {@code aggressive}, {@code kite}, {@code flee}, {@code hex}.
     */
    public record VoidResponse(String taunt, String behavior) {
        public static VoidResponse fallback() {
            return new VoidResponse("Your fate is already written.", "aggressive");
        }
    }

    /**
     * A single entry in the fight history log — records what decision was made
     * and what the HP outcomes were over the following interval.
     */
    public record HistoryEntry(
            String behavior,
            float myHpBefore, float myHpAfter,
            float playerHpBefore, float playerHpAfter) {

        /** Human-readable summary sent to the LLM as context. */
        public String toPromptLine() {
            float myDelta     = (myHpAfter     - myHpBefore)     * 100f;
            float playerDelta = (playerHpAfter  - playerHpBefore) * 100f;
            return String.format(
                "- Used [%s]: my HP %+.0f%%, player HP %+.0f%%",
                behavior, myDelta, playerDelta);
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public static boolean isConfigured() {
        try {
            String key = Config.OPENROUTER_API_KEY.get();
            return key != null && !key.isBlank();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Fires an async combat-response request to OpenRouter with fight history context.
     *
     * @param playerClass   The player's Kingdom SMP class ID.
     * @param playerHpPct   Player HP as a 0–1 fraction.
     * @param invokerHpPct  Void Invoker HP as a 0–1 fraction.
     * @param distance      Block distance to the target.
     * @param history       Recent fight decisions and their outcomes (oldest first).
     * @param onResult      Called on the HTTP executor thread when a response is ready.
     */
    public static void requestCombatResponse(
            String playerClass,
            float playerHpPct,
            float invokerHpPct,
            double distance,
            List<HistoryEntry> history,
            Consumer<VoidResponse> onResult) {

        String apiKey = Config.OPENROUTER_API_KEY.get();
        String model  = Config.OPENROUTER_MODEL.get();

        if (apiKey == null || apiKey.isBlank()) {
            Ironhold.LOGGER.warn("[VoidInvoker] OpenRouter API key is not set — skipping LLM call.");
            onResult.accept(VoidResponse.fallback());
            return;
        }

        // Build user message — current state + history
        StringBuilder userMsg = new StringBuilder();
        userMsg.append(String.format(
            "Current state — Player class: %s | Player HP: %.0f%% | My HP: %.0f%% | Distance: %.1f blocks.",
            playerClass, playerHpPct * 100, invokerHpPct * 100, distance));

        if (!history.isEmpty()) {
            userMsg.append("\n\nFight history (most recent last):");
            for (HistoryEntry entry : history) {
                userMsg.append("\n").append(entry.toPromptLine());
            }
            userMsg.append("\n\nBased on this history, choose your next behavior wisely.");
        }

        Map<String, Object> payload = Map.of(
            "model",    model,
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user",   "content", userMsg.toString())
            ),
            "stream", false
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type",  "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("X-Title",       "Kingdom SMP - Void Invoker")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                try {
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

                    if (!root.has("choices")) {
                        Ironhold.LOGGER.warn("[VoidInvoker] Unexpected OpenRouter response: {}", response.body());
                        onResult.accept(VoidResponse.fallback());
                        return;
                    }

                    JsonArray choices = root.getAsJsonArray("choices");
                    String content = choices
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString()
                        .trim();

                    if (content.startsWith("```")) {
                        content = content.replaceAll("(?s)```[a-z]*\\s*", "").trim();
                    }

                    JsonObject parsed  = JsonParser.parseString(content).getAsJsonObject();
                    String taunt    = parsed.has("taunt")    ? parsed.get("taunt").getAsString()    : "You cannot stop me.";
                    String behavior = parsed.has("behavior") ? parsed.get("behavior").getAsString() : "aggressive";

                    Ironhold.LOGGER.info("[VoidInvoker] Decision: [{}] — \"{}\"", behavior.toUpperCase(), taunt);
                    onResult.accept(new VoidResponse(taunt, behavior));

                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[VoidInvoker] Failed to parse LLM response: {}", e.getMessage());
                    onResult.accept(VoidResponse.fallback());
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[VoidInvoker] LLM request failed: {}", ex.getMessage());
                onResult.accept(VoidResponse.fallback());
                return null;
            });
    }

    /**
     * Generic async request that returns the raw LLM response text via the callback.
     * Used by mobs that have their own JSON contract to parse (e.g. FilcherKing).
     *
     * @param systemPrompt The full system prompt.
     * @param userMessage  The current-state message.
     * @param onResult     Called on the HTTP executor thread with the raw response string,
     *                     or null if the call failed.
     */
    public static void requestRawText(
            String systemPrompt,
            String userMessage,
            String xTitle,
            Consumer<String> onResult) {

        String apiKey = Config.OPENROUTER_API_KEY.get();
        String model  = Config.OPENROUTER_MODEL.get();

        if (apiKey == null || apiKey.isBlank()) {
            onResult.accept(null);
            return;
        }

        Map<String, Object> payload = Map.of(
            "model",    model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userMessage)
            ),
            "stream", false
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type",  "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("X-Title",       xTitle)
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                try {
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!root.has("choices")) {
                        Ironhold.LOGGER.warn("[LLM] Unexpected response body: {}", response.body());
                        onResult.accept(null);
                        return;
                    }
                    String content = root.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
                    onResult.accept(content);
                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[LLM] Failed to extract content: {}", e.getMessage());
                    onResult.accept(null);
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[LLM] Request failed: {}", ex.getMessage());
                onResult.accept(null);
                return null;
            });
    }

    /**
     * Generic async request — accepts a custom system prompt and raw user message.
     * Used by mobs with their own personality/behavior set (e.g. Null Stalker).
     *
     * @param systemPrompt The full system prompt defining personality and JSON contract.
     * @param userMessage  The current-state message to send.
     * @param onResult     Called on the HTTP executor thread when a response is ready.
     */
    public static void requestRaw(
            String systemPrompt,
            String userMessage,
            Consumer<VoidResponse> onResult) {

        String apiKey = Config.OPENROUTER_API_KEY.get();
        String model  = Config.OPENROUTER_MODEL.get();

        if (apiKey == null || apiKey.isBlank()) {
            onResult.accept(VoidResponse.fallback());
            return;
        }

        Map<String, Object> payload = Map.of(
            "model",    model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userMessage)
            ),
            "stream", false
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type",  "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .header("X-Title",       "Kingdom SMP - Null Stalker")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                try {
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (!root.has("choices")) {
                        Ironhold.LOGGER.warn("[NullStalker] Unexpected response: {}", response.body());
                        onResult.accept(VoidResponse.fallback());
                        return;
                    }
                    JsonArray choices = root.getAsJsonArray("choices");
                    String content = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message").get("content").getAsString().trim();
                    if (content.startsWith("```")) {
                        content = content.replaceAll("(?s)```[a-z]*\\s*", "").trim();
                    }
                    JsonObject parsed  = JsonParser.parseString(content).getAsJsonObject();
                    String taunt    = parsed.has("taunt")    ? parsed.get("taunt").getAsString()    : "...";
                    String behavior = parsed.has("behavior") ? parsed.get("behavior").getAsString() : "stalk";
                    Ironhold.LOGGER.info("[NullStalker] Decision: [{}] — \"{}\"", behavior.toUpperCase(), taunt);
                    onResult.accept(new VoidResponse(taunt, behavior));
                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[NullStalker] Failed to parse LLM response: {}", e.getMessage());
                    onResult.accept(VoidResponse.fallback());
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[NullStalker] LLM request failed: {}", ex.getMessage());
                onResult.accept(VoidResponse.fallback());
                return null;
            });
    }
}
