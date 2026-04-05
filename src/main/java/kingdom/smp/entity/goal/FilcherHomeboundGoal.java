package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Priority-2 goal: when a filcher strays more than 65 blocks from its den it
 * urgently navigates straight home. This enforces the 75-block home range —
 * filchers are cave creatures and do not willingly wander far from their den.
 */
public class FilcherHomeboundGoal extends Goal {

    private static final double TRIGGER_DIST_SQ      = 65.0 * 65.0;
    private static final double ARRIVE_DIST_SQ        = 10.0 * 10.0;
    private static final double KING_TRIGGER_DIST_SQ  = 5.0 * 5.0;
    private static final double KING_ARRIVE_DIST_SQ   = 2.0 * 2.0;
    private static final double SPEED                  = 1.1;

    private final FilcherEntity filcher;

    public FilcherHomeboundGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos den = filcher.getDenPos();
        if (den == null) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        return filcher.distanceToSqr(Vec3.atCenterOf(den)) > (filcher.isKing() ? KING_TRIGGER_DIST_SQ : TRIGGER_DIST_SQ);
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos den = filcher.getDenPos();
        if (den == null) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        return filcher.distanceToSqr(Vec3.atCenterOf(den)) > (filcher.isKing() ? KING_ARRIVE_DIST_SQ : ARRIVE_DIST_SQ);
    }

    @Override
    public void tick() {
        BlockPos den = filcher.getDenPos();
        if (den == null) return;
        filcher.getNavigation().moveTo(den.getX() + 0.5, den.getY(), den.getZ() + 0.5, SPEED);
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
    }
}
