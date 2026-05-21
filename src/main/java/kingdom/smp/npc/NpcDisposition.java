package kingdom.smp.npc;

/**
 * What a recruitable NPC is currently doing relative to the player who bonded
 * with them.
 *
 * <ul>
 *   <li>{@link #FREE} — living their own life at their spawn (default).</li>
 *   <li>{@link #FOLLOWING} — travelling with their owner as a companion.</li>
 *   <li>{@link #STATIONED} — "moved in": anchored to a chosen spot (e.g. the
 *       player's base) where they idle and keep watch.</li>
 * </ul>
 */
public enum NpcDisposition {
    FREE,
    FOLLOWING,
    STATIONED;

    public static NpcDisposition byName(String s, NpcDisposition fallback) {
        if (s == null) return fallback;
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
