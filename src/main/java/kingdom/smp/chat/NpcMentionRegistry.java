package kingdom.smp.chat;

import kingdom.smp.ai.NpcChatPartner;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side index of loaded, mention-able voiced NPCs keyed by lowercase name.
 *
 * <p>Lets the {@link MentionRouter} resolve an {@code @Tobias} chat token to the
 * actual {@link NpcChatPartner} entity without scanning every loaded entity on
 * each message. NPCs register themselves on join ({@code EntityJoinLevelEvent})
 * and unregister on removal. Each NPC is indexed under both its {@link
 * NpcChatPartner#tag() tag} and every whitespace token of its {@link
 * NpcChatPartner#displayName() display name}, so {@code @Tobias} matches a
 * "Master Tobias".
 */
public final class NpcMentionRegistry {

    private static final Map<String, Set<NpcChatPartner>> BY_NAME = new ConcurrentHashMap<>();

    private NpcMentionRegistry() {}

    private static Set<String> keysFor(NpcChatPartner npc) {
        Set<String> keys = new HashSet<>();
        if (npc.tag() != null && !npc.tag().isBlank()) {
            keys.add(npc.tag().toLowerCase(Locale.ROOT));
        }
        String display = npc.displayName();
        if (display != null) {
            for (String token : display.split("\\s+")) {
                if (!token.isBlank()) keys.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return keys;
    }

    /** @return true if this introduced a name not previously known (clients need a resync). */
    public static boolean register(NpcChatPartner npc) {
        if (npc == null) return false;
        boolean newName = false;
        for (String key : keysFor(npc)) {
            newName |= !BY_NAME.containsKey(key);
            BY_NAME.computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(npc);
        }
        return newName;
    }

    /** All currently mention-able NPC names (lowercase). */
    public static Set<String> allNames() {
        return Set.copyOf(BY_NAME.keySet());
    }

    public static void unregister(NpcChatPartner npc) {
        if (npc == null) return;
        for (String key : keysFor(npc)) {
            Set<NpcChatPartner> set = BY_NAME.get(key);
            if (set != null) {
                set.remove(npc);
                if (set.isEmpty()) BY_NAME.remove(key);
            }
        }
    }

    /**
     * Resolve a mention token to the nearest live NPC of that name in the
     * player's level, or null if none are loaded. Falls back to any live match
     * if none share the player's level.
     */
    public static @Nullable NpcChatPartner resolveNearest(String name, ServerPlayer player) {
        if (name == null || player == null) return null;
        Set<NpcChatPartner> set = BY_NAME.get(name.toLowerCase(Locale.ROOT));
        if (set == null || set.isEmpty()) return null;

        NpcChatPartner bestSameLevel = null;
        double bestDistSqr = Double.MAX_VALUE;
        NpcChatPartner anyAlive = null;

        for (NpcChatPartner npc : set) {
            if (!(npc instanceof Entity e) || !e.isAlive() || e.isRemoved()) continue;
            anyAlive = npc;
            if (e.level() != player.level()) continue;
            double d = e.distanceToSqr(player);
            if (d < bestDistSqr) {
                bestDistSqr = d;
                bestSameLevel = npc;
            }
        }
        return bestSameLevel != null ? bestSameLevel : anyAlive;
    }
}
