package kingdom.smp.gear;

/** Affix grouping — controls which gear type can roll an affix (see {@link AffixRoller}). */
public enum AffixCategory {
    OFFENSIVE,  // weapons
    DEFENSIVE,  // armor
    UTILITY,    // tools / armor
    ON_HIT      // weapons (special abilities)
}
