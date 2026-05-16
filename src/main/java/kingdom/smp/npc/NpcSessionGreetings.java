package kingdom.smp.npc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory record of "this NPC has already given player X their full
 * introduction this login session". Cleared when the player logs out (see
 * {@code IronholdGameEvents.onPlayerLoggedOut}) and on server shutdown.
 *
 * <p>Used by every voiced NPC so a returning player gets a short hello on
 * subsequent right-clicks rather than the long intro they got the first time.
 */
public final class NpcSessionGreetings {

    private static final Map<UUID, Set<UUID>> BY_PLAYER = new ConcurrentHashMap<>();

    private NpcSessionGreetings() {}

    public static boolean hasBeenGreetedBy(UUID playerId, UUID npcId) {
        if (playerId == null || npcId == null) return false;
        Set<UUID> s = BY_PLAYER.get(playerId);
        return s != null && s.contains(npcId);
    }

    public static void recordGreeting(UUID playerId, UUID npcId) {
        if (playerId == null || npcId == null) return;
        BY_PLAYER.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(npcId);
    }

    public static void forgetPlayer(UUID playerId) {
        if (playerId == null) return;
        BY_PLAYER.remove(playerId);
    }
}
