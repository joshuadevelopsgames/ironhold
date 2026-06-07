package kingdom.smp.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent record of which doors are locked, who owns them, and the key id that grants shared
 * access.
 *
 * <p>Chests, shelves, and armor stands carry this on {@link kingdom.smp.ModAttachments#LOCK_OWNER}
 * / {@link kingdom.smp.ModAttachments#LOCK_KEY_ID} attachments, but doors have <em>no</em> block
 * entity, so the same two fields live in side storage here — mirroring
 * {@link kingdom.smp.honeycomb.WaxedBlockData}. Keys are the <b>lower-half</b>
 * {@link net.minecraft.core.BlockPos#asLong()}, so clicking either half resolves to one entry (see
 * {@code LockProtectionHandler.doorLowerPos}).
 *
 * <p>Stored <b>per dimension</b> (each {@link ServerLevel} has its own instance): a packed position
 * is only unique within one dimension, and doors exist in all of them, so a single overworld-backed
 * store would let same-XYZ doors in different dimensions share a lock.
 */
public final class LockedDoorData extends SavedData {

    private static final Identifier DATA_KEY = Identifier.fromNamespaceAndPath(Ironhold.MODID, "locked_doors");

    /** Ownership + shared-access key for one locked door. */
    public record DoorLock(String owner, String keyId) {}

    private final Map<Long, DoorLock> locks;

    public LockedDoorData() {
        this.locks = new HashMap<>();
    }

    private LockedDoorData(Map<Long, DoorLock> locks) {
        this.locks = new HashMap<>(locks);
    }

    private record Entry(long pos, String owner, String keyId) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
        Codec.STRING.fieldOf("owner").forGetter(Entry::owner),
        Codec.STRING.fieldOf("keyId").forGetter(Entry::keyId)
    ).apply(inst, Entry::new));

    private static final Codec<LockedDoorData> CODEC = ENTRY_CODEC.listOf().xmap(
        list -> {
            Map<Long, DoorLock> map = new HashMap<>();
            for (Entry e : list) map.put(e.pos(), new DoorLock(e.owner(), e.keyId()));
            return new LockedDoorData(map);
        },
        data -> data.locks.entrySet().stream()
            .map(e -> new Entry(e.getKey(), e.getValue().owner(), e.getValue().keyId()))
            .toList()
    );

    private static final SavedDataType<LockedDoorData> TYPE =
        new SavedDataType<>(DATA_KEY, LockedDoorData::new, CODEC, DataFixTypes.SAVED_DATA_SCOREBOARD);

    public static LockedDoorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isLocked(long packedPos) {
        return locks.containsKey(packedPos);
    }

    /** @return the owner's UUID string, or {@code ""} if this door isn't locked. */
    public String owner(long packedPos) {
        DoorLock lock = locks.get(packedPos);
        return lock != null ? lock.owner() : "";
    }

    /** @return the door's shared-access key id, or {@code ""} if this door isn't locked. */
    public String keyId(long packedPos) {
        DoorLock lock = locks.get(packedPos);
        return lock != null ? lock.keyId() : "";
    }

    /** @return true if newly locked; false if it was already locked by anyone. */
    public boolean lock(long packedPos, String ownerUuid, String keyId) {
        if (locks.containsKey(packedPos)) return false;
        locks.put(packedPos, new DoorLock(ownerUuid, keyId));
        setDirty();
        return true;
    }

    public void unlock(long packedPos) {
        if (locks.remove(packedPos) != null) {
            setDirty();
        }
    }
}
