package kingdom.smp.skill.useskill;

/**
 * XP → level curve for use-skills. Skyrim-ish: each level costs more than
 * the last, sub-linearly enough that early levels feel fast.
 *
 * <pre>
 * level n requires:  25 * (n + 1)^1.5   XP into that level
 * cumulative XP for level L:  sum_{n=0..L-1} 25 * (n+1)^1.5
 * </pre>
 */
public final class UseSkillCurve {
    private UseSkillCurve() {}

    private static final float BASE = 25f;
    private static final float EXP = 1.5f;

    /** XP required to advance FROM level {@code level} to level+1. */
    public static float xpForNext(int level) {
        return BASE * (float) Math.pow(level + 1, EXP);
    }

    /** Compute the current level given total accumulated XP. */
    public static int levelFor(float totalXp) {
        if (totalXp <= 0f) return 0;
        float remaining = totalXp;
        for (int lvl = 0; lvl < UseSkill.MAX_LEVEL; lvl++) {
            float cost = xpForNext(lvl);
            if (remaining < cost) return lvl;
            remaining -= cost;
        }
        return UseSkill.MAX_LEVEL;
    }

    /** XP that's been earned toward the next level after the current one. */
    public static float xpIntoLevel(float totalXp) {
        float remaining = Math.max(0f, totalXp);
        for (int lvl = 0; lvl < UseSkill.MAX_LEVEL; lvl++) {
            float cost = xpForNext(lvl);
            if (remaining < cost) return remaining;
            remaining -= cost;
        }
        return 0f;
    }
}
