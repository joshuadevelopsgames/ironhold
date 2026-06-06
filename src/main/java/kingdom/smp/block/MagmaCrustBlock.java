package kingdom.smp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Temporary cooled-lava crust laid down by the Magma Boots so the wearer can
 * walk across lava (the Frost-Walker pattern, but for lava instead of water).
 *
 * <p>Unlike vanilla {@code MAGMA_BLOCK} it does not burn entities standing on
 * it. It ages out via scheduled ticks and collapses back into a lava source
 * once the wearer walks away — so it never permanently fills a lava lake.
 * {@link kingdom.smp.game.MagmaBootsHandler} keeps the crust under an active
 * wearer fresh (age 0); the moment they leave, it ages to {@link #MAX_AGE} and
 * reverts.</p>
 */
public class MagmaCrustBlock extends Block {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public MagmaCrustBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // First melt-back check is well in the future; refreshed to age 0 each
        // tick by the boots handler while the wearer is still standing nearby.
        level.scheduleTick(pos, this, Mth.nextInt(level.getRandom(), 40, 80));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            level.scheduleTick(pos, this, Mth.nextInt(random, 10, 20));
        } else {
            // Fully cooled with no wearer to keep it warm — collapse into lava.
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
