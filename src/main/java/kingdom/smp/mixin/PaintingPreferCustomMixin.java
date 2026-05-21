package kingdom.smp.mixin;

import java.util.List;
import java.util.stream.Stream;

import kingdom.smp.Ironhold;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Gives Ironhold's own paintings priority when a player places a blank painting.
 *
 * <p>Vanilla {@code Painting.create} gathers every {@code #minecraft:placeable} variant that fits
 * the chosen wall, narrows to the largest-area ones, then picks at random — so our handful of
 * custom variants almost never win against the full vanilla set. We hook the {@code stream()} call
 * that begins the largest-area pass and, if any Ironhold variant fits the spot, drop everything
 * else first. The vanilla largest-area + random logic then runs over the custom variants only,
 * cycling through them while still respecting which ones physically fit.
 */
@Mixin(Painting.class)
public abstract class PaintingPreferCustomMixin {

    @Redirect(
        method = "create",
        at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private static Stream<Holder<PaintingVariant>> ironhold$preferCustom(
            List<Holder<PaintingVariant>> fittingVariants) {
        List<Holder<PaintingVariant>> custom = fittingVariants.stream()
            .filter(h -> h.unwrapKey()
                .map(key -> key.identifier().getNamespace().equals(Ironhold.MODID))
                .orElse(false))
            .toList();
        if (!custom.isEmpty()) {
            fittingVariants.clear();
            fittingVariants.addAll(custom);
        }
        return fittingVariants.stream();
    }
}
