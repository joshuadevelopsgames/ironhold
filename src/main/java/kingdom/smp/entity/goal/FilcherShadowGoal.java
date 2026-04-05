package kingdom.smp.entity.goal;

import kingdom.smp.entity.FilcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Non-bold filchers melt into the environment when a player draws near —
 * they find the nearest shaded spot with overhead cover (tree canopy,
 * overhangs, cave lips) and crouch there silently until the coast is clear.
 *
 * <p>Only fires for filchers with boldness ≤ 0.6. Bold filchers use
 * {@link FilcherStealGoal} instead.
 */
public class FilcherShadowGoal extends Goal {

    private static final double PLAYER_ALERT_RANGE = 18.0;
    private static final double ARRIVE_DIST_SQ     = 2.0 * 2.0;
    private static final double SPEED              = 0.95;
    private static final int    PROBE_ATTEMPTS     = 24;
    private static final int    SEARCH_RADIUS      = 10;
    /** Max sky/block light level considered "shaded". */
    private static final int    SHADE_LIGHT        = 7;

    private final FilcherEntity filcher;
    private BlockPos hidePos;
    private Player nearestPlayer;

    public FilcherShadowGoal(FilcherEntity filcher) {
        this.filcher = filcher;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (filcher.isKing()) return false;
        // Only shy filchers hide; bold ones stalk
        if (filcher.getBoldness() > 0.6f) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getScatterTicks() > 0) return false;
        if (filcher.getTarget() != null) return false;

        nearestPlayer = filcher.level().getNearestPlayer(filcher, PLAYER_ALERT_RANGE);
        if (nearestPlayer == null) return false;

        hidePos = findShadySpot();
        return hidePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (filcher.getBoldness() > 0.6f) return false;
        if (!filcher.getMainHandItem().isEmpty()) return false;
        if (filcher.getScatterTicks() > 0) return false;
        if (filcher.getTarget() != null) return false;
        // Stay hidden while a player is nearby
        nearestPlayer = filcher.level().getNearestPlayer(filcher, PLAYER_ALERT_RANGE + 4.0);
        return nearestPlayer != null;
    }

    @Override
    public void start() {
        filcher.setShiftKeyDown(true);
    }

    @Override
    public void stop() {
        filcher.getNavigation().stop();
        filcher.setShiftKeyDown(false);
        hidePos = null;
        nearestPlayer = null;
    }

    @Override
    public void tick() {
        if (hidePos == null) return;

        double distSq = filcher.distanceToSqr(
            hidePos.getX() + 0.5, hidePos.getY(), hidePos.getZ() + 0.5);

        if (distSq > ARRIVE_DIST_SQ) {
            filcher.getNavigation().moveTo(
                hidePos.getX() + 0.5, hidePos.getY(), hidePos.getZ() + 0.5, SPEED);
        } else {
            // Arrived at hide spot — stay crouched and still
            filcher.getNavigation().stop();
            filcher.setShiftKeyDown(true);
            // Subtly face away from the player
            if (nearestPlayer != null) {
                net.minecraft.world.phys.Vec3 away =
                    filcher.position().subtract(nearestPlayer.position()).normalize();
                filcher.setYRot((float) Math.toDegrees(Math.atan2(-away.x, away.z)));
            }
        }
    }

    /**
     * Probes random nearby positions for one that is shaded (low light)
     * and has overhead cover within 4 blocks — tree canopy, overhangs, or
     * cave lips.
     */
    private BlockPos findShadySpot() {
        BlockPos base = filcher.blockPosition();
        var rand = filcher.getRandom();

        for (int i = 0; i < PROBE_ATTEMPTS; i++) {
            int dx = rand.nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS);
            int dz = rand.nextIntBetweenInclusive(-SEARCH_RADIUS, SEARCH_RADIUS);

            // Search a small vertical band around current Y
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos pos = base.offset(dx, dy, dz);
                if (!filcher.level().getBlockState(pos).isAir()) continue;
                if (!filcher.level().getBlockState(pos.below()).isSolid()) continue;
                if (filcher.level().getMaxLocalRawBrightness(pos) > SHADE_LIGHT) continue;

                // Must have overhead cover (tree canopy, overhang, rock)
                boolean hasRoof = false;
                for (int up = 1; up <= 4; up++) {
                    if (!filcher.level().getBlockState(pos.above(up)).isAir()) {
                        hasRoof = true;
                        break;
                    }
                }
                if (!hasRoof) continue;

                return pos;
            }
        }
        return null;
    }
}
