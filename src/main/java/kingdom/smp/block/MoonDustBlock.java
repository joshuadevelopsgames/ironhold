package kingdom.smp.block;

import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class MoonDustBlock extends ColoredFallingBlock {
    public MoonDustBlock(ColorRGBA colorData, Properties properties) {
        super(colorData, properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.dimension() == ModMoonDimensions.MOON_LEVEL) {
            return;
        }
        super.tick(state, level, pos, random);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.dimension() == ModMoonDimensions.MOON_LEVEL) {
            return;
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }
}
