package kingdom.smp.mixin;

import kingdom.smp.dynlight.DynamicLights;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches the per-entity lightmap value so entities standing in a dynamically lit area aren't
 * rendered dark. Without this, terrain near a held torch lights up but the holder's model stays
 * shadowed.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class DynLightEntityRenderDispatcherMixin {

    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void ironhold$injectDynamicLight(
            E entity,
            float partialTick,
            CallbackInfoReturnable<Integer> cir) {
        if (entity == null || entity.level() == null) return;
        int original = cir.getReturnValueI();
        int patched = DynamicLights.patchLightmap(entity.level(), entity.blockPosition(), original);
        if (patched != original) cir.setReturnValue(patched);
    }
}
