package kingdom.smp.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Implemented by NPC entities that a sufficiently-bonded player can recruit to
 * travel with them or settle at their base. The shared companion goals
 * ({@link kingdom.smp.entity.goal.NpcFollowOwnerGoal},
 * {@link kingdom.smp.entity.goal.NpcStationGoal}) drive movement off this state;
 * {@link kingdom.smp.npc.NpcGiftHandler} flips it when the player crouch-interacts
 * with an empty hand.
 */
public interface NpcCompanion {

    /** The entity itself — used by the companion goals for navigation. */
    PathfinderMob companionMob();

    /** Current FREE / FOLLOWING / STATIONED state. */
    NpcDisposition disposition();

    /** UUID of the player this NPC is following, or null when not following. */
    @Nullable UUID companionOwnerId();

    /** Anchor position when STATIONED, or null otherwise. */
    @Nullable BlockPos stationPos();

    /**
     * Advance the companion state machine (FREE → FOLLOWING → STATIONED → FREE)
     * and tell the player what just happened. Called only after the rapport gate
     * has been checked.
     */
    void cycleCompanionState(ServerPlayer player);
}
