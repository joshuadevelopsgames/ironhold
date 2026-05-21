package kingdom.smp.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-wide tally of naturally-generated chests discovered so far, used to turn
 * every Nth one into a Mimic. Stored once (always on the overworld's data storage)
 * so the count is global across dimensions rather than per-dimension.
 */
public class MimicChestData extends SavedData {

    public static final Codec<MimicChestData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.LONG.fieldOf("natural_chest_count").forGetter(d -> d.naturalChestCount))
            .apply(i, MimicChestData::new));

    public static final SavedDataType<MimicChestData> TYPE =
        new SavedDataType<>(
            Identifier.parse("ironhold:mimic_chests"),
            MimicChestData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private long naturalChestCount;

    public MimicChestData() {
        this(0L);
    }

    private MimicChestData(long naturalChestCount) {
        this.naturalChestCount = naturalChestCount;
    }

    /**
     * Records one freshly-discovered natural chest and reports whether it lands on
     * the conversion interval (i.e. it should become a Mimic). The first chest is
     * #1, so with interval 20 the 20th, 40th, 60th… chests are the mimics.
     */
    public boolean recordAndShouldConvert(int interval) {
        naturalChestCount++;
        setDirty();
        return naturalChestCount % interval == 0;
    }
}
