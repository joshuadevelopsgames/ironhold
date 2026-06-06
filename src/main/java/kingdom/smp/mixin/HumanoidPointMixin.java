package kingdom.smp.mixin;

import kingdom.smp.client.emote.PointableRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses the right arm forward and tilts the head while a player plays the
 * "point" emote. Targets {@link HumanoidModel} (where the arm/head parts are
 * declared) and runs for every humanoid, but only the player render state
 * implements {@link PointableRenderState}, so non-players are untouched.
 *
 * <p>{@code PlayerModel.setupAnim} calls {@code super.setupAnim} before copying
 * arm rotations onto its sleeve/jacket overlay, so posing here also carries
 * through to the outer skin layer.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidPointMixin {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart hat;

    // Held-pose targets (radians). xRot ~ -1.62 raises the arm to just above
    // horizontal-forward; a small inward yaw reads as a deliberate point.
    private static final float ARM_X = -1.62f;
    private static final float ARM_Y = -0.12f;
    private static final float HEAD_X = 0.18f;

    @Inject(
        method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
        at = @At("TAIL"))
    private void ironhold$pointPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof PointableRenderState pointable)) {
            return;
        }
        float a = pointable.ironhold$point();
        if (a <= 0f) {
            return;
        }
        this.rightArm.xRot = lerp(this.rightArm.xRot, ARM_X, a);
        this.rightArm.yRot = lerp(this.rightArm.yRot, ARM_Y, a);
        this.rightArm.zRot = lerp(this.rightArm.zRot, 0f, a);
        this.head.xRot = lerp(this.head.xRot, HEAD_X, a);
        this.hat.xRot = this.head.xRot;
        this.hat.yRot = this.head.yRot;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
