package kingdom.smp.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-player set of boss ids whose signature artifact has already been earned, so guaranteed
 * first-kill drops never repeat (later kills give normal loot). copyOnDeath via the
 * {@code BOSS_ARTIFACTS_EARNED} attachment. Spec: {@code specs/fantasia-ports/02-boss-accessories.md}.
 */
public record EarnedArtifacts(Set<String> ids) {

    public static final EarnedArtifacts EMPTY = new EarnedArtifacts(Set.of());

    public static final MapCodec<EarnedArtifacts> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("ids", List.of()).forGetter(e -> List.copyOf(e.ids()))
    ).apply(i, list -> new EarnedArtifacts(Set.copyOf(list))));

    public boolean has(String id) {
        return ids.contains(id);
    }

    public EarnedArtifacts with(String id) {
        Set<String> next = new HashSet<>(ids);
        next.add(id);
        return new EarnedArtifacts(Set.copyOf(next));
    }
}
