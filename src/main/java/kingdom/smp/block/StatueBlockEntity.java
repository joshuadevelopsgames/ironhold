package kingdom.smp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for {@link StatueBlock}. Holds no data of its own — the statue
 * variant (skin) comes from the Block instance and the pose is fixed — it
 * exists purely so the client can attach a BlockEntityRenderer that draws the
 * humanoid figure and plinth.
 */
public class StatueBlockEntity extends BlockEntity {

    public StatueBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.STATUE_BLOCK_ENTITY.get(), pos, state);
    }
}
