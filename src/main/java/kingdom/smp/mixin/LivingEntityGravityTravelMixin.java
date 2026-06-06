package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the player's movement in the moon's gravity-local frame, so a side/ceiling
 * face is treated as the ground: walking, friction, resting and jumping all happen
 * relative to whichever face you're standing on. Top face (gravity DOWN) and
 * everywhere off-moon fall through to vanilla untouched.
 *
 * Velocity is rotated world->local (down = -Y) via {@link GravityHelper}, vanilla-style
 * physics run in that frame, then it's rotated back to world for collision via move().
 * Ground is detected by probing the collision box a hair along the gravity axis.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityTravelMixin {

    @Shadow protected boolean jumping;

    private static final double MOON_PULL = 0.08 / 6.0;
    private static final float JUMP_POWER = 0.42f;

    private boolean ironhold$moonActive(LivingEntity self) {
        return self instanceof Player
            && self.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)
            && GravityHelper.getGravityDirection(self) != Direction.DOWN;
    }

    /** Movement input rotated by yaw, in the local (down=-Y) frame. */
    private static Vec3 ironhold$inputVector(Vec3 rel, float scale, float yawDeg) {
        double len2 = rel.lengthSqr();
        if (len2 < 1.0e-7) return Vec3.ZERO;
        Vec3 v = (len2 > 1.0 ? rel.normalize() : rel).scale(scale);
        double rad = Math.toRadians(yawDeg);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.z * cos + v.x * sin);
    }

    private static Vec3 ironhold$rot(Vec3 v, Quaternionf q) {
        Vector3f f = q.transform(new Vector3f((float) v.x, (float) v.y, (float) v.z));
        return new Vec3(f.x(), f.y(), f.z());
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void ironhold$moonTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ironhold$moonActive(self)) return;
        Direction g = GravityHelper.getGravityDirection(self);
        // The SAME path-consistent frame the camera eases toward, so the controls always line
        // up with the view (no inverse left/right or fwd/back on the bottom face).
        Quaternionf gq = GravityHelper.getGravityRotation(self);
        Quaternionf gqInv = gq.conjugate(new Quaternionf());

        // Creative/elytra-less flight, in the gravity-local frame: jump = away from the
        // face, sneak = toward it, WASD along the face plane. (Vanilla would apply these
        // along world-Y, i.e. sideways on a wall.)
        if (self instanceof Player p && p.getAbilities().flying) {
            float fly = p.getAbilities().getFlyingSpeed() * (p.isSprinting() ? 2.0f : 1.0f);
            Vec3 fv = ironhold$rot(self.getDeltaMovement(), gqInv).scale(0.6);
            fv = fv.add(ironhold$inputVector(input, fly * 10.0f, self.getYRot()));
            double vy = fv.y;
            if (this.jumping) vy += fly * 7.0;
            if (self.isShiftKeyDown()) vy -= fly * 7.0;
            fv = new Vec3(fv.x, vy, fv.z);
            self.setDeltaMovement(ironhold$rot(fv, gq));
            self.move(MoverType.SELF, self.getDeltaMovement());
            self.setDeltaMovement(ironhold$rot(fv.scale(0.6), gq));
            self.setOnGround(false);
            ci.cancel();
            return;
        }

        // Carried velocity in the local frame (down = -Y).
        Vec3 lv = ironhold$rot(self.getDeltaMovement(), gqInv);
        boolean wasGround = self.onGround();   // last tick's result drives input speed/friction

        float friction = wasGround ? 0.6f * 0.91f : 0.91f;
        double ms = self.getAttributeValue(Attributes.MOVEMENT_SPEED);
        float speed = wasGround
            ? (float) (ms * (0.21600002 / (friction * friction * friction)))
            : 0.026f;

        // Accelerate along the face plane from WASD (relative to look yaw), then add gravity
        // toward the face.
        lv = lv.add(ironhold$inputVector(input, speed, self.getYRot()));
        Vec3 cmd = new Vec3(lv.x, lv.y - MOON_PULL, lv.z);

        // Move (rotate command to world for collision).
        self.setDeltaMovement(ironhold$rot(cmd, gq));
        self.move(MoverType.SELF, self.getDeltaMovement());

        // Ground detection from the real collision result: move() zeroes the world axis it
        // collided on, so if we commanded "down" (toward the face) and the local-down
        // component came back near zero, we're standing on the face.
        Vec3 post = ironhold$rot(self.getDeltaMovement(), gqInv);
        boolean onGround = cmd.y < 0.0 && post.y > cmd.y + 1.0e-4;
        // Terminal velocity: drag the toward-face speed like vanilla (x0.98) so it can't
        // run away and punch through the face (which caused the overshoot/pushout bounce).
        double ny = onGround ? 0.0 : post.y * 0.98;

        // Friction on the tangent plane for next tick; keep the collision-resolved vertical.
        Vec3 lvNext = new Vec3(post.x * friction, ny, post.z * friction);
        self.setDeltaMovement(ironhold$rot(lvNext, gq));
        self.setOnGround(onGround);

        if (!self.level().isClientSide() && (ironhold$dbg++ % 4 == 0)) {
            double posAlongG = g.getStepX() * self.getX() + g.getStepY() * self.getY() + g.getStepZ() * self.getZ();
            kingdom.smp.Ironhold.LOGGER.info(
                "[MoonMove] g={} onGround={} cmdY={} postY={} posAlongG={}",
                g, onGround, String.format("%.4f", cmd.y), String.format("%.4f", post.y),
                String.format("%.3f", posAlongG));
        }

        ci.cancel();
    }

    private static int ironhold$dbg = 0;

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ironhold$moonJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ironhold$moonActive(self)) return;
        Quaternionf gq = GravityHelper.getGravityRotation(self);

        // Jump away from the face (local +Y), preserving tangential motion.
        Vec3 lv = ironhold$rot(self.getDeltaMovement(), gq.conjugate(new Quaternionf()));
        lv = new Vec3(lv.x, JUMP_POWER, lv.z);
        self.setDeltaMovement(ironhold$rot(lv, gq));
        ci.cancel();
    }
}
