package kingdom.smp.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Ender Totem — Totem of Undying + Ender Pearl. Its only use is fueling an
 * {@link kingdom.smp.block.EnderShrineBlock}: right-click the shrine while holding one to add a charge
 * (handled in the block's {@code useWithoutItem}). It is NOT a handheld revive — that's the vanilla totem.
 *
 * <p>Spec: {@code specs/fantasia-ports/03-ender-shrine.md}.
 */
public class EnderTotemItem extends Item {

    public EnderTotemItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Right-click an Ender Shrine to add a revive charge.")
            .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, ctx, display, tooltip, flag);
    }
}
