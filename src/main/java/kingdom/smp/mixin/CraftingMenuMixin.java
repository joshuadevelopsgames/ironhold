package kingdom.smp.mixin;

import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.gear.QualityPropagation;
import kingdom.smp.gear.QualityScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apply quality to the crafting result at the moment the result is computed, so the preview
 * slot shows the actual quality the player will receive (not a vanilla placeholder that gets
 * upgraded only when the player takes the item).
 *
 * Hook: {@link CraftingMenu#slotChangedCraftingGrid}, the static helper called by both the
 * standalone crafting table and the inventory 2×2 grid whenever an input slot changes.
 *
 * Quality computed via {@link QualityPropagation#computeResultQuality} (weakest-link drag
 * algorithm — see spec §6.1).
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §6.1</a>
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void ironhold$applyQualityToResult(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci) {
        ItemStack result = resultContainer.getItem(0);
        if (result.isEmpty() || !QualityScope.isEligible(result)) return;

        ItemQuality computed = QualityPropagation.computeResultQuality(craftingContainer);
        // No eligible inputs (rare — most gear recipes use ingots) → leave result alone.
        if (computed == ItemQuality.defaultQuality()) return;

        GearComponents.setQuality(result, computed);
        // Re-set the slot to mark it changed so the new quality syncs to the client preview.
        resultContainer.setItem(0, result);
    }
}
