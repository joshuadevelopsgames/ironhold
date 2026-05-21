package kingdom.smp.seasons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Event-driven seasonal weather effects: lays down snow during winter sub-seasons and melts
 * snow/ice during spring/summer. Avoids the mixin route for Biome temperature thresholds (which
 * would require a thread-local level context across many call sites); instead, we run a
 * periodic random-tick sweep over chunks near players, doing placement/melt directly.
 *
 * <p>The roll budget per tick is small (a few dozen positions per chunk per sweep interval)
 * so cost is comparable to vanilla random-tick snow placement.
 */
public final class SeasonWeatherHandler {
    private SeasonWeatherHandler() {}

    /** Sweep every this-many server ticks. 200 = 10 seconds. */
    private static final int SWEEP_INTERVAL_TICKS = 200;
    /** Player-loaded radius (in chunks) we sweep. */
    private static final int CHUNK_RADIUS = 6;
    /** Random positions per chunk per sweep. */
    private static final int ROLLS_PER_CHUNK = 6;

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!SeasonConfig.isDimensionEnabled(level.dimension())) return;
        if ((level.getGameTime() % SWEEP_INTERVAL_TICKS) != 0) return;

        SeasonState state = Seasons.current(level);
        Season.SubSeason sub = state.subSeason();
        float tempAdjust = SeasonConfig.biomeTempAdjustment(sub);
        boolean wintry = tempAdjust <= -0.5f;     // EARLY/MID/LATE_WINTER
        boolean meltSeason = tempAdjust >= 0f && sub.parent() != Season.WINTER;
        if (!wintry && !meltSeason) return;

        RandomSource random = level.getRandom();
        for (ChunkPos cp : collectChunksNearPlayers(level)) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x(), cp.z());
            if (chunk == null) continue;
            for (int i = 0; i < ROLLS_PER_CHUNK; i++) {
                int x = cp.getMinBlockX() + random.nextInt(16);
                int z = cp.getMinBlockZ() + random.nextInt(16);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (wintry) {
                    trySnowOrFreeze(level, top, sub);
                } else {
                    tryMelt(level, top);
                }
            }
        }
    }

    private static Set<ChunkPos> collectChunksNearPlayers(ServerLevel level) {
        Set<ChunkPos> out = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos pc = player.chunkPosition();
            int px = pc.x();
            int pz = pc.z();
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    out.add(new ChunkPos(px + dx, pz + dz));
                }
            }
        }
        return out;
    }

    private static void trySnowOrFreeze(ServerLevel level, BlockPos top, Season.SubSeason sub) {
        BlockState atTop = level.getBlockState(top);
        BlockPos below = top.below();
        BlockState belowState = level.getBlockState(below);

        if (atTop.isAir() && level.getBrightness(LightLayer.SKY, top) > 0) {
            if (Blocks.SNOW.defaultBlockState().canSurvive(level, top)) {
                level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                return;
            }
        }
        if (sub == Season.SubSeason.MID_WINTER || sub == Season.SubSeason.LATE_WINTER) {
            if (belowState.is(Blocks.WATER) && level.getBrightness(LightLayer.SKY, below) > 0) {
                level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
            }
        }
    }

    private static void tryMelt(ServerLevel level, BlockPos top) {
        BlockState atTop = level.getBlockState(top);
        if (atTop.is(Blocks.SNOW)) {
            level.removeBlock(top, false);
            return;
        }
        BlockPos below = top.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.ICE)) {
            level.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
        }
    }
}
