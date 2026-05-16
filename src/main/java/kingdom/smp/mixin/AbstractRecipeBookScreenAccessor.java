package kingdom.smp.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code recipeBookComponent} field on {@link AbstractRecipeBookScreen}
 * so the Ironhold inventory can hide it (Ironhold drops the vanilla recipe book in favor of
 * companion mods like JEI/REI/EMI).
 */
@Mixin(AbstractRecipeBookScreen.class)
public interface AbstractRecipeBookScreenAccessor {

    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> ironhold$getRecipeBookComponent();
}
