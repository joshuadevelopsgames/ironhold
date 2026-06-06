package kingdom.smp.moon;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Directional-gravity math for the moon cube. Gravity points toward the moon's
 * core (the cube centre), snapped to whichever cardinal axis dominates, so an
 * entity is always held against the nearest of the six outer faces. A little
 * hysteresis stops the "down" axis from flickering when crossing an edge.
 */
public class GravityHelper {

    // Centre of the FLOATING solid cube (matches MoonChunkGenerator: x/z in [-128,127],
    // y in [32,223]).
    private static final double CORE_X = -0.5;
    private static final double CORE_Y = 127.5;
    private static final double CORE_Z = -0.5;

    // Half-extent of the cube along each axis. The cube is 256 wide in x/z but only ~192
    // tall, so we must normalise each axis by its own half-extent before comparing — that's
    // what makes the "diagonal planes from the core to each edge" land on the REAL cube
    // edges. (Comparing raw core-distances flipped gravity at x~96 instead of the edge,
    // because the shorter Y axis lost the comparison far too early.)
    private static final double HALF_X = 127.5;   // (127 - -128)/2
    private static final double HALF_Y = 95.5;    // (223 - 32)/2
    private static final double HALF_Z = 127.5;

    private static final Map<Entity, Direction> LAST_DIR =
        Collections.synchronizedMap(new WeakHashMap<>());

    // Once you're on a face, the adjacent-face normalised coordinate must beat the current
    // one by this margin before "down" flips. Kept small: on a side wall you're pinned so the
    // wall axis sits at |n|~1.0, and the cross axis only reaches 1.0 at the very edge — a big
    // margin would force you to fall well past the edge into the void before gravity caught
    // the next face (the "hard to jump west->up" feel). ~0.02 ≈ catch within a block or two of
    // the edge, while still absorbing float jitter exactly on the 45° diagonal.
    private static final double HYSTERESIS = 0.02;

    /**
     * Current gravity direction — a pure, side-effect-free READ. Returns the value the
     * tick chose ({@link #updateGravityDirection}), or a dominant-axis fallback. Safe from
     * any thread/context (camera render thread, makeBoundingBox during construction): it
     * neither probes collision nor mutates state, so the render thread can't perturb the
     * physics tick.
     */
    public static Direction getGravityDirection(Entity entity) {
        if (entity.level().dimension() != ModMoonDimensions.MOON_LEVEL) {
            return Direction.DOWN;
        }
        Direction cur = LAST_DIR.get(entity);
        if (cur != null) return cur;
        return dominantFace(entity.getX(), entity.getY(), entity.getZ());
    }

    /**
     * "Diagonal planes from the core to each edge" model (the user's mental model), made
     * correct for the non-cubic floating cube by normalising each axis by its half-extent.
     * Gravity points toward the core along whichever NORMALISED axis dominates, so each face
     * owns the pyramid between its 45° edge planes and the boundaries fall on the real cube
     * edges. A normalised hysteresis deadzone keeps the choice from flickering when you sit
     * right on a corner/edge (the g:east <-> g:up flutter). Single per-tick writer.
     */
    public static Direction updateGravityDirection(Entity entity) {
        if (entity.level().dimension() != ModMoonDimensions.MOON_LEVEL) {
            LAST_DIR.remove(entity);
            return Direction.DOWN;
        }

        double nx = (entity.getX() - CORE_X) / HALF_X;
        double ny = (entity.getY() - CORE_Y) / HALF_Y;
        double nz = (entity.getZ() - CORE_Z) / HALF_Z;

        Direction candidate = dominantAxisNorm(nx, ny, nz);
        Direction last = LAST_DIR.get(entity);
        if (last != null && last != candidate
                && normMag(candidate, nx, ny, nz) - normMag(last, nx, ny, nz) < HYSTERESIS) {
            candidate = last;
        }
        LAST_DIR.put(entity, candidate);
        return candidate;
    }

    /**
     * The shared gravity orientation quaternion. Read by the camera (smoothing target), the
     * pick-ray view vector, and the movement code as their world&lt;-&gt;local frame. Built
     * directly from the {@link RotationUtil} basis for the current cardinal direction, so it is
     * the EXACT same frame collision uses — and it is purely a function of the direction, so
     * there is no path-accumulated roll and therefore no holonomy twist on the top face.
     */
    public static Quaternionf getGravityRotation(Entity entity) {
        return gravityRotation(getGravityDirection(entity));
    }

    /** Quaternion mapping the gravity-local frame onto world for a cardinal direction (DOWN =
     *  identity). Columns are the world images of local +X/+Y/+Z under {@link RotationUtil}. */
    public static Quaternionf gravityRotation(Direction g) {
        if (g == Direction.DOWN) return new Quaternionf();
        Vec3 bx = RotationUtil.vecPlayerToWorld(1.0, 0.0, 0.0, g);
        Vec3 by = RotationUtil.vecPlayerToWorld(0.0, 1.0, 0.0, g);
        Vec3 bz = RotationUtil.vecPlayerToWorld(0.0, 0.0, 1.0, g);
        Matrix3f m = new Matrix3f(
            (float) bx.x, (float) bx.y, (float) bx.z,
            (float) by.x, (float) by.y, (float) by.z,
            (float) bz.x, (float) bz.y, (float) bz.z);
        return new Quaternionf().setFromNormalized(m).normalize();
    }

