package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.GuillotineSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placeable guillotine block. Faces the player on placement.
 * Rendered via GeckoLib (see GuillotineBlockRenderer).
 */
public class GuillotineBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<GuillotineBlock> CODEC = simpleCodec(GuillotineBlock::new);

    /** Whether the block is currently receiving a redstone signal (used for rising-edge release). */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** True once the blade has dropped; it stays down (held) until someone resets it. */
    public static final BooleanProperty CHOPPED = BooleanProperty.create("chopped");

    // ── Hitbox shapes per facing ──────────────────────────────────────────────
    // Model coords → Block.box: add 8 to X and Z (model origin = block center).
    // We use a simplified bounding box covering the model's footprint.
    // The model is ~1.75 blocks wide (X), ~4.27 blocks tall (Y), ~2.5 blocks deep (Z).
    //
    // Rather than one massive shape (which breaks ray-trace interaction), we build
    // a simple two-part shape: the footprint slab + the main vertical column.

    // NORTH: model-X → world-X, model-Z → world-Z
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        // Base footprint: model X[-13.25,14.75] Y[0,2] Z[-18,19] → box coords + 8
        Block.box(-5.25, 0, -10, 22.75, 2, 27),
        // Main vertical column (posts + headplate + crossbars): simplified bounding box
        // model X[-1.5,3.5] Y[0,68.25] Z[-17,18] → box coords + 8
        Block.box(6.5, 0, -9, 11.5, 68, 26)
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(16 - 22.75, 0, 16 - 27, 16 + 5.25, 2, 16 + 10),
        Block.box(16 - 11.5, 0, 16 - 26, 16 - 6.5, 68, 16 + 9)
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
        // Rotated 90° CW: (x,z) → (16-z, x)
        Block.box(16 - 27, 0, -5.25, 16 + 10, 2, 22.75),
        Block.box(16 - 26, 0, 6.5, 16 + 9, 68, 11.5)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
        // Rotated 90° CCW: (x,z) → (z, 16-x)
        Block.box(-10, 0, 16 - 22.75, 27, 2, 16 + 5.25),
        Block.box(-9, 0, 16 - 11.5, 26, 68, 16 - 6.5)
    );

    public GuillotineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, Boolean.FALSE)
            .setValue(CHOPPED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, CHOPPED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // The model's head-hole opens along model-X, but GeckoLib rotates based on
        // FACING along model-Z. Rotate 90° so the head-hole faces the player.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite().getClockWise());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuillotineBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != kingdom.smp.ModBlocks.GUILLOTINE_BLOCK_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<GuillotineBlockEntity>)
            (lvl, pos, st, be) -> GuillotineBlockEntity.serverTick((ServerLevel) lvl, pos, st, be);
    }

    // ── Redstone: a rising power edge drops the blade ─────────────────────────
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        boolean signal = level.hasNeighborSignal(pos);
        if (signal == state.getValue(POWERED)) {
            return;
        }
        BlockState next = state.setValue(POWERED, signal);
        // Rising edge drops the blade — but only if it's armed (not already chopped).
        if (signal && !state.getValue(CHOPPED)) {
            level.setBlock(pos, next.setValue(CHOPPED, Boolean.TRUE), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof GuillotineBlockEntity be) {
                be.release((ServerLevel) level);
            }
        } else {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    // ── Right-click: seat the player in the guillotine ────────────────────────
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Sneak + right-click (empty hand) is the executioner's lever: toggle the blade.
        if (player.isSecondaryUseActive()) {
            if (state.getValue(CHOPPED)) {
                // Blade is down → reset it: teleport back up into position.
                level.setBlock(pos, state.setValue(CHOPPED, Boolean.FALSE), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 0.7f);
            } else {
                // Armed → drop the blade on whoever is locked in.
                level.setBlock(pos, state.setValue(CHOPPED, Boolean.TRUE), Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(pos) instanceof GuillotineBlockEntity be) {
                    be.release((ServerLevel) level);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Plain right-click seats a player — but not while the blade is down.
        if (state.getValue(CHOPPED) || player.isPassenger()) {
            return InteractionResult.PASS;
        }

        // Check for an existing seat entity at this block
        var existing = level.getEntitiesOfClass(GuillotineSeatEntity.class,
            new AABB(pos).inflate(1.0));
        for (GuillotineSeatEntity seat : existing) {
            if (!seat.getPassengers().isEmpty()) {
                return InteractionResult.PASS; // already occupied
            }
            // Empty seat → sit the player down.
            seat.setYRot(state.getValue(FACING).toYRot() + 90f);
            player.startRiding(seat);
            return InteractionResult.SUCCESS;
        }

        // Spawn a new seat entity at the head-hole center
        Direction facing = state.getValue(FACING);
        GuillotineSeatEntity seat = new GuillotineSeatEntity(
            kingdom.smp.ModEntities.GUILLOTINE_SEAT_ENTITY.get(), level);

        // Head hole center: model (X=0, Y=12.875px ≈ 0.805 blocks, Z=0.5px ≈ 0)
        // → world: block center + 0.805 Y
        double seatX = pos.getX() + 0.5;
        double seatY = pos.getY() + 0.805;
        double seatZ = pos.getZ() + 0.5;

        seat.setPos(seatX, seatY, seatZ);
        // Rotate seat yaw 90° so the player swims into the head-hole, not along the posts
        seat.setYRot(facing.toYRot() + 90f);
        ((ServerLevel) level).addFreshEntity(seat);
        player.startRiding(seat);

        return InteractionResult.SUCCESS;
    }
}
