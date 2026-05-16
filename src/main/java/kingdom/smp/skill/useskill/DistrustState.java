package kingdom.smp.skill.useskill;

import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-entity persistent record of which players a villager / knight has
 * caught pickpocketing them (or witnessed pickpocketing in their village).
 *
 * <p>Stored on {@link Entity#getPersistentData()} as a flat {@code long[]}
 * with two longs per UUID (most-significant, least-significant). The data
 * persists across world saves with the entity.
 *
 * <p>Used by:
 * <ul>
 *   <li>{@link VillagerStealthHandler} — distrusted players don't have
 *       villager attention cleared, so the villager keeps watching them.</li>
 *   <li>{@link PickpocketHandler} — records distrust when a thief is
 *       caught.</li>
 * </ul>
 */
public final class DistrustState {
    private DistrustState() {}

    private static final String TAG_KEY = "ironhold_distrust";

    public static boolean isDistrusted(Entity entity, UUID playerUuid) {
        long msb = playerUuid.getMostSignificantBits();
        long lsb = playerUuid.getLeastSignificantBits();
        long[] arr = entity.getPersistentData().getLongArray(TAG_KEY).orElse(EMPTY);
        for (int i = 0; i + 1 < arr.length; i += 2) {
            if (arr[i] == msb && arr[i + 1] == lsb) return true;
        }
        return false;
    }

    public static void markDistrust(Entity entity, UUID playerUuid) {
        if (isDistrusted(entity, playerUuid)) return;
        long[] arr = entity.getPersistentData().getLongArray(TAG_KEY).orElse(EMPTY);
        long[] next = new long[arr.length + 2];
        System.arraycopy(arr, 0, next, 0, arr.length);
        next[arr.length] = playerUuid.getMostSignificantBits();
        next[arr.length + 1] = playerUuid.getLeastSignificantBits();
        entity.getPersistentData().putLongArray(TAG_KEY, next);
    }

    /** Returns a snapshot of all distrusted player UUIDs on this entity. */
    public static Set<UUID> getAll(Entity entity) {
        long[] arr = entity.getPersistentData().getLongArray(TAG_KEY).orElse(EMPTY);
        Set<UUID> uuids = new HashSet<>();
        for (int i = 0; i + 1 < arr.length; i += 2) {
            uuids.add(new UUID(arr[i], arr[i + 1]));
        }
        return uuids;
    }

    private static final long[] EMPTY = new long[0];
}
