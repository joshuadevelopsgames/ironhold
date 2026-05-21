package kingdom.smp.mixin;

import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The survival inventory is nudged up by {@link IronholdInventoryLayout#GUI_SHIFT_UP}, but the
 * recipe book panel positions itself by screen height, so it ended up lower than the menu. When
 * the active screen is the inventory, shift the whole book up by the same amount — background,
 * page, search box and filter (all derive from {@code getYOrigin}) plus the category tabs (which
 * compute their own y). Other recipe-book screens (crafting table, etc.) are untouched.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow private int height;
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;

    private static boolean ironhold$inInventory() {
        return Minecraft.getInstance().screen instanceof InventoryScreen;
    }

    @Inject(method = "getYOrigin", at = @At("HEAD"), cancellable = true)
    private void ironhold$shiftYOrigin(CallbackInfoReturnable<Integer> cir) {
        if (ironhold$inInventory()) {
            cir.setReturnValue((this.height - 166) / 2 - IronholdInventoryLayout.GUI_SHIFT_UP);
        }
    }

    @Inject(method = "updateTabs", at = @At("TAIL"))
    private void ironhold$shiftTabs(boolean isFiltering, CallbackInfo ci) {
        if (ironhold$inInventory()) {
            for (RecipeBookTabButton button : this.tabButtons) {
                button.setY(button.getY() - IronholdInventoryLayout.GUI_SHIFT_UP);
            }
        }
    }
}
