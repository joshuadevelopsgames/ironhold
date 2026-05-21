package kingdom.smp.seasons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persists one int per level: the current season cycle tick. Every season-related value (current
 * season, sub-season, day-of-year, tropical season) is derived from this counter on demand.
 */
public final class SeasonSavedData extends SavedData {

    public static final Codec<SeasonSavedData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.INT.fieldOf("cycleTicks").forGetter(d -> d.cycleTicks))
            .apply(i, SeasonSavedData::new));

    public static final SavedDataType<SeasonSavedData> TYPE =
        new SavedDataType<>(Identifier.parse("ironhold:seasons"),
            () -> new SeasonSavedData(SeasonConfig.STARTING_CYCLE_TICKS),
            CODEC,
            DataFixTypes.LEVEL);

    private int cycleTicks;

    private SeasonSavedData(int cycleTicks) {
        this.cycleTicks = cycleTicks;
    }

    public int cycleTicks() { return cycleTicks; }

    public void setCycleTicks(int value) {
        int wrapped = ((value % SeasonConfig.CYCLE_DURATION_TICKS) + SeasonConfig.CYCLE_DURATION_TICKS)
                       % SeasonConfig.CYCLE_DURATION_TICKS;
        if (wrapped != this.cycleTicks) {
            this.cycleTicks = wrapped;
            setDirty();
        }
    }
}
