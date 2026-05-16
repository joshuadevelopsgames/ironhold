package kingdom.smp.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@link ItemStackRenderState}'s private {@code layers} array and
 * {@code activeLayerCount} so {@link kingdom.smp.mixin.ItemModelResolverMixin}
 * can iterate the populated layers post-bake and override their per-context
 * {@link net.minecraft.client.resources.model.cuboid.ItemTransform} with the
 * live values from {@link kingdom.smp.client.WizardStickTransformDebug}.
 *
 * <p>Read-only access: we don't need to write to either field. Layer count is
 * needed because the array is preallocated larger than the populated region.
 */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] ironhold$getLayers();

    @Accessor("activeLayerCount")
    int ironhold$getActiveLayerCount();
}
