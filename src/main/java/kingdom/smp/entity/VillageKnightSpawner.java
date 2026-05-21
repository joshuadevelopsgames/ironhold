package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps each vanilla / Terralith village stocked with 5–8 living
 * {@link KnightEntity}s. Knights spawn once per in-game day, near the
 * village bell ({@link PoiTypes#MEETING}), and persist until killed.
 *
 * <p>Behavior summary:
 * <ul>
 *   <li>Detects villages via bell POIs in chunks loaded around any player.</li>
 *   <li>Per-bell refill day-tracking: refill at most once per in-game day per
 *       village, so a 3-knight village stays at 3 until the next sunrise even
 *       if a player kills more during the day.</li>
 *   <li>Spawned knights get {@code setPersistenceRequired()} so they don't
 *       naturally despawn — only player kills clear them.</li>
 *   <li>Random type picked from the registered knight roster
 *       ({@link #KNIGHT_TYPES}) — Recruit, Man-at-Arms, Veteran, etc.</li>
 * </ul>
 */
public final class VillageKnightSpawner {
    private VillageKnightSpawner() {}

    private static final int CHECK_INTERVAL_TICKS = 200;
    /** Required minimum knights per village. */
    public static final int MIN_KNIGHTS = 5;
    /** Maximum per spawn cycle. Random target in [MIN..MAX]. */
    public static final int MAX_KNIGHTS = 8;
    /** Radius around bell to count + spawn knights. */
    public static final double VILLAGE_RADIUS = 48.0;
    /** How wide to look around each player for bells. */
    private static final int BELL_LOOKUP_RADIUS = 64;

    /** Per-village last refill day. ConcurrentHashMap so we can safely mutate from event. */
    private static final Map<BlockPos, Long> lastRefillDay = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if ((level.getGameTime() % CHECK_INTERVAL_TICKS) != 0) return;

        long currentDay = level.getOverworldClockTime() / 24000L;

        Set<BlockPos> bells = collectLoadedBells(level);
        for (BlockPos bell : bells) {
            refillAt(level, bell, currentDay);
        }
    }

    private static Set<BlockPos> collectLoadedBells(ServerLevel level) {
        Set<BlockPos> bells = new HashSet<>();
        PoiManager poi = level.getPoiManager();
        for (ServerPlayer player : level.players()) {
            poi.findAll(
                holder -> holder.is(PoiTypes.MEETING),
                pos -> true,
                player.blockPosition(),
                BELL_LOOKUP_RADIUS,
                PoiManager.Occupancy.ANY
            ).forEach(bells::add);
        }
        return bells;
    }

    private static void refillAt(ServerLevel level, BlockPos bellPos, long currentDay) {
        Long last = lastRefillDay.get(bellPos);
        if (last != null && currentDay <= last) return;

        AABB box = new AABB(bellPos).inflate(VILLAGE_RADIUS);
        int count = level.getEntitiesOfClass(KnightEntity.class, box).size();
        if (count >= MIN_KNIGHTS) {
            lastRefillDay.put(bellPos, currentDay);
            return;
        }

        RandomSource random = level.getRandom();
        int target = MIN_KNIGHTS + random.nextInt(MAX_KNIGHTS - MIN_KNIGHTS + 1);
        int needed = target - count;
        for (int i = 0; i < needed; i++) {
            spawnKnightNear(level, bellPos, random);
        }
        lastRefillDay.put(bellPos, currentDay);
    }

    private static void spawnKnightNear(ServerLevel level, BlockPos bellPos, RandomSource random) {
        EntityType<? extends KnightEntity> type = KNIGHT_TYPES.get(random.nextInt(KNIGHT_TYPES.size()));
        KnightEntity knight = type.create(level, EntitySpawnReason.STRUCTURE);
        if (knight == null) return;

        BlockPos spawnPos = findSpawnPos(level, bellPos, random);
        knight.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
            random.nextFloat() * 360f, 0f);
        knight.setPersistenceRequired();
        level.addFreshEntity(knight);
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos bellPos, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = random.nextInt(24) - 12;
            int dz = random.nextInt(24) - 12;
            int x = bellPos.getX() + dx;
            int z = bellPos.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos candidate = new BlockPos(x, y, z);
            if (level.getBlockState(candidate).isAir()
                && level.getBlockState(candidate.below()).isSolid()) {
                return candidate;
            }
        }
        // Fallback: spawn at bell's top (heightmap)
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bellPos.getX(), bellPos.getZ());
        return new BlockPos(bellPos.getX(), y, bellPos.getZ());
    }

    /** Roster of knight EntityTypes to randomly pick from. */
    private static final List<EntityType<? extends KnightEntity>> KNIGHT_TYPES = List.of(
        kingdom.smp.ModEntities.KNIGHT_RECRUIT.get(),
        kingdom.smp.ModEntities.KNIGHT_MAN_AT_ARMS.get(),
        kingdom.smp.ModEntities.KNIGHT_CROSSBOWMAN.get(),
        kingdom.smp.ModEntities.KNIGHT_ARMORED.get(),
        kingdom.smp.ModEntities.KNIGHT_CRUSADER.get(),
        kingdom.smp.ModEntities.KNIGHT_GOTHIC.get(),
        kingdom.smp.ModEntities.KNIGHT_GOLD.get(),
        kingdom.smp.ModEntities.KNIGHT_JOUSTER.get(),
        kingdom.smp.ModEntities.KNIGHT_VETERAN.get()
    );
}
