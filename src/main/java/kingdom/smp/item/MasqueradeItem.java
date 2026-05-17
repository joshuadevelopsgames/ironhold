package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * The "Empty" plant — what reverse-pickpocket leaves in a player's inventory.
 *
 * <p>Rendered with the vanilla {@code minecraft:item/air} model so it shows
 * up as a blank slot — visually nothing. Hovering reveals:
 * <ul>
 *   <li>What item was actually planted (display name only).</li>
 *   <li>Who planted it — UNLESS the planter's Pickpocket skill was at or
 *       above {@link #ANONYMITY_LEVEL}, in which case the trail goes cold
 *       and only "???" is shown.</li>
 * </ul>
 *
 * <p>Data is stored on the stack's {@link DataComponents#CUSTOM_DATA} via
 * NBT keys — no Codec-based ItemStack wrapping yet, just enough display
 * info for the tooltip. A future "cure" mechanic can extend this to
 * actually deliver the wrapped item back when removed.
 */
public class MasqueradeItem extends Item {

    private static final String TAG_WRAPPED_NAME = "ironhold_wrapped_name";
    private static final String TAG_PLANTER_NAME = "ironhold_planter_name";
    private static final String TAG_PLANTER_LEVEL = "ironhold_planter_level";

    /** Pickpocket level at or above which the planter's identity is hidden in the tooltip. */
    public static final int ANONYMITY_LEVEL = 75;

    public MasqueradeItem(Properties props) {
        super(props);
    }

    /**
     * Build a Masquerade stack wrapping the planted item's display info.
     * @param original what was actually planted (we only keep its name for the tooltip)
     * @param planterName the planter's display name
     * @param planterPickpocketLevel the planter's current Pickpocket level (frozen at plant time)
     * @param masqueradeItem the registered MASQUERADE item from Ironhold.java
     */
    public static ItemStack wrap(ItemStack original, String planterName,
                                  int planterPickpocketLevel, Item masqueradeItem) {
        ItemStack masquerade = new ItemStack(masqueradeItem);
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_WRAPPED_NAME, original.getHoverName().getString());
        tag.putString(TAG_PLANTER_NAME, planterName);
        tag.putInt(TAG_PLANTER_LEVEL, planterPickpocketLevel);
        masquerade.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return masquerade;
    }

    @Override
    public Component getName(ItemStack stack) {
        // Empty display name — the slot reads as nothing in inventory lists,
        // chat displays, /give output, etc. Tooltip fills in the details.
        return Component.literal("");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();

        String wrappedName = tag.getStringOr(TAG_WRAPPED_NAME, "Unknown");
        String planterName = tag.getStringOr(TAG_PLANTER_NAME, "???");
        int planterLevel = tag.getIntOr(TAG_PLANTER_LEVEL, 0);

        tooltip.accept(Component.literal("Planted: ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(wrappedName).withStyle(ChatFormatting.WHITE)));

        if (planterLevel >= ANONYMITY_LEVEL) {
            tooltip.accept(Component.literal("By: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("??? — the trail is cold")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        } else {
            tooltip.accept(Component.literal("By: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(planterName).withStyle(ChatFormatting.YELLOW)));
        }
    }
}
