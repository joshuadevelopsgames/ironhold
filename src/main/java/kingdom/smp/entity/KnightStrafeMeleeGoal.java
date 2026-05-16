package kingdom.smp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Occasionally sidesteps during melee (veteran footwork).
 */
public final class KnightStrafeMeleeGoal extends Goal {

    private final KnightEntity knight;

    public KnightStrafeMeleeGoal(KnightEntity knight) {
        this.knight = knight;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = knight.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = knight.distanceTo(target);
        return distance > 2.4 && distance < 11;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = knight.getTarget();
        if (target == null || knight.swinging) {
            return;
        }

        if (((knight.tickCount + knight.getId()) % 22) != 0) {
            return;
        }

        Vec3 toTarget = target.position().subtract(knight.position());
        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horizontal < 0.08) {
            return;
        }

        Vec3 perpendicular =
            new Vec3(-toTarget.z / horizontal, 0.0, toTarget.x / horizontal)
                .scale(knight.getRandom().nextBoolean() ? 2.35 : -2.35);
        Vec3 dest = knight.position().add(perpendicular);
        knight.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.96);
    }
}
