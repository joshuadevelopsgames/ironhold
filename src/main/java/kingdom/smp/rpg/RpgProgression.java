package kingdom.smp.rpg;

/** Class level curve and promotion thresholds. */
public final class RpgProgression {
    private RpgProgression() {}

    // ── Promotion levels — the class level at which each tier can promote ─────
    // Reaching this level marks the current class as "completed" and opens
    // the promotion screen for the next tier.
    public static final int PEASANT_PROMOTION_LEVEL = 5;
    public static final int TIER1_PROMOTION_LEVEL   = 10;
    public static final int TIER2_PROMOTION_LEVEL   = 15;
    public static final int TIER3_PROMOTION_LEVEL   = 20;

    /** Returns the level at which the given class tier can promote, or -1 if no promotion. */
    public static int promotionLevelForTier(int tier) {
        return switch (tier) {
            case 0 -> PEASANT_PROMOTION_LEVEL;
            case 1 -> TIER1_PROMOTION_LEVEL;
            case 2 -> TIER2_PROMOTION_LEVEL;
            case 3 -> TIER3_PROMOTION_LEVEL;
            default -> -1; // Tier 4 is max — no promotion
        };
    }

    /** True if this class level just hit the promotion threshold for the given tier. */
    public static boolean justReachedPromotion(int tier, int oldLevel, int newLevel) {
        int threshold = promotionLevelForTier(tier);
        return threshold > 0 && oldLevel < threshold && newLevel >= threshold;
    }

    /**
     * Tier index from class level: first tier at L5, second at L10, … (L1–L4 → 0).
     * Used for periodic bonuses (e.g. extra stats every 5 levels).
     */
    public static int classTier(int classLevel) {
        if (classLevel < 1) {
            return 0;
        }
        return classLevel / 5;
    }

    public static int xpToReachNextLevel(int currentLevel) {
        if (currentLevel < 1) {
            currentLevel = 1;
        }
        // Slightly softer than +30/level so mid–late game does not crawl as hard vs kill XP.
        return 40 + currentLevel * 26;
    }

    /** Returns updated data after adding class XP (may multi-level). */
    public static PlayerKingdomRpgData addClassXp(PlayerKingdomRpgData in, int amount) {
        if (amount <= 0) {
            return in;
        }
        int level = in.classLevel();
        int xp = in.xpIntoLevel() + amount;
        while (xp >= xpToReachNextLevel(level)) {
            xp -= xpToReachNextLevel(level);
            level++;
            if (level > 999) {
                break;
            }
        }
        return new PlayerKingdomRpgData(in.kingdomIndex(), in.classIndex(), level, xp);
    }

    /** Returns updated data after removing class XP (may de-level, floor at L1 / 0 XP). */
    public static PlayerKingdomRpgData removeClassXp(PlayerKingdomRpgData in, int amount) {
        if (amount <= 0) {
            return in;
        }
        int level = in.classLevel();
        int xp = in.xpIntoLevel() - amount;
        while (xp < 0 && level > 1) {
            level--;
            xp += xpToReachNextLevel(level);
        }
        if (xp < 0) {
            xp = 0;
        }
        return new PlayerKingdomRpgData(in.kingdomIndex(), in.classIndex(), level, xp);
    }
}
