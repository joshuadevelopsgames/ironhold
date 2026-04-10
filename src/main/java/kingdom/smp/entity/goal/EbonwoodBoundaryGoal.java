package kingdom.smp.entity.goal;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Keeps a mob within the Ebonwood Hollow biome. When the mob reaches a
 * non-ebonwood block, it turns around and walks back toward the biome center.
 *
 * <p>Optimized: only checks biome every 40 ticks (~2 seconds).
 */
public class EbonwoodBoundaryGoal extends Goal {

    private final PathfinderMob mob;
    private int checkCooldown = 0;
    private boolean redirecting = false;

    public EbonwoodBoundaryGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (checkCooldown > 0) {
            checkCooldown--;
            return false;
        }
        checkCooldown = 40;

        // Only activate if the mob is outside ebonwood
        BlockPos pos = mob.blockPosition();
        return !mob.level().getBiome(pos).is(Ironhold.EBONWOOD_HOLLOW);
    }

    @Override
    public boolean canContinueToUse() {
        // Keep redirecting until we're back in ebonwood or navigation finishes
        if (mob.level().getBiome(mob.blockPosition()).is(Ironhold.EBONWOOD_HOLLOW)) {
            return false;
        }
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        redirecting = true;
        // Search nearby for an ebonwood block to walk toward
        Vec3 target = findNearestEbonwoodDirection();
        if (target != null) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
    }

    @Override
    public void stop() {
        redirecting = false;
    }

    /**
     * Sample 8 directions + center to find the nearest block that's still
     * in ebonwood, and return a position 8 blocks toward it.
     */
    private Vec3 findNearestEbonwoodDirection() {
        BlockPos pos = mob.blockPosition();
        double bestDistSq = Double.MAX_VALUE;
        BlockPos bestPos = null;

        // Check 8 compass directions at distances 8, 16
        for (int dist = 8; dist <= 16; dist += 8) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos check = pos.offset(dx * dist, 0, dz * dist);
                    if (mob.level().getBiome(check).is(Ironhold.EBONWOOD_HOLLOW)) {
                        double d = check.distSqr(pos);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            bestPos = check;
                        }
                    }
                }
            }
        }

        if (bestPos != null) {
            return Vec3.atCenterOf(bestPos);
        }
        // Fallback: walk back the way we came
        Vec3 motion = mob.getDeltaMovement();
        return mob.position().subtract(motion.normalize().scale(12));
    }
}
