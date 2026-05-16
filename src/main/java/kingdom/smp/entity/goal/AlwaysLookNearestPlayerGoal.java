package kingdom.smp.entity.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Look goal for voiced NPCs: continuously face the nearest player within
 * {@code range}, with an optional "preferred target" override (used by
 * conversational NPCs to keep looking at their conversation partner even if
 * another player walks closer).
 *
 * <p>Two design decisions for TPS-safety:
 * <ul>
 *   <li>Target re-acquisition runs every 10 ticks (0.5 s), not every tick.
 *       Cost per re-acquisition is {@code O(players-in-level)} via
 *       {@link net.minecraft.world.level.Level#getNearestPlayer}. With a
 *       typical SMP that's negligible.</li>
 *   <li>The look-control update DOES run every tick, but it's just setting
 *       fields on the mob — no math, no entity lookup.</li>
 * </ul>
 *
 * <p>Replaces vanilla {@code LookAtPlayerGoal} on voiced NPCs. The vanilla
 * goal has a 0.02 probability gate and 40-80 tick duration window — fine for
 * mobs but causes voiced NPCs to "drift off" between glances. This one is
 * always on as long as a player is in range.
 */
public class AlwaysLookNearestPlayerGoal extends Goal {

    private static final int RETARGET_INTERVAL_TICKS = 10;

    /** Maximum yaw / pitch turn rate in degrees per tick. 30 is moderate — feels natural. */
    private static final float MAX_YAW_TURN  = 30.0F;
    private static final float MAX_PITCH_TURN = 30.0F;

    private final Mob mob;
    private final double rangeSqr;
    private final double range;
    /** Optional supplier of a preferred target UUID (e.g. conversation partner). */
    private final @Nullable Supplier<@Nullable UUID> preferredTargetId;

    private @Nullable Player target;
    private int retargetCooldown;

    public AlwaysLookNearestPlayerGoal(Mob mob, double range) {
        this(mob, range, null);
    }

    public AlwaysLookNearestPlayerGoal(Mob mob, double range,
                                       @Nullable Supplier<@Nullable UUID> preferredTargetId) {
        this.mob = mob;
        this.range = range;
        this.rangeSqr = range * range;
        this.preferredTargetId = preferredTargetId;
        // Only conflicts with other LOOK goals — leaves MOVE/JUMP/TARGET goals untouched.
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    private @Nullable Player findTarget() {
        // 1. Conversation partner if available and in range.
        if (preferredTargetId != null && mob.level() instanceof ServerLevel sl) {
            UUID id = preferredTargetId.get();
            if (id != null) {
                ServerPlayer partner = sl.getServer().getPlayerList().getPlayer(id);
                if (partner != null
                    && partner.level() == sl
                    && partner.distanceToSqr(mob) <= rangeSqr) {
                    return partner;
                }
            }
        }
        // 2. Fall back to nearest player.
        Player nearest = mob.level().getNearestPlayer(mob, range);
        if (nearest != null && nearest.distanceToSqr(mob) <= rangeSqr) {
            return nearest;
        }
        return null;
    }

    @Override
    public boolean canUse() {
        target = findTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && target.distanceToSqr(mob) <= rangeSqr;
    }

    @Override
    public void start() {
        retargetCooldown = 0;
    }

    @Override
    public void tick() {
        if (retargetCooldown <= 0) {
            Player p = findTarget();
            if (p != null) target = p;
            retargetCooldown = RETARGET_INTERVAL_TICKS;
        } else {
            retargetCooldown--;
        }
        if (target != null) {
            mob.getLookControl().setLookAt(target, MAX_YAW_TURN, MAX_PITCH_TURN);
        }
    }

    @Override
    public void stop() {
        target = null;
        retargetCooldown = 0;
    }
}
