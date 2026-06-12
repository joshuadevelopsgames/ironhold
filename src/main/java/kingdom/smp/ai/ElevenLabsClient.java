package kingdom.smp.ai;

import com.google.gson.Gson;
import kingdom.smp.Config;
import kingdom.smp.Ironhold;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
     * can have its own ElevenLabs voice and TTS model. Buffers the whole clip —
     * prefer {@link #speakStream} for anything played to a waiting player.
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
        ByteArrayOutputStream buf = new ByteArrayOutputStream(64 * 1024);
        speakStream(text, voiceId, model, settings, null, new AudioStreamObserver() {
            @Override public void onChunk(byte[] pcmChunk) {
                buf.writeBytes(pcmChunk);
            }
            @Override public void onComplete(boolean success) {
                onResult.accept(success && buf.size() > 0 ? buf.toByteArray() : null);
            }
        });
    }

    /** Receives PCM audio incrementally as ElevenLabs synthesizes it. */
    public interface AudioStreamObserver {
        /** Raw PCM-S16LE @ 24 kHz chunk, called on the HTTP executor thread. */
        void onChunk(byte[] pcmChunk);
        /** Always called exactly once after the last chunk (or on failure). */
        void onComplete(boolean success);
    }

    /**
     * Streaming synthesis: PCM chunks are delivered to {@code observer} as they
     * arrive from ElevenLabs instead of waiting for the full clip. With Flash
     * v2.5 the first chunk typically lands in ~150–300 ms, so playback can start
     * almost immediately.
     *
     * @param previousText text of the line spoken just before this one (same
     *                     reply), or null — lets ElevenLabs keep prosody
     *                     continuous across sentence-chunked synthesis
     */
    public static void speakStream(String text, String voiceId, String model,
                                   VoiceSettings settings, String previousText,
                                   AudioStreamObserver observer) {
        String apiKey = apiKey();
        if (voiceId == null || voiceId.isBlank()) voiceId = voiceId();
        if (model == null || model.isBlank()) model = Config.ELEVENLABS_MODEL.get();
        if (settings == null) settings = VoiceSettings.DEFAULT;

        if (apiKey.isEmpty() || voiceId.isEmpty()) {
            Ironhold.LOGGER.warn("[ElevenLabs] not configured (API key or voice id missing).");
            observer.onComplete(false);
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("model_id", model);
        if (previousText != null && !previousText.isBlank()) {
            body.put("previous_text", previousText);
        }

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

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenAccept(response -> {
                // We're on a cached-pool executor thread; blocking reads are fine.
                try (InputStream in = response.body()) {
                    if (response.statusCode() / 100 != 2) {
                        Ironhold.LOGGER.warn("[ElevenLabs] HTTP {}: {}",
                            response.statusCode(),
                            AiLog.snippet(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                        observer.onComplete(false);
                        return;
                    }
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        if (n > 0) {
                            byte[] chunk = new byte[n];
                            System.arraycopy(buf, 0, chunk, 0, n);
                            observer.onChunk(chunk);
                        }
                    }
                    observer.onComplete(true);
                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[ElevenLabs] stream read failed: {}", e.getMessage());
                    observer.onComplete(false);
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[ElevenLabs] request failed: {}", ex.getMessage());
                observer.onComplete(false);
                return null;
            });
    }

    // ── Connection pre-warming ───────────────────────────────────────────────

    private static volatile long lastPrewarmMillis;

    /**
     * Fire a tiny request through the shared {@link HttpClient} so the TLS
     * handshake is already done when the first real TTS call goes out. Throttled
     * to once a minute; result is ignored.
     */
    public static void prewarm() {
        if (!isConfigured()) return;
        long now = System.currentTimeMillis();
        if (now - lastPrewarmMillis < 60_000L) return;
        lastPrewarmMillis = now;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.elevenlabs.io/v1/user"))
            .header("xi-api-key", apiKey())
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .exceptionally(ex -> null);
    }
}
