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
    // Box coords are derived directly from the GeckoLib model
    // (assets/.../geckolib/models/block/guillotine.geo.json). GeckoLib's
    // GeoBlockRenderer translates the model origin to the block centre before
    // drawing, so model-px → block-px is simply: add 8 to X and Z, Y unchanged.
    // The fully-rendered model (rotations on the brace bones applied) spans
    // X[-5.25,22.75] Y[0,68.25] Z[-11.5,28.5] in block-px.
    //
    // SHAPE_NORTH below is hand-fitted to the model's parts. GeoBlockRenderer
    // rotates by FACING (NORTH=0°, WEST=90°, SOUTH=180°, EAST=270° about the
    // block centre), so we derive the other three facings by rotating NORTH the
    // same way — keeping the collision shape locked to the visual model.
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        // Foot plates (full footprint slab)
        Block.box(-5.25, 0, -10, 22.75, 2, 27),
        // Posts + head clamp + blade + lower crossbars (central column)
        Block.box(6.5, 0, -9, 11.5, 64, 26),
        // Top crossbeam + cap (overhangs the posts front & back)
        Block.box(6.5, 63, -11.5, 11.5, 68.25, 28.5),
        // Lower diagonal brace-feet (splay outward +X), back & front legs
        Block.box(11.5, 0, -9, 20.5, 13, -5),
        Block.box(11.5, 0, 22, 20.5, 13, 26),
        // Upper diagonal braces (splay outward -X), back & front
        Block.box(-0.75, 30, -9, 6.5, 43, -5),
        Block.box(-0.75, 30, 22, 6.5, 43, 26)
    );

    private static final VoxelShape SHAPE_WEST  = rotateY90(SHAPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = rotateY90(SHAPE_WEST);
    private static final VoxelShape SHAPE_EAST  = rotateY90(SHAPE_SOUTH);

    /** Rotate a shape 90° clockwise (viewed from above) about the block centre:
     *  (x,z) → (z, 1-x). Matches GeckoLib's per-facing YP rotation so the four
     *  facing shapes stay aligned with the rendered model. */
    private static VoxelShape rotateY90(VoxelShape src) {
        VoxelShape out = Shapes.empty();
        for (AABB b : src.toAabbs()) {
            out = Shapes.or(out,
                Shapes.box(b.minZ, b.minY, 1.0 - b.maxX, b.maxZ, b.maxY, 1.0 - b.minX));
        }
        return out;
    }

    // ── Foot-plate footprint (placement spacing) ──────────────────────────────
    // The base slab reaches well past the 1×1 cell (NORTH: X[-0.33,1.42],
    // Z[-0.63,1.69] blocks), so two guillotines placed within ~2 blocks can
    // overlap foot-plates even though each sits in its own block. We reject any
    // placement whose foot-plate would overlap a nearby guillotine's.
    // AABB is block-relative (world = block pos + this), rotated per facing the
    // same way the collision shape is.
    private static final AABB FOOTPLATE_NORTH =
        new AABB(-5.25 / 16.0, 0, -10.0 / 16.0, 22.75 / 16.0, 2.0 / 16.0, 27.0 / 16.0);
    private static final AABB FOOTPLATE_WEST  = rotateAabbY90(FOOTPLATE_NORTH);
    private static final AABB FOOTPLATE_SOUTH = rotateAabbY90(FOOTPLATE_WEST);
    private static final AABB FOOTPLATE_EAST  = rotateAabbY90(FOOTPLATE_SOUTH);

    /** 90° CW about the block centre — same mapping as {@link #rotateY90}. */
    private static AABB rotateAabbY90(AABB b) {
        return new AABB(b.minZ, b.minY, 1.0 - b.maxX, b.maxZ, b.maxY, 1.0 - b.minX);
    }

    private static AABB footplate(Direction facing) {
        return switch (facing) {
            case WEST  -> FOOTPLATE_WEST;
            case SOUTH -> FOOTPLATE_SOUTH;
            case EAST  -> FOOTPLATE_EAST;
            default    -> FOOTPLATE_NORTH;
        };
    }

    /** True if a guillotine placed at {@code pos}/{@code facing} would have its
     *  foot-plate slab overlap that of an existing guillotine nearby. Touching
     *  edge-to-edge is allowed ({@link AABB#intersects} is strict). */
    private static boolean footplateBlocked(Level level, BlockPos pos, Direction facing) {
        AABB mine = footplate(facing).move(pos.getX(), pos.getY(), pos.getZ());
        // Plates reach ~1.69 blocks out, so a Δ≤2 neighbour is the farthest that
        // can overlap; scan the 5×5 ring at this Y (plates are a thin floor slab).
        for (BlockPos other : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 0, 2))) {
            if (other.equals(pos)) {
                continue;
            }
            BlockState st = level.getBlockState(other);
            if (!(st.getBlock() instanceof GuillotineBlock)) {
                continue;
            }
            AABB theirs = footplate(st.getValue(FACING)).move(other.getX(), other.getY(), other.getZ());
            if (mine.intersects(theirs)) {
                return true;
            }
        }
        return false;
    }

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
        Direction facing = ctx.getHorizontalDirection().getOpposite().getClockWise();
        // Refuse to place if our foot-plate would overlap a nearby guillotine's.
        if (footplateBlocked(ctx.getLevel(), ctx.getClickedPos(), facing)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
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
