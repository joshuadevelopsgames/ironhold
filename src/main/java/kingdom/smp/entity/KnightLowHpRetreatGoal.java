package kingdom.smp.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Backs away from the current target while badly wounded (recruits).
 */
public final class KnightLowHpRetreatGoal extends Goal {

    private final KnightEntity knight;
    private int ticksLeft;

    public KnightLowHpRetreatGoal(KnightEntity knight) {
        this.knight = knight;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = knight.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (knight.getHealth() > knight.getMaxHealth() * 0.34f) {
            return false;
        }
        return knight.getRandom().nextInt(10) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = knight.getTarget();
        return ticksLeft > 0
            && target != null
            && target.isAlive()
            && knight.getHealth() <= knight.getMaxHealth() * 0.48f;
    }

    @Override
    public void start() {
        ticksLeft = 35 + knight.getRandom().nextInt(35);
    }

    @Override
    public void tick() {
        ticksLeft--;
        LivingEntity target = knight.getTarget();
        if (target == null) {
            return;
        }

        Vec3 away = knight.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-6) {
            return;
        }
        away = away.normalize().scale(14);
        knight.getNavigation().moveTo(knight.getX() + away.x, knight.getY(), knight.getZ() + away.z, 1.22);
    }

    @Override
    public void stop() {
        ticksLeft = 0;
    }
}
