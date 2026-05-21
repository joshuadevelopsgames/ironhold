package kingdom.smp.food;

import kingdom.smp.Ironhold;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 0 placeholder recipe registry. Hand-registered in code so we can stress-test the
 * gating UX before investing in a Farmer's Delight port. Real recipes will move to datapack
 * JSON once the cooking-pot block lands.
 *
 * <p>The single Phase 0 recipe is <b>Forest Stew</b>: vanilla mushrooms + bowl produce a
 * vanilla mushroom stew. Picked because all ingredients exist in vanilla MC 1.26.x, so we
 * don't have to register placeholder ingredient items just to validate the gating loop.
 */
public final class CookingRecipes {
    private CookingRecipes() {}

    public static final Identifier FOREST_STEW =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "recipe/forest_stew");

    private static final Map<Identifier, CookingRecipe> RECIPES = new LinkedHashMap<>();

    static {
        register(new CookingRecipe(
                FOREST_STEW,
                List.of(
                        new CookingRecipe.Ingredient(() -> Items.RED_MUSHROOM, 1),
                        new CookingRecipe.Ingredient(() -> Items.BROWN_MUSHROOM, 1),
                        new CookingRecipe.Ingredient(() -> Items.BOWL, 1)),
                () -> new ItemStack(Items.MUSHROOM_STEW),
                Profession.COOKING,
                ProfessionRank.NOVICE));
    }

    private static void register(CookingRecipe recipe) {
        RECIPES.put(recipe.id(), recipe);
    }

    public static @Nullable CookingRecipe get(Identifier id) {
        return RECIPES.get(id);
    }

    public static Collection<CookingRecipe> all() {
        return RECIPES.values();
    }
}
