package kingdom.smp.admin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kingdom.smp.Ironhold;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent side storage for {@code /admin} mode: when an op flips into admin (build) mode we
 * stash everything needed to put them back exactly where they were — their position, facing,
 * dimension, game mode, and full inventory. A player with an entry here is "in admin mode"; the
 * absence of an entry means they are not. {@link kingdom.smp.command.AdminModeCommand} reads that
 * presence to decide whether the next {@code /admin} should enter or exit.
 *
 * <p>Backed by the <b>overworld</b> {@link ServerLevel} (a single global store, keyed by player
 * UUID) so it survives relog and server restart — an op who is mid-build when the server cycles
 * still gets their gear back on the next {@code /admin}. Mirrors the codec-based
 * {@link kingdom.smp.game.LockedDoorData} pattern.
 */
public final class AdminModeData extends SavedData {

    private static final Identifier DATA_KEY =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "admin_mode");

    /** One occupied inventory slot. Empty slots are not stored. */
    public record SlotStack(int slot, ItemStack stack) {}

    /** Everything captured at the moment a player entered admin mode. */
    public record Snapshot(
        List<SlotStack> items,
        double x, double y, double z,
        float yaw, float pitch,
        String dimension,
        String gameType
    ) {}

    private final Map<UUID, Snapshot> snapshots;

    public AdminModeData() {
        this.snapshots = new HashMap<>();
    }

    private AdminModeData(Map<UUID, Snapshot> snapshots) {
        this.snapshots = new HashMap<>(snapshots);
    }

    private static final Codec<SlotStack> SLOT_STACK_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.INT.fieldOf("slot").forGetter(SlotStack::slot),
        ItemStack.CODEC.fieldOf("item").forGetter(SlotStack::stack)
    ).apply(inst, SlotStack::new));

    private static final Codec<Snapshot> SNAPSHOT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        SLOT_STACK_CODEC.listOf().fieldOf("items").forGetter(Snapshot::items),
        Codec.DOUBLE.fieldOf("x").forGetter(Snapshot::x),
        Codec.DOUBLE.fieldOf("y").forGetter(Snapshot::y),
        Codec.DOUBLE.fieldOf("z").forGetter(Snapshot::z),
        Codec.FLOAT.fieldOf("yaw").forGetter(Snapshot::yaw),
        Codec.FLOAT.fieldOf("pitch").forGetter(Snapshot::pitch),
        Codec.STRING.fieldOf("dimension").forGetter(Snapshot::dimension),
        Codec.STRING.fieldOf("gameType").forGetter(Snapshot::gameType)
    ).apply(inst, Snapshot::new));

    private record Entry(UUID uuid, Snapshot snapshot) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        UUIDUtil.CODEC.fieldOf("uuid").forGetter(Entry::uuid),
        SNAPSHOT_CODEC.fieldOf("snapshot").forGetter(Entry::snapshot)
    ).apply(inst, Entry::new));

    private static final Codec<AdminModeData> CODEC = ENTRY_CODEC.listOf().xmap(
        list -> {
            Map<UUID, Snapshot> map = new HashMap<>();
            for (Entry e : list) map.put(e.uuid(), e.snapshot());
            return new AdminModeData(map);
        },
        data -> data.snapshots.entrySet().stream()
            .map(e -> new Entry(e.getKey(), e.getValue()))
            .toList()
    );

    private static final SavedDataType<AdminModeData> TYPE =
        new SavedDataType<>(DATA_KEY, AdminModeData::new, CODEC, DataFixTypes.SAVED_DATA_SCOREBOARD);

    /** Global store, anchored to the overworld so it is shared across all dimensions. */
    public static AdminModeData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isInAdminMode(UUID player) {
        return snapshots.containsKey(player);
    }

    public void put(UUID player, Snapshot snapshot) {
        snapshots.put(player, snapshot);
        setDirty();
    }

    /** Removes and returns the player's snapshot, or {@code null} if they had none. */
    public Snapshot remove(UUID player) {
        Snapshot removed = snapshots.remove(player);
        if (removed != null) setDirty();
        return removed;
    }
}
