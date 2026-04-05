package kingdom.smp.entity.goal;

import kingdom.smp.entity.KingdomVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Wander goal that keeps Kingdom Villagers within a radius of their spawn/home point.
 * Movement speed scales with the villager's energy trait.
 */
public class VillagerWanderGoal extends Goal {

    private final KingdomVillagerEntity villager;
    private final double maxRange;
    private double targetX, targetY, targetZ;

    public VillagerWanderGoal(KingdomVillagerEntity villager, double maxRange) {
        this.villager = villager;
        this.maxRange = maxRange;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.getNavigation().isInProgress()) return false;
        if (villager.getRandom().nextInt(120) != 0) return false;

        BlockPos home = villager.getHomePos();
        Vec3 pos;

        // If too far from home, head back
        if (home != null && villager.distanceToSqr(Vec3.atCenterOf(home)) > maxRange * maxRange) {
            pos = Vec3.atCenterOf(home).add(
                (villager.getRandom().nextDouble() - 0.5) * 8,
                0,
                (villager.getRandom().nextDouble() - 0.5) * 8);
        } else {
            pos = LandRandomPos.getPos(villager, 10, 7);
        }

        if (pos == null) return false;

        this.targetX = pos.x;
        this.targetY = pos.y;
        this.targetZ = pos.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !villager.getNavigation().isDone();
    }

    @Override
    public void start() {
        float speed = 0.5f + villager.getPersonality().energy() * 0.3f;
        villager.getNavigation().moveTo(targetX, targetY, targetZ, speed);
    }
}
