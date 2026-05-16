package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.inventory.ChestLayout;
import kingdom.smp.inventory.EnderChestLayout;
import kingdom.smp.inventory.LargeChestLayout;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repositions slots for 3-row {@link ChestMenu}s (single chests + barrels).
 * Other row counts (1/2/4/5/6) keep vanilla layouts.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuLayoutMixin extends AbstractContainerMenu {

    protected ChestMenuLayoutMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At("RETURN"))
    private void ironhold$applyLayout(MenuType<?> type, int id, Inventory inv,
                                       Container container, int rows, CallbackInfo ci) {
        if (rows == 3) {
            // Ender chest is also 3 rows but uses a different texture/layout.
            if (container instanceof PlayerEnderChestContainer) {
                ironhold$applyEnderChest();
            } else {
                ironhold$applyThreeRow();
            }
        } else if (rows == 6) {
            ironhold$applySixRow();
        }
    }

    private void ironhold$applyEnderChest() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(row * 9 + col,
                        EnderChestLayout.COL_X[col], EnderChestLayout.CHEST_ROW_Y[row]);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(27 + row * 9 + col,
                        EnderChestLayout.COL_X[col], EnderChestLayout.INV_ROW_Y[row]);
            }
        }
        for (int col = 0; col < 9; col++) {
            ironhold$move(54 + col, EnderChestLayout.COL_X[col], EnderChestLayout.HOTBAR_Y);
        }
    }

    private void ironhold$applyThreeRow() {
        // 0–26: chest grid (3×9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(row * 9 + col,
                        ChestLayout.COL_X[col], ChestLayout.CHEST_ROW_Y[row]);
            }
        }
        // 27–53: 3×9 inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(27 + row * 9 + col,
                        ChestLayout.COL_X[col], ChestLayout.INV_ROW_Y[row]);
            }
        }
        // 54–62: hotbar
        for (int col = 0; col < 9; col++) {
            ironhold$move(54 + col, ChestLayout.COL_X[col], ChestLayout.HOTBAR_Y);
        }
    }

    private void ironhold$applySixRow() {
        // 0–53: chest grid (6×9)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(row * 9 + col,
                        LargeChestLayout.COL_X[col], LargeChestLayout.CHEST_ROW_Y[row]);
            }
        }
        // 54–80: 3×9 inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ironhold$move(54 + row * 9 + col,
                        LargeChestLayout.COL_X[col], LargeChestLayout.INV_ROW_Y[row]);
            }
        }
        // 81–89: hotbar
        for (int col = 0; col < 9; col++) {
            ironhold$move(81 + col, LargeChestLayout.COL_X[col], LargeChestLayout.HOTBAR_Y);
        }
    }

    private void ironhold$move(int index, int x, int y) {
        Slot slot = this.slots.get(index);
        SlotAccessor acc = (SlotAccessor) slot;
        acc.ironhold$setX(x);
        acc.ironhold$setY(y);
    }
}
