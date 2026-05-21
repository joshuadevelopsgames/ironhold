package kingdom.smp.food;

import kingdom.smp.ModAttachments;
import kingdom.smp.skill.SkillEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side recipe gating + ingredient consumption. The whole point of Phase 0: validate
 * that knowledge gate + rank gate + ingredient check all flow cleanly before we invest in
 * porting Farmer's Delight content.
 *
 * <p>Failure modes are distinct {@link Status} values so the caller (debug command in Phase 0,
 * cooking-pot block UI in Phase 1+, NPC tutor in Phase 2+) can render its own affordance.
 */
public final class CookingService {
    private CookingService() {}

    public enum Status { SUCCESS, UNKNOWN_RECIPE, NOT_LEARNED, RANK_TOO_LOW, MISSING_INGREDIENTS }

    public record Result(Status status, Component message) {}

    public static Result tryCook(ServerPlayer player, Identifier recipeId) {
        CookingRecipe recipe = CookingRecipes.get(recipeId);
        if (recipe == null) {
            return new Result(Status.UNKNOWN_RECIPE,
                    Component.literal("No such recipe: " + recipeId));
        }

        KnownRecipes known = player.getData(ModAttachments.KNOWN_RECIPES.get());
        if (!known.knows(recipeId)) {
            return new Result(Status.NOT_LEARNED,
                    Component.literal("Unknown Recipe — you haven't learned this yet."));
        }

        if (!SkillEffects.hasAtLeast(player, recipe.profession(), recipe.requiredRank())) {
            return new Result(Status.RANK_TOO_LOW,
                    Component.literal("Requires " + recipe.profession().displayName()
                            + " " + recipe.requiredRank().displayName()));
        }

        Inventory inv = player.getInventory();
        List<int[]> consumePlan = new ArrayList<>();
        for (CookingRecipe.Ingredient need : recipe.ingredients()) {
            int remaining = need.count();
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty() || !stack.is(need.item().get())) continue;
                int take = Math.min(remaining, stack.getCount());
                consumePlan.add(new int[]{ slot, take });
                remaining -= take;
            }
            if (remaining > 0) {
                return new Result(Status.MISSING_INGREDIENTS,
                        Component.literal("Missing ingredient: "
                                + Component.translatable(need.item().get().getDescriptionId()).getString()
                                + " x" + need.count()));
            }
        }

        for (int[] entry : consumePlan) {
            inv.removeItem(entry[0], entry[1]);
        }
        ItemStack output = recipe.output().get();
        if (!inv.add(output.copy())) {
            player.drop(output, false);
        }
        return new Result(Status.SUCCESS,
                Component.literal("Cooked " + output.getHoverName().getString()));
    }

    /** Mark a recipe as known for the player. Idempotent. Returns true if newly learned. */
    public static boolean learn(ServerPlayer player, Identifier recipeId) {
        KnownRecipes known = player.getData(ModAttachments.KNOWN_RECIPES.get());
        if (known.knows(recipeId)) return false;
        player.setData(ModAttachments.KNOWN_RECIPES.get(), known.withLearned(recipeId));
        return true;
    }
}
