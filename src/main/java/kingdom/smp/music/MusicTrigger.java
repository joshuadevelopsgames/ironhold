package kingdom.smp.music;

/**
 * The vocabulary of situational triggers the reactive-music engine evaluates each
 * client tick. {@link ReactiveMusicState} recomputes which of these are currently
 * active; {@link ReactiveSongbook} maps trigger combinations onto tracks.
 *
 * <p>Clean-room equivalent of ReactiveMusic's {@code SongpackEventType}, trimmed to
 * what Ironhold actually uses and expressed in Mojang mappings. Add a trigger here,
 * set it in {@link ReactiveMusicState}, then bind it in {@link ReactiveSongbook}.
 */
public enum MusicTrigger {
    MAIN_MENU,

    // time of day
    DAY,
    NIGHT,
    SUNSET,
    SUNRISE,

    // weather
    RAIN,
    STORM,

    // space
    NETHER,
    END,
    UNDERWATER,
    UNDERGROUND,
    DEEP_UNDERGROUND,
    HIGH_UP,

    // travel
    MINECART,
    BOAT,
    HORSE,

    // activity / state
    FISHING,
    DYING,

    // social / combat
    NEAR_NPC,        // within range of an Ironhold voiced NPC
    IN_DIALOGUE,     // an Ironhold NPC dialogue screen is open
    VILLAGE,         // villagers nearby
    NEARBY_MOBS,     // hostiles nearby
    BOSS,            // a boss bar is showing

    // player-vs-player escalation (see PvpEscalation)
    PVP_SKIRMISH,    // an active duel with another player
    PVP_CLIMAX,      // that duel has lasted more than 4 back-and-forth turns

    GENERIC          // always true; lowest-priority fallback
}
