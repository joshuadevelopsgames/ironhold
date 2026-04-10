package kingdom.smp.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which classes a player has mastered (reached promotion level).
 * Persisted as a list of PlayerClass ordinals.
 */
public record CompletedClasses(List<Integer> classOrdinals) {

    public static final MapCodec<CompletedClasses> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.INT.listOf().optionalFieldOf("completed", List.of())
                .forGetter(CompletedClasses::classOrdinals))
            .apply(i, CompletedClasses::new));

    public static CompletedClasses empty() {
        return new CompletedClasses(List.of());
    }

    public Set<PlayerClass> asSet() {
        Set<PlayerClass> set = new HashSet<>();
        for (int ord : classOrdinals) {
            PlayerClass pc = PlayerClass.fromIndex(ord);
            if (pc != null) set.add(pc);
        }
        return set;
    }

    public boolean hasCompleted(PlayerClass pc) {
        return classOrdinals.contains(pc.ordinal());
    }

    public CompletedClasses withCompleted(PlayerClass pc) {
        if (classOrdinals.contains(pc.ordinal())) return this;
        List<Integer> next = new ArrayList<>(classOrdinals);
        next.add(pc.ordinal());
        return new CompletedClasses(List.copyOf(next));
    }
}
