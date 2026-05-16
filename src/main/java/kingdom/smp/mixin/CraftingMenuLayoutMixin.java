package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.inventory.CraftingTableLayout;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repositions vanilla crafting-table slots to the Ironhold layout
 * (see {@link CraftingTableLayout}). Quality propagation is handled
 * separately by {@link CraftingMenuMixin}.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuLayoutMixin extends AbstractContainerMenu {

    protected CraftingMenuLayoutMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN"))
    private void ironhold$repositionCraftingSlots(int containerId,
                                                  net.minecraft.world.entity.player.Inventory inv,
                                                  net.minecraft.world.inventory.ContainerLevelAccess access,
                                                  CallbackInfo ci) {
        // 0: result
        ironhold$move(0, CraftingTableLayout.CRAFT_RESULT_X, CraftingTableLayout.CRAFT_RESULT_Y);

        // 1–9: 3×3 crafting grid (row-major)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ironhold$move(1 + row * 3 + col,
                        CraftingTableLayout.CRAFT_GRID_X0 + col * CraftingTableLayout.CRAFT_GRID_PITCH,
                        CraftingTableLayout.CRAFT_GRID_Y0 + row * CraftingTableLayout.CRAFT_GRID_PITCH);
            }
        }

        // 10–36: 3×9 inventory (per-row y, per-col x)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(10 + row * 9 + col,
                        CraftingTableLayout.INV_COL_X[col],
                        CraftingTableLayout.INV_ROW_Y[row]);
            }
        }

        // 37–45: hotbar (same column x as the inv grid)
        for (int col = 0; col < 9; col++) {
            ironhold$move(37 + col,
                    CraftingTableLayout.INV_COL_X[col],
                    CraftingTableLayout.HOTBAR_Y);
        }
    }

    private void ironhold$move(int index, int x, int y) {
        Slot slot = this.slots.get(index);
        SlotAccessor acc = (SlotAccessor) slot;
        acc.ironhold$setX(x);
        acc.ironhold$setY(y);
    }
}
