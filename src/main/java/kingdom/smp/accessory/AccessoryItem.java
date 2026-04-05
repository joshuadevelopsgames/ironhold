package kingdom.smp.accessory;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Base class for equippable accessories (Terraria-style).
 * <p>
 * Subclass this and override {@link #onAccessoryTick} to grant buffs while the
 * item sits in an accessory slot. Accessories only provide their effects when
 * placed in the dedicated accessory slots — not in the normal inventory.
 */
public class AccessoryItem extends Item {

    public AccessoryItem(Properties props) {
        super(props.stacksTo(1));
    }

    /**
     * Called every server tick while this item occupies an accessory slot.
     * Override to apply effects, attribute modifiers, etc.
     */
    public void onAccessoryTick(Player player, ItemStack stack) {
        // Override in subclasses
    }

    /**
     * Called once when the player equips this accessory into a slot.
     */
    public void onEquipped(Player player, ItemStack stack) {
        // Override in subclasses
    }

    /**
     * Called once when the player unequips this accessory from a slot.
     */
    public void onUnequipped(Player player, ItemStack stack) {
        // Override in subclasses
    }

    /**
     * Extra tooltip lines shown below the "Accessory" tag.
     * Override to describe the accessory's effect.
     */
    public List<Component> getAccessoryTooltip() {
        return List.of();
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext ctx,
        TooltipDisplay display,
        Consumer<Component> tooltip,
        TooltipFlag flag
    ) {
        tooltip.accept(Component.literal("\u2726 Accessory").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (Component line : getAccessoryTooltip()) {
            tooltip.accept(line);
        }
        super.appendHoverText(stack, ctx, display, tooltip, flag);
    }
}
