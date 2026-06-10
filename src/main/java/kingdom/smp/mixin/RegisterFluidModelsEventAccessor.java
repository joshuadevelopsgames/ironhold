package kingdom.smp.mixin;

import java.util.Map;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the event's internal model map + material baker so we can <em>override</em> the (already-present)
 * vanilla {@code Fluids.WATER} model with a tinted one — the public {@code register} throws on duplicates,
 * so we put directly. Vanilla models are seeded before the event fires (see
 * {@code ClientHooks.gatherFluidModels}), so this override is order-independent. Used by
 * {@code DyedWaterClientEvents}.
 */
@Mixin(RegisterFluidModelsEvent.class)
public interface RegisterFluidModelsEventAccessor {

    @Accessor("models")
    Map<Fluid, FluidModel> ironhold$getModels();
}
