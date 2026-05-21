package kingdom.smp.world;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-wide running tallies of mobs killed, keyed by an arbitrary string, used
 * to grant a special drop on every Nth kill of a given kind. Stored once (always
 * on the overworld's data storage) so counts are global across dimensions and
 * survive restarts.
 */
public class KillTallyData extends SavedData {

    public static final Codec<KillTallyData> CODEC =
        Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .xmap(KillTallyData::new, d -> d.counts);

    public static final SavedDataType<KillTallyData> TYPE =
        new SavedDataType<>(
            Identifier.parse("ironhold:kill_tally"),
            KillTallyData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Long> counts;

    public KillTallyData() {
        this.counts = new HashMap<>();
    }

    private KillTallyData(Map<String, Long> counts) {
        this.counts = new HashMap<>(counts);
    }

    /**
     * Records one kill of {@code key} and reports whether it lands on the drop
     * interval. The first kill is #1, so with interval 8 the 8th, 16th, 24th… land.
     */
    public boolean recordAndShouldDrop(String key, int interval) {
        long n = counts.getOrDefault(key, 0L) + 1L;
        counts.put(key, n);
        setDirty();
        return n % interval == 0L;
    }
}
