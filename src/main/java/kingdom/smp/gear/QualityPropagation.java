package kingdom.smp.gear;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Quality propagation algorithm for crafting and smelting.
 *
 * <p><b>Rule:</b> the result inherits a quality biased toward the *average* of the eligible
 * inputs, with a small "weakest-link drag" pulling the result toward the lowest input.
 * Pure batches preserve their tier exactly; mixed batches degrade gracefully.
 *
 * <pre>
 * avg      = mean of eligible-input multipliers
 * min      = lowest eligible-input multiplier
 * weighted = avg − (avg − min) × {@value #WEAKEST_LINK_DRAG}
 * result   = tier whose multiplier is closest to weighted
 * </pre>
 *
 * <p>Worked examples (see spec §6.1 for full table):
 * <ul>
 *   <li>8× Mint → Mint (pure batch)</li>
 *   <li>7× Mint + 1× Poor → Good (one bad ingot drops 1 tier)</li>
 *   <li>4× Mint + 4× Poor → Fine (heavy contamination drops 2)</li>
 *   <li>1× Mint + 7× Poor → Poor (mostly Poor → Poor)</li>
 * </ul>
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §6.1</a>
 */
public final class QualityPropagation {
    private QualityPropagation() {}

    /** How strongly the lowest-quality input drags the result down. 0 = pure average, 1 = lowest wins. */
    public static final float WEAKEST_LINK_DRAG = 0.25f;

    /**
     * Compute the result quality for a craft from the given inputs. Inputs that aren't
     * quality-eligible (sticks, planks, dyes, etc.) are ignored.
     *
     * @return the computed result quality, or {@link ItemQuality#defaultQuality()} if no
     *         eligible inputs are present (no quality tag should be applied to the result).
     */
    public static ItemQuality computeResultQuality(Container inputs) {
        float sum = 0f;
        float min = Float.MAX_VALUE;
        int count = 0;
        int size = inputs.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inputs.getItem(i);
            if (stack.isEmpty() || !QualityScope.isEligible(stack)) continue;
            float m = GearComponents.getQuality(stack).durabilityMultiplier();
            sum += m;
            if (m < min) min = m;
            count++;
        }
        if (count == 0) return ItemQuality.defaultQuality();
        float avg = sum / count;
        float weighted = avg - (avg - min) * WEAKEST_LINK_DRAG;
        return nearestTier(weighted);
    }

    /** Find the tier whose multiplier is closest to the given target. */
    public static ItemQuality nearestTier(float target) {
        ItemQuality nearest = ItemQuality.defaultQuality();
        float bestDelta = Float.MAX_VALUE;
        for (ItemQuality q : ItemQuality.values()) {
            float delta = Math.abs(q.durabilityMultiplier() - target);
            if (delta < bestDelta) {
                bestDelta = delta;
                nearest = q;
            }
        }
        return nearest;
    }
}
