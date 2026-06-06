package milkucha.trmt.erosion;

import milkucha.trmt.TRMTBlocks;
import milkucha.trmt.TRMTConfig;
import milkucha.trmt.block.ErodedDirtBlock;
import milkucha.trmt.block.ErodedSandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One-shot, full erosion of a single block position. Where the passive TRMT system wears
 * terrain over many footsteps, this jumps a block straight to its terminal eroded state in
 * a single call — used by external features (the Ironhold Battle Hammer ground slam) so a
 * smash instantly stamps a worn path/crater into the ground.
 *
 * <p>Mirrors the terminal transforms in the TRMT step mixin:
 * grass/dirt → eroded_coarse_dirt, sand → fully-sunk eroded_sand, leaves/vegetation → broken.
 * Respects the same per-category config toggles as passive erosion.</p>
 */
public final class SlamErosion {

    private SlamErosion() {}

    /** Fully erodes the block at {@code pos}; returns true if something was eroded/broken. */
    public static boolean forceErode(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        TRMTConfig.ErosionToggles erosion = TRMTConfig.get().erosion;
        ErosionMapManager manager = ErosionMapManager.getInstance();
        Direction facing = rotationToFacing(BlockThresholds.posRotation(pos));

        // Grass / dirt family → terminal eroded coarse-dirt path.
        if ((erosion.grassEnabled && (state.is(Blocks.GRASS_BLOCK) || state.is(TRMTBlocks.ERODED_GRASS_BLOCK)))
                || (erosion.dirtEnabled && (state.is(Blocks.DIRT) || state.is(TRMTBlocks.ERODED_DIRT)))) {
            world.setBlock(pos,
                TRMTBlocks.ERODED_COARSE_DIRT.defaultBlockState().setValue(ErodedDirtBlock.FACING, facing),
                Block.UPDATE_ALL);
            manager.removeEntry(pos);
            return true;
        }

        // Sand → fully-sunk eroded sand (only where it can sink, matching the passive rule).
        if (erosion.sandEnabled && (state.is(Blocks.SAND) || state.is(TRMTBlocks.ERODED_SAND))) {
            if (!world.getBlockState(pos.above()).isAir()) return false;
            world.setBlock(pos,
                TRMTBlocks.ERODED_SAND.defaultBlockState()
                    .setValue(ErodedSandBlock.FACING, facing)
                    .setValue(ErodedSandBlock.STAGE, 4),
                Block.UPDATE_ALL);
            manager.removeEntry(pos);
            return true;
        }

        // Leaves / vegetation → shattered by the impact.
        if ((erosion.leavesEnabled && BlockThresholds.isLeaves(block))
                || (erosion.vegetationEnabled && BlockThresholds.isVegetation(block))) {
            world.destroyBlock(pos, false);
            manager.removeEntry(pos);
            return true;
        }

        return false;
    }

    private static Direction rotationToFacing(int rotation) {
        return switch (rotation) {
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }
}
