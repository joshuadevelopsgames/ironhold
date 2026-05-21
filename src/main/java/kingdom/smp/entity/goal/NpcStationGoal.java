package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import kingdom.smp.npc.NpcDisposition;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

/**
 * The leash for a "moved-in" NPC ({@link NpcDisposition#STATIONED}). The other
 * station goals (wander, chests, flowers, sleep) let them roam their settled
 * area freely; this one only kicks in when they approach the edge of the
 * {@link #LEASH_RADIUS}-block boundary and walks them back toward the anchor,
 * guaranteeing they never drift away from the base.
 */
public class NpcStationGoal extends StationGoalBase {

    /** Start heading back a few blocks before the hard leash, so they never cross it. */
    private static final double RETURN_AT_SQ = (LEASH_RADIUS - 4) * (LEASH_RADIUS - 4);

    public NpcStationGoal(NpcCompanion companion) {
        super(companion);
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!isStationed()) return false;
        BlockPos s = station();
        return s != null && s.distSqr(mob.blockPosition()) > RETURN_AT_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        return isStationed() && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        BlockPos s = station();
        if (s != null) {
            mob.getNavigation().moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
