package kingdom.smp.entity;

/**
 * Operational role assigned to a {@link FilcherEntity} by the pack king's LLM brain.
 *
 * <p>Roles are temporary — the king reassigns them each operation cycle.
 * Most goals check the filcher's current role before activating.
 *
 * <ul>
 *   <li>{@link #IDLE}        — no active operation; uses default wandering/social goals</li>
 *   <li>{@link #SCOUT}       — orbits a target player at range, observing their inventory</li>
 *   <li>{@link #THIEF}       — the primary pickpocket; waits for distraction signal</li>
 *   <li>{@link #DISTRACTOR}  — gets in the player's face, draws attention away from the thief</li>
 *   <li>{@link #LOOKOUT}     — patrols the perimeter, warns on danger approach</li>
 *   <li>{@link #CARRIER}     — takes stolen goods directly to the den without detour</li>
 *   <li>{@link #GUARD}       — shields the thief/carrier from player retaliation</li>
 * </ul>
 */
public enum FilcherRole {
    IDLE,
    SCOUT,
    THIEF,
    DISTRACTOR,
    LOOKOUT,
    CARRIER,
    GUARD;

    /** Short code used in king broadcast messages and history logs. */
    public String shortCode() {
        return switch (this) {
            case IDLE        -> "idle";
            case SCOUT       -> "sct";
            case THIEF       -> "thf";
            case DISTRACTOR  -> "dst";
            case LOOKOUT     -> "lkt";
            case CARRIER     -> "car";
            case GUARD       -> "grd";
        };
    }
}
