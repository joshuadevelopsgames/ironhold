package kingdom.smp.entity;

/**
 * Character archetypes for filchers — derived from their personality traits at spawn.
 *
 * <p>Each archetype encodes a set of human-readable character descriptors that the
 * king's LLM brain uses to assign roles and craft heist strategy. The LLM is told
 * to treat each filcher as an <em>actor</em> with a specific character to embody,
 * not just a set of numerical stats.
 *
 * <p>Archetypes are deterministically assigned from boldness + greed + sociability:
 * <pre>
 *   boldness high + greed high + social low  → THE OPPORTUNIST
 *   boldness high + greed low  + social high → THE DAREDEVIL
 *   boldness low  + greed high + social low  → THE MISER
 *   boldness low  + greed low  + social high → THE LOYALIST
 *   boldness high + greed high + social high → THE KINGMAKER
 *   boldness low  + greed low  + social low  → THE GHOST
 *   boldness mid  + greed high + social mid  → THE SCHEMER
 *   (else)                                  → THE FENCE (balanced, adaptable)
 * </pre>
 */
public enum FilcherArchetype {

    /**
     * High boldness, high greed, low social.
     * A solo artist who takes what they want and answers to nobody — except the king,
     * barely. Secretly believes they should be wearing the crown.
     */
    OPPORTUNIST(
        "the Opportunist",
        "to pull off the biggest score, alone, and take all the glory",
        "being overshadowed, being given the boring jobs",
        "always grabs one extra item beyond what was agreed",
        FilcherRole.THIEF
    ),

    /**
     * High boldness, low greed, high social.
     * Does it for the thrill, not the treasure. Loves an audience.
     * Gets reckless when they sense everyone is watching.
     */
    DAREDEVIL(
        "the Daredevil",
        "to do the impossible thing, right in front of the player's face",
        "being told to hang back, being called the distraction",
        "whistles faintly when closing in — can't help it",
        FilcherRole.THIEF
    ),

    /**
     * Low boldness, high greed, low social.
     * Deeply covetous but deeply cautious. Will only act when the odds are perfect.
     * Obsessed with item rarity — will abandon a job if the target loot isn't worth it.
     */
    MISER(
        "the Miser",
        "to hoard something truly rare — something no other filcher has",
        "wasting effort on common loot, being caught with nothing to show for it",
        "inspects every item for rarity before committing to a steal",
        FilcherRole.SCOUT
    ),

    /**
     * Low boldness, low greed, high social.
     * The pack glue. Deeply loyal to the king and the group.
     * Will sacrifice their own haul to ensure the pack escapes safely.
     */
    LOYALIST(
        "the Loyalist",
        "for the pack to succeed together — no filcher left behind",
        "the king being disappointed, the pack fragmenting",
        "always moves to shield nearby filchers from sight lines",
        FilcherRole.LOOKOUT
    ),

    /**
     * High boldness, high greed, high social.
     * Charismatic and hungry. Naturally takes charge of sub-groups.
     * The king respects them — and eyes them as a threat.
     */
    KINGMAKER(
        "the Kingmaker",
        "to run the most perfect, most profitable heist ever conceived",
        "chaos, filchers acting out of turn, amateur mistakes",
        "quietly coordinates nearby filchers without being asked",
        FilcherRole.DISTRACTOR
    ),

    /**
     * Low boldness, low greed, low social.
     * Silent, unreadable, barely seems to exist until it's already too late.
     * Even other filchers aren't sure what it wants.
     */
    GHOST(
        "the Ghost",
        "to slip in and out without a single entity noticing they were ever there",
        "being seen, being known, being remembered",
        "goes completely still when a player turns to face their direction",
        FilcherRole.CARRIER
    ),

    /**
     * Mid boldness, high greed, mid social.
     * A manipulative planner. Always three steps ahead.
     * Pushes the risky jobs onto bolder filchers while collecting a cut.
     */
    SCHEMER(
        "the Schemer",
        "to get the highest-value item with the lowest personal risk",
        "having to do the dangerous work themselves, being double-crossed",
        "positions themselves near the exit before the job even starts",
        FilcherRole.GUARD
    ),

    /**
     * Fallback — balanced traits.
     * Adaptable and professional. No strong identity beyond the job itself.
     * Reliable but predictable.
     */
    FENCE(
        "the Fence",
        "to do the job cleanly and get paid",
        "unnecessary risk, drama, complicated plans",
        "always volunteers for whichever role nobody else wants",
        FilcherRole.IDLE
    );

    // ─── Fields ───────────────────────────────────────────────────────────────

    /** Human-readable title used in the king's LLM brief, e.g. "the Opportunist". */
    public final String title;

    /** What this character fundamentally wants — drives their decisions. */
    public final String desire;

    /** What this character avoids — shapes which roles they resist. */
    public final String fear;

    /** A behavioral quirk the LLM can use for flavor in its dialogue. */
    public final String quirk;

    /** The natural role this archetype gravitates toward by default. */
    public final FilcherRole naturalRole;

    FilcherArchetype(String title, String desire, String fear, String quirk, FilcherRole naturalRole) {
        this.title       = title;
        this.desire      = desire;
        this.fear        = fear;
        this.quirk       = quirk;
        this.naturalRole = naturalRole;
    }

    // ─── Derivation ───────────────────────────────────────────────────────────

    /**
     * Deterministically derives an archetype from the three personality floats.
     * Thresholds: HIGH ≥ 0.6, LOW ≤ 0.4, MID = in between.
     */
    public static FilcherArchetype from(float boldness, float greed, float sociability) {
        boolean bHigh = boldness     >= 0.6f;
        boolean gHigh = greed        >= 0.6f;
        boolean sHigh = sociability  >= 0.6f;
        boolean bLow  = boldness     <= 0.4f;
        boolean gLow  = greed        <= 0.4f;
        boolean sLow  = sociability  <= 0.4f;

        if  (bHigh && gHigh && sHigh) return KINGMAKER;
        if  (bLow  && gLow  && sLow)  return GHOST;
        if  (bHigh && gHigh && sLow)  return OPPORTUNIST;
        if  (bHigh && gLow  && sHigh) return DAREDEVIL;
        if  (bLow  && gHigh && sLow)  return MISER;
        if  (bLow  && gLow  && sHigh) return LOYALIST;
        if  (!bLow && gHigh && !sLow) return SCHEMER;
        return FENCE;
    }

    // ─── Names ────────────────────────────────────────────────────────────────

    /**
     * Pool of filcher names. Seeded by the entity's UUID so each filcher
     * always gets the same name across server restarts.
     */
    private static final String[] NAME_POOL = {
        "Skrit", "Nib",   "Pock",  "Vell",  "Twig",  "Snip",  "Fenn",
        "Grub",  "Quill", "Murk",  "Titch", "Wren",  "Dross", "Pip",
        "Lurk",  "Scurf", "Hobb",  "Zeck",  "Flit",  "Prill", "Snatch",
        "Didge", "Crib",  "Welt",  "Nock",  "Sprig", "Filch", "Slink",
        "Burr",  "Crimp", "Gnash", "Scut",  "Wisp",  "Priss", "Crook"
    };

    /**
     * Derives a stable name for a filcher given the least-significant bits
     * of its UUID. Same entity always gets the same name.
     */
    public static String nameFor(long uuidLeastSig) {
        int idx = (int) (Math.abs(uuidLeastSig) % NAME_POOL.length);
        return NAME_POOL[idx];
    }
}
