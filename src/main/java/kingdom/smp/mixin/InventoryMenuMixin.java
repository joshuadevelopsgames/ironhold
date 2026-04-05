package kingdom.smp.mixin;

import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryInventoryAttachmentContainer;
import kingdom.smp.accessory.AccessoryItem;
import kingdom.smp.inventory.IronholdInventoryLayout;
import kingdom.smp.accessory.AccessorySlot;
import kingdom.smp.accessory.VanitySlot;
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
 * Injects 9 extra slots into the vanilla {@link InventoryMenu}:
 * <ul>
 *   <li>5 accessory slots (row below hotbar)</li>
 *   <li>4 vanity armor slots (left-side island, aligned with real armor)</li>
 * </ul>
 * <p>
 * Slots are backed by the player's {@link AccessoryInventory} attachment,
 * so vanilla container sync handles all client↔server data transfer.
 * <p>
 * Slot indices after injection: see {@link IronholdInventoryLayout#ACCESSORY_SLOT_FIRST} /
 * {@link IronholdInventoryLayout#VANITY_SLOT_END}. If Mojang changes {@link InventoryMenu} layout, update those.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    protected InventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$addAccessorySlots(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        // Delegate to getData(ACCESSORY_INV) so ticks and menus share one attachment instance
        // (raw map put during ctor could orphan data from NeoForge getData()).
        var accContainer = new AccessoryInventoryAttachmentContainer(player);

        // 5 accessory slots — horizontal row under the hotbar (avoids status effect HUD)
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            this.addSlot(new AccessorySlot(
                    accContainer, i,
                    IronholdInventoryLayout.ACCESSORY_SLOT_X0 + i * 18,
                    IronholdInventoryLayout.ACCESSORY_SLOT_Y));
        }

        // 4 vanity slots — left of the vanilla background, aligned with armor column
        EquipmentSlot[] order = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < AccessoryInventory.VANITY_SLOTS; i++) {
            this.addSlot(new VanitySlot(
                    accContainer, AccessoryInventory.ACCESSORY_SLOTS + i,
                    IronholdInventoryLayout.VANITY_SLOT_X,
                    IronholdInventoryLayout.VANITY_SLOT_Y0 + i * 18, order[i]));
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
            int v = ironhold$vanitySlotIndex(eq.slot());
            if (v >= 0 && this.moveItemStackTo(current, v, v + 1, false)) {
                ironhold$finishQuickMove(slot, player, current, original, cir);
            }
        }
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
