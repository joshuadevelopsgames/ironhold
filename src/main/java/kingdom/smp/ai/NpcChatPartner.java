package kingdom.smp.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
}
