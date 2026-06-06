package kingdom.smp.ai;

import com.google.gson.Gson;
import kingdom.smp.Config;
import kingdom.smp.Ironhold;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ElevenLabs text-to-speech client for Kangarude (and any future voiced NPC).
 *
 * <p>Returns 24 kHz mono PCM-S16LE audio. The SVC bridge upsamples to the
 * 48 kHz mono Opus frames Simple Voice Chat expects. PCM 24 kHz is on the
 * Free tier; pcm_44100 / pcm_48000 require paid tiers but match SVC's rate
 * directly (set {@code OUTPUT_FORMAT} below if you upgrade).
 */
public final class ElevenLabsClient {

    /** PCM-S16LE sample rate of audio returned by speak(). */
    public static final int SAMPLE_RATE_HZ = 24_000;

    private static final String OUTPUT_FORMAT = "pcm_24000";
    private static final String API_BASE = "https://api.elevenlabs.io/v1/text-to-speech/";

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Kangarude-TTS");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .executor(EXECUTOR)
        .build();

    private static final Gson GSON = new Gson();

    private ElevenLabsClient() {}

    public static boolean isConfigured() {
        return !apiKey().isEmpty() && !voiceId().isEmpty();
    }

    /** Env var wins over toml config so secrets can stay out of the repo. */
    private static String apiKey() {
        String env = System.getenv("ELEVENLABS_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        try {
            String cfg = Config.ELEVENLABS_API_KEY.get();
            return cfg == null ? "" : cfg.trim();
        } catch (IllegalStateException e) {
            return "";
        }
    }

    private static String voiceId() {
        String env = System.getenv("ELEVENLABS_VOICE_ID");
        if (env != null && !env.isBlank()) return env.trim();
        try {
            String cfg = Config.ELEVENLABS_VOICE_ID.get();
            return cfg == null ? "" : cfg.trim();
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /** Tuning knobs sent to ElevenLabs. Null fields fall back to the voice's dashboard defaults. */
    public record VoiceSettings(
        Double stability,
        Double similarityBoost,
        Double style,
        Boolean useSpeakerBoost
    ) {
        public static final VoiceSettings DEFAULT = new VoiceSettings(null, null, null, null);

        /** Halric-style preset — slight wobble + theatrical weight for an old wizard cadence. */
        public static final VoiceSettings OLD_WIZARD = new VoiceSettings(0.45, 0.80, 0.35, true);

        /** Stoic, declamatory recitation with theatrical weight — for a records-herald. */
        public static final VoiceSettings BRISK_HERALD = new VoiceSettings(0.70, 0.75, 0.45, true);

        /** Theatrical doom-monger — low stability for wobble/dread, high style for sermonizing
         *  prosody. Pairs with {@code eleven_multilingual_v2} for a plague-doctor cadence. */
        public static final VoiceSettings PLAGUE_DOCTOR = new VoiceSettings(0.35, 0.75, 0.65, true);
    }

    /**
     * Synthesize {@code text} with the global Kangarude voice config — backwards compat.
     */
    public static void speak(String text, Consumer<byte[]> onResult) {
        speak(text, voiceId(), Config.ELEVENLABS_MODEL.get(), VoiceSettings.DEFAULT, onResult);
    }

    /**
     * Synthesize {@code text} with explicit voice + model overrides so each NPC
     * can have its own ElevenLabs voice and TTS model.
     *
     * @param text     line of dialogue to speak (don't include SSML; Flash ignores it)
     * @param voiceId  ElevenLabs voice id — falls back to global config if blank
     * @param model    ElevenLabs model id (eleven_flash_v2_5, eleven_turbo_v2_5, etc.)
     * @param settings per-call voice tuning knobs (use {@link VoiceSettings#DEFAULT} for none)
     * @param onResult invoked on the HTTP executor thread with raw PCM bytes,
     *                 or {@code null} on any failure (logs the cause)
     */
    public static void speak(String text, String voiceId, String model,
                             VoiceSettings settings, Consumer<byte[]> onResult) {
        String apiKey = apiKey();
        if (voiceId == null || voiceId.isBlank()) voiceId = voiceId();
        if (model == null || model.isBlank()) model = Config.ELEVENLABS_MODEL.get();
        if (settings == null) settings = VoiceSettings.DEFAULT;

        if (apiKey.isEmpty() || voiceId.isEmpty()) {
            Ironhold.LOGGER.warn("[ElevenLabs] not configured (API key or voice id missing).");
            onResult.accept(null);
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("model_id", model);

        Map<String, Object> vs = new LinkedHashMap<>();
        if (settings.stability() != null)        vs.put("stability", settings.stability());
        if (settings.similarityBoost() != null)  vs.put("similarity_boost", settings.similarityBoost());
        if (settings.style() != null)            vs.put("style", settings.style());
        if (settings.useSpeakerBoost() != null)  vs.put("use_speaker_boost", settings.useSpeakerBoost());
        if (!vs.isEmpty()) body.put("voice_settings", vs);

        URI uri = URI.create(API_BASE + voiceId + "/stream?output_format=" + OUTPUT_FORMAT);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/json")
            .header("xi-api-key", apiKey)
            .header("Accept", "audio/pcm")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenAccept(response -> {
                if (response.statusCode() / 100 != 2) {
                    Ironhold.LOGGER.warn("[ElevenLabs] HTTP {}: {}",
                        response.statusCode(), AiLog.snippet(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8)));
                    onResult.accept(null);
                    return;
                }
                onResult.accept(response.body());
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[ElevenLabs] request failed: {}", ex.getMessage());
                onResult.accept(null);
                return null;
            });
    }
}
