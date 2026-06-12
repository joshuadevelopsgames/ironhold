package kingdom.smp.ai;

import net.minecraft.world.entity.Entity;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared streamed-TTS playback path for every voiced NPC: ElevenLabs chunks
 * flow straight into a Simple Voice Chat stream as they arrive, so the NPC
 * starts talking ~200ms after synthesis begins instead of after the whole
 * clip is rendered.
 *
 * <p>A {@link Session} additionally lets a reply be spoken sentence-by-sentence
 * while the LLM is still generating: each {@link Session#speakSentence} fires
 * its ElevenLabs request immediately (synthesis runs in parallel), but audio is
 * fed to SVC strictly in submission order — later sentences buffer in memory
 * until every earlier one has fully arrived.
 *
 * <p>The session also maintains the partner's {@link MicGate} window
 * incrementally: as audio is fed, the mute extends to cover the projected
 * playback time plus the usual 2s echo grace period.
 */
public final class NpcSpeech {

    /** PCM-S16LE @ 24 kHz mono = 48 bytes per millisecond of audio. */
    private static final long BYTES_PER_MS = 48L;
    /**
     * How long the partner's mic stays gated past the end of playback, covering
     * the acoustic echo path (SVC queue + speaker latency + room). Was 2s; cut
     * to 1s because the gate wipes any in-progress PTT recording, so every
     * extra ms here eats the start of an eager player's next utterance.
     */
    private static final long ECHO_GRACE_MS = 1_000L;

    private NpcSpeech() {}

    /**
     * Speak a single complete line through the streaming pipeline. Drop-in
     * replacement for the old buffered ElevenLabs → speakAs path.
     *
     * @param npc           the speaking entity (SVC positions audio at it)
     * @param partnerToMute player whose mic gets suppressed during playback, or null
     * @param voiceId/model null falls back to the global Kangarude voice config
     */
    public static void speak(Entity npc, UUID partnerToMute, String line,
                             String voiceId, String model,
                             ElevenLabsClient.VoiceSettings settings) {
        Session session = beginSession(npc, partnerToMute);
        session.speakSentence(line, voiceId, model, settings);
        session.finish();
    }

    /** Start an ordered multi-sentence speech session for one NPC reply. */
    public static Session beginSession(Entity npc, UUID partnerToMute) {
        return new Session(npc, partnerToMute);
    }

    /** One NPC reply's worth of ordered, streamed sentences. */
    public static final class Session {
        private final Entity npc;
        private final UUID partnerToMute;
        private final Object lock = new Object();

        private int nextIndex = 0;          // next sentence index to hand out
        private int playCursor = 0;         // sentence currently allowed to feed SVC
        private final Map<Integer, ByteArrayOutputStream> buffered = new HashMap<>();
        private final Set<Integer> completed = new HashSet<>();
        private boolean finished;           // finish() called — no more sentences
        private boolean streamEnded;
        private String lastSentence;        // previous_text for prosody continuity

        private Object bridgeStream;        // SVC handle, created on first audio
        private boolean bridgeTried;
        private long firstFeedMillis;
        private long totalFedBytes;

        private Session(Entity npc, UUID partnerToMute) {
            this.npc = npc;
            this.partnerToMute = partnerToMute;
        }

        /**
         * Queue a sentence. Synthesis starts immediately and in parallel with
         * any earlier sentences; playback stays in call order.
         */
        public void speakSentence(String text, String voiceId, String model,
                                  ElevenLabsClient.VoiceSettings settings) {
            if (text == null || text.isBlank()) return;
            int index;
            String previous;
            synchronized (lock) {
                if (finished) return;
                index = nextIndex++;
                previous = lastSentence;
                lastSentence = text;
            }
            ElevenLabsClient.speakStream(text, voiceId, model, settings, previous,
                new ElevenLabsClient.AudioStreamObserver() {
                    @Override public void onChunk(byte[] pcmChunk) {
                        onAudio(index, pcmChunk);
                    }
                    @Override public void onComplete(boolean success) {
                        onSentenceDone(index);
                    }
                });
        }

        /**
         * No more sentences are coming. The SVC stream closes once every
         * queued sentence has fully arrived; buffered audio drains normally.
         */
        public void finish() {
            synchronized (lock) {
                finished = true;
                maybeEndLocked();
            }
        }

        private void onAudio(int index, byte[] chunk) {
            synchronized (lock) {
                if (index == playCursor) {
                    feedLocked(chunk);
                } else {
                    buffered.computeIfAbsent(index, i -> new ByteArrayOutputStream(32 * 1024))
                        .writeBytes(chunk);
                }
            }
        }

        private void onSentenceDone(int index) {
            synchronized (lock) {
                completed.add(index);
                while (completed.contains(playCursor)) {
                    completed.remove(playCursor);
                    playCursor++;
                    ByteArrayOutputStream pending = buffered.remove(playCursor);
                    if (pending != null && pending.size() > 0) {
                        feedLocked(pending.toByteArray());
                    }
                }
                maybeEndLocked();
            }
        }

        /** Caller must hold {@code lock}. */
        private void feedLocked(byte[] chunk) {
            if (streamEnded) return;
            if (!bridgeTried) {
                bridgeTried = true;
                bridgeStream = SvcVoiceBridge.beginStreamAs(npc);
                firstFeedMillis = System.currentTimeMillis();
            }
            if (bridgeStream == null) return; // SVC absent — text-only NPC
            SvcVoiceBridge.feedStream(bridgeStream, chunk);
            totalFedBytes += chunk.length;
            if (partnerToMute != null) {
                // Playback started roughly at firstFeedMillis; keep the mic
                // gated until the audio fed so far finishes playing + grace.
                long playbackEnd = firstFeedMillis + totalFedBytes / BYTES_PER_MS;
                long remaining = playbackEnd - System.currentTimeMillis();
                MicGate.muteFor(partnerToMute, remaining + ECHO_GRACE_MS);
            }
        }

        /** Caller must hold {@code lock}. */
        private void maybeEndLocked() {
            if (!finished || streamEnded) return;
            if (playCursor < nextIndex) return; // sentences still in flight
            streamEnded = true;
            if (bridgeStream != null) {
                SvcVoiceBridge.endStream(bridgeStream);
            }
        }
    }
}
