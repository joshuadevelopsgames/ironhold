package kingdom.smp.food;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-player set of recipe ids the player has learned. Backs the knowledge half of the
 * cooking progression model: a recipe must be in this set before the player can cook it,
 * regardless of profession rank.
 *
 * <p>Registered as an {@link net.neoforged.neoforge.attachment.AttachmentType} on each
 * player in {@link kingdom.smp.ModAttachments#KNOWN_RECIPES}. Server-side persistent;
 * client sync is deferred to Phase 2 (visible recipe-book UI).
 */
public record KnownRecipes(Set<Identifier> learned) {

    public static final MapCodec<KnownRecipes> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.listOf()
                    .optionalFieldOf("learned", List.of())
                    .forGetter(kr -> List.copyOf(kr.learned))
    ).apply(i, list -> new KnownRecipes(new HashSet<>(list))));

    public KnownRecipes {
        learned = new HashSet<>(learned);
    }

    public static KnownRecipes empty() {
        return new KnownRecipes(new HashSet<>());
    }

    public boolean knows(Identifier recipeId) {
        return learned.contains(recipeId);
    }

    public KnownRecipes withLearned(Identifier recipeId) {
        Set<Identifier> next = new HashSet<>(learned);
        next.add(recipeId);
        return new KnownRecipes(next);
    }
}