    private static double normMag(Direction dir, double nx, double ny, double nz) {
        switch (dir.getAxis()) {
            case X: return Math.abs(nx);
            case Y: return Math.abs(ny);
            case Z: return Math.abs(nz);
        }
        return 0.0;
    }

    /** Position-based pull direction (no hysteresis) — for per-frame rendering / models. */
    public static Direction getGravityDirectionAt(double x, double y, double z) {
        return dominantFace(x, y, z);
    }

    /**
     * The clamped first-person eye / pick-ray origin on a moon face. Cast OUTWARD from the
     * centre of the 0.6^3 collision box — the one point collision guarantees is clear, so the
     * ray can never start inside a wall — along the gravity-up normal, then stop just short of
     * the first block so it can't poke through an overhang. The camera render AND the logical
     * {@code getEyePosition} both call this, so the crosshair you see and the block you can
     * actually break/use always originate from the same point (previously the camera clamped
     * but getEyePosition didn't, so the pick-ray could sit buried in the wall). On the top
     * face (gravity DOWN) it's just the vanilla feet+eyeHeight.
     */
    public static Vec3 gravityEye(Entity entity, Vec3 feet, Direction gravity) {
        double eh = entity.getEyeHeight();
        if (gravity == Direction.DOWN) return feet.add(0.0, eh, 0.0);
        Vec3 normal = new Vec3(-gravity.getStepX(), -gravity.getStepY(), -gravity.getStepZ());
        // Body centre = feet + 0.9 along the face normal: the middle of the rotated 0.6×0.6×1.8
        // collision box, so this point is ALWAYS inside the entity and therefore always clear of
        // any moon block — on every face. (The old (0, 0.3, 0) world-Y offset was a leftover from
        // the 0.6³ hack-box era; on the bottom face it landed inside the floor block, which made
        // the eye clip snap to nonsense and showed up as the camera "clipping into blocks".)
        Vec3 boxCenter = feet.add(normal.scale(0.9));
        Vec3 desired = feet.add(normal.scale(eh));
        BlockHitResult hit = entity.level().clip(new ClipContext(
            boxCenter, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK
            ? hit.getLocation().subtract(normal.scale(0.1))
            : desired;
    }

    private static Direction dominantFace(double x, double y, double z) {
        return dominantAxisNorm((x - CORE_X) / HALF_X, (y - CORE_Y) / HALF_Y, (z - CORE_Z) / HALF_Z);
    }

    /** Largest normalised component decides the face; its sign decides which of the pair. */
    private static Direction dominantAxisNorm(double nx, double ny, double nz) {
        double ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ay >= ax && ay >= az) return ny >= 0 ? Direction.DOWN : Direction.UP;   // high y -> top -> pull down
        if (ax >= az)             return nx >= 0 ? Direction.WEST : Direction.EAST; // high x -> +X face -> pull -X
        return nz >= 0 ? Direction.NORTH : Direction.SOUTH;                          // high z -> +Z face -> pull -Z
    }


    // --- Frame transforms (world <-> gravity-local), used by the movement/camera
    //     reorientation in the next iteration. Local frame always has down = -Y. ---

    public static Vec3 worldToLocal(Vec3 vec, Direction gravity) {
        switch (gravity) {
            case DOWN:  return vec;
            case UP:    return new Vec3(-vec.x, -vec.y, vec.z);
            case WEST:  return new Vec3(vec.y, -vec.x, vec.z);
            case EAST:  return new Vec3(-vec.y, vec.x, vec.z);
            case NORTH: return new Vec3(vec.x, vec.z, -vec.y);
            case SOUTH: return new Vec3(vec.x, -vec.z, vec.y);
        }
        return vec;
    }

    public static Vec3 localToWorld(Vec3 vec, Direction gravity) {
        switch (gravity) {
            case DOWN:  return vec;
            case UP:    return new Vec3(-vec.x, -vec.y, vec.z);
            case WEST:  return new Vec3(-vec.y, vec.x, vec.z);
            case EAST:  return new Vec3(vec.y, -vec.x, vec.z);
            case NORTH: return new Vec3(vec.x, -vec.z, vec.y);
            case SOUTH: return new Vec3(vec.x, vec.z, vec.y);
        }
        return vec;
    }

    public static AABB localToWorld(AABB aabb, Direction gravity) {
        if (gravity == Direction.DOWN) return aabb;

        Vec3 min = localToWorld(new Vec3(aabb.minX, aabb.minY, aabb.minZ), gravity);
        Vec3 max = localToWorld(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ), gravity);

        return new AABB(
            Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z),
            Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z)
        );
    }
}
