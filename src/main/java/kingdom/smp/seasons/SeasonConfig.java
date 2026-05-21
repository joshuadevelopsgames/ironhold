package kingdom.smp.seasons;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Compile-time defaults for the seasons system. Kept as plain constants rather than a runtime
 * config file — keeps the implementation simple while we tune. Promote to NeoForge ModConfigSpec
 * later if players ask for dials.
 *
 * <p>Day-cycle defaults match Serene Seasons: 8 in-game days per sub-season → 96-day year.
 */
public final class SeasonConfig {
    private SeasonConfig() {}

    public static final int DAY_DURATION_TICKS = 24_000;
    public static final int SUB_SEASON_DAYS = 8;
    public static final int SUB_SEASON_DURATION_TICKS = DAY_DURATION_TICKS * SUB_SEASON_DAYS;
    public static final int CYCLE_DURATION_TICKS = SUB_SEASON_DURATION_TICKS * Season.SubSeason.VALUES.length;

    /** Cycle tick we initialize a fresh level to. 1 sub-season in = EARLY_SUMMER-ish; SS default. */
    public static final int STARTING_CYCLE_TICKS = SUB_SEASON_DURATION_TICKS;

    /** Dimensions that run a season cycle. Other dimensions stay at their fixed start sub-season. */
    public static final Set<ResourceKey<Level>> ENABLED_DIMENSIONS = Set.of(Level.OVERWORLD);

    /** How often (server ticks) the seasons handler broadcasts the cycle to clients. */
    public static final int SYNC_INTERVAL_TICKS = 20;

    /** Per-sub-season biome temperature adjustment. Index matches {@code SubSeason.ordinal()}. */
    public static final float[] BIOME_TEMP_ADJUSTMENT = {
        -0.25f, // EARLY_SPRING
         0.00f, // MID_SPRING
         0.00f, // LATE_SPRING
         0.00f, // EARLY_SUMMER
         0.00f, // MID_SUMMER
         0.00f, // LATE_SUMMER
         0.00f, // EARLY_AUTUMN
         0.00f, // MID_AUTUMN
        -0.25f, // LATE_AUTUMN
        -0.80f, // EARLY_WINTER
        -0.80f, // MID_WINTER
        -0.80f, // LATE_WINTER
    };

    public static final float TEMP_MIN = -0.5f;
    public static final float TEMP_MAX =  2.0f;
    /** Vanilla "warm enough to rain (not snow)" threshold. */
    public static final float WARM_RAIN_THRESHOLD = 0.15f;

    /** Out-of-season crop behavior. 0 = slow growth (5/6 cancellation), 1 = full block, 2 = destroy. */
    public static final int OUT_OF_SEASON_BEHAVIOR = 0;

    /** Crops below this Y always grow regardless of season. */
    public static final int UNDERGROUND_FERTILITY_Y = 48;

    /** When out-of-season-behavior == 0, growth tick is skipped with this probability (5/6). */
    public static final int SLOW_GROWTH_SKIP_DIVISOR = 6;

    public static boolean isDimensionEnabled(ResourceKey<Level> dim) {
        return ENABLED_DIMENSIONS.contains(dim);
    }

    public static float biomeTempAdjustment(Season.SubSeason sub) {
        return BIOME_TEMP_ADJUSTMENT[sub.ordinal()];
    }
}
