package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reorients the whole camera to the moon's directional gravity so the face you stand
 * on reads as the floor.
 *
 * We flip {@link Camera}'s rotation quaternion once per frame, right after it's rebuilt
 * in update() (after alignWithEntity) and before the view/projection matrices are
 * recomputed — so the world view, the cull frustum, AND camera-facing billboards
 * (particles use {@code Camera.rotation()}) all derive from the same flipped rotation.
 * (An earlier approach flipped only the extracted view matrix, which left particles
 * billboarding 90 deg off because they read the unflipped rotation.)
 */
@Mixin(Camera.class)
public abstract class CameraGravityMixin {

    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow private int matrixPropertiesDirty;
    @Shadow public abstract Vec3 position();
    @Shadow protected abstract void setPosition(Vec3 vec);

    // Time constant for the gravity-flip easing, in seconds. The applied correction reaches
    // ~95% of the new face after ~3*TAU (~0.24s), so a flip glides over a quarter-second
    // instead of snapping in one frame. Smaller = snappier, larger = floatier.
    @Unique private static final float IRONHOLD_SMOOTH_TAU = 0.08f;

    // Persistent, frame-to-frame smoothed state (one main Camera instance). Null = "not yet
    // initialised / off the moon", so re-entry starts clean.
    @Unique private Quaternionf ironhold$smoothGrav;     // eased gravity-up correction
    @Unique private Vec3 ironhold$smoothEyeOffset;       // eased first-person eye displacement
    @Unique private long ironhold$lastFrameNanos;        // for framerate-independent easing

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V",
                     shift = At.Shift.AFTER))
    private void ironhold$reorientCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (entity == null) return;
        if (!entity.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)) {
            // Reset so coming back to the moon doesn't lerp from a stale orientation.
            ironhold$smoothGrav = null;
            ironhold$smoothEyeOffset = null;
            ironhold$lastFrameNanos = 0L;
            return;
        }

        Direction g = GravityHelper.getGravityDirection(entity);
        ironhold$log("g=" + g);

        // Framerate-independent exponential smoothing factor for this frame.
        long now = System.nanoTime();
        float dt = ironhold$lastFrameNanos == 0L ? 0.0f : (now - ironhold$lastFrameNanos) / 1.0e9f;
        ironhold$lastFrameNanos = now;
        float alpha = 1.0f - (float) Math.exp(-dt / IRONHOLD_SMOOTH_TAU);
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        // Ease toward the SHARED gravity orientation (GravityHelper rolls it <=90 deg per edge
        // and the movement code reads the same quaternion, so view and controls always share a
        // frame — including the bottom face's otherwise-ambiguous roll). Because the target only
        // ever moves 90 deg at a time, the slerp is always a clean short arc; no 180 deg tumble.
        Quaternionf target = GravityHelper.getGravityRotation(entity);
        if (ironhold$smoothGrav == null) ironhold$smoothGrav = new Quaternionf(target);
        ironhold$smoothGrav.slerp(target, alpha).normalize();
        // Applied here (before the view / projection / frustum rebuild) so the world view, cull
        // frustum and camera-facing billboards (particles use Camera.rotation()) all derive
        // from the one correction.
        ironhold$smoothGrav.mul(this.rotation, this.rotation);
        this.matrixPropertiesDirty |= 3;

        // First-person eye: ease the gravity-induced eye displacement on the SAME schedule, so
        // the viewpoint glides onto the new face in lockstep with the rotation instead of
        // popping. The offset is taken relative to the live vanilla camera position (which
        // already tracks the player every frame), so steady walking on a face doesn't drift.
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            Vec3 targetOffset = Vec3.ZERO;
            if (g != Direction.DOWN) {
                // Shared clamped-eye helper: casts outward from the (always-clear) box centre
                // and stops short of the first block, so the eye can't bury in a wall or poke
                // through an overhang. getEyePosition uses the SAME helper, so the pick-ray
                // origin and this rendered viewpoint stay in lockstep.
                Vec3 feet = position().subtract(0.0, entity.getEyeHeight(), 0.0);
                Vec3 eye = GravityHelper.gravityEye(entity, feet, g);
                targetOffset = eye.subtract(position());
            }
            if (ironhold$smoothEyeOffset == null) ironhold$smoothEyeOffset = targetOffset;
            else ironhold$smoothEyeOffset = ironhold$smoothEyeOffset.lerp(targetOffset, alpha);
            Vec3 finalEye = position().add(ironhold$smoothEyeOffset);
            setPosition(finalEye);
            if (g != Direction.DOWN) ironhold$diag(g, finalEye, targetOffset);
        } else {
            ironhold$smoothEyeOffset = null;
        }
    }

    @Unique private static long ironhold$lastDiag = 0L;
    @Unique
    private void ironhold$diag(Direction g, Vec3 eye, Vec3 targetOffset) {
        long t = System.currentTimeMillis();
        if (t - ironhold$lastDiag < 400) return;
        ironhold$lastDiag = t;
        var lvl = entity.level();
        net.minecraft.core.BlockPos ep = net.minecraft.core.BlockPos.containing(eye);
        boolean eyeInBlock = !lvl.getBlockState(ep).getCollisionShape(lvl, ep).isEmpty();
        net.minecraft.core.BlockPos bp = entity.blockPosition();
        boolean bodyInBlock = !lvl.getBlockState(bp).getCollisionShape(lvl, bp).isEmpty();
        Vec3 p = entity.position();
        kingdom.smp.Ironhold.LOGGER.info(
            "[MoonCamDiag] g={} eyeInBlock={} bodyInBlock={} eye=({},{},{}) body=({},{},{}) off=({},{},{})",
            g, eyeInBlock, bodyInBlock,
            String.format("%.2f", eye.x), String.format("%.2f", eye.y), String.format("%.2f", eye.z),
            String.format("%.2f", p.x), String.format("%.2f", p.y), String.format("%.2f", p.z),
            String.format("%.2f", targetOffset.x), String.format("%.2f", targetOffset.y), String.format("%.2f", targetOffset.z));
    }

    private static String ironhold$lastTok = "";
    private static void ironhold$log(String tok) {
        if (!tok.equals(ironhold$lastTok)) {
            ironhold$lastTok = tok;
            kingdom.smp.Ironhold.LOGGER.info("[MoonCam] flip {}", tok);
        }
    }
}
