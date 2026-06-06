package kingdom.smp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.GravityRenderState;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rotates living-entity models to stand on the moon's nearest face, so bodies
 * (including the player in 3rd person and other players/mobs) visibly turn
 * sideways with their feet on the wall rather than standing upright in mid-air.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGravityMixin {

    // Stamp the entity's hysteresis gravity dir onto its render state so setupRotations turns
    // the model to the SAME face the camera/physics chose (a raw position lookup here would
    // flip the body at a slightly different point than gravity actually flips, near edges).
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ironhold$captureGravity(LivingEntity entity, LivingEntityRenderState state,
                                         float partialTicks, CallbackInfo ci) {
        ((GravityRenderState) state).ironhold$setGravity(
            entity.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)
                ? GravityHelper.getGravityDirection(entity)
                : Direction.DOWN);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
            at = @At("HEAD"))
    private void ironhold$rotateForGravity(LivingEntityRenderState state, PoseStack pose,
                                           float bob, float yRot, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(ModMoonDimensions.MOON_LEVEL)) return;

        Direction g = ((GravityRenderState) state).ironhold$gravity();
        if (g == Direction.DOWN) return;

        // Use the SAME table-derived frame as the camera/physics so the 3rd-person body's roll
        // matches the view exactly (a minimal rotationTo would differ by a roll about the normal).
        pose.mulPose(GravityHelper.gravityRotation(g));
    }
}
