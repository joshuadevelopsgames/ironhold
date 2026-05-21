package kingdom.smp.ai;

import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Anything a player can hold a voiced AI conversation with — Kangarude, Warden
 * Halric, and any future voiced NPC. Used by the shared PTT/STT pipeline so the
 * microphone listener doesn't need to know about each entity type.
 */
public interface NpcChatPartner {

    /** UUID of the player currently in conversation, or null if none. */
    UUID getPartnerId();

    /** Forward a player utterance (typed or transcribed) into this NPC's conversation. */
    void onPartnerChat(ServerPlayer player, String message);

    /** The level this NPC lives in — used to hop back onto the server thread. */
    Level level();

    /** Short tag for log messages — e.g. "Kangarude", "Halric". */
    String tag();

    // ── Shared one-line-into-the-dialogue-UI surface ─────────────────────────
    //
    // Used by NpcGiftHandler so gift reactions land in the voiced dialogue
    // screen instead of plain chat. Each NPC supplies the small per-instance
    // details (entity id, display name, subtitle, voice driver) and inherits
    // the open-or-update routing logic via {@link #giftReaction}.

    /** The entity id used by the dialogue payloads — typically {@code getId()}. */
    int entityId();

    /** Header name shown in the dialogue screen — e.g. "Master Tobias". */
    String displayName();

    /** Subtitle under the name — e.g. "Blacksmith • The Iron Hearth". */
    String displaySubtitle();

    /** Speak the line aloud through this NPC's voice (no-op if voice disabled / muted). */
    void speakAloud(ServerPlayer player, String line);

    /**
     * Mark this player as the current chat partner — clears history, sets the
     * partnerId, registers with the chat registry, etc. Mirrors what each NPC
     * does on a normal right-click interact, so a gift-initiated dialogue
     * becomes a real session the player can reply into.
     */
    void beginConversationWith(ServerPlayer player);

    /**
     * Surface a one-shot line in the dialogue UI and speak it aloud.
     *
     * <p>If the player already has a dialogue open with this NPC, pushes the
     * line into the existing screen as a server reply. Otherwise opens a fresh
     * dialogue screen with the line as its opener. Either way the line is
     * spoken via {@link #speakAloud}.
     *
     * <p>Does not start a chat session — the player can close the screen and
     * re-interact normally to begin a typed/voiced conversation.
     */
    default void giftReaction(ServerPlayer player, String line) {
        if (line == null || line.isBlank()) return;
        boolean alreadyTalkingToThisPlayer = player.getUUID().equals(getPartnerId());
        if (!alreadyTalkingToThisPlayer) {
            // Promote this gift into a real session so the player can chat back.
            beginConversationWith(player);
        }
        if (alreadyTalkingToThisPlayer) {
            PacketDistributor.sendToPlayer(player,
                new UpdateWardenScreenPayload(entityId(),
                    UpdateWardenScreenPayload.STATUS_REPLY, line));
        } else {
            boolean isMuted = (level() instanceof ServerLevel sl)
                && NpcMuteRegistry.get(sl).isMuted(player.getUUID(), tag());
            PacketDistributor.sendToPlayer(player,
                new OpenWardenScreenPayload(entityId(), displayName(), tag(),
                    displaySubtitle(), line, isMuted));
        }
        speakAloud(player, line);
    }
}
