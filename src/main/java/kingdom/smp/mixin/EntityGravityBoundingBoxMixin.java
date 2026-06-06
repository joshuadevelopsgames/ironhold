package kingdom.smp.mixin;

import kingdom.smp.moon.GravityHelper;
import kingdom.smp.moon.ModMoonDimensions;
import kingdom.smp.moon.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives moon-face entities their REAL 0.6×1.8×0.6 box, rotated into the gravity frame, instead
 * of the old 0.6³ cube hack. Because gravity is cardinal the rotation is exactly 90°, so the box
 * stays axis-aligned in world space (e.g. 0.6×0.6 footprint on the wall, 1.8 along the wall
 * normal) — and the gravity-frame collision rewrite resolves movement against it without
 * tunnelling. This is what lets vanilla pose/step/standing work identically to the ground.
 */
@Mixin(Entity.class)
public abstract class EntityGravityBoundingBoxMixin {

    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;",
            at = @At("RETURN"), cancellable = true)
    private void ironhold$reorientBox(Vec3 pos, CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().dimension().equals(ModMoonDimensions.MOON_LEVEL)) return;

        Direction g = GravityHelper.getGravityDirection(self);
        if (g == Direction.DOWN) return;

        // Rotate the vanilla box about the entity's position into the gravity frame — no epsilon
        // nudge. Earlier code (and the previous "fix") shifted the local box down 1e-6 to overlap
        // the floor block, on the theory that a slight penetration would let Shapes.collide push
        // the entity back to flush each tick. The opposite happens: VoxelShape.collideX (see
        // {@code findIndex(... min + 1e-7)} + the {@code for a = aMin - 1} loop) deliberately
        // SKIPS any block the entity is already overlapping on the motion axis. So a 1e-6 nudge
        // causes the gravity sweep to find no collider, motion returns unclipped, and gravity
        // pulls the entity straight through the face every tick (the "burying" the user reported
        // on all faces; on NORTH/WEST gravities the original code lacked the nudge, which is why
        // they were the only faces that ever rested). With a flush rotated box (minY == floor's
        // max on the gravity axis), Shapes.collide correctly clips gravity-pull motion to 0 on
        // both positive- and negative-axis gravity directions — verified analytically for g=UP
        // and g=NORTH.
        AABB local = cir.getReturnValue().move(-pos.x, -pos.y, -pos.z);
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(local, g).move(pos.x, pos.y, pos.z));
    }
}
