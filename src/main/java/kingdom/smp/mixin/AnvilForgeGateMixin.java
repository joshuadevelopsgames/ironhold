package kingdom.smp.mixin;

import kingdom.smp.blacksmithing.ForgeEligibility;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces blacksmithing repairs through the forge minigame: when the anvil holds
 * damaged reforgeable gear + a valid repair material, the normal repair output
 * is suppressed (cleared + cost zeroed). Vanilla then renders its "no result"
 * error icon (the red X) on the output slot, and the player must click the
 * hammer button to forge instead.
 *
 * <p>Other anvil uses (renaming, enchant-book combines, non-gear items) are
 * untouched. Runs at the tail of {@code createResult}, after vanilla has
 * computed its result, so we only override the forge-repair case.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilForgeGateMixin {

    @Shadow @Final private DataSlot cost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void ironhold$gateForgeRepair(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack gear = self.getSlot(0).getItem();
        ItemStack material = self.getSlot(1).getItem();
        if (ForgeEligibility.isForgeRepair(gear, material)) {
            self.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
        }
    }
}
