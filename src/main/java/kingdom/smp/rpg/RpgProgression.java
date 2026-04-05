package kingdom.smp.rpg;

/** Class level curve (stub until CLASS_SYSTEM tuning). */
public final class RpgProgression {
    private RpgProgression() {}

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
