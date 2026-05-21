package kingdom.smp.seasons;

import net.minecraft.util.Mth;

/**
 * Pure-function conversion between an internal cycle-tick counter and the derived season state.
 * Cycle wraps at {@link SeasonConfig#CYCLE_DURATION_TICKS}.
 */
public final class SeasonTime {
    private SeasonTime() {}

    public static SeasonState of(int cycleTicks) {
        int normalized = Mth.positiveModulo(cycleTicks, SeasonConfig.CYCLE_DURATION_TICKS);
        Season.SubSeason sub = subSeasonAt(normalized);
        TropicalSeason trop = tropicalAt(normalized);
        int day = normalized / SeasonConfig.DAY_DURATION_TICKS;
        return new SeasonState(normalized, day, sub, trop);
    }

    public static Season.SubSeason subSeasonAt(int cycleTicks) {
        int idx = Mth.positiveModulo(cycleTicks / SeasonConfig.SUB_SEASON_DURATION_TICKS,
                                     Season.SubSeason.VALUES.length);
        return Season.SubSeason.VALUES[idx];
    }

    /**
     * Tropical wet/dry indexing. Six tropical sub-seasons spread over twelve standard sub-seasons,
     * phase-shifted so tropical wet season aligns with non-tropical late-summer / autumn.
     */
    public static TropicalSeason tropicalAt(int cycleTicks) {
        int subIdx = cycleTicks / SeasonConfig.SUB_SEASON_DURATION_TICKS;
        int idx = Mth.positiveModulo(((subIdx + 11) / 2) + 5, TropicalSeason.VALUES.length);
        return TropicalSeason.VALUES[idx];
    }
}
