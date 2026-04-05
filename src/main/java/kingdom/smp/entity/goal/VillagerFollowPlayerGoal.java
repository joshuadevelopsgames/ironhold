package kingdom.smp.entity.goal;

import kingdom.smp.entity.KingdomVillagerEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * GUARD profession goal: follow the assigned player as a combat escort.
 * Expires after a set duration (ticks). Only one guard per player.
 */
public class VillagerFollowPlayerGoal extends Goal {

    private final KingdomVillagerEntity villager;
    private Player target;
    private int remainingTicks;
    private int recalcCooldown;

    public VillagerFollowPlayerGoal(KingdomVillagerEntity villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player assigned = villager.getFollowTarget();
        if (assigned == null || !assigned.isAlive()) return false;
        if (villager.distanceToSqr(assigned) > 256 * 256) return false; // too far, give up
        this.target = assigned;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !target.isAlive()) return false;
        if (villager.getFollowTicksRemaining() <= 0) return false;
        return villager.distanceToSqr(target) < 256 * 256;
    }

    @Override
    public void start() {
        this.recalcCooldown = 0;
    }

    @Override
    public void stop() {
        villager.clearFollowTarget();
        this.target = null;
    }

    @Override
    public void tick() {
        villager.getLookControl().setLookAt(target, 10.0F, villager.getMaxHeadXRot());

        if (--recalcCooldown <= 0) {
            recalcCooldown = 10;
            double distSq = villager.distanceToSqr(target);

            if (distSq > 144) { // > 12 blocks: teleport near
                villager.teleportTo(
                    target.getX() + (villager.getRandom().nextDouble() - 0.5) * 4,
                    target.getY(),
                    target.getZ() + (villager.getRandom().nextDouble() - 0.5) * 4);
            } else if (distSq > 9) { // > 3 blocks: walk to
                villager.getNavigation().moveTo(target, 1.1);
            } else {
                villager.getNavigation().stop();
            }
        }
    }
}
