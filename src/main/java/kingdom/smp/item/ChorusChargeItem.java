package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Crystallized Ender energy used to fuel a Chorus Wardheart.
 * Right-click on a wardheart to deposit charge.
 */
public class ChorusChargeItem extends Item {
    public ChorusChargeItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                 Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.ironhold.chorus_charge.line1")
            .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("tooltip.ironhold.chorus_charge.line2")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
