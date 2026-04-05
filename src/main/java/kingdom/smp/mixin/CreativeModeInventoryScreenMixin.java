package kingdom.smp.mixin;

import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Creative "inventory" tab wraps every {@link net.minecraft.world.inventory.InventoryMenu} slot.
 * Extra slots (accessory + vanity) miss vanilla's layout branches and render on the hotbar row.
 * Move them off-screen like vanilla does for crafting slots.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @Redirect(
            method = "selectTab(Lnet/minecraft/world/item/CreativeModeTab;)V",
            at =
                    @At(
                            value = "NEW",
                            target =
                                    "(Lnet/minecraft/world/inventory/Slot;III)Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;"))
    private CreativeModeInventoryScreen.SlotWrapper ironhold$hideExtraSlotsInCreativeInventoryTab(
            Slot target, int index, int x, int y) {
        if (index >= IronholdInventoryLayout.ACCESSORY_SLOT_FIRST
                && index < IronholdInventoryLayout.VANITY_SLOT_END) {
            return new CreativeModeInventoryScreen.SlotWrapper(target, index, -2000, -2000);
        }
        return new CreativeModeInventoryScreen.SlotWrapper(target, index, x, y);
    }
}
