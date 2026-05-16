package kingdom.smp.ai;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry mapping a player UUID to the NPC they're currently
 * holding an AI conversation with. Replaces the old Kangarude-only routing
 * table — both Kangarude and Warden Halric (and future voiced NPCs) register
 * themselves here so the {@link kingdom.smp.ai.svc.MicrophoneListener} can
 * deliver PTT transcripts without per-entity special-casing.
 */
public final class NpcChatRegistry {

    private static final Map<UUID, NpcChatPartner> PARTNERS = new ConcurrentHashMap<>();

    private NpcChatRegistry() {}

    /** Bind {@code playerId} to {@code npc} as their active conversation partner. */
    public static void setActive(UUID playerId, NpcChatPartner npc) {
        if (playerId == null || npc == null) return;
        PARTNERS.put(playerId, npc);
    }

    /** Unbind {@code playerId} only if their current partner is still {@code expected}. */
    public static void clearActive(UUID playerId, NpcChatPartner expected) {
        if (playerId == null || expected == null) return;
        PARTNERS.remove(playerId, expected);
    }

    /** Unbind {@code playerId} unconditionally. */
    public static void clearActive(UUID playerId) {
        if (playerId == null) return;
        PARTNERS.remove(playerId);
    }

    /** Returns the NPC this player is currently talking to, or null. */
    public static @Nullable NpcChatPartner getActive(UUID playerId) {
        return playerId == null ? null : PARTNERS.get(playerId);
    }
}
