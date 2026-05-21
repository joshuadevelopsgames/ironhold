package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import kingdom.smp.npc.NpcDisposition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Shared scaffolding for the ambient behaviours a "moved-in"
 * ({@link NpcDisposition#STATIONED}) NPC performs around its anchor: wandering,
 * peeking in chests, planting flowers, sleeping. Every one of these is hard-
 * leashed to {@link #LEASH_RADIUS} blocks of the station so the NPC never
 * strays from the base the player settled them at.
 */
public abstract class StationGoalBase extends Goal {

    /** Hard leash — a stationed NPC never wanders more than this from the anchor. */
    public static final int LEASH_RADIUS = 40;
    protected static final double LEASH_SQ = (double) LEASH_RADIUS * LEASH_RADIUS;

    protected final NpcCompanion companion;
    protected final PathfinderMob mob;

    protected StationGoalBase(NpcCompanion companion) {
        this.companion = companion;
        this.mob = companion.companionMob();
    }

    protected boolean isStationed() {
        return companion.disposition() == NpcDisposition.STATIONED && companion.stationPos() != null;
    }

    protected @Nullable BlockPos station() {
        return companion.stationPos();
    }

    protected boolean withinLeash(BlockPos p) {
        BlockPos s = station();
        return s != null && s.distSqr(p) <= LEASH_SQ;
    }

    protected boolean withinLeash(double x, double y, double z) {
        BlockPos s = station();
        if (s == null) return false;
        double dx = x - (s.getX() + 0.5);
        double dy = y - (s.getY() + 0.5);
        double dz = z - (s.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= LEASH_SQ;
    }
}
