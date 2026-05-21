package kingdom.smp.seasons;

/**
 * Immutable snapshot of the season cycle for one moment in time. Recomputed cheaply from
 * {@code cycleTicks}; everything else is derived.
 */
public record SeasonState(int cycleTicks, int day, Season.SubSeason subSeason, TropicalSeason tropicalSeason) {

    public Season season() { return subSeason.parent(); }
}
