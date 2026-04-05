package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Den-maintenance goal: when idle, a filcher returns to its shared den site
 * (a dark, sheltered position it and its pack-mates consider home).
 *
 * <p>If this filcher has no den yet, it searches nearby for a dark air block
 * with solid ground beneath it and claims that position. It then broadcasts
 * the den coordinates to nearby filchers that also lack one via
 * {@link FilcherEntity#shareDenWithNearby()}.
 *
 * <p>Once at the den the filcher rests quietly, periodically sharing the
 * den location with any newcomers that wander close enough.
 */
public class FilcherDenGoal extends Goal {

    private static final double ARRIVE_DIST_SQ = 3.0 * 3.0;
    private static final int    MAX_TICKS      = 300;
    private static final int    RECALC_INTERVAL = 40;

    private final FilcherEntity filcher;
    private int ticksActive;

    public FilcherDenGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;
        // Heavily loaded filchers reliably head home; lightly loaded ones wander home occasionally
        int stashed = filcher.countStashedItems();
        return stashed >= 4 || filcher.getRandom().nextInt(120) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;
        return ticksActive < MAX_TICKS;
    }

    @Override
    public void start() {
        ticksActive = 0;

        // Establish a den if we don't have one yet
        if (filcher.getDenPos() == null) {
            BlockPos den = filcher.findCaveEntrance();
            if (den != null) {
                filcher.setDenPos(den);
                filcher.shareDenWithNearby();
            }
        }
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        ticksActive = 0;
    }

    @Override
    public void tick() {
        ticksActive++;

        BlockPos den = filcher.getDenPos();
        if (den == null) return;

        double distSq = filcher.blockPosition().distSqr(den);
        if (distSq <= ARRIVE_DIST_SQ) {
            filcher.getNavigation().stop();
            // Periodically share den location with nearby newcomers
            if (ticksActive % 100 == 0) {
                filcher.shareDenWithNearby();
            }
            return;
        }

        if (ticksActive % RECALC_INTERVAL == 0) {
            filcher.getNavigation().moveTo(den.getX() + 0.5, den.getY(), den.getZ() + 0.5, 0.85);
        }
    }

}
