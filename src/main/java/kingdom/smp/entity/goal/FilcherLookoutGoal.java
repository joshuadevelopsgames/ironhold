package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Pack alarm goal: when a filcher is caught at close range or takes
 * aggression from a player, it sounds a distress call that triggers a
 * scatter-flee response in all nearby filchers.
 *
 * <p>Unlike the post-death scatter (which fires unconditionally from
 * {@link FilcherEntity#die}), this goal fires while the filcher is still
 * alive — acting as an early-warning system when the group is threatened
 * but no one has died yet.
 *
 * <h3>Trigger condition</h3>
 * <p>Fires when this filcher has {@code hurtTime > 0} — i.e. it was just
 * hit. Proximity alone is not enough; the filcher must actually take damage.
 *
 * <h3>Response</h3>
 * <ol>
 *   <li>Plays {@link FilcherEntity#playDistress()} once.</li>
 *   <li>Sets {@link FilcherEntity#setScatterTicks(int)} on every nearby
 *       filcher (including itself), causing them to bolt in random
 *       directions via the scatter logic already in {@code aiStep()}.</li>
 * </ol>
 *
 * <p>The goal ends after a single broadcast tick so it doesn't loop
 * endlessly — the scatter state drives subsequent fleeing behaviour.
 */
public class FilcherLookoutGoal extends Goal {

    /** Distance at which a nearby player counts as a threat. */
    private static final double DANGER_RANGE   = 7.0;
    /** Scatter duration broadcast to all nearby filchers (ticks). */
    private static final int    SCATTER_TICKS  = 100;
    /** Radius within which pack members receive the alarm. */
    private static final double ALARM_RADIUS   = 20.0;

    private final FilcherEntity filcher;

    public FilcherLookoutGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        // No movement flags — the scatter in aiStep() handles actual fleeing
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        // Don't fire if already scattering — alarm was already sent
        if (filcher.getScatterTicks() > 0) return false;
        // Only trigger when actually caught or hit — not just from proximity
        return filcher.hurtTime > 0;
    }

    @Override
    public boolean canContinueToUse() {
        // One-shot: broadcast fires in start(), goal ends immediately
        return false;
    }

    @Override
    public void start() {
        broadcastAlarm();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Plays the distress call and sets scatter state on this filcher and
     * all pack members within alarm radius, each fleeing in a random
     * direction away from the nearest threat.
     */
    private void broadcastAlarm() {
        if (filcher.level().isClientSide()) return;

        filcher.playDistress();

        // Find the nearest threatening player for scatter direction
        List<Player> threats = filcher.level().getEntitiesOfClass(
            Player.class,
            filcher.getBoundingBox().inflate(DANGER_RANGE),
            p -> !p.isCreative() && !p.isSpectator()
        );

        // Scatter this filcher
        scatterFrom(filcher, threats);

        // Scatter all nearby pack members
        List<FilcherEntity> pack = filcher.level().getEntitiesOfClass(
            FilcherEntity.class,
            filcher.getBoundingBox().inflate(ALARM_RADIUS),
            f -> f != filcher && f.isAlive() && f.getScatterTicks() == 0
        );
        for (FilcherEntity packMember : pack) {
            packMember.playDistress();
            scatterFrom(packMember, threats);
        }
    }

    /**
     * Sets scatter state on {@code target} — picks a random direction
     * away from the nearest threat and starts it moving.
     */
    private static void scatterFrom(FilcherEntity target, List<Player> threats) {
        target.setScatterTicks(SCATTER_TICKS);

        Vec3 away;
        if (!threats.isEmpty()) {
            // Average away-vector from all threats
            away = Vec3.ZERO;
            for (Player p : threats) {
                away = away.add(target.position().subtract(p.position()).normalize());
            }
            away = away.normalize();
        } else {
            // No visible threat — pick a random direction
            double angle = target.getRandom().nextDouble() * Math.PI * 2;
            away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }

        double angle   = Math.atan2(away.z, away.x)
            + (target.getRandom().nextDouble() - 0.5) * Math.PI * 0.5;
        double dist    = 14 + target.getRandom().nextInt(8);
        Vec3 fleeTarget = target.position().add(
            Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        target.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.8);
    }
}
