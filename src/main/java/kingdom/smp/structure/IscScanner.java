package kingdom.smp.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Scans block regions into {@link IscStructure} and writes them back into the level. */
public final class IscScanner {
    private IscScanner() {}

    public static IscStructure scan(Level level, BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > IscStructure.MAX_VOLUME) {
            throw new IllegalArgumentException("Volume " + volume + " exceeds max "
                + IscStructure.MAX_VOLUME + " (cap each dim at " + IscStructure.MAX_DIM + ")");
        }

        // Build palette as we go. Air always gets index 0 so the format reads cleanly.
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> indexOf = new HashMap<>();
        BlockState air = Blocks.AIR.defaultBlockState();
        palette.add(air);
        indexOf.put(air, 0);

        int[] data = new int[(int) volume];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    cursor.set(minX + x, minY + y, minZ + z);
                    BlockState state = level.getBlockState(cursor);
                    Integer existing = indexOf.get(state);
                    if (existing == null) {
                        if (palette.size() >= IscStructure.MAX_PALETTE) {
                            throw new IllegalStateException("Structure has more than "
                                + IscStructure.MAX_PALETTE + " unique blockstates — too varied to encode in base-36 palette");
                        }
                        existing = palette.size();
                        palette.add(state);
                        indexOf.put(state, existing);
                    }
                    data[(y * sizeZ + z) * sizeX + x] = existing;
                }
            }
        }
        return new IscStructure(sizeX, sizeY, sizeZ, palette, data);
    }

    /** Places {@code structure} so its (0,0,0) corner sits at {@code origin}. Returns blocks placed. */
    public static int build(Level level, IscStructure structure, BlockPos origin, boolean placeAir) {
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int y = 0; y < structure.sizeY(); y++) {
            for (int z = 0; z < structure.sizeZ(); z++) {
                for (int x = 0; x < structure.sizeX(); x++) {
                    BlockState state = structure.at(x, y, z);
                    if (!placeAir && state.isAir()) continue;
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (level.setBlock(cursor, state, flags)) placed++;
                }
            }
        }
        return placed;
    }
}
