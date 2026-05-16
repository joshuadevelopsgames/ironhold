package kingdom.smp.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Periodic sprint bursts toward the target (tournament charge cadence).
 */
public final class KnightJousterChargeGoal extends Goal {

    private final KnightJousterEntity jouster;
    private int chargeTicks;
    private int cooldown;

    public KnightJousterChargeGoal(KnightJousterEntity jouster) {
        this.jouster = jouster;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = jouster.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = jouster.distanceTo(target);
        return jouster.onGround() && distance > 4.2 && distance < 22;
    }

    @Override
    public boolean canContinueToUse() {
        return chargeTicks > 0 && jouster.getTarget() != null && jouster.getTarget().isAlive();
    }

    @Override
    public void start() {
        chargeTicks = 36;
        jouster.addEffect(new MobEffectInstance(MobEffects.SPEED, 42, 1, false, false, true));
        LivingEntity target = jouster.getTarget();
        if (target != null) {
            jouster.getNavigation().moveTo(target, 1.88);
        }
    }

    @Override
    public void tick() {
        chargeTicks--;
        LivingEntity target = jouster.getTarget();
        if (target != null && jouster.tickCount % 5 == 0) {
            jouster.getNavigation().moveTo(target, 1.88);
        }
    }

    @Override
    public void stop() {
        chargeTicks = 0;
        cooldown = 95 + jouster.getRandom().nextInt(35);
    }
}
