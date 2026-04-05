package kingdom.smp.entity.goal;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.FilcherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * King-only goal: while a player is within PLAYER_RANGE blocks of the den,
 * spawns one new filcher at the den mouth every SPAWN_INTERVAL ticks —
 * reinforcing the pack up to MAX_PACK filchers total in the area.
 */
public class FilcherKingSpawnGoal extends Goal {

    private static final double PLAYER_RANGE   = 48.0;
    private static final int    SPAWN_INTERVAL = 1200; // 60 seconds
    private static final int    MAX_PACK       = 30;
    private static final double COUNT_RADIUS   = 80.0;

    private final FilcherEntity king;
    private int cooldown = 0;

    public FilcherKingSpawnGoal(FilcherEntity king) {
        this.king = king;
        setFlags(EnumSet.noneOf(Flag.class)); // background ticker — no flag conflicts
    }

    @Override
    public boolean canUse() {
        return king.isKing() && king.getDenPos() != null && !king.level().isClientSide();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (++cooldown < SPAWN_INTERVAL) return;
        cooldown = 0;

        if (!(king.level() instanceof ServerLevel serverLevel)) return;

        // Only spawn if a player is nearby
        Player player = serverLevel.getNearestPlayer(king, PLAYER_RANGE);
        if (player == null) return;

        // Count existing filchers in area
        long count = serverLevel.getEntitiesOfClass(
            FilcherEntity.class,
            king.getBoundingBox().inflate(COUNT_RADIUS),
            f -> f.isAlive()
        ).size();
        if (count >= MAX_PACK) return;

        // Spawn at den mouth
        BlockPos den = king.getDenPos();
        FilcherEntity spawn = (FilcherEntity) Ironhold.FILCHER.get()
            .create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (spawn == null) return;

        spawn.setPos(den.getX() + 0.5, den.getY(), den.getZ() + 0.5);
        spawn.setDenPos(den);
        serverLevel.addFreshEntity(spawn);
    }
}
