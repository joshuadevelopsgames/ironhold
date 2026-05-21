package kingdom.smp.mine;

import kingdom.smp.gear.ItemQuality;
import net.minecraft.util.RandomSource;

/**
 * Geographic zones that determine the quality distribution of broken-ore drops.
 *
 * Two zones in the v2 worldgen model: WILD (everywhere outside a mine structure)
 * and MINE_* depth bands inside a generated mine. Wild guarantees Minecraft is
 * still solo-playable — Good is rare (~5%) and Mint is very rare (~1%), so
 * vanilla mining still yields the occasional jackpot but the high tiers remain
 * a mine-zone reward. Mine depth bands collapse the original spec §5 four-tier
 * table (Wild/Claimed/Deep/Royal) into "deeper inside the same shaft = better,"
 * which retires open-question §11.3.
 *
 * Weights are integers summing to 100 (POOR, FINE, GOOD, MINT) so a single
 * 0–99 roll is enough to pick a tier.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §5</a>
 */
public enum MineGeography {
    /** Default for any ore broken outside a mine structure. Good is rare, Mint is very rare. */
    WILD(64, 30, 5, 1),
    /** Upper band of a mine — visible from the entrance. */
    MINE_SHALLOW(25, 50, 25, 0),
    /** Mid band — Mint becomes reachable. */
    MINE_MID(10, 35, 50, 5),
    /** Deep band — Mint is the modal outcome. */
    MINE_DEEP(5, 20, 50, 25);

    private final int[] weights;

    MineGeography(int poor, int fine, int good, int mint) {
        if (poor + fine + good + mint != 100) {
            throw new IllegalArgumentException(name() + " weights must sum to 100");
        }
        this.weights = new int[]{poor, fine, good, mint};
    }

    public ItemQuality rollQuality(RandomSource random) {
        int n = random.nextInt(100);
        int acc = 0;
        ItemQuality[] tiers = ItemQuality.values();
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (n < acc) return tiers[i];
        }
        return ItemQuality.GOOD;
    }
}
