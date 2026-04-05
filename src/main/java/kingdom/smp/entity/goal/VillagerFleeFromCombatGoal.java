package kingdom.smp.entity.goal;

import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.entity.VillagerProfession;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Villagers with low boldness flee when hostile mobs are nearby.
 * Guards never flee. Boldness determines the flee threshold distance.
 */
public class VillagerFleeFromCombatGoal extends Goal {

    private final KingdomVillagerEntity villager;
    private Vec3 fleeTarget;

    public VillagerFleeFromCombatGoal(KingdomVillagerEntity villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Guards don't flee
        if (villager.getProfession() == VillagerProfession.GUARD) return false;

        float boldness = villager.getPersonality().boldness();
        // Bold villagers (>0.7) don't flee at all
        if (boldness > 0.7f) return false;

        // Detection range: less bold = more sensitive (12-20 blocks)
        double range = 12.0 + (1.0 - boldness) * 8.0;

        AABB area = villager.getBoundingBox().inflate(range);
        List<Monster> threats = villager.level().getEntitiesOfClass(Monster.class, area,
            m -> m.isAlive() && villager.distanceToSqr(m) < range * range);

        if (threats.isEmpty()) return false;

        // Flee away from the nearest threat
        LivingEntity nearest = threats.getFirst();
        for (Monster m : threats) {
            if (villager.distanceToSqr(m) < villager.distanceToSqr(nearest)) {
                nearest = m;
            }
        }

        Vec3 away = villager.position().subtract(nearest.position()).normalize().scale(16);
        Vec3 target = villager.position().add(away);
        Vec3 safe = LandRandomPos.getPosTowards(villager, 16, 7, target);

        if (safe == null) {
            safe = target;
        }

        this.fleeTarget = safe;

        // Mood hit: fear
        villager.getPersonality().shiftMood(-0.15f);
        villager.sendEmote(KingdomVillagerEntity.EmoteType.SWEAT);

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !villager.getNavigation().isDone();
    }

    @Override
    public void start() {
        float speed = 1.0f + villager.getPersonality().energy() * 0.3f;
        villager.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, speed);
    }
}
