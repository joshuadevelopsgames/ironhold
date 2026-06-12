package kingdom.smp.ai.svc;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import kingdom.smp.Ironhold;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcMuteRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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
    /** One output frame consumes 480 input samples (24kHz) = 960 bytes of PCM-S16LE. */
    private static final int FRAME_INPUT_BYTES = 960;
    /**
     * Don't spin up an AudioPlayer for a still-open stream until this much
     * audio is queued (10 frames = 200ms). Cushions against an instant
     * underrun-stop-restart cycle when the first network chunk is tiny.
     */
    private static final int MIN_START_FRAMES = 10;

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

    /**
     * Speaking-entity UUID → NPC mute tag (e.g. {@code "Halric"}). Populated when an NPC speaks,
     * and read by {@link #onEntitySoundPacket} so a per-receiver mute can be enforced: every NPC
     * shares {@link #ALL_NPCS_UUID}, so the entity UUID on the packet is what tells us which NPC
     * is talking.
     */
    private static final Map<UUID, String> ENTITY_TAGS = new ConcurrentHashMap<>();

    private SvcVoiceBridgeImpl() {}

    /** Per-NPC playback state. Synchronized via the {@code lock} object. */
    private static final class EntityState {
        final Object lock = new Object();
        final Deque<short[]> queue = new ArrayDeque<>();
        EntityAudioChannel channel;
        /** Currently-active AudioPlayer if {@code playing}, else null. */
        AudioPlayer activePlayer;
        boolean playing;
        /** Streams begun via {@link #beginStream} but not yet ended. */
        int openStreams;
    }

    /**
     * Handle for one incremental playback stream, returned by {@link #beginStream}
     * and passed back (as Object, through the reflective facade) to
     * {@link #feedStream}/{@link #endStream}. Carries the sub-frame byte
     * remainder between chunks so frames are only zero-padded once, at
     * end-of-stream — padding every chunk would inject audible 20ms gaps.
     */
    public static final class StreamSession {
        private final Entity entity;
        private final EntityState state;
        private byte[] carry = new byte[0];
        private boolean ended;

        private StreamSession(Entity entity, EntityState state) {
            this.entity = entity;
            this.state = state;
        }
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

        // Remember which NPC this entity is, so per-receiver mute can be applied on delivery.
        if (mcEntity instanceof NpcChatPartner partner) {
            ENTITY_TAGS.put(mcEntity.getUUID(), partner.tag());
        }

        EntityState state = STATES.computeIfAbsent(mcEntity.getUUID(), id -> new EntityState());

        synchronized (state.lock) {
            // Frame into 960-sample chunks, zero-padding the final partial frame.
            for (int off = 0; off < samples48k.length; off += FRAME_SAMPLES) {
                short[] frame = new short[FRAME_SAMPLES];
                int len = Math.min(FRAME_SAMPLES, samples48k.length - off);
                System.arraycopy(samples48k, off, frame, 0, len);
                state.queue.addLast(frame);
            }

            if (!ensureChannelLocked(api, mcEntity, state)) {
                state.queue.clear();
                return false;
            }

            if (!state.playing) {
                startPlayer(api, mcEntity, state);
            }
        }
        return true;
    }

    /** Caller must hold {@code state.lock}. Creates/refreshes the SVC channel. */
    private static boolean ensureChannelLocked(VoicechatServerApi api, Entity mcEntity, EntityState state) {
        if (state.channel != null && !state.channel.isClosed()) return true;
        state.channel = api.createEntityAudioChannel(ALL_NPCS_UUID, api.fromEntity(mcEntity));
        if (state.channel == null) {
            Ironhold.LOGGER.warn("[Kangarude] SVC refused to create audio channel for {}", mcEntity.getUUID());
            return false;
        }
        // Tag the channel so SVC routes its volume through the
        // "Voiced NPCs" slider in the client's volume menu.
        state.channel.setCategory(IronholdVoicechatPlugin.NPC_VOICE_CATEGORY_ID);
        return true;
    }

    // ── Incremental streaming playback ───────────────────────────────────────

    /**
     * Open an incremental playback stream for the entity. Audio fed via
     * {@link #feedStream} starts playing as soon as a small prebuffer fills,
     * instead of waiting for the whole clip. Returns null when SVC isn't ready.
     */
    public static Object beginStream(Entity mcEntity) {
        VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
        if (api == null) {
            Ironhold.LOGGER.debug("[Kangarude] SVC server API not ready; stream refused");
            return null;
        }
        if (mcEntity instanceof NpcChatPartner partner) {
            ENTITY_TAGS.put(mcEntity.getUUID(), partner.tag());
        }
        EntityState state = STATES.computeIfAbsent(mcEntity.getUUID(), id -> new EntityState());
        synchronized (state.lock) {
            state.openStreams++;
        }
        return new StreamSession(mcEntity, state);
    }

    /** Feed a PCM-S16LE @ 24kHz chunk into an open stream. Any chunk size is fine. */
    public static boolean feedStream(Object sessionObj, byte[] pcmChunk) {
        if (!(sessionObj instanceof StreamSession session)) return false;
        if (pcmChunk == null || pcmChunk.length == 0) return true;
        VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
        if (api == null) return false;

        EntityState state = session.state;
        synchronized (state.lock) {
            if (session.ended) return false;

            byte[] merged = new byte[session.carry.length + pcmChunk.length];
            System.arraycopy(session.carry, 0, merged, 0, session.carry.length);
            System.arraycopy(pcmChunk, 0, merged, session.carry.length, pcmChunk.length);

            int usable = (merged.length / FRAME_INPUT_BYTES) * FRAME_INPUT_BYTES;
            session.carry = new byte[merged.length - usable];
            System.arraycopy(merged, usable, session.carry, 0, session.carry.length);
            if (usable == 0) return true;

            if (!ensureChannelLocked(api, session.entity, state)) return false;
            enqueueFramesLocked(state, merged, usable);
            maybeStartPlayerLocked(api, session.entity, state);
        }
        return true;
    }

    /** Close a stream: pad + flush the sub-frame remainder and let playback drain. */
    public static void endStream(Object sessionObj) {
        if (!(sessionObj instanceof StreamSession session)) return;
        EntityState state = session.state;
        synchronized (state.lock) {
            if (session.ended) return;
            session.ended = true;
            state.openStreams = Math.max(0, state.openStreams - 1);

            VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
            if (api == null) return;
            if (session.carry.length > 0) {
                byte[] padded = new byte[FRAME_INPUT_BYTES];
                System.arraycopy(session.carry, 0, padded, 0, session.carry.length);
                session.carry = new byte[0];
                if (ensureChannelLocked(api, session.entity, state)) {
                    enqueueFramesLocked(state, padded, FRAME_INPUT_BYTES);
                }
            }
            maybeStartPlayerLocked(api, session.entity, state);
        }
    }

    /**
     * Caller must hold {@code state.lock}. Converts {@code usable} bytes of
     * 24kHz PCM (a multiple of {@link #FRAME_INPUT_BYTES}) into 48kHz frames
     * on the queue.
     */
    private static void enqueueFramesLocked(EntityState state, byte[] pcm, int usable) {
        for (int off = 0; off < usable; off += FRAME_INPUT_BYTES) {
            byte[] slice = new byte[FRAME_INPUT_BYTES];
            System.arraycopy(pcm, off, slice, 0, FRAME_INPUT_BYTES);
            state.queue.addLast(upsample2x(bytesToShortsLE(slice)));
        }
    }

    /**
     * Caller must hold {@code state.lock}. Starts a player once enough audio is
     * buffered — or immediately if no stream is still open (nothing more is
     * coming, so waiting would only add latency).
     */
    private static void maybeStartPlayerLocked(VoicechatServerApi api, Entity mcEntity, EntityState state) {
        if (state.playing || state.queue.isEmpty()) return;
        if (state.openStreams > 0 && state.queue.size() < MIN_START_FRAMES) return;
        startPlayer(api, mcEntity, state);
    }

    /**
     * Per-receiver mute enforcement. SVC fires this for every player about to receive an NPC's
     * entity audio; we cancel delivery to any listener who has muted that specific NPC. Because
     * the cancel is per-receiver, other nearby players still hear the line normally — unlike the
     * old server-side approach that dropped the whole line based only on the speaking partner.
     */
    public static void onEntitySoundPacket(EntitySoundPacketEvent event) {
        if (!event.isCancellable()) return;
        EntitySoundPacket packet = event.getPacket();
        // Only our voiced-NPC audio carries this category; ignore real player voice.
        if (!IronholdVoicechatPlugin.NPC_VOICE_CATEGORY_ID.equals(packet.getCategory())) return;

        String tag = ENTITY_TAGS.get(packet.getEntityUuid());
        if (tag == null) return;

        VoicechatConnection receiver = event.getReceiverConnection();
        if (receiver == null || receiver.getPlayer() == null) return;
        UUID listenerId = receiver.getPlayer().getUuid();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        if (NpcMuteRegistry.get(server.overworld()).isMuted(listenerId, tag)) {
            event.cancel();
        }
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
        ENTITY_TAGS.clear();
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
