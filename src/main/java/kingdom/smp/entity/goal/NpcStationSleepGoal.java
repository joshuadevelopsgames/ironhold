package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * At night a stationed NPC seeks out an unoccupied bed within the leash and
 * sleeps in it until morning, then gets up and resumes their day. Mirrors the
 * vanilla "go to bed" feel without the villager brain system.
 */
public class NpcStationSleepGoal extends StationGoalBase {

    private static final int SEARCH = 8;
    private static final double ARRIVE_SQ = 4.0;

    private @Nullable BlockPos bed;

    public NpcStationSleepGoal(NpcCompanion companion) {
        super(companion);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    private boolean isNight() {
        // True after dusk / during storms — the NPC turns in when it's dark out.
        return mob.level().isDarkOutside();
    }

    @Override
    public boolean canUse() {
        if (!isStationed() || !isNight()) return false;
        if (mob.isSleeping()) return true;
        bed = findBed();
        return bed != null;
    }

    private @Nullable BlockPos findBed() {
        Level lvl = mob.level();
        BlockPos c = mob.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = lvl.getBlockState(m);
                    if (!(s.getBlock() instanceof BedBlock)) continue;
                    if (s.getValue(BedBlock.PART) != BedPart.HEAD) continue;
                    if (s.getValue(BedBlock.OCCUPIED)) continue;
                    BlockPos im = m.immutable();
                    if (!withinLeash(im)) continue;
                    double d = im.distSqr(c);
                    if (d < bestD) { bestD = d; best = im; }
                }
            }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isStationed() || !isNight()) return false;
        return bed != null && mob.level().getBlockState(bed).getBlock() instanceof BedBlock;
    }

    @Override
    public void start() {
        if (bed != null) {
            mob.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.8);
        }
    }

    @Override
    public void tick() {
        if (bed == null || mob.isSleeping()) return;
        double d2 = mob.distanceToSqr(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
        if (d2 <= ARRIVE_SQ) {
            mob.getNavigation().stop();
            mob.startSleeping(bed);
        } else {
            mob.getLookControl().setLookAt(bed.getX() + 0.5, bed.getY() + 0.5, bed.getZ() + 0.5);
        }
    }

    @Override
    public void stop() {
        if (mob.isSleeping()) mob.stopSleeping();
        bed = null;
        mob.getNavigation().stop();
    }
}
