package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * When a filcher is hurt, it flees from the attacker. Once it reaches a safe
 * distance and the player looks away, it stalks back to try stealing again.
 */
public class FilcherFleeAndStalkGoal extends Goal {

    private final FilcherEntity filcher;
    private Player attacker;
    private int fleeTicks;
    private int waitTicks;

    private enum Phase { FLEE, WAIT, STALK_BACK }
    private Phase phase = Phase.FLEE;

    public FilcherFleeAndStalkGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (filcher.getLastHurtByMob() instanceof Player p
                && filcher.tickCount - filcher.getLastHurtByMobTimestamp() < 5) {
            this.attacker = p;
            this.phase = Phase.FLEE;
            this.fleeTicks = 60 + filcher.getRandom().nextInt(40); // 3-5 sec flee
            this.waitTicks = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (attacker == null || !attacker.isAlive() || attacker.isRemoved()) return false;
        if (filcher.distanceToSqr(attacker) > 60.0 * 60.0) return false; // gave up, too far
        return phase != Phase.STALK_BACK || filcher.distanceToSqr(attacker) > 4.0;
    }

    @Override
    public void start() {
        filcher.playDistress();
        filcher.setStealCooldownTicks(100); // 5 sec before stealing again
        fleeFromAttacker();
    }

    @Override
    public void tick() {
        if (attacker == null) return;

        switch (phase) {
            case FLEE -> {
                fleeTicks--;
                if (fleeTicks <= 0 || filcher.distanceToSqr(attacker) > 20.0 * 20.0) {
                    phase = Phase.WAIT;
                    waitTicks = 40 + filcher.getRandom().nextInt(60); // 2-5 sec wait
                    filcher.getNavigation().stop();
                } else if (filcher.getNavigation().isDone()) {
                    fleeFromAttacker();
                }
            }
            case WAIT -> {
                // Hide and watch the attacker
                filcher.getLookControl().setLookAt(attacker, 30.0F, 30.0F);
                waitTicks--;
                if (waitTicks <= 0 && !isPlayerLookingAtFilcher()) {
                    // Player looked away — stalk back
                    phase = Phase.STALK_BACK;
                }
            }
            case STALK_BACK -> {
                // Approach from behind while player isn't looking
                if (isPlayerLookingAtFilcher()) {
                    // Caught! Freeze and look innocent
                    filcher.getNavigation().stop();
                } else {
                    // Sneak closer
                    filcher.getNavigation().moveTo(attacker, 0.9);
                }
                // Close enough — goal ends, steal goals can activate
                if (filcher.distanceToSqr(attacker) <= 4.0) {
                    filcher.setStealCooldownTicks(0);
                }
            }
        }
    }

    @Override
    public void stop() {
        this.attacker = null;
        filcher.getNavigation().stop();
    }

    private void fleeFromAttacker() {
        Vec3 away = filcher.position().subtract(attacker.position()).normalize();
        double angle = Math.atan2(away.z, away.x) + (filcher.getRandom().nextDouble() - 0.5) * 0.8;
        double dist = 18 + filcher.getRandom().nextInt(10);
        Vec3 target = filcher.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        filcher.getNavigation().moveTo(target.x, target.y, target.z, 1.6);
    }

    private boolean isPlayerLookingAtFilcher() {
        if (attacker == null) return false;
        Vec3 look = attacker.getLookAngle();
        Vec3 toFilcher = filcher.position().subtract(attacker.getEyePosition()).normalize();
        return look.dot(toFilcher) > 0.5; // within ~60° cone
    }
}
