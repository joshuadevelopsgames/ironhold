package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A stationed NPC ambles around their settled area at a relaxed pace, picking
 * walk targets that stay within the {@link #LEASH_RADIUS}-block leash of the
 * anchor.
 */
public class NpcStationWanderGoal extends StationGoalBase {

    private static final double SPEED = 0.7;
    private double tx, ty, tz;

    public NpcStationWanderGoal(NpcCompanion companion) {
        super(companion);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!isStationed()) return false;
        if (mob.getRandom().nextInt(80) != 0) return false;
        Vec3 v = LandRandomPos.getPos(mob, 10, 5);
        if (v == null || !withinLeash(v.x, v.y, v.z)) return false;
        this.tx = v.x;
        this.ty = v.y;
        this.tz = v.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return isStationed() && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(tx, ty, tz, SPEED);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
