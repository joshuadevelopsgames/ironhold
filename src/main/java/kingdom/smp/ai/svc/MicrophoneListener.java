package kingdom.smp.ai.svc;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import kingdom.smp.Ironhold;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.ai.OpenAiWhisperClient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffers Simple Voice Chat microphone audio per active NPC conversation
 * during a push-to-talk window, then ships the buffered PCM to
 * {@link OpenAiWhisperClient}. The transcript is fed back into the partner
 * NPC's conversation pipeline via {@link NpcChatPartner#onPartnerChat}.
 *
 * <p>PTT model: the player taps the configured key (default K) once to start
 * recording, taps again to stop and submit. While recording is OFF, all
 * incoming Opus frames for that player are dropped — there is no
 * silence-detection fallback, by design.
 */
public final class MicrophoneListener {

    /** SVC pumps audio at 48 kHz mono; one Opus frame = 20 ms = 960 samples. */
    private static final int SAMPLE_RATE_HZ = 48_000;
    /** Hard cap each utterance at 30 s of audio so a stuck mic can't memory-leak us. */
    private static final int MAX_SAMPLES = SAMPLE_RATE_HZ * 30;

    private static final Map<UUID, BufferState> STATES = new ConcurrentHashMap<>();
    /** Player UUIDs currently in a push-to-talk recording window. */
    private static final Set<UUID> RECORDING = ConcurrentHashMap.newKeySet();

    private MicrophoneListener() {}

    /** Per-player rolling buffer + decoder. Synchronized via {@code lock}. */
    private static final class BufferState {
        final Object lock = new Object();
        final OpusDecoder decoder;
        short[] samples = new short[0];
        boolean transcribing;

        BufferState(OpusDecoder decoder) {
            this.decoder = decoder;
        }
    }

    /** Called from {@link IronholdVoicechatPlugin#registerEvents}. */
    public static void onMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
        if (api == null) return;

        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;

        // Map SVC's ServerPlayer back to the vanilla one to look up the partner.
        Object rawPlayer = sender.getPlayer().getPlayer();
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();

        NpcChatPartner npc = NpcChatRegistry.getActive(playerId);
        if (npc == null) return; // Player isn't in any NPC conversation — ignore.

        // Push-to-talk: only buffer while the player has explicitly toggled
        // recording on. No silence detection — the player decides when the
        // utterance starts and ends.
        if (!RECORDING.contains(playerId)) return;

        // Drop incoming audio while Kangarude is speaking through the player's
        // speakers — otherwise the open mic picks up his voice, Whisper
        // transcribes it, and we get a feedback loop.
        if (MicGate.isMuted(playerId)) {
            BufferState s = STATES.get(playerId);
            if (s != null) {
                synchronized (s.lock) {
                    s.samples = new short[0];
                }
            }
            return;
        }

        byte[] opus = event.getPacket().getOpusEncodedData();
        BufferState state = STATES.computeIfAbsent(playerId,
            id -> new BufferState(api.createDecoder()));

        synchronized (state.lock) {
            if (opus == null || opus.length == 0) return;
            short[] frame;
            try {
                frame = state.decoder.decode(opus);
            } catch (Throwable t) {
                Ironhold.LOGGER.debug("[Kangarude] Opus decode failed: {}", t.toString());
                return;
            }
            if (frame == null || frame.length == 0) return;

            int newLen = state.samples.length + frame.length;
            if (newLen > MAX_SAMPLES) {
                // Cap reached — flush whatever we have so the utterance isn't lost.
                flushLocked(state, player, npc);
                newLen = frame.length;
            }
            short[] grown = new short[newLen];
            System.arraycopy(state.samples, 0, grown, 0, state.samples.length);
            System.arraycopy(frame, 0, grown, state.samples.length, frame.length);
            state.samples = grown;
        }
    }

    // ── Push-to-talk control ────────────────────────────────────────────────

    /**
     * Toggle the player's PTT recording state. Tap-on starts a fresh
     * recording; tap-off flushes the buffered audio to Whisper. Only valid
     * when the player has an active NPC conversation partner — otherwise the
     * toggle is ignored with a hint message.
     */
    public static void togglePtt(ServerPlayer player) {
        UUID playerId = player.getUUID();
        NpcChatPartner npc = NpcChatRegistry.getActive(playerId);
        if (npc == null) {
            player.sendSystemMessage(Component.literal(
                "§7Walk up to an NPC and right-click first, then press to talk."));
            return;
        }

        if (RECORDING.contains(playerId)) {
            // Stop recording and flush.
            RECORDING.remove(playerId);
            BufferState state = STATES.get(playerId);
            if (state != null) {
                synchronized (state.lock) {
                    flushLocked(state, player, npc);
                }
            }
            player.sendSystemMessage(Component.literal("§7🎤 Sent."));
        } else {
            // Start recording — clear any stale buffer.
            BufferState state = STATES.computeIfAbsent(playerId, id -> {
                VoicechatServerApi api = IronholdVoicechatPlugin.getApi();
                return new BufferState(api == null ? null : api.createDecoder());
            });
            synchronized (state.lock) {
                state.samples = new short[0];
            }
            RECORDING.add(playerId);
            player.sendSystemMessage(Component.literal("§e🎤 Recording…"));
        }
    }

    /**
     * Drop any PTT state for the player. Called when their NPC conversation
     * ends so the toggle can't leak across conversations.
     */
    public static void clearForPlayer(UUID playerId) {
        RECORDING.remove(playerId);
        BufferState state = STATES.remove(playerId);
        if (state != null) {
            synchronized (state.lock) {
                if (state.decoder != null) {
                    try { state.decoder.close(); } catch (Throwable ignored) {}
                }
                state.samples = new short[0];
            }
        }
    }

    /** Caller must hold {@code state.lock}. */
    private static void flushLocked(BufferState state, ServerPlayer player, NpcChatPartner npc) {
        if (state.samples.length == 0 || state.transcribing) return;
        short[] toSend = state.samples;
        state.samples = new short[0];
        state.transcribing = true;
        long bufferedMs = (toSend.length * 1000L) / SAMPLE_RATE_HZ;
        Ironhold.LOGGER.debug("[{}] Flushing {} ms of {} -> Whisper",
            npc.tag(), bufferedMs, player.getName().getString());

        OpenAiWhisperClient.transcribe(toSend, transcript -> {
            synchronized (state.lock) {
                state.transcribing = false;
            }
            if (transcript == null || transcript.isBlank()) return;
            Ironhold.LOGGER.info("[{}] {} said (mic): \"{}\"",
                npc.tag(), player.getName().getString(), transcript);
            // Hop back to the server thread before mutating entity conversation state.
            var server = npc.level().getServer();
            if (server == null) return;
            server.execute(() -> {
                if (NpcChatRegistry.getActive(player.getUUID()) == npc) {
                    npc.onPartnerChat(player, transcript);
                }
            });
        });
    }
}
