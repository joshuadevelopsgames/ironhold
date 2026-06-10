package kingdom.smp.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
    /** List of key ids this key opens — one entry per lock the owner bound it to. */
    private static final String TAG_LOCK_KEY_IDS = "ironhold_lock_key_ids";
    /** Pre-multi single-id binding; still read so old keys keep working, folded into the list on next bind. */
    private static final String TAG_LEGACY_KEY_ID = "ironhold_lock_key_id";

    public LockKeyItem(Properties properties) {
        super(properties);
    }

    /** Add {@code keyId} to the set this key opens (a key can be bound to many locks). */
    public static void bind(ItemStack stack, String keyId) {
        if (keyId == null || keyId.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag ids = tag.getListOrEmpty(TAG_LOCK_KEY_IDS);
            String legacy = tag.getStringOr(TAG_LEGACY_KEY_ID, "");
            if (!legacy.isEmpty()) {
                if (!contains(ids, legacy)) ids.add(StringTag.valueOf(legacy));
                tag.remove(TAG_LEGACY_KEY_ID);
            }
            if (!contains(ids, keyId)) ids.add(StringTag.valueOf(keyId));
            tag.put(TAG_LOCK_KEY_IDS, ids);
        });
    }

    public static boolean matches(ItemStack stack, String keyId) {
        if (keyId == null || keyId.isEmpty() || !(stack.getItem() instanceof LockKeyItem)) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (keyId.equals(tag.getStringOr(TAG_LEGACY_KEY_ID, ""))) return true; // legacy single-bound keys
        return contains(tag.getListOrEmpty(TAG_LOCK_KEY_IDS), keyId);
    }

    public static boolean isBound(ItemStack stack) {
        return boundCount(stack) > 0;
    }

    /** How many distinct locks this key opens. */
    public static int boundCount(ItemStack stack) {
        if (!(stack.getItem() instanceof LockKeyItem)) return 0;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int n = tag.getListOrEmpty(TAG_LOCK_KEY_IDS).size();
        if (!tag.getStringOr(TAG_LEGACY_KEY_ID, "").isEmpty()) n++;
        return n;
    }

    private static boolean contains(ListTag ids, String keyId) {
        for (int i = 0; i < ids.size(); i++) {
            if (keyId.equals(ids.getStringOr(i, ""))) return true;
        }
        return false;
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
        int bound = boundCount(stack);
        if (bound > 0) {
            tooltip.accept(Component.literal(
                "Bound to " + bound + " locked " + (bound == 1 ? "thing" : "things")).withStyle(ChatFormatting.GOLD));
            tooltip.accept(Component.literal(
                "Carry to access " + (bound == 1 ? "it" : "them")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.literal("Unbound").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.literal("A lock owner can bind this key").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
