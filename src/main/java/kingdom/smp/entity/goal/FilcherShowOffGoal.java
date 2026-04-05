package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Social vanity goal: a filcher with loot seeks out nearby peers and
 * shows off its valuables to them.
 *
 * <h3>Phases</h3>
 * <ol>
 *   <li><b>Approach</b> — walks toward the nearest cluster of filchers.</li>
 *   <li><b>Display</b> — on arrival, calls {@link FilcherEntity#triggerShowOff()};
 *       the hop-and-particle animation runs via {@code aiStep()} while the goal
 *       holds MOVE+LOOK so nothing interrupts the performance.</li>
 * </ol>
 *
 * <p>Greedier filchers show off more often — they want others to know
 * how well they've done. The nearby filchers automatically turn to watch
 * (handled inside {@code aiStep()} when {@code showOffTicks} hits 35).
 *
 * <p>A spontaneous post-stash show-off is also triggered directly by
 * {@link FilcherCarryFleeGoal} the moment it stashes an item, so this
 * goal picks that up in the display phase immediately.
 */
public class FilcherShowOffGoal extends Goal {

    private static final double SCAN_RANGE    = 14.0;
    private static final double ARRIVE_DIST   = 3.5;
    private static final double GIVE_UP_DIST  = 22.0;

    private final FilcherEntity filcher;
    private FilcherEntity audience;
    private boolean approaching;

    public FilcherShowOffGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (filcher.getScatterTicks() > 0) return false;
        if (filcher.getTarget() != null) return false;

        // Must have something worth showing (mainhand OR stash)
        boolean hasLoot = !filcher.getMainHandItem().isEmpty()
            || !filcher.getLowestValueItem().isEmpty();
        if (!hasLoot) return false;

        // If already mid-celebration (triggered by CarryFleeGoal after stash),
        // skip the approach phase and just hold the flags
        if (filcher.isShowingOff()) {
            audience = null;
            approaching = false;
            return true;
        }

        // Greedy filchers show off more eagerly
        if (filcher.getRandom().nextFloat() > filcher.getGreed() * 0.6F) return false;

        audience = findNearestFilcher();
        if (audience == null) return false;
        approaching = true;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (filcher.getScatterTicks() > 0) return false;
        if (filcher.getTarget() != null) return false;
        // Stay active through the walk AND the animation
        if (approaching) {
            return audience != null && audience.isAlive()
                && filcher.distanceToSqr(audience) <= GIVE_UP_DIST * GIVE_UP_DIST;
        }
        return filcher.isShowingOff();
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        audience = null;
        approaching = false;
    }

    @Override
    public void tick() {
        if (approaching && audience != null) {
            double distSq = filcher.distanceToSqr(audience);
            if (distSq > ARRIVE_DIST * ARRIVE_DIST) {
                filcher.getNavigation().moveTo(
                    audience.getX(), audience.getY(), audience.getZ(), 0.85);
            } else {
                // Arrived — start the display
                filcher.getNavigation().stop();
                filcher.getLookControl().setLookAt(audience, 30.0F, 30.0F);
                if (!filcher.isShowingOff()) {
                    filcher.triggerShowOff();
                }
                approaching = false;
            }
        } else {
            // Display phase — stand still and let aiStep() run the animation
            filcher.getNavigation().stop();
            if (audience != null) {
                filcher.getLookControl().setLookAt(audience, 30.0F, 30.0F);
            }
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
