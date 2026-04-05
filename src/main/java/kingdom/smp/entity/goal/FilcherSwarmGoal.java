package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Distraction goal: when recruited by a mastermind for a coordinated heist,
 * this filcher rushes toward the player and chatters noisily — creating a
 * visual distraction while the mastermind circles around to steal from behind.
 *
 * <p>Unlike setting the filcher's combat target, this goal never triggers
 * melee; it only moves and makes noise.
 */
public class FilcherSwarmGoal extends Goal {

    private static final double CROWD_DIST_SQ    = 3.0 * 3.0;
    private static final double SPEED            = 1.3;
    private static final int    CHATTER_INTERVAL = 20;

    private final FilcherEntity filcher;
    private int ticksActive;

    public FilcherSwarmGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return filcher.getSwarmTicks() > 0 && filcher.getSwarmTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        Player target = filcher.getSwarmTarget();
        return filcher.getSwarmTicks() > 0 && target != null && target.isAlive();
    }

    @Override
    public void start() { ticksActive = 0; }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        ticksActive = 0;
    }

    @Override
    public void tick() {
        Player target = filcher.getSwarmTarget();
        if (target == null) return;
        ticksActive++;

        filcher.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (ticksActive % CHATTER_INTERVAL == 0) filcher.playChatter();

        double distSq = filcher.distanceToSqr(target);
        if (distSq > CROWD_DIST_SQ) {
            filcher.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), SPEED);
        } else {
            filcher.getNavigation().stop();
        }
    }
}
