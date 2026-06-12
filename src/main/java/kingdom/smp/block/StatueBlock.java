package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A carved-stone player statue as a placeable block (formerly the
 * {@link kingdom.smp.entity.StoneStatueEntity} — legacy entities are converted
 * to this block on load, see IronholdGameEvents). One class serves every statue
 * variant; the per-variant skin is resolved from the Block instance by the
 * client renderer (StatueBlockRenderer), which draws the same frozen humanoid
 * figure on the same two-tier plinth the entity renderer used.
 *
 * <p>Faces the player on placement. The visual stands ~2.5 blocks tall but the
 * block occupies a single cell; the collision shape extends upward past 16px
 * (legal, like walls/fences) so the figure can't be walked through.
 */
public class StatueBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<StatueBlock> CODEC = simpleCodec(StatueBlock::new);

    /** Plinth steps + figure column, matching the rendered model's footprint.
     *  Symmetric about the block centre, so one shape serves all four facings. */
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0, 0, 0, 16, 4, 16),    // wide bottom step
        Block.box(2, 4, 2, 14, 8, 14),    // narrow top step
        Block.box(4, 8, 4, 12, 40, 12));  // figure (head tops out at 40px = 2.5 blocks)

    public StatueBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Face whoever places it — same greeting the entity gave its placer.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Everything visible is drawn by the block entity renderer; the static
        // model only supplies break/sprint particles (see blockstates/<id>.json).
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueBlockEntity(pos, state);
    }
}
