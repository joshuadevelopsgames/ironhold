package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On moon side/ceiling faces, offsets the player's eye along the gravity-up axis
 * (so the first-person camera and pick-ray origin sit at the head out from the wall),
 * and rotates the look/view vector into the gravity frame so the block-break/use
 * raycast lines up with the flipped crosshair (otherwise you can't hit anything).
 */
@Mixin(Entity.class)
public abstract class EntityGravityEyeMixin {

    private boolean ironhold$applies() {
        Entity self = (Entity) (Object) this;
        return self instanceof Player
            && self.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)
            && GravityHelper.getGravityDirection(self) != Direction.DOWN;
    }

    private Vec3 ironhold$remap(Vec3 orig) {
        Entity self = (Entity) (Object) this;
        Direction g = GravityHelper.getGravityDirection(self);
        Vec3 feet = orig.subtract(0.0, self.getEyeHeight(), 0.0);   // undo vanilla +Y offset
        // Pure gravity-frame transform: the real rotated box keeps this point in clear space, so
        // no clip is needed. Same frame as the camera/view, so crosshair and pick-ray agree.
        return feet.add(kingdom.smp.moon.RotationUtil.vecPlayerToWorld(0.0, self.getEyeHeight(), 0.0, g));
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void ironhold$eyeNow(CallbackInfoReturnable<Vec3> cir) {
        if (ironhold$applies()) cir.setReturnValue(ironhold$remap(cir.getReturnValue()));
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void ironhold$eyePartial(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (ironhold$applies()) cir.setReturnValue(ironhold$remap(cir.getReturnValue()));
    }

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void ironhold$viewVector(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (!ironhold$applies()) return;
        Entity self = (Entity) (Object) this;
        // Use the SAME path-consistent rotation the camera applies (getGravityRotation = the
        // rolled LAST_ROT), NOT a fresh per-face rotationTo. The two differ by the accumulated
        // roll about the up axis, and that mismatch is exactly why the block selector drifted
        // off the crosshair on side faces. Now the pick-ray and the view share one frame.
        Quaternionf gq = GravityHelper.getGravityRotation(self);
        Vec3 v = cir.getReturnValue();
        Vector3f f = gq.transform(new Vector3f((float) v.x, (float) v.y, (float) v.z));
        cir.setReturnValue(new Vec3(f.x(), f.y(), f.z()));
    }
}
