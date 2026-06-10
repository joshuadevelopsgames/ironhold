package kingdom.smp.fishing;

/**
 * Tunable bait parameters for a single bait item, modeled on Terraria's bait
 * system. {@code power} is a percentage (Terraria uses 5–50% for normal bait);
 * higher power means better catch quality, faster bites, and a lower chance the
 * bait is consumed on a successful catch.
 *
 * @param power   bait power as a percentage (e.g. 25 = 25%). Clamped to ≥ 1 so a
 *                misconfigured bait can never have zero effect.
 * @param themeId optional themed-loot key (e.g. {@code "nether"}, {@code "end"})
 *                that biases the catch toward dimension-appropriate loot, or
 *                {@code null} for no theme.
 */
public record BaitProfile(int power, String themeId) {
    public BaitProfile {
        if (power < 1) power = 1;
    }

    /** Convenience for plain bait with no loot theme. */
    public BaitProfile(int power) {
        this(power, null);
    }
}
