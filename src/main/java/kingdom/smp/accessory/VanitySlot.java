package kingdom.smp.accessory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Inventory slot that only accepts armor items matching a specific
 * {@link EquipmentSlot} (HEAD, CHEST, LEGS, or FEET).
 * Used in the vanity portion (slots 5–8) of the {@link AccessoryMenu}.
 */
public class VanitySlot extends Slot {

    private final EquipmentSlot requiredSlot;

    public VanitySlot(Container container, int index, int x, int y, EquipmentSlot requiredSlot) {
        super(container, index, x, y);
        this.requiredSlot = requiredSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == requiredSlot;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * Show the same empty-slot silhouette vanilla armor slots use (helmet / chestplate /
     * leggings / boots), so players know what goes where. Vanilla only renders this when the
     * slot is empty, so it disappears once a piece is equipped.
     */
    @Override
    public Identifier getNoItemIcon() {
        return switch (requiredSlot) {
            case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            default -> super.getNoItemIcon();
        };
    }

    public EquipmentSlot getRequiredSlot() {
        return requiredSlot;
    }
}
