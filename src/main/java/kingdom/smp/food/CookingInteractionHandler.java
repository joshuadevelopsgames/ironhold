package kingdom.smp.food;

import kingdom.smp.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Phase 0.5 cooking interaction — right-click a lit campfire (with an empty hand) to
 * cook anything you know a recipe for and have the ingredients for. Stand-in for the
 * not-yet-ported Farmer's Delight cooking pot.
 *
 * <p>Empty-hand-only so we don't conflict with vanilla "place food on campfire grid"
 * behavior (which fires when the player is holding a campfire-cookable food item).
 *
 * <p>Failure-priority for the message shown when no recipe succeeds:
 * MISSING_INGREDIENTS &gt; RANK_TOO_LOW &gt; NOT_LEARNED — the more actionable problem wins.
 */
public final class CookingInteractionHandler {
    private CookingInteractionHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCampfireRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.isEmpty()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CampfireBlock)) return;
        if (!state.getValue(CampfireBlock.LIT)) {
            player.sendSystemMessage(Component.literal("The fire is out."));
            event.setCanceled(true);
            return;
        }

        KnownRecipes known = player.getData(ModAttachments.KNOWN_RECIPES.get());
        if (known.learned().isEmpty()) {
            player.sendSystemMessage(
                Component.literal("You don't know any cooking recipes yet."));
            event.setCanceled(true);
            return;
        }

        CookingService.Result best = null;
        for (Identifier recipeId : known.learned()) {
            CookingService.Result result = CookingService.tryCook(player, recipeId);
            if (result.status() == CookingService.Status.SUCCESS) {
                player.sendSystemMessage(result.message());
                event.setCanceled(true);
                return;
            }
            best = pickMoreActionable(best, result);
        }

        if (best != null) {
            player.sendSystemMessage(best.message());
        }
        event.setCanceled(true);
    }

    private static CookingService.Result pickMoreActionable(
            CookingService.Result a, CookingService.Result b) {
        if (a == null) return b;
        if (b == null) return a;
        return actionability(b.status()) > actionability(a.status()) ? b : a;
    }

    private static int actionability(CookingService.Status s) {
        return switch (s) {
            case SUCCESS -> 4;
            case MISSING_INGREDIENTS -> 3;
            case RANK_TOO_LOW -> 2;
            case NOT_LEARNED -> 1;
            case UNKNOWN_RECIPE -> 0;
        };
    }
}
