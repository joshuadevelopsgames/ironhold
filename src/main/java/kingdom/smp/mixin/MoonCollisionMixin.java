package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import kingdom.smp.moon.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Resolves collision in the gravity-local frame so the real (rotated) box sweeps without
 * tunnelling. Mirrors vanilla {@code Entity.collide}'s per-axis {@code Shapes.collide}, but the
 * "vertical" axis is the gravity axis (local +Y mapped to the world face-normal) and the two
 * tangent axes are the wall plane — so landing, wall-stop and standing all resolve against the
 * face you're on. World-space colliders are reused as-is; only the axis the sweep runs along is
 * remapped (valid because cardinal gravity = 90° rotation keeps everything axis-aligned).
 *
 * <p>NOTE: vanilla auto step-up (slabs/1-block lips) is not yet reimplemented for faces; that's
 * the next refinement. Basic walk/stand/jump/land work here.
 */
@Mixin(Entity.class)
public abstract class MoonCollisionMixin {

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void ironhold$collideInGravityFrame(Vec3 worldMove, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)) return;
        Direction g = GravityHelper.getGravityDirection(self);
        if (g == Direction.DOWN) return;
        if (worldMove.lengthSqr() == 0.0) { cir.setReturnValue(worldMove); return; }

        AABB box = self.getBoundingBox();
        List<VoxelShape> shapes = Entity.collectAllColliders(self, self.level(), box.expandTowards(worldMove));
        if (shapes.isEmpty()) { cir.setReturnValue(worldMove); return; }

        // Movement in the local frame: x/z tangent to the face, y along the face normal (down).
        Vec3 lm = RotationUtil.vecWorldToPlayer(worldMove, g);
        double pmX = lm.x, pmY = lm.y, pmZ = lm.z;

        // World axis + sign that each local axis maps onto.
        Direction dirX = RotationUtil.dirPlayerToWorld(Direction.EAST, g);
        Direction dirY = RotationUtil.dirPlayerToWorld(Direction.UP, g);
        Direction dirZ = RotationUtil.dirPlayerToWorld(Direction.SOUTH, g);

        // Resolve the gravity axis first (so we settle onto the floor), then the larger tangent,
        // then the smaller — exactly vanilla's ordering, just relabeled into the local frame.
        if (pmY != 0.0) {
            int s = dirY.getAxisDirection().getStep();
            pmY = Shapes.collide(dirY.getAxis(), box, shapes, pmY * s) * s;
            if (pmY != 0.0) box = box.move(RotationUtil.vecPlayerToWorld(0.0, pmY, 0.0, g));
        }

        boolean zLargerThanX = Math.abs(pmX) < Math.abs(pmZ);
        if (zLargerThanX && pmZ != 0.0) {
            int s = dirZ.getAxisDirection().getStep();
            pmZ = Shapes.collide(dirZ.getAxis(), box, shapes, pmZ * s) * s;
            if (pmZ != 0.0) box = box.move(RotationUtil.vecPlayerToWorld(0.0, 0.0, pmZ, g));
        }

        if (pmX != 0.0) {
            int s = dirX.getAxisDirection().getStep();
            pmX = Shapes.collide(dirX.getAxis(), box, shapes, pmX * s) * s;
            if (!zLargerThanX && pmX != 0.0) box = box.move(RotationUtil.vecPlayerToWorld(pmX, 0.0, 0.0, g));
        }

        if (!zLargerThanX && pmZ != 0.0) {
            int s = dirZ.getAxisDirection().getStep();
            pmZ = Shapes.collide(dirZ.getAxis(), box, shapes, pmZ * s) * s;
        }

        cir.setReturnValue(RotationUtil.vecPlayerToWorld(pmX, pmY, pmZ, g));
    }

    /**
     * Recompute onGround / horizontalCollision in the gravity frame. Vanilla {@code move()}
     * derives "grounded" from a world-Y check (delta.y &lt; 0 with a vertical stop), which is
     * meaningless on a wall where the floor axis is world-X/Z. We re-derive both flags from the
     * local-frame delta vs. clipped movement so friction, jumping and step logic treat the face
     * as the floor. (For the player our custom travel also sets onGround; this fixes mobs and is
     * the correct baseline.)
     */
    @Redirect(method = "move",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"))
    private void ironhold$gravityOnGround(Entity entity, boolean onGround, boolean horizontalCollision, Vec3 movement) {
        Direction g = entity.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)
                ? GravityHelper.getGravityDirection(entity) : Direction.DOWN;
        if (g == Direction.DOWN) {
            entity.setOnGroundWithMovement(onGround, horizontalCollision, movement);
            return;
        }
        Vec3 ld = RotationUtil.vecWorldToPlayer(entity.getDeltaMovement(), g);
        Vec3 lm = RotationUtil.vecWorldToPlayer(movement, g);
        boolean groundBelow = ld.y != lm.y && ld.y < 0.0;
        boolean horiz = (ld.x != lm.x) || (ld.z != lm.z);
        entity.setOnGroundWithMovement(groundBelow, horiz, movement);
    }
}
