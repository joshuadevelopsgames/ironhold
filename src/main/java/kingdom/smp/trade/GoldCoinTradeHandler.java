package kingdom.smp.trade;

import java.util.Optional;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Re-skins the villager economy from emeralds to gold coins. Vanilla trades are
 * now fully data-driven ({@code VillagerTrade} registry objects), so there is no
 * registration-time hook to edit them — instead we rewrite the generated offers
 * on the merchant the moment the player interacts, before the trade menu opens.
 *
 * Only offers that actually mention an emerald are rebuilt, which keeps the pass
 * idempotent: once an offer is gold-coin denominated it is skipped on every later
 * interaction, so demand/restock/reputation state is preserved untouched.
 */
public final class GoldCoinTradeHandler {

    private GoldCoinTradeHandler() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof AbstractVillager villager)) return;

        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) return;

        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer swapped = swapEmeralds(offers.get(i));
            if (swapped != null) {
                offers.set(i, swapped);
            }
        }
    }

    /** Returns a rebuilt offer with emeralds replaced by gold coins, or null if untouched. */
    private static MerchantOffer swapEmeralds(MerchantOffer offer) {
        ItemCost costA = offer.getItemCostA();
        Optional<ItemCost> costB = offer.getItemCostB();
        ItemStack result = offer.getResult();

        boolean costAHit = isEmerald(costA);
        boolean costBHit = costB.isPresent() && isEmerald(costB.get());
        boolean resultHit = result.is(Items.EMERALD);
        if (!costAHit && !costBHit && !resultHit) return null;

        ItemCost newCostA = costAHit ? toGoldCost(costA.count()) : costA;
        Optional<ItemCost> newCostB = costB.map(c -> isEmerald(c) ? toGoldCost(c.count()) : c);
        ItemStack newResult = resultHit
            ? new ItemStack(kingdom.smp.ModItems.GOLD_COIN.get(), result.getCount())
            : result;

        return new MerchantOffer(
            newCostA,
            newCostB,
            newResult,
            offer.getUses(),
            offer.getMaxUses(),
            offer.getXp(),
            offer.getPriceMultiplier(),
            offer.getDemand()
        );
    }

    private static boolean isEmerald(ItemCost cost) {
        return cost.itemStack().is(Items.EMERALD);
    }

    private static ItemCost toGoldCost(int count) {
        return new ItemCost(kingdom.smp.ModItems.GOLD_COIN.get(), count);
    }
}
