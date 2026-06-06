package kingdom.smp.moon;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Pure coordinate transforms between WORLD space and a gravity-LOCAL ("player") frame in which
 * down is always -Y. Because moon gravity is always one of the six cardinal directions, every
 * transform is an exact 90° rotation — a permutation of the axes with sign flips — so an
 * axis-aligned box stays axis-aligned and vanilla collision works untouched. DOWN is the
 * identity transform, which is what makes the top face behave byte-for-byte like vanilla and
 * every other face run the SAME vanilla physics through a relabeling of axes.
 *
 * <p>Tables are taken verbatim from the GravityChanger / Gravity API mods (which are proper
 * inverses of each other and keep horizontal facing sensible); see
 * https://github.com/Gaider10/GravityChanger. {@code vecWorldToPlayer} and
 * {@code vecPlayerToWorld} are inverses; {@code mask*} variants ignore sign (for friction
 * multipliers etc. that are applied per-axis-magnitude).
 */
public final class RotationUtil {

    private RotationUtil() {}

    // --- world -> player (gravity-local, down = -Y) ---

    public static Vec3 vecWorldToPlayer(double x, double y, double z, Direction gravity) {
        return switch (gravity) {
            case DOWN  -> new Vec3( x,  y,  z);
            case UP    -> new Vec3(-x, -y,  z);
            case NORTH -> new Vec3( x,  z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST  -> new Vec3(-z,  x, -y);
            case EAST  -> new Vec3( z, -x, -y);
        };
    }

    public static Vec3 vecWorldToPlayer(Vec3 v, Direction gravity) {
        return vecWorldToPlayer(v.x, v.y, v.z, gravity);
    }

    // --- player -> world (inverse of the above) ---

    public static Vec3 vecPlayerToWorld(double x, double y, double z, Direction gravity) {
        return switch (gravity) {
            case DOWN  -> new Vec3( x,  y,  z);
            case UP    -> new Vec3(-x, -y,  z);
            case NORTH -> new Vec3( x, -z,  y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST  -> new Vec3( y, -z, -x);
            case EAST  -> new Vec3(-y, -z,  x);
        };
    }

    public static Vec3 vecPlayerToWorld(Vec3 v, Direction gravity) {
        return vecPlayerToWorld(v.x, v.y, v.z, gravity);
    }

    public static Vector3f vecPlayerToWorld(float x, float y, float z, Direction gravity) {
        Vec3 v = vecPlayerToWorld((double) x, (double) y, (double) z, gravity);
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    // --- sign-less (magnitude) permutations, for per-axis multipliers like friction ---

    public static Vec3 maskWorldToPlayer(double x, double y, double z, Direction gravity) {
        return switch (gravity) {
            case DOWN, UP     -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST   -> new Vec3(z, x, y);
        };
    }

    public static Vec3 maskWorldToPlayer(Vec3 v, Direction gravity) {
        return maskWorldToPlayer(v.x, v.y, v.z, gravity);
    }

    public static Vec3 maskPlayerToWorld(double x, double y, double z, Direction gravity) {
        return switch (gravity) {
            case DOWN, UP     -> new Vec3(x, y, z);
            case NORTH, SOUTH -> new Vec3(x, z, y);
            case WEST, EAST   -> new Vec3(y, z, x);
        };
    }

    public static Vec3 maskPlayerToWorld(Vec3 v, Direction gravity) {
        return maskPlayerToWorld(v.x, v.y, v.z, gravity);
    }

    // --- AABB transforms (corner-rotate then re-min/max; valid because 90° keeps it axis-aligned) ---

    public static AABB boxWorldToPlayer(AABB box, Direction gravity) {
        if (gravity == Direction.DOWN) return box;
        Vec3 a = vecWorldToPlayer(box.minX, box.minY, box.minZ, gravity);
        Vec3 b = vecWorldToPlayer(box.maxX, box.maxY, box.maxZ, gravity);
        return new AABB(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    public static AABB boxPlayerToWorld(AABB box, Direction gravity) {
        if (gravity == Direction.DOWN) return box;
        Vec3 a = vecPlayerToWorld(box.minX, box.minY, box.minZ, gravity);
        Vec3 b = vecPlayerToWorld(box.maxX, box.maxY, box.maxZ, gravity);
        return new AABB(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    // --- Direction transforms (exact, so nearest is exact) ---

    public static Direction dirWorldToPlayer(Direction dir, Direction gravity) {
        if (gravity == Direction.DOWN) return dir;
        Vec3 v = vecWorldToPlayer((double) dir.getStepX(), (double) dir.getStepY(), (double) dir.getStepZ(), gravity);
        return Direction.getApproximateNearest(v.x, v.y, v.z);
    }

    public static Direction dirPlayerToWorld(Direction dir, Direction gravity) {
        if (gravity == Direction.DOWN) return dir;
        Vec3 v = vecPlayerToWorld((double) dir.getStepX(), (double) dir.getStepY(), (double) dir.getStepZ(), gravity);
        return Direction.getApproximateNearest(v.x, v.y, v.z);
    }
}
