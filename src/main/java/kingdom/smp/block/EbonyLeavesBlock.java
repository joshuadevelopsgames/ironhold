package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Concrete subclass of LeavesBlock for Ebony tree foliage.
 *
 * LeavesBlock became abstract in MC 26.x and requires overrides for:
 *   - codec()                      — serialisation identity
 *   - spawnFallingLeavesParticle() — particle behaviour
 *
 * Ebony leaves intentionally produce NO falling particles. The canopy clings
 * silently in the dark, reinforcing the eerie Ebonwood Hollow atmosphere.
 *
 * All other behaviour (distance-based decay, waterlogging, random ticks)
 * is inherited from LeavesBlock unchanged.
 */
public class EbonyLeavesBlock extends LeavesBlock {

    public static final MapCodec<EbonyLeavesBlock> CODEC = simpleCodec(EbonyLeavesBlock::new);

    public EbonyLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.02f, properties);
    }

    @Override
    public MapCodec<EbonyLeavesBlock> codec() {
        return CODEC;
    }

    /** No falling leaf particles — ebony leaves cling to the canopy in silence. */
    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        // intentionally empty
    }
}
