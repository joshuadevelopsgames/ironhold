package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

/**
 * Pack-hierarchy goal: a filcher gravitates toward the richest member of
 * its local group and trails it at a respectful distance.
 *
 * <p>"Richest" is defined by {@link FilcherEntity#getTotalLootValue()} —
 * rarity and count across both mainhand and stash. The alpha filcher must
 * be strictly wealthier than this one; a filcher that IS the alpha does
 * not follow anyone.
 *
 * <p>More sociable filchers are more likely to engage this goal on any
 * given evaluation.
 */
public class FilcherFollowAlphaGoal extends Goal {

    private static final double SCAN_RANGE    = 20.0;
    private static final double FOLLOW_DIST   = 4.0;
    private static final double GIVE_UP_DIST  = 30.0;
    private static final int    CHATTER_TICKS = 60;

    private final FilcherEntity filcher;
    private FilcherEntity alpha;
    private int ticksActive;

    public FilcherFollowAlphaGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;
        alpha = findAlpha();
        return alpha != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (alpha == null || !alpha.isAlive()) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getTarget() != null) return false;
        if (filcher.getScatterTicks() > 0) return false;
        return filcher.distanceToSqr(alpha) <= GIVE_UP_DIST * GIVE_UP_DIST;
    }

    @Override
    public void start() {
        ticksActive = 0;
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        alpha = null;
        ticksActive = 0;
    }

    @Override
    public void tick() {
        if (alpha == null) return;
        ticksActive++;

        if (ticksActive % CHATTER_TICKS == 0) {
            filcher.playChatter();
        }

        double distSq = filcher.distanceToSqr(alpha);
        if (distSq > FOLLOW_DIST * FOLLOW_DIST) {
            filcher.getNavigation().moveTo(alpha.getX(), alpha.getY(), alpha.getZ(), 0.9);
        } else {
            filcher.getNavigation().stop();
            filcher.getLookControl().setLookAt(alpha, 30.0F, 30.0F);
        }
    }

    /**
     * Finds the nearby filcher with the highest total loot value that
     * strictly exceeds this filcher's own wealth.
     */
    private FilcherEntity findAlpha() {
        int myValue = filcher.getTotalLootValue();

        List<FilcherEntity> richer = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(SCAN_RANGE),
            f -> f != filcher && f.isAlive() && f.getTotalLootValue() > myValue
        );
        if (richer.isEmpty()) return null;

        return richer.stream()
            .max((a, b) -> Integer.compare(a.getTotalLootValue(), b.getTotalLootValue()))
            .orElse(null);
    }
}
