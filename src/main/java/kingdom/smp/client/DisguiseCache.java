package kingdom.smp.client;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;

/**
 * Client-side cache of which players are disguised, and as what entity type.
 * Populated by {@link kingdom.smp.net.SyncDisguisePayload}; read by
 * {@link kingdom.smp.mixin.EntityRenderDispatcherDisguiseMixin} to substitute the
 * disguised entity for the player model.
 */
public final class DisguiseCache {
    private DisguiseCache() {}

    private static final Map<UUID, Identifier> CACHE = new ConcurrentHashMap<>();

    public static void update(UUID uuid, Optional<Identifier> typeId) {
        if (typeId.isPresent()) {
            CACHE.put(uuid, typeId.get());
        } else {
            CACHE.remove(uuid);
        }
    }

    /** Returns the disguise entity-type id for the player, or {@code null} if undisguised. */
    public static Identifier get(UUID uuid) {
        return CACHE.get(uuid);
    }

    /** Snapshot for client tick systems that need to visit every active disguise. */
    public static Map<UUID, Identifier> snapshot() {
        return Map.copyOf(CACHE);
    }

    public static void clear() {
        CACHE.clear();
    }
}
