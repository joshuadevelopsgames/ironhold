package kingdom.smp.mixin;

import kingdom.smp.dynlight.DynamicLights;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches the per-entity lightmap value so entities standing in a dynamically lit area aren't
 * rendered dark. Sampling the lookup point at the entity's interpolated render position (instead
 * of the discrete {@code blockPosition()}) means entity lighting tracks at full framerate
 * instead of stepping per tick.
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
        double px = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
        double pyBase = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
        double pz = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
        double py = (entity instanceof LivingEntity)
            ? pyBase + entity.getEyeHeight()
            : pyBase + entity.getBbHeight() * 0.5;
        int patched = DynamicLights.patchLightmapAtPoint(px, py, pz, original);
        if (patched != original) cir.setReturnValue(patched);
    }
}
