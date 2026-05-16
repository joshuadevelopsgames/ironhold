package kingdom.smp.ai.svc;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import kingdom.smp.Ironhold;
import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real SVC playback path. Loaded reflectively by {@link kingdom.smp.ai.SvcVoiceBridge}
 * so SVC types stay out of the verifier when the mod isn't installed.
 *
 * <p>Pipeline: ElevenLabs PCM-S16LE @ 24kHz mono → upsample 2× to 48kHz →
 * 20ms (960-sample) frames → Opus encode → SVC EntityAudioChannel.
 *
 * <p>Per-entity queueing keeps back-to-back lines from talking over each
 * other: a single {@link AudioPlayer} drains all queued frames, and we only
 * spin up a new player after the previous one stops.
 */
public final class SvcVoiceBridgeImpl {

    private static final int INPUT_RATE_HZ  = 24_000;
    private static final int OUTPUT_RATE_HZ = 48_000;
    /** SVC requires 20ms frames at 48kHz mono → 960 samples. */
    private static final int FRAME_SAMPLES  = 960;

    /**
     * Stable source UUID for ALL voiced NPC audio. SVC's per-player volume
     * menu keys off this UUID — using one constant means every NPC's voice
     * is controlled by a single slider in the SVC menu, labeled by the
     * virtual player profile we broadcast at server start (see
     * {@link kingdom.smp.ai.NpcVoiceProfile}).
     *
     * <p>Previously we passed {@code UUID.randomUUID()} per playback, which
     * meant SVC never saw a stable speaker identity and the audio fell back
     * to the master voice slider.
     */
    public static final UUID ALL_NPCS_UUID =
        UUID.fromString("a7c0f1ce-6e7c-4b1d-9001-1c1e1d1a1aa1");

    private static final Map<UUID, EntityState> STATES = new ConcurrentHashMap<>();

    private SvcVoiceBridgeImpl() {}

    /** Per-NPC playback state. Synchronized via the {@code lock} object. */
    private static final class EntityState {
        final Object lock = new Object();
        final Deque<short[]> queue = new ArrayDeque<>();
        EntityAudioChannel channel;
        /** Currently-active AudioPlayer if {@code playing}, else null. */
        AudioPlayer activePlayer;
        boolean playing;
    }

    /**
     * Invoked reflectively from {@link kingdom.smp.ai.SvcVoiceBridge#speakAs(Entity, byte[])}.
     * Returns true if the audio was queued for playback.
     */
    public static boolean speakAs(Entity mcEntity, byte[] pcmS16le24kHz) {
        VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
        if (api == null) {
            // SVC is loaded but its server hasn't started yet (or the integrated
            // server isn't running). Don't crash — just drop the clip.
            Ironhold.LOGGER.debug("[Kangarude] SVC server API not ready; dropping {} bytes", pcmS16le24kHz.length);
            return false;
        }

        short[] samples48k = upsample2x(bytesToShortsLE(pcmS16le24kHz));
        if (samples48k.length == 0) return false;

        EntityState state = STATES.computeIfAbsent(mcEntity.getUUID(), id -> new EntityState());

        synchronized (state.lock) {
            // Frame into 960-sample chunks, zero-padding the final partial frame.
            for (int off = 0; off < samples48k.length; off += FRAME_SAMPLES) {
                short[] frame = new short[FRAME_SAMPLES];
                int len = Math.min(FRAME_SAMPLES, samples48k.length - off);
                System.arraycopy(samples48k, off, frame, 0, len);
                state.queue.addLast(frame);
            }

            if (state.channel == null || state.channel.isClosed()) {
                state.channel = api.createEntityAudioChannel(ALL_NPCS_UUID, api.fromEntity(mcEntity));
                if (state.channel == null) {
                    Ironhold.LOGGER.warn("[Kangarude] SVC refused to create audio channel for {}", mcEntity.getUUID());
                    state.queue.clear();
                    return false;
                }
                // Tag the channel so SVC routes its volume through the
                // "Voiced NPCs" slider in the client's volume menu.
                state.channel.setCategory(IronholdVoicechatPlugin.NPC_VOICE_CATEGORY_ID);
            }

            if (!state.playing) {
                startPlayer(api, mcEntity, state);
            }
        }
        return true;
    }

    /** Caller must hold {@code state.lock}. */
    private static void startPlayer(VoicechatServerApi api, Entity mcEntity, EntityState state) {
        if (state.queue.isEmpty()) return;

        OpusEncoder encoder = api.createEncoder();
        // Refresh the channel's entity reference in case the entity moved or
        // was re-created between speeches.
        state.channel.updateEntity(api.fromEntity(mcEntity));

        AudioPlayer player = api.createAudioPlayer(state.channel, encoder, () -> {
            synchronized (state.lock) {
                return state.queue.pollFirst();
            }
        });
        player.setOnStopped(() -> {
            try {
                if (!encoder.isClosed()) encoder.close();
            } catch (Throwable ignored) {}
            synchronized (state.lock) {
                state.playing = false;
                state.activePlayer = null;
                if (!state.queue.isEmpty()) {
                    startPlayer(api, mcEntity, state);
                }
            }
        });
        state.playing = true;
        state.activePlayer = player;
        player.startPlaying();
    }

    /**
     * Drop all per-entity state. Called when the SVC server stops. Crucially,
     * stops every active {@link AudioPlayer} first — SVC spawns an internal
     * thread per player, and a server shutdown that arrives mid-playback can
     * otherwise leave those threads alive and prevent the JVM from exiting
     * (Minecraft server hangs at "Closing FML Loader / Clearing ModLoader").
     */
    public static void shutdown() {
        int stopped = 0;
        for (EntityState state : STATES.values()) {
            synchronized (state.lock) {
                state.queue.clear();
                if (state.activePlayer != null) {
                    try {
                        state.activePlayer.stopPlaying();
                        stopped++;
                    } catch (Throwable t) {
                        Ironhold.LOGGER.debug("[Ironhold] AudioPlayer stop failed: {}", t.toString());
                    }
                    state.activePlayer = null;
                }
                state.playing = false;
                state.channel = null;
            }
        }
        STATES.clear();
        if (stopped > 0) {
            Ironhold.LOGGER.info("[Ironhold] Stopped {} active NPC AudioPlayer(s) on shutdown", stopped);
        }
    }

    private static short[] bytesToShortsLE(byte[] bytes) {
        int n = bytes.length / 2;
        short[] out = new short[n];
        for (int i = 0; i < n; i++) {
            int lo = bytes[i * 2] & 0xff;
            int hi = bytes[i * 2 + 1];
            out[i] = (short) ((hi << 8) | lo);
        }
        return out;
    }

    /**
     * 24kHz → 48kHz via linear interpolation. Cheap and good enough for
     * speech; a sinc filter would be marginally cleaner but isn't worth the
     * extra CPU for NPC dialogue.
     */
    private static short[] upsample2x(short[] in) {
        if (in.length == 0) return in;
        short[] out = new short[in.length * 2];
        for (int i = 0; i < in.length - 1; i++) {
            out[i * 2]     = in[i];
            out[i * 2 + 1] = (short) ((in[i] + in[i + 1]) / 2);
        }
        out[(in.length - 1) * 2]     = in[in.length - 1];
        out[(in.length - 1) * 2 + 1] = in[in.length - 1];
        return out;
    }
}
