package kingdom.smp.item;

import kingdom.smp.effect.BleedingHandler;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Bandage — consumable item that instantly cures the Bleeding effect.
 */
public class BandageItem extends Item {

    public BandageItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // You can only use a bandage if you are actually bleeding
        if (player.hasEffect(kingdom.smp.ModEffects.BLEEDING_EFFECT)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && kingdom.smp.ModEffects.BLEEDING_EFFECT != null) {
            BleedingHandler.runWithBypass(() -> {
                entity.removeEffect(kingdom.smp.ModEffects.BLEEDING_EFFECT);
            });
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                 Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Instantly cures Bleeding.")
            .withStyle(ChatFormatting.GRAY));
    }
}
