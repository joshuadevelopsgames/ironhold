package kingdom.smp.item;

import kingdom.smp.Ironhold;
import kingdom.smp.effect.PlagueHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Plague Tonic — drinkable cure for the {@code ironhold:plague} effect at any stage.
 * Stage 0 is also curable with milk; from Stage 1 onward the tonic is the only option.
 *
 * <p>Use animation: standard drink. Returns an empty bottle on consume (matches honey bottle).
 */
public class PlagueTonicItem extends Item {

    public PlagueTonicItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && kingdom.smp.ModEffects.PLAGUE_EFFECT != null) {
            boolean hadPlague = entity.hasEffect(kingdom.smp.ModEffects.PLAGUE_EFFECT);
            PlagueHandler.runWithBypass(() -> entity.removeEffect(kingdom.smp.ModEffects.PLAGUE_EFFECT));
            if (hadPlague) {
                PlagueHandler.grantCureImmunity(entity);
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                 Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.ironhold.plague_tonic.line1")
            .withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.ironhold.plague_tonic.line2")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
