package kingdom.smp.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A transferable key bound by a lock owner to one locked chest, shelf group,
 * armor stand, or door. Access is granted only while the matching key remains in a
 * player's inventory.
 */
public final class LockKeyItem extends Item {
    private static final String TAG_LOCK_KEY_ID = "ironhold_lock_key_id";

    public LockKeyItem(Properties properties) {
        super(properties);
    }

    public static void bind(ItemStack stack, String keyId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putString(TAG_LOCK_KEY_ID, keyId));
    }

    public static boolean matches(ItemStack stack, String keyId) {
        if (keyId.isEmpty() || !(stack.getItem() instanceof LockKeyItem)) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return keyId.equals(tag.getStringOr(TAG_LOCK_KEY_ID, ""));
    }

    public static boolean isBound(ItemStack stack) {
        if (!(stack.getItem() instanceof LockKeyItem)) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.getStringOr(TAG_LOCK_KEY_ID, "").isEmpty();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        if (isBound(stack)) {
            tooltip.accept(Component.literal("Bound to a locked object").withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.literal("Carry to access it").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.literal("Unbound").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.literal("A lock owner can bind this key").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
