package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Now and then a stationed NPC wanders over to a nearby chest, opens the lid
 * for a few seconds as if rummaging, then closes it and moves on. Purely
 * cosmetic — nothing is taken. Chests are only considered within the leash.
 */
public class NpcStationChestGoal extends StationGoalBase {

    private static final int SEARCH = 10;
    private static final double ARRIVE_SQ = 6.0;

    private @Nullable BlockPos chest;
    private int cooldown;
    private boolean looking;
    private int lookTimer;

    public NpcStationChestGoal(NpcCompanion companion) {
        super(companion);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!isStationed()) return false;
        if (cooldown > 0) { cooldown--; return false; }
        if (mob.getRandom().nextInt(200) != 0) return false;
        chest = findChest();
        return chest != null;
    }

    private @Nullable BlockPos findChest() {
        Level lvl = mob.level();
        BlockPos c = mob.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!(lvl.getBlockState(m).getBlock() instanceof ChestBlock)) continue;
                    if (!withinLeash(m)) continue;
                    double d = m.distSqr(c);
                    if (d < bestD) { bestD = d; best = m.immutable(); }
                }
            }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isStationed() || chest == null) return false;
        if (!(mob.level().getBlockState(chest).getBlock() instanceof ChestBlock)) return false;
        return looking ? lookTimer > 0 : true;
    }

    @Override
    public void start() {
        looking = false;
        mob.getNavigation().moveTo(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5, 0.8);
    }

    @Override
    public void tick() {
        if (chest == null) return;
        mob.getLookControl().setLookAt(chest.getX() + 0.5, chest.getY() + 0.5, chest.getZ() + 0.5);
        if (!looking) {
            double d2 = mob.distanceToSqr(chest.getX() + 0.5, chest.getY() + 0.5, chest.getZ() + 0.5);
            if (d2 <= ARRIVE_SQ) {
                looking = true;
                lookTimer = 50;
                mob.getNavigation().stop();
                setChestOpen(true);
            } else if (mob.getNavigation().isDone()) {
                chest = null; // couldn't reach it; bail
            }
        } else {
            lookTimer--;
        }
    }

    @Override
    public void stop() {
        if (chest != null && looking) setChestOpen(false);
        cooldown = 600 + mob.getRandom().nextInt(600);
        looking = false;
        lookTimer = 0;
        chest = null;
        mob.getNavigation().stop();
    }

    private void setChestOpen(boolean open) {
        Level lvl = mob.level();
        if (lvl.isClientSide() || chest == null) return;
        BlockState state = lvl.getBlockState(chest);
        // Drives the vanilla lid animation on clients (eventId 1 = viewer count).
        lvl.blockEvent(chest, state.getBlock(), 1, open ? 1 : 0);
        lvl.playSound(null, chest, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
            mob.getSoundSource(), 0.5f, 1.0f);
    }
}
