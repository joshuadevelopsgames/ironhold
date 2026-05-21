package kingdom.smp.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

/**
 * Five condition stages derived purely from the durability bar ratio
 * (current / max). Not stored — computed on demand from
 * {@link ItemStack#getDamageValue()} / {@link ItemStack#getMaxDamage()}.
 *
 * <pre>
 * durability 80–100%  → Pristine               (green)   fully functional, only slight wear
 * durability 60–79%   → Moderately Damaged     (yellow)  functional, lost a chunk of usage
 * durability 40–59%   → Significantly Damaged  (orange)  obvious wear, performance compromised
 * durability 20–39%   → Critical               (dark red) on the verge of breaking
 * durability  0–19%   → Broken                 (purple)  effectively unusable until repaired
 * </pre>
 *
 * <p>Top tier is reached by durability alone — no separate "refinement" flag.
 */
public enum ItemCondition {
    PRISTINE("Pristine", 0.80f, 0.00f, 0, ChatFormatting.GREEN),
    MODERATELY_DAMAGED("Moderately Damaged", 0.60f, -0.05f, -1, ChatFormatting.YELLOW),
    SIGNIFICANTLY_DAMAGED("Significantly Damaged", 0.40f, -0.12f, -1, ChatFormatting.GOLD),
    CRITICAL("Critical", 0.20f, -0.30f, -2, ChatFormatting.DARK_RED),
    BROKEN("Broken", 0.0f, -0.60f, -99, ChatFormatting.DARK_PURPLE);

    /** Minimum durability ratio (inclusive) for this tier. */
    private final float minRatio;
    private final String displayName;
    private final float protectionModifier;
    private final int toughnessModifier;
    private final ChatFormatting tooltipColor;

    ItemCondition(String displayName, float minRatio, float protectionModifier,
                  int toughnessModifier, ChatFormatting tooltipColor) {
        this.displayName = displayName;
        this.minRatio = minRatio;
        this.protectionModifier = protectionModifier;
        this.toughnessModifier = toughnessModifier;
        this.tooltipColor = tooltipColor;
    }

    public float protectionModifier() { return protectionModifier; }
    public int toughnessModifier() { return toughnessModifier; }
    public ChatFormatting tooltipColor() { return tooltipColor; }
    public String displayName() { return displayName; }

    /**
     * Compute condition from a stack's current damage. Non-damageable items
     * report {@link #PRISTINE} (no penalty).
     */
    public static ItemCondition fromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return PRISTINE;
        int max = stack.getMaxDamage();
        if (max <= 0) return PRISTINE;
        float ratio = 1.0f - ((float) stack.getDamageValue() / (float) max);
        return fromRatio(ratio);
    }

    /** @param ratio durability ratio in [0.0, 1.0] where 1.0 = full durability. */
    public static ItemCondition fromRatio(float ratio) {
        if (ratio >= PRISTINE.minRatio) return PRISTINE;
        if (ratio >= MODERATELY_DAMAGED.minRatio) return MODERATELY_DAMAGED;
        if (ratio >= SIGNIFICANTLY_DAMAGED.minRatio) return SIGNIFICANTLY_DAMAGED;
        if (ratio >= CRITICAL.minRatio) return CRITICAL;
        return BROKEN;
    }
}
