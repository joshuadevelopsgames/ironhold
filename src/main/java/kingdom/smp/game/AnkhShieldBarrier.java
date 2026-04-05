package kingdom.smp.game;

import kingdom.smp.item.AnkhShieldItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Geometry for the Ankh Shield magic barrier: clips line segments so particle beams stop on the
 * shell instead of passing through (see {@link AnkhShieldHandler} for gameplay).
 */
public final class AnkhShieldBarrier {
    /** Must match projectile interception in {@link AnkhShieldHandler}. */
    public static final double RADIUS = 2.35;

    private AnkhShieldBarrier() {}

    public static Vec3 barrierCenter(Player player) {
        return player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
    }

    /**
     * Shortens {@code end} toward {@code start} when the segment crosses any active Ankh barrier
     * sphere, so the effective end lies just inside the first barrier surface (from {@code start}).
     */
    public static Vec3 clipSegmentEnd(Vec3 start, Vec3 end, ServerLevel sl) {
        Vec3 d = end.subtract(start);
        double lenSq = d.lengthSqr();
        if (lenSq < 1.0e-8) {
            return end;
        }
        AABB box = segmentAabb(start, end).inflate(RADIUS + 0.5);
        double minT = 1.0;
        for (Player player : sl.getEntitiesOfClass(Player.class, box, LivingEntity::isAlive)) {
            if (!AnkhShieldItem.isBlockingWithAnkh(player)) {
                continue;
            }
            Vec3 c = barrierCenter(player);
            Double t = firstSegmentSphereIntersectionT(start, end, c, RADIUS);
            if (t != null && t < minT) {
                minT = t;
            }
        }
        if (minT >= 1.0) {
            return end;
        }
        double len = Math.sqrt(lenSq);
        double pullT = Math.min(0.02 / len, minT * 0.45);
        return start.add(d.scale(Math.max(0.0, minT - pullT)));
    }

    private static AABB segmentAabb(Vec3 a, Vec3 b) {
        return new AABB(
            Math.min(a.x, b.x),
            Math.min(a.y, b.y),
            Math.min(a.z, b.z),
            Math.max(a.x, b.x),
            Math.max(a.y, b.y),
            Math.max(a.z, b.z));
    }

    /**
     * Smallest {@code t} in {@code [0, 1]} where {@code start + t * (end - start)} lies on the
     * sphere surface, or {@code null} if the segment does not intersect the sphere in that range.
     */
    private static @Nullable Double firstSegmentSphereIntersectionT(Vec3 start, Vec3 end, Vec3 center, double radius) {
        Vec3 d = end.subtract(start);
        Vec3 f = start.subtract(center);
        double a = d.dot(d);
        if (a < 1.0e-12) {
            return null;
        }
        double b = 2.0 * f.dot(d);
        double c = f.dot(f) - radius * radius;
        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return null;
        }
        double sqrtD = Math.sqrt(disc);
        double t1 = (-b - sqrtD) / (2.0 * a);
        double t2 = (-b + sqrtD) / (2.0 * a);
        double best = Double.POSITIVE_INFINITY;
        if (t1 >= 0.0 && t1 <= 1.0) {
            best = Math.min(best, t1);
        }
        if (t2 >= 0.0 && t2 <= 1.0) {
            best = Math.min(best, t2);
        }
        return best == Double.POSITIVE_INFINITY ? null : best;
    }
}
