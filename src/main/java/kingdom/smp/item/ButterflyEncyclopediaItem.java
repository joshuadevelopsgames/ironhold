package kingdom.smp.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Butterfly Encyclopedia — a field guide that catalogues every butterfly species. Right-click
 * opens a paginated two-page screen showing each species' rarity, location, and favourite
 * flower. Entries are masked until the player has caught that species (see
 * {@link kingdom.smp.entity.ButterflyDex}).
 *
 * <p>The screen is purely client-side; the dex it reads is a synced player attachment, so no
 * container/menu round-trip is needed. {@code use()} only references the client screen inside
 * the {@code isClientSide} branch, so the class is never linked on a dedicated server.
 */
public class ButterflyEncyclopediaItem extends Item {

    public ButterflyEncyclopediaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            kingdom.smp.client.screen.ButterflyEncyclopediaScreen.open();
        }
        return InteractionResult.SUCCESS;
    }
}
