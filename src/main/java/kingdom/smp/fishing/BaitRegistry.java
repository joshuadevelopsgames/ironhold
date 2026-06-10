package kingdom.smp.fishing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry + lookup for fishing bait, modeled on Terraria's bait system.
 *
 * <p>An item counts as bait if it carries the {@code #ironhold:fishing_bait} item
 * tag. Its {@link BaitProfile} (power + optional loot theme) comes from
 * {@link #PROFILES}; items that are tagged but not explicitly registered fall
 * back to {@link #DEFAULT} so a new bait can never silently have zero power.
 *
 * <p>Jarred butterfly items register their profiles here as they are added
 * (Phase 2/3); see the {@code project_butterflies_bait} memory for the power
 * ladder. Until then the tag holds a temporary placeholder bait for testing.
 *
 * <p>The strict "no bait, no bites" gate lives in {@code FishingHookBiteMixin};
 * catch-quality and consumption are applied in {@link FishingMinigameManager}.
 */
public final class BaitRegistry {
    private BaitRegistry() {}

    /** Items usable as fishing bait. */
    public static final TagKey<Item> FISHING_BAIT = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("ironhold", "fishing_bait"));

    /** Fallback for tagged-but-unregistered bait — entry-tier power, no theme. */
    public static final BaitProfile DEFAULT = new BaitProfile(10);

    private static final Map<Item, BaitProfile> PROFILES = new HashMap<>();

    /** Register a bait item's profile. Called from item registration (Phase 2/3). */
    public static void register(Item item, BaitProfile profile) {
        PROFILES.put(item, profile);
    }

    public static boolean isBait(ItemStack stack) {
        return !stack.isEmpty() && stack.is(FISHING_BAIT);
    }

    /** Profile for a bait stack — caller should have checked {@link #isBait}. */
    public static BaitProfile profileFor(ItemStack stack) {
        return PROFILES.getOrDefault(stack.getItem(), DEFAULT);
    }

    /** True if the player has any bait anywhere in their inventory. */
    public static boolean hasBait(Player player) {
        return findBest(player) != null;
    }

    /**
     * The highest-power bait in the player's inventory, or {@code null} if they
     * have none. Mirrors Terraria, which always spends the strongest bait first.
     */
    public static BestBait findBest(Player player) {
        BestBait best = null;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!isBait(stack)) continue;
            BaitProfile profile = profileFor(stack);
            if (best == null || profile.power() > best.profile().power()) {
                best = new BestBait(stack.getItem(), profile);
            }
        }
        return best;
    }

    /**
     * Consume one unit of {@code baitItem} from the player's inventory.
     *
     * @return true if a unit was removed
     */
    public static boolean consumeOne(Player player, Item baitItem) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == baitItem && isBait(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * Terraria's bait-consumption check: on a successful catch, bait is consumed
     * with probability {@code 1 / (1 + power/6)}. Higher power lasts longer
     * (50% power ≈ 1-in-9.3, so ~10 catches on average).
     */
    public static boolean rollConsume(RandomSource random, int power) {
        double chance = 1.0 / (1.0 + power / 6.0);
        return random.nextDouble() < chance;
    }

    /** The chosen bait for a fishing session: item type (for consumption) + profile. */
    public record BestBait(Item item, BaitProfile profile) {
        public int power() { return profile.power(); }

        public String themeId() { return profile.themeId(); }
    }
}
