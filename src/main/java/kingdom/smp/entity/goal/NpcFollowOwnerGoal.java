package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import kingdom.smp.npc.NpcDisposition;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Makes a recruited NPC trail its owner like a companion while its disposition
 * is {@link NpcDisposition#FOLLOWING}. Walks when nearby, teleports to catch up
 * when the owner gets far (e.g. sprinted off or teleported). No expiry — the
 * follow lasts until the player cycles the companion state again.
 */
public class NpcFollowOwnerGoal extends Goal {

    private final NpcCompanion companion;
    private final PathfinderMob mob;
    private Player owner;
    private int recalcCooldown;

    public NpcFollowOwnerGoal(NpcCompanion companion) {
        this.companion = companion;
        this.mob = companion.companionMob();
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private Player resolveOwner() {
        UUID id = companion.companionOwnerId();
        return id == null ? null : mob.level().getPlayerByUUID(id);
    }

    @Override
    public boolean canUse() {
        if (companion.disposition() != NpcDisposition.FOLLOWING) return false;
        Player o = resolveOwner();
        if (o == null || !o.isAlive()) return false;
        this.owner = o;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (companion.disposition() != NpcDisposition.FOLLOWING) return false;
        return owner != null && owner.isAlive();
    }

    @Override
    public void stop() {
        this.owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
        if (--recalcCooldown > 0) return;
        recalcCooldown = 10;

        double distSq = mob.distanceToSqr(owner);
        if (distSq > 20 * 20) {
            mob.teleportTo(
                owner.getX() + (mob.getRandom().nextDouble() - 0.5) * 3,
                owner.getY(),
                owner.getZ() + (mob.getRandom().nextDouble() - 0.5) * 3);
        } else if (distSq > 9) {
            mob.getNavigation().moveTo(owner, 1.15);
        } else {
            mob.getNavigation().stop();
        }
    }
}
