package kingdom.smp.accessory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory slot that only accepts {@link AccessoryItem} instances.
 * Used in the accessory portion (slots 0–4) of the {@link AccessoryMenu}.
 */
public class AccessorySlot extends Slot {

    public AccessorySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof AccessoryItem;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
