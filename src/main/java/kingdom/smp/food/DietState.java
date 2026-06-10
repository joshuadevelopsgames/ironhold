package kingdom.smp.food;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player diet state: food-group id → game-tick the group stays "satisfied" until. Backs the
 * reward-only diet (Phase 5 ⑪). Stored in the {@code DIET} attachment (copyOnDeath).
 */
public record DietState(Map<String, Long> until) {

    public static final DietState EMPTY = new DietState(Map.of());

    public static final MapCodec<DietState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("until", Map.of()).forGetter(DietState::until)
    ).apply(i, DietState::new));

    /** How many distinct groups are currently satisfied at {@code now}. */
    public int satisfiedCount(long now) {
        int n = 0;
        for (long expiry : until.values()) {
            if (expiry > now) {
                n++;
            }
        }
        return n;
    }
}
