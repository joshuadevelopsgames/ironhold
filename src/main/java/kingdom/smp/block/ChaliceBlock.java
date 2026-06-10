package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A placeable golden goblet you can pour any liquid into. Right-click with a
 * water / lava / milk / powder-snow bucket, a honey bottle, or any potion to fill
 * it; the {@code ChaliceRenderer} then draws that liquid's surface inside the cup,
 * tinted to match (lava even glows). Right-click with an empty hand to tip it out.
 *
 * <p>Filling hands back the appropriate empty container, exactly as the liquid's
 * normal use would. Breaking the chalice just drops the (empty) goblet — the
 * liquid spills. Held state lives on {@link ChaliceBlockEntity}.
 */
public class ChaliceBlock extends Block implements EntityBlock {

    public static final MapCodec<ChaliceBlock> CODEC = simpleCodec(ChaliceBlock::new);

    /** Matches the goblet model footprint (x5.75–10.25, y0–7, z5.75–10.25). */
    private static final VoxelShape SHAPE = Block.box(5.75, 0, 5.75, 10.25, 7, 10.25);

    public ChaliceBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChaliceBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChaliceBlockEntity be)) {
            return InteractionResult.PASS;
        }
        ChaliceLiquids.Fill fill = ChaliceLiquids.fromHeld(stack);
        if (fill == null) {
            // Not a liquid container — let the item's own behaviour run (or nothing).
            return InteractionResult.PASS;
        }
        // Already holds this exact liquid (potions may differ in colour, so always re-pour those):
        // swallow the click so we don't dump the liquid into the world, but change nothing.
        if (!fill.id().equals("potion") && fill.id().equals(be.liquidId())) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            be.fill(fill);
            // Hand back the empty container, vanilla bucket/bottle style.
            if (!player.getAbilities().instabuild && fill.container() != null) {
                ItemStack returned = new ItemStack(fill.container());
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, returned);
                } else if (!player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            level.playSound(null, pos, fill.sound(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChaliceBlockEntity be) || be.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Sneak: tip it out without drinking (e.g. to dump a chalice of lava safely).
                be.empty();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7f, 1.1f);
            } else {
                // Drink: apply the liquid's effect (read state BEFORE emptying), then the cup is empty.
                if (level instanceof ServerLevel server) {
                    ChaliceLiquids.applyDrinkEffects(server, player, be.liquidId(), be.potion());
                }
                be.empty();
                level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.BLOCKS, 0.7f, 1.0f);
            }
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5,
                    6, 0.12, 0.04, 0.12, 0.0);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
