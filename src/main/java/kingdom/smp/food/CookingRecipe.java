package kingdom.smp.food;

import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * A multi-ingredient cooking recipe with two progression gates.
 *
 * <p>Both gates must pass for a player to cook this:
 * <ul>
 *   <li><b>Knowledge gate</b> — the recipe id must be in the player's {@link KnownRecipes}.
 *       Learned by NPC tutors, recipe scrolls, or watching an NPC cook (Phase 1+).</li>
 *   <li><b>Rank gate</b> — the player's rank in {@link #profession()} must be at least
 *       {@link #requiredRank()}. Locked recipes still appear in UI so players see what they
 *       are working toward.</li>
 * </ul>
 *
 * <p>Output is supplied lazily because the result item may be registered via
 * {@link net.neoforged.neoforge.registries.DeferredRegister} and not resolvable at static init.
 */
public record CookingRecipe(
        Identifier id,
        List<Ingredient> ingredients,
        Supplier<ItemStack> output,
        Profession profession,
        ProfessionRank requiredRank) {

    /** One required ingredient — matched by item identity. No tags or NBT in Phase 0. */
    public record Ingredient(Supplier<? extends Item> item, int count) {}
}
