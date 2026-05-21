package kingdom.smp.entity.goal;

import kingdom.smp.npc.NpcCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Once in a long while a stationed NPC tends their patch — planting a flower on
 * an open, sky-lit grass block within the leash. Only grass blocks that can see
 * the sky qualify, so they decorate the yard rather than caves or interiors.
 */
public class NpcStationFlowerGoal extends StationGoalBase {

    private static final int SEARCH = 8;
    private static final double ARRIVE_SQ = 4.0;
    private static final Block[] FLOWERS = {
        Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER,
        Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM
    };

    private @Nullable BlockPos plantPos;
    private int cooldown;

    public NpcStationFlowerGoal(NpcCompanion companion) {
        super(companion);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!isStationed()) return false;
        if (cooldown > 0) { cooldown--; return false; }
        if (mob.getRandom().nextInt(400) != 0) return false;
        plantPos = findSpot();
        return plantPos != null;
    }

    private @Nullable BlockPos findSpot() {
        Level lvl = mob.level();
        BlockPos c = mob.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int tries = 0; tries < 24; tries++) {
            int dx = mob.getRandom().nextInt(SEARCH * 2 + 1) - SEARCH;
            int dy = mob.getRandom().nextInt(5) - 2;
            int dz = mob.getRandom().nextInt(SEARCH * 2 + 1) - SEARCH;
            m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            if (lvl.getBlockState(m).getBlock() != Blocks.GRASS_BLOCK) continue;
            BlockPos above = m.above();
            if (!lvl.getBlockState(above).isAir()) continue;
            if (!lvl.canSeeSky(above)) continue;
            if (!withinLeash(above)) continue;
            return above;
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!isStationed() || plantPos == null) return false;
        if (!mob.level().getBlockState(plantPos).isAir()) return false;
        return !mob.getNavigation().isDone()
            || mob.distanceToSqr(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5) > ARRIVE_SQ;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5, 0.7);
    }

    @Override
    public void tick() {
        if (plantPos == null) return;
        mob.getLookControl().setLookAt(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5);
        if (mob.distanceToSqr(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5) <= ARRIVE_SQ) {
            plant();
        }
    }

    private void plant() {
        Level lvl = mob.level();
        if (!lvl.isClientSide() && plantPos != null && lvl.getBlockState(plantPos).isAir()) {
            BlockState flower = FLOWERS[mob.getRandom().nextInt(FLOWERS.length)].defaultBlockState();
            if (flower.canSurvive(lvl, plantPos)) {
                lvl.setBlock(plantPos, flower, 3);
                lvl.playSound(null, plantPos, flower.getSoundType().getPlaceSound(),
                    mob.getSoundSource(), 0.6f, 1.1f);
            }
        }
        cooldown = 1200 + mob.getRandom().nextInt(1200);
        plantPos = null;
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        if (cooldown == 0) cooldown = 400;
        plantPos = null;
        mob.getNavigation().stop();
    }
}
