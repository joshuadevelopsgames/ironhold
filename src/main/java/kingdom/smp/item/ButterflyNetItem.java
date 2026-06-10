package kingdom.smp.item;

import kingdom.smp.ModItems;
import kingdom.smp.entity.ButterflyEntity;
import kingdom.smp.entity.ButterflySpecies;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Butterfly Net — right-click a {@link ButterflyEntity} to capture it as a loose
 * {@link ButterflyItem}. The butterfly can then be combined with an empty jar.
 */
public class ButterflyNetItem extends Item {

    public ButterflyNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ButterflyEntity butterfly)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            ButterflySpecies species = butterfly.getSpecies();
            ItemStack caught = ModItems.butterflyFor(species);
            if (!player.addItem(caught)) {
                player.drop(caught, false);
            }
            butterfly.discard();
            stack.hurtAndBreak(1, sp, hand);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.7F, 1.4F);
            // Record the species in the player's Butterfly Encyclopedia. First sighting only:
            // chime + actionbar so a new discovery feels rewarding.
            if (kingdom.smp.entity.ButterflyDex.discover(player, species)) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.7F);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "New encyclopedia entry: " + species.displayName() + "!")
                    .withStyle(net.minecraft.ChatFormatting.AQUA), true);
                // Completing the encyclopedia grants the Lepidopterist achievement + a skill point.
                kingdom.smp.game.ButterflyCollectionRewards.checkComplete(sp);
            }
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }
}
