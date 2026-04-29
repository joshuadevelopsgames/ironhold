package kingdom.smp.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

/**
 * Five condition stages derived from the durability bar ratio. Not stored — computed on demand
 * from {@link ItemStack#getDamageValue()} / {@link ItemStack#getMaxDamage()}.
 *
 * Pristine is sticky: it requires explicit refinement and drops to Worn at &lt;90% durability,
 * but does NOT drop on first combat hit. The "isPristine" flag is the gate that's tracked
 * separately (a Pristine bit on the item); the *display* condition is what this class returns.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §4</a>
 */
public enum ItemCondition {
    PRISTINE(1.0f, 0.90f, 0.03f, 1, ChatFormatting.GOLD),
    WORN(0.899f, 0.60f, 0.0f, 0, ChatFormatting.WHITE),
    DAMAGED(0.599f, 0.30f, -0.08f, -1, ChatFormatting.YELLOW),
    BATTERED(0.299f, 0.10f, -0.20f, -2, ChatFormatting.RED),
    BROKEN(0.099f, 0.0f, -0.60f, -99, ChatFormatting.DARK_RED);

    private final float maxRatio;
    private final float minRatio;
    private final float protectionModifier;
    private final int toughnessModifier;
    private final ChatFormatting tooltipColor;

    ItemCondition(float maxRatio, float minRatio, float protectionModifier,
                  int toughnessModifier, ChatFormatting tooltipColor) {
        this.maxRatio = maxRatio;
        this.minRatio = minRatio;
        this.protectionModifier = protectionModifier;
        this.toughnessModifier = toughnessModifier;
        this.tooltipColor = tooltipColor;
    }

    public float protectionModifier() { return protectionModifier; }
    public int toughnessModifier() { return toughnessModifier; }
    public ChatFormatting tooltipColor() { return tooltipColor; }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    /**
     * Compute condition from a stack's current damage. Returns {@link #WORN} for items that
     * are not damageable. Pristine is only returned if the item has the Pristine flag set
     * AND damage is &lt;= 10% — which is checked here against the flag.
     */
    public static ItemCondition fromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return WORN;
        int max = stack.getMaxDamage();
        if (max <= 0) return WORN;
        float ratio = 1.0f - ((float) stack.getDamageValue() / (float) max);
        return fromRatio(ratio, GearComponents.isPristine(stack));
    }

    /**
     * @param ratio        durability ratio in [0.0, 1.0] where 1.0 = full durability.
     * @param pristineFlag whether the item has been refined to Pristine state.
     */
    public static ItemCondition fromRatio(float ratio, boolean pristineFlag) {
        if (ratio >= 0.90f && pristineFlag) return PRISTINE;
        if (ratio >= 0.60f) return WORN;
        if (ratio >= 0.30f) return DAMAGED;
        if (ratio >= 0.10f) return BATTERED;
        return BROKEN;
    }
}
