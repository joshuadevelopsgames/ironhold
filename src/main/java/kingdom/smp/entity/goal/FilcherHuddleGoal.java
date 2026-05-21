package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Low-priority idle goal: clusters filchers together in small groups
 * when they have nothing better to do.
 *
 * <p>A filcher finds the nearest peer and sidles up to within
 * {@link #HUDDLE_DIST} blocks, then stands still and chatters quietly.
 * More sociable filchers are more likely to initiate a huddle on any
 * given tick.
 */
public class FilcherHuddleGoal extends Goal {

    private static final double SCAN_RANGE      = 16.0;
    private static final double HUDDLE_DIST     = 3.0;
    private static final double GIVE_UP_DIST_SQ = 24.0 * 24.0;
    private static final int    CHATTER_INTERVAL = 40;

    private final FilcherEntity filcher;
    private FilcherEntity partner;
    private int ticksActive;

    public FilcherHuddleGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    // Goal-evaluation throttle: canUse() does an AABB entity scan, too costly to
    // run every tick. Cap to once per 15 ticks per filcher, staggered by entity id.
    private static final int CAN_USE_INTERVAL = 15;
    private boolean cachedCanUse = false;
    private int lastCanUseTick = -1;

    @Override
    public boolean canUse() {
        int tick = filcher.tickCount;
        int stagger = filcher.getId() % CAN_USE_INTERVAL;
        if (tick - lastCanUseTick < CAN_USE_INTERVAL && ((tick + stagger) % CAN_USE_INTERVAL) != 0) {
            return cachedCanUse;
        }
        lastCanUseTick = tick;
        cachedCanUse = evaluateCanUse();
        return cachedCanUse;
    }

    private boolean evaluateCanUse() {
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;

        partner = findNearestFilcher();
        return partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (partner == null || !partner.isAlive()) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;
        return filcher.distanceToSqr(partner) <= GIVE_UP_DIST_SQ;
    }

    @Override
    public void start() {
        ticksActive = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        partner = null;
        ticksActive = 0;
    }

    @Override
    public void tick() {
        if (partner == null) return;
        ticksActive++;

        if (ticksActive % CHATTER_INTERVAL == 0) {
            filcher.playChatter();
        }

        double distSq = filcher.distanceToSqr(partner);
        if (distSq > HUDDLE_DIST * HUDDLE_DIST) {
            filcher.getNavigation().moveTo(partner.getX(), partner.getY(), partner.getZ(), 0.8);
        } else {
            filcher.getNavigation().stop();
            filcher.getLookControl().setLookAt(partner, 30.0F, 30.0F);
        }
    }

    private FilcherEntity findNearestFilcher() {
        List<FilcherEntity> nearby = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(SCAN_RANGE),
            f -> f != filcher && f.isAlive()
        );
        if (nearby.isEmpty()) return null;
        return nearby.stream()
            .min((a, b) -> Double.compare(filcher.distanceToSqr(a), filcher.distanceToSqr(b)))
            .orElse(null);
    }
}
