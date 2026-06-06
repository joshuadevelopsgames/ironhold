package kingdom.smp.honeycomb;

import com.mojang.serialization.Codec;
import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent set of block positions that have been "waxed" with honeycomb so the TRMT erosion
 * system leaves them alone (see {@link milkucha.trmt.erosion.ErosionMapManager#onStep}).
 *
 * <p>Positions are stored packed via {@link net.minecraft.core.BlockPos#asLong()} in a single
 * overworld-backed store, matching how TRMT itself keeps a single global erosion state regardless
 * of dimension — erosion only ever targets overworld terrain (grass/dirt/sand/leaves/vegetation),
 * so a per-dimension split would add complexity for no practical gain.
 */
public final class WaxedBlockData extends SavedData {

    private static final Identifier DATA_KEY = Identifier.fromNamespaceAndPath(Ironhold.MODID, "waxed_blocks");

    private final Set<Long> positions;

    public WaxedBlockData() {
        this.positions = new HashSet<>();
    }

    private WaxedBlockData(Set<Long> positions) {
        this.positions = new HashSet<>(positions);
    }

    private static final Codec<WaxedBlockData> CODEC = Codec.LONG.listOf().xmap(
        list -> new WaxedBlockData(new HashSet<>(list)),
        data -> new ArrayList<>(data.positions)
    );

    private static final SavedDataType<WaxedBlockData> TYPE =
        new SavedDataType<>(DATA_KEY, WaxedBlockData::new, CODEC, DataFixTypes.SAVED_DATA_SCOREBOARD);

    public static WaxedBlockData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isWaxed(long packedPos) {
        return positions.contains(packedPos);
    }

    /** @return true if the position was newly waxed (false if it was already waxed). */
    public boolean wax(long packedPos) {
        if (positions.add(packedPos)) {
            setDirty();
            return true;
        }
        return false;
    }

    public void unwax(long packedPos) {
        if (positions.remove(packedPos)) {
            setDirty();
        }
    }
}
