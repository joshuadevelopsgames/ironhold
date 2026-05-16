package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.inventory.FurnaceLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repositions vanilla furnace slots to the Ironhold layout (see {@link FurnaceLayout}).
 * Targets {@link FurnaceMenu} (regular furnace only) — blast furnace + smoker
 * keep vanilla layouts.
 */
@Mixin(FurnaceMenu.class)
public abstract class FurnaceMenuLayoutMixin extends AbstractContainerMenu {

    protected FurnaceMenuLayoutMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("RETURN"))
    private void ironhold$layoutSimple(int id, Inventory inv, CallbackInfo ci) {
        ironhold$applyLayout();
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("RETURN"))
    private void ironhold$layoutFull(int id, Inventory inv,
                                      net.minecraft.world.Container container,
                                      net.minecraft.world.inventory.ContainerData data,
                                      CallbackInfo ci) {
        ironhold$applyLayout();
    }

    private void ironhold$applyLayout() {
        ironhold$move(0, FurnaceLayout.INPUT_X,  FurnaceLayout.INPUT_Y);   // input
        ironhold$move(1, FurnaceLayout.FUEL_X,   FurnaceLayout.FUEL_Y);    // fuel
        ironhold$move(2, FurnaceLayout.RESULT_X, FurnaceLayout.RESULT_Y);  // result

        // 3–29: 3×9 inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(3 + row * 9 + col,
                        FurnaceLayout.INV_COL_X[col],
                        FurnaceLayout.INV_ROW_Y[row]);
            }
        }

        // 30–38: hotbar
        for (int col = 0; col < 9; col++) {
            ironhold$move(30 + col, FurnaceLayout.INV_COL_X[col], FurnaceLayout.HOTBAR_Y);
        }
    }

    private void ironhold$move(int index, int x, int y) {
        Slot slot = this.slots.get(index);
        SlotAccessor acc = (SlotAccessor) slot;
        acc.ironhold$setX(x);
        acc.ironhold$setY(y);
    }
}
