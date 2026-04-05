package kingdom.smp.game;

import java.util.concurrent.ConcurrentHashMap;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * During world generation only, some lava/water obsidian formations become a tanzanite
 * core with a flat ring of obsidian. {@link WorldGenLevel} excludes normal gameplay
 * (including player-placed fluids on {@link net.minecraft.server.level.ServerLevel}).
 */
public final class TanzaniteWorldgenFluidHandler {
    private TanzaniteWorldgenFluidHandler() {}

    /** ~1 in 20 eligible worldgen obsidian formations (before per-chunk cap). */
    private static final float FORMATION_CHANCE = 0.05f;

    private static final int Y_MIN = -64;
    private static final int Y_MAX = 12;

    /** At most one tanzanite formation per chunk (worldgen). */
    private static final ConcurrentHashMap<Long, Boolean> TANZANITE_CHUNK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onFluidPlacesBlock(BlockEvent.FluidPlaceBlockEvent event) {
        LevelAccessor level = event.getLevel();
        if (!(level instanceof WorldGenLevel worldGen)) {
            return;
        }

        BlockState planned = event.getNewState();
        if (!planned.is(Blocks.OBSIDIAN)) {
            return;
        }

        BlockPos pos = event.getPos();
        if (pos.getY() < Y_MIN || pos.getY() > Y_MAX) {
            return;
        }

        RandomSource random = RandomSource.create(pos.asLong() ^ worldGen.getSeed() ^ 0x54E7A1E8CAFEL);
        if (random.nextFloat() >= FORMATION_CHANCE) {
            return;
        }

        long chunkKey = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        if (TANZANITE_CHUNK.putIfAbsent(chunkKey, Boolean.TRUE) != null) {
            return;
        }

        event.setNewState(Ironhold.TANZANITE_ORE.get().defaultBlockState());
        placeObsidianRing(worldGen, pos);
    }

    private static void placeObsidianRing(WorldGenLevel level, BlockPos center) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos shell = center.offset(dx, 0, dz);
                BlockState there = level.getBlockState(shell);
                if (!canShellOver(level, shell, there)) {
                    continue;
                }
                level.setBlock(shell, obsidian, Block.UPDATE_ALL);
            }
        }
        // Optional vertical caps (same column): one obsidian above/below if air/water
        for (Direction dir : new Direction[] { Direction.UP, Direction.DOWN }) {
            BlockPos shell = center.relative(dir);
            BlockState there = level.getBlockState(shell);
            if (canShellOver(level, shell, there)) {
                level.setBlock(shell, obsidian, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean canShellOver(WorldGenLevel level, BlockPos shell, BlockState there) {
        if (there.isAir()) {
            return true;
        }
        if (there.is(Blocks.WATER)) {
            return true;
        }
        if (there.getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TANZANITE_CHUNK.clear();
    }
}
