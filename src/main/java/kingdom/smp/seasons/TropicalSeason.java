package kingdom.smp.seasons;

/**
 * Wet/dry phase for biomes tagged tropical. Six values cycle once per year, offset half-cycle
 * from the standard sub-seasons so the tropical wet season aligns roughly with non-tropical
 * spring/summer rainy months.
 */
public enum TropicalSeason {
    EARLY_DRY,
    MID_DRY,
    LATE_DRY,
    EARLY_WET,
    MID_WET,
    LATE_WET;

    public static final TropicalSeason[] VALUES = values();
}
