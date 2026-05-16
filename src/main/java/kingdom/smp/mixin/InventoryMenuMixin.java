package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

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
        ironhold$repositionVanillaSlots();
        ironhold$addAccessoryAndVanitySlots(player);
    }

    /** Move vanilla slots 0–45 to the Ironhold layout positions. */
    private void ironhold$repositionVanillaSlots() {
        // 0: craft result
        ironhold$move(0, IronholdInventoryLayout.CRAFT_RESULT_X, IronholdInventoryLayout.CRAFT_RESULT_Y);

        // 1–4: 2×2 craft grid (TL, TR, BL, BR)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int idx = 1 + row * 2 + col;
                ironhold$move(idx,
                        IronholdInventoryLayout.CRAFT_GRID_X0 + col * IronholdInventoryLayout.CRAFT_GRID_PITCH,
                        IronholdInventoryLayout.CRAFT_GRID_Y0 + row * IronholdInventoryLayout.CRAFT_GRID_PITCH);
            }
        }

        // 5–8: armor HEAD, CHEST, LEGS, FEET (per-slot y; not uniform pitch)
        for (int i = 0; i < 4; i++) {
            ironhold$move(5 + i,
                    IronholdInventoryLayout.ARMOR_X,
                    IronholdInventoryLayout.ARMOR_Y[i]);
        }

        // 9–35: 3×9 inventory (per-row y, per-col x — non-uniform drag-tuned pitch)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(9 + row * 9 + col,
                        IronholdInventoryLayout.INV_COL_X[col],
                        IronholdInventoryLayout.INV_ROW_Y[row]);
            }
        }

        // 36–44: hotbar (same column x as the inventory grid)
        for (int col = 0; col < 9; col++) {
            ironhold$move(36 + col,
                    IronholdInventoryLayout.INV_COL_X[col],
                    IronholdInventoryLayout.HOTBAR_Y);
        }

        // 45: offhand
        ironhold$move(45, IronholdInventoryLayout.OFFHAND_X, IronholdInventoryLayout.OFFHAND_Y);
    }

    private void ironhold$move(int index, int x, int y) {
        Slot slot = this.slots.get(index);
        SlotAccessor acc = (SlotAccessor) slot;
        acc.ironhold$setX(x);
        acc.ironhold$setY(y);
    }

    private void ironhold$addAccessoryAndVanitySlots(Player player) {
        // Delegate to getData(ACCESSORY_INV) so ticks and menus share one attachment instance
        var accContainer = new AccessoryInventoryAttachmentContainer(player);

        // 5 accessory slots — bottom bar (slots 46–50)
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            this.addSlot(new AccessorySlot(
                    accContainer, i,
                    IronholdInventoryLayout.ACCESSORY_X[i],
                    IronholdInventoryLayout.ACCESSORY_Y));
        }

        // 4 vanity slots — left vanity panel (slots 51–54)
        EquipmentSlot[] order = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < AccessoryInventory.VANITY_SLOTS; i++) {
            this.addSlot(new VanitySlot(
                    accContainer, AccessoryInventory.ACCESSORY_SLOTS + i,
                    IronholdInventoryLayout.VANITY_SLOT_X,
                    IronholdInventoryLayout.VANITY_PANEL_OFFSET_Y + IronholdInventoryLayout.VANITY_SLOT_Y_LOCAL[i],
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
