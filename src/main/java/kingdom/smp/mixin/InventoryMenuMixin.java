package kingdom.smp.mixin;

import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryInventoryAttachmentContainer;
import kingdom.smp.accessory.AccessoryItem;
import kingdom.smp.accessory.AccessorySlot;
import kingdom.smp.accessory.VanitySlot;
import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Re-skin and extension of vanilla {@link InventoryMenu}.
 *
 * <ul>
 *   <li>Repositions vanilla slots 0–45 to the Ironhold layout (see {@link IronholdInventoryLayout}).</li>
 *   <li>Adds 5 accessory slots and 4 vanity-armor slots backed by the player's
 *       {@link AccessoryInventory} attachment, so vanilla container sync handles
 *       all client↔server data transfer.</li>
 *   <li>Routes shift-click of accessory items / equippables into the new slots.</li>
 * </ul>
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    protected InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$applyIronholdLayout(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        // Vanilla slots 0–45 keep their vanilla positions; we only append the
        // accessory + vanity slots in the docked side panels.
        ironhold$addAccessoryAndVanitySlots(player);
    }

    private void ironhold$addAccessoryAndVanitySlots(Player player) {
        // Delegate to getData(ACCESSORY_INV) so ticks and menus share one attachment instance
        var accContainer = new AccessoryInventoryAttachmentContainer(player);

        // 5 accessory slots — "Accessories" row below the inventory (slots 46–50)
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            this.addSlot(new AccessorySlot(
                    accContainer, i,
                    IronholdInventoryLayout.ACC_SLOT_X0 + i * IronholdInventoryLayout.COSMETIC_SLOT_PITCH,
                    IronholdInventoryLayout.ACC_ROW_Y));
        }

        // 4 vanity slots — vertical column docked left of the inventory (slots 51–54)
        EquipmentSlot[] order = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < AccessoryInventory.VANITY_SLOTS; i++) {
            this.addSlot(new VanitySlot(
                    accContainer, AccessoryInventory.ACCESSORY_SLOTS + i,
                    IronholdInventoryLayout.VANITY_DOCK_SLOT_X,
                    IronholdInventoryLayout.VANITY_DOCK_SLOT_Y[i],
                    order[i]));
        }
    }

    /**
     * Vanilla {@link InventoryMenu#quickMoveStack} only knows about slots 0–45, so shift-click
     * from the main inventory sends items to the hotbar — never into accessory/vanity slots.
     * Intercept player-inv + hotbar moves and try those destinations first.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ironhold$quickMoveIntoAccessorySlots(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < InventoryMenu.INV_SLOT_START || index >= InventoryMenu.USE_ROW_SLOT_END) {
            return;
        }
        InventoryMenu menu = (InventoryMenu) (Object) this;
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem()) {
            return;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (current.getItem() instanceof AccessoryItem) {
            if (this.moveItemStackTo(
                    current, IronholdInventoryLayout.ACCESSORY_SLOT_FIRST, IronholdInventoryLayout.ACCESSORY_SLOT_END, false)) {
                ironhold$finishQuickMove(slot, player, current, original, cir);
            }
            return;
        }

        Equippable eq = current.get(DataComponents.EQUIPPABLE);
        if (eq != null) {
            // Favour the real armor slot first; only fall back to the vanity slot
            // if the matching armor slot is occupied (or the item can't go there).
            int armor = ironhold$armorSlotIndex(eq.slot());
            if (armor >= 0 && this.moveItemStackTo(current, armor, armor + 1, false)) {
                ironhold$finishQuickMove(slot, player, current, original, cir);
                return;
            }
            int v = ironhold$vanitySlotIndex(eq.slot());
            if (v >= 0 && this.moveItemStackTo(current, v, v + 1, false)) {
                ironhold$finishQuickMove(slot, player, current, original, cir);
            }
        }
    }

    /** Vanilla {@link InventoryMenu} armor slot indices: 5=HEAD, 6=CHEST, 7=LEGS, 8=FEET. */
    private static int ironhold$armorSlotIndex(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    private static int ironhold$vanitySlotIndex(EquipmentSlot slot) {
        int base = IronholdInventoryLayout.ACCESSORY_SLOT_END;
        return switch (slot) {
            case HEAD -> base;
            case CHEST -> base + 1;
            case LEGS -> base + 2;
            case FEET -> base + 3;
            default -> -1;
        };
    }

    private static void ironhold$finishQuickMove(
            Slot slot, Player player, ItemStack current, ItemStack originalCopy, CallbackInfoReturnable<ItemStack> cir) {
        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, originalCopy);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == originalCopy.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, current);
        cir.setReturnValue(originalCopy);
    }
}
