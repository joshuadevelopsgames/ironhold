package kingdom.smp.block.wardheart;

import com.mojang.serialization.MapCodec;
import kingdom.smp.Ironhold;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chorus Wardheart — projects a translucent purple force-field dome.
 * Block state holds no variants; all state lives in the BlockEntity.
 */
public class WardheartBlock extends Block implements EntityBlock {

    public static final MapCodec<WardheartBlock> CODEC = simpleCodec(WardheartBlock::new);

    /** When true, the block model is invisible (only the BER's dome renders).
     *  Used for natural shields that auto-spawn around things like dragon end
     *  crystals — there's no physical block we want the player to see. */
    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);
    private static final VoxelShape EMPTY_SHAPE = net.minecraft.world.phys.shapes.Shapes.empty();

    public WardheartBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HIDDEN);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Hidden wardhearts (e.g., on dragon-crystal pillars) shouldn't have a
        // collision/selection box — players shouldn't be able to mine the empty
        // air where the shield generator is hiding.
        return state.getValue(HIDDEN) ? EMPTY_SHAPE : SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HIDDEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Hidden wardhearts are unbreakable — they're meant to protect things
        // permanently (dragon crystals etc.) and shouldn't be cheesable with a pickaxe.
        if (state.getValue(HIDDEN)) return 0.0f;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardheartBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != kingdom.smp.ModBlocks.WARDHEART_BLOCK_ENTITY.get()) return null;
        if (level.isClientSide()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<WardheartBlockEntity>) WardheartBlockEntity::clientTick;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<WardheartBlockEntity>) WardheartBlockEntity::serverTick;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player p) {
            if (level.getBlockEntity(pos) instanceof WardheartBlockEntity be) {
                be.setOwner(p.getUUID(), p.getName().getString());
                be.setChanged();
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        // Shield collapse — cosmetic burst when the wardheart is destroyed
        level.sendParticles(ParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            80, 0.6, 0.6, 0.6, 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            40, 0.4, 0.4, 0.4, 0.3);
        level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.4f, 0.55f);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WardheartBlockEntity be)) {
            return InteractionResult.PASS;
        }

        // Chorus Charge feeds fuel to the wardheart
        if (stack.is(Ironhold.CHORUS_CHARGE.get())) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (be.canFeed(player)) {
                int before = be.getFuel();
                be.addFuel(WardheartBlockEntity.FUEL_PER_CHARGE);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 0.8f, 1.4f);
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("message.ironhold.wardheart.fueled",
                                be.getFuel(), be.getTier().displayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE)));
                }
                if (be.getFuel() != before) be.setChanged();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WardheartBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Shift-right-click: cycle access mode (owner only)
        if (player.isShiftKeyDown()) {
            if (!be.canConfigure(player)) {
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("message.ironhold.wardheart.not_owner")
                            .withStyle(ChatFormatting.RED)));
                }
                return InteractionResult.FAIL;
            }
            WardheartAccess next = be.getAccess().next();
            be.setAccess(next);
            be.setChanged();
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.7f, 1.7f);
            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("message.ironhold.wardheart.access_set", next.displayName())
                        .withStyle(ChatFormatting.AQUA)));
            }
            return InteractionResult.SUCCESS;
        }

        // Plain right-click: status readout
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("message.ironhold.wardheart.status",
                        be.getTier().displayName(),
                        be.getTier().radius(),
                        be.getFuel(),
                        be.getAccess().displayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        return InteractionResult.SUCCESS;
    }
}
