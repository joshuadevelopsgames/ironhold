package kingdom.smp.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Config;
import kingdom.smp.Ironhold;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Async speech-to-text via OpenAI's Whisper transcription API.
 *
 * <p>Pipeline: PCM-S16LE @ 48 kHz mono → wrap in WAV → multipart POST to
 * /v1/audio/transcriptions → reply to {@code onResult} with the transcribed
 * text (or {@code null} on failure).
 */
public final class OpenAiWhisperClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";
    private static final int SAMPLE_RATE_HZ = 48_000;

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Kangarude-STT");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .executor(EXECUTOR)
        .build();

    private OpenAiWhisperClient() {}

    public static boolean isConfigured() {
        return !apiKey().isEmpty();
    }

    private static String apiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        try {
            String cfg = Config.OPENAI_API_KEY.get();
            return cfg == null ? "" : cfg.trim();
        } catch (IllegalStateException e) {
            return "";
        }
    }

    /**
     * Transcribe an utterance. {@code pcmS16le48kHz} is mono PCM at the SVC
     * native rate (48 kHz). Spoken text comes back via {@code onResult} on the
     * HTTP executor thread.
     */
    public static void transcribe(short[] pcmS16le48kHz, Consumer<String> onResult) {
        String key = apiKey();
        if (key.isEmpty()) {
            Ironhold.LOGGER.warn("[Kangarude] OpenAI API key not set — STT disabled.");
            onResult.accept(null);
            return;
        }
        if (pcmS16le48kHz.length < SAMPLE_RATE_HZ / 4) {
            // <250 ms — not worth a billed STT call. Probably noise.
            onResult.accept(null);
            return;
        }

        byte[] wav = encodeWav(pcmS16le48kHz);
        String boundary = "----IronholdBoundary" + UUID.randomUUID();
        byte[] body = buildMultipart(boundary, wav, Config.OPENAI_WHISPER_MODEL.get());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Authorization", "Bearer " + key)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() / 100 != 2) {
                    Ironhold.LOGGER.warn("[Kangarude] Whisper HTTP {}: {}",
                        response.statusCode(), AiLog.snippet(response.body()));
                    onResult.accept(null);
                    return;
                }
                try {
                    JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                    String text = root.has("text") ? root.get("text").getAsString().trim() : "";
                    onResult.accept(text.isEmpty() ? null : text);
                } catch (Exception e) {
                    Ironhold.LOGGER.warn("[Kangarude] Failed to parse Whisper response: {}", e.getMessage());
                    onResult.accept(null);
                }
            })
            .exceptionally(ex -> {
                Ironhold.LOGGER.warn("[Kangarude] Whisper request failed: {}", ex.getMessage());
                onResult.accept(null);
                return null;
            });
    }

    // ── WAV + multipart helpers ──────────────────────────────────────────────

    private static byte[] encodeWav(short[] samples) {
        int numBytes = samples.length * 2;
        ByteBuffer buf = ByteBuffer.allocate(44 + numBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
        buf.putInt(36 + numBytes);
        buf.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');
        buf.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
        buf.putInt(16);                // fmt chunk size
        buf.putShort((short) 1);       // PCM
        buf.putShort((short) 1);       // mono
        buf.putInt(SAMPLE_RATE_HZ);
        buf.putInt(SAMPLE_RATE_HZ * 2); // byte rate (16-bit mono)
        buf.putShort((short) 2);       // block align
        buf.putShort((short) 16);      // bits per sample
        buf.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
        buf.putInt(numBytes);
        for (short s : samples) buf.putShort(s);
        return buf.array();
    }

    private static byte[] buildMultipart(String boundary, byte[] wav, String model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(wav.length + 512);
        try {
            // file part
            out.write(("--" + boundary + "\r\n").getBytes());
            out.write("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n".getBytes());
            out.write("Content-Type: audio/wav\r\n\r\n".getBytes());
            out.write(wav);
            out.write("\r\n".getBytes());
            // model part
            out.write(("--" + boundary + "\r\n").getBytes());
            out.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n".getBytes());
            out.write(model.getBytes());
            out.write("\r\n".getBytes());
            // closing boundary
            out.write(("--" + boundary + "--\r\n").getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }
}
