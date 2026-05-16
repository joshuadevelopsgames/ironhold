package kingdom.smp.rtf.feature.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class BlockReader implements BlockGetter {
    private BlockState state;

    public BlockReader setState(BlockState state) {
        this.state = state;
        return this;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.state;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }
}
