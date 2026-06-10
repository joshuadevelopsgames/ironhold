package kingdom.smp.block;

import com.mojang.serialization.MapCodec;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ender Shrine — a placed sanctuary block. Empty-hand right-click binds it as the player's revive
 * point ({@code BOUND_SHRINE} attachment); using an Ender Totem on it stocks a charge (see
 * {@link kingdom.smp.item.EnderTotemItem}). On death with no handheld totem, a charge teleports the
 * player here (see {@link kingdom.smp.game.EnderShrineDeathHandler}).
 *
 * <p>Spec: {@code specs/fantasia-ports/03-ender-shrine.md}.
 */
public class EnderShrineBlock extends Block implements EntityBlock {

    public static final MapCodec<EnderShrineBlock> CODEC = simpleCodec(EnderShrineBlock::new);

    public EnderShrineBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderShrineBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof EnderShrineBlockEntity shrine)) {
            return InteractionResult.PASS;
        }

        ItemStack held = sp.getMainHandItem();

        // Holding an Ender Totem → stock a charge. (This runs even with an item in hand: the block's
        // default useItemOn returns TRY_WITH_EMPTY_HAND, so useWithoutItem fires before the item's useOn.)
        if (held.is(ModItems.ENDER_TOTEM.get())) {
            if (shrine.addCharge()) {
                held.shrink(1);
                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 0.8F, 1.4F);
                sp.sendSystemMessage(Component.literal("✦ The shrine drinks the totem. ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(shrine.getCharges() + " / " + EnderShrineBlockEntity.MAX_CHARGES + " charges.")
                        .withStyle(ChatFormatting.GRAY)));
            } else {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6F, 0.6F);
                sp.sendSystemMessage(Component.literal("The shrine is already full.").withStyle(ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS;
        }

        // Any other held item → do nothing (let normal item use proceed).
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }

        // Empty hand → bind this shrine as the player's sanctuary (one shrine per player; rebinding moves it).
        shrine.setOwner(sp.getUUID());
        sp.setData(ModAttachments.BOUND_SHRINE.get(),
            new BoundShrine(GlobalPos.of(level.dimension(), pos)));

        sp.sendSystemMessage(Component.literal("✦ Sanctuary bound. ")
            .withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal("The shrine holds " + shrine.getCharges() + " / "
                    + EnderShrineBlockEntity.MAX_CHARGES + " charges.")
                .withStyle(ChatFormatting.GRAY)));
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.3F);
        return InteractionResult.SUCCESS;
    }
}
