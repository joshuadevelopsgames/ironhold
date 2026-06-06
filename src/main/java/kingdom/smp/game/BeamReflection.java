package kingdom.smp.game;

import kingdom.smp.entity.MirrorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Traces a scepter beam through the world, bouncing off {@link MirrorEntity} panes so players can
 * bank shots around corners for trick-shot attacks. A single range budget is shared across all
 * bounces; the path stops at the first non-mirror target, the first solid block, the bounce limit,
 * or when the budget runs out.
 *
 * <p>This is pure geometry plus read-only world queries: it spawns nothing and deals no damage.
 * Callers turn {@link Result#segments} into beam visuals and apply their own hit effects at
 * {@link Result#entityHit} / {@link Result#impact}.
 */
public final class BeamReflection {

    /** A straight portion of the path between two bounce points (or origin/impact). */
    public record Segment(Vec3 start, Vec3 end) {}

    /** The traced path: ordered segments, the mirror bounce points, and the final hit. */
    public record Result(List<Segment> segments, List<Vec3> bouncePoints,
                         @Nullable EntityHitResult entityHit, Vec3 impact) {}

    /** Max mirror bounces before the beam gives up — guards against parallel-mirror loops. */
    private static final int MAX_BOUNCES = 5;
    /** Nudge the next segment off the mirror surface so it can't immediately re-hit the same pane. */
    private static final double SURFACE_EPSILON = 0.01;

    private BeamReflection() {}

    public static Result trace(ServerLevel level, Entity caster, Vec3 start, Vec3 direction,
                               double range, Predicate<Entity> targetFilter) {
        List<Segment> segments = new ArrayList<>();
        List<Vec3> bouncePoints = new ArrayList<>();
        Vec3 segStart = start;
        Vec3 dir = direction.normalize();
        double remaining = range;
        EntityHitResult finalEntity = null;
        Vec3 impact = start;

        for (int bounce = 0; bounce <= MAX_BOUNCES; bounce++) {
            Vec3 segEnd = segStart.add(dir.scale(remaining));

            // A solid block bounds the segment: nothing past a wall can be hit or reflected.
            BlockHitResult blockHit = level.clip(new ClipContext(
                segStart, segEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
            Vec3 blockPoint = blockHit.getType() == HitResult.Type.MISS ? segEnd : blockHit.getLocation();
            double limitSqr = segStart.distanceToSqr(blockPoint);

            // Nearest reflecting mirror in front of the beam, ahead of the block.
            MirrorEntity mirror = null;
            Vec3 mirrorPoint = null;
            double mirrorDistSqr = Double.MAX_VALUE;
            AABB span = new AABB(segStart, blockPoint).inflate(1.0);
            for (MirrorEntity m : level.getEntitiesOfClass(MirrorEntity.class, span)) {
                Vec3 normal = m.getDirection().getUnitVec3();
                if (dir.dot(normal) >= 0) continue; // only the reflective front face bounces
                Optional<Vec3> clip = m.getBoundingBox().clip(segStart, blockPoint);
                if (clip.isEmpty()) continue;
                double dSqr = segStart.distanceToSqr(clip.get());
                if (dSqr < mirrorDistSqr && dSqr <= limitSqr) {
                    mirrorDistSqr = dSqr;
                    mirror = m;
                    mirrorPoint = clip.get();
                }
            }

            // Nearest targetable entity, also bounded by the block.
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                caster, segStart, blockPoint, span, targetFilter, limitSqr);
            double entityDistSqr = entityHit != null
                ? segStart.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

            // Mirror is the closest thing the beam meets -> reflect and keep tracing.
            if (mirror != null && mirrorDistSqr <= entityDistSqr) {
                segments.add(new Segment(segStart, mirrorPoint));
                bouncePoints.add(mirrorPoint);
                impact = mirrorPoint;
                Vec3 normal = mirror.getDirection().getUnitVec3();
                dir = dir.subtract(normal.scale(2 * dir.dot(normal))).normalize();
                remaining -= Math.sqrt(mirrorDistSqr);
                segStart = mirrorPoint.add(dir.scale(SURFACE_EPSILON));
                if (remaining <= SURFACE_EPSILON) break;
                continue;
            }

            // A targetable entity is closer than the block -> that is the impact.
            if (entityHit != null && entityDistSqr <= limitSqr) {
                segments.add(new Segment(segStart, entityHit.getLocation()));
                finalEntity = entityHit;
                impact = entityHit.getLocation();
                break;
            }

            // Otherwise the beam ends on a block or simply runs out of range.
            segments.add(new Segment(segStart, blockPoint));
            impact = blockPoint;
            break;
        }

        return new Result(segments, bouncePoints, finalEntity, impact);
    }
}
