package kingdom.smp.dynlight;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndLightGetter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only dynamic-lighting coordinator for Ironhold.
 *
 * <p><b>Scope</b> — held items light up only if registered in {@link ItemLightRegistry} or are
 * {@link net.minecraft.world.item.BlockItem}s with positive light emission; entities glow only
 * if their type is in {@link EntityLightRegistry}. Dropped items and item frames are evaluated
 * the same as held items.
 *
 * <p><b>Smoothness</b> — five mechanisms work together so the light tracks the source instead of
 * snapping at section boundaries:
 *
 * <ol>
 *   <li><b>Per-frame render positions.</b> {@link #updateRenderPositions} interpolates each
 *       source's coordinates from the entity's prior + current tick positions using the current
 *       partial tick, so {@link #patchLightmap} reads positions that glide with the rendered
 *       model rather than stepping at 20 Hz.</li>
 *   <li><b>Distance-based rebuild trigger.</b> Each tick we re-mesh affected sections when the
 *       source has moved {@link #REBUILD_MOVE_THRESHOLD} blocks since the last rebuild
 *       (throttled by {@link #REBUILD_THROTTLE_TICKS}), not only on 16-block section crossings.
 *       This is the fix for the "lag-then-glitch" terrain stepping.</li>
 *   <li><b>Luminance ramp.</b> {@code displayLuminance} lerps toward {@code targetLuminance}
 *       by {@link #LUMINANCE_RAMP_PER_TICK} per tick; chunk rebuilds re-fire each time the
 *       integer floor changes, so light fades in/out instead of snapping.</li>
 *   <li><b>Section bucketing.</b> Sources are bucketed by 16³ section. A query inspects only
 *       the 27 sections around the query point — query cost stays O(local sources), not
 *       O(all sources).</li>
 *   <li><b>Per-thread BlockPos cache.</b> Chunk-build workers each cache patched values keyed
 *       by packed block position. Invalidated when the bucket snapshot changes (versioned by
 *       {@link #bucketSeq}). Adjacent blocks share results for free.</li>
 * </ol>
 *
 * <p>Clean-room implementation. The underlying lightmap-injection technique is documented openly
 * by LambdAurora (see {@code THIRD_PARTY.md}); credit for the approach is theirs.
 */
public final class DynamicLights {
    private DynamicLights() {}

    /** Falloff radius in blocks — matches the canonical 7.75 cap (keeps affected sections to 3³). */
    public static final double RADIUS = 7.75;
    /**
     * Re-mesh affected sections if the source has moved at least this many blocks since the last
     * rebuild. The size of the per-rebuild visual "jump" on terrain equals the source's movement
     * between rebuilds, so this needs to stay well below 1 block for the lighting to look smooth
     * rather than stepping at block boundaries. 0.25 ≈ 0.2–0.4 block jumps at walk/sprint pace.
     */
    public static final double REBUILD_MOVE_THRESHOLD = 0.25;
    /** Minimum client ticks between rebuilds for a single source (1 = up to 20 Hz). */
    public static final int REBUILD_THROTTLE_TICKS = 1;
    /** Display luminance moves at most this much per tick toward the target (≈700 ms ramp 0↔15). */
    public static final double LUMINANCE_RAMP_PER_TICK = 1.0;

    private static final int BLOCK_BITS_MASK = 0x000FFFFF;
    private static final int SKY_BITS_MASK = 0xFFF00000;

    /** Entity id → source. Mutated only from the client tick thread. */
    private static final Map<Integer, DynamicLightSource> SOURCES = new ConcurrentHashMap<>();

    /** Versioned snapshot read by render threads. {@link #bucketSeq} is bumped on every swap. */
    private static volatile Bucket bucket = new Bucket(new Long2ObjectOpenHashMap<>(), 0);
    private static volatile long bucketSeq = 0L;

    /** Per-thread cache: packed BlockPos → patched lightmap value, valid while bucketSeq is stable. */
    private static final ThreadLocal<PatchCache> CACHE = ThreadLocal.withInitial(PatchCache::new);

    public static int activeCount() { return bucket.totalSources; }

    // ---------- patch path (called from render + chunk-build worker threads) ----------

    /** Block-pos overload — used by terrain meshing; goes through the per-thread cache. */
    public static int patchLightmap(BlockAndLightGetter level, BlockPos pos, int original) {
        Bucket b = bucket;
        if (b.totalSources == 0) return original;
        long key = blockKey(pos.getX(), pos.getY(), pos.getZ());
        long seq = bucketSeq;
        PatchCache cache = CACHE.get();
        int hit = cache.lookup(key, seq, original);
        if (hit != PatchCache.MISS) return hit;
        int result = computeAt(b, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, original);
        cache.store(key, original, result);
        return result;
    }

    /** Point overload — used by entity lighting; no cache because positions are fractional and unique per frame. */
    public static int patchLightmapAtPoint(double x, double y, double z, int original) {
        Bucket b = bucket;
        if (b.totalSources == 0) return original;
        return computeAt(b, x, y, z, original);
    }

    private static int computeAt(Bucket b, double cx, double cy, double cz, int original) {
        int bsx = ((int) Math.floor(cx)) >> 4;
        int bsy = ((int) Math.floor(cy)) >> 4;
        int bsz = ((int) Math.floor(cz)) >> 4;
        double best = 0.0;
        double radiusSq = RADIUS * RADIUS;
        Long2ObjectMap<DynamicLightSource[]> map = b.bySection;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long sectKey = sectionKey(bsx + dx, bsy + dy, bsz + dz);
                    DynamicLightSource[] arr = map.get(sectKey);
                    if (arr == null) continue;
                    for (DynamicLightSource s : arr) {
                        // A source's contribution peaks at its full luminance (dist→0) and only
                        // falls from there, so if it can't beat the current best even at zero
                        // distance, skip it before paying for the sqrt. Exact — no visual change.
                        double lum = s.displayLuminance();
                        if (lum <= best) continue;
                        double rx = s.renderX() - cx;
                        double ry = s.renderY() - cy;
                        double rz = s.renderZ() - cz;
                        double distSq = rx * rx + ry * ry + rz * rz;
                        if (distSq >= radiusSq) continue;
                        double dist = Math.sqrt(distSq);
                        double contribution = (1.0 - dist / RADIUS) * lum;
                        if (contribution > best) best = contribution;
                    }
                }
            }
        }

        if (best <= 0.0) return original;
        int origBlockBits = original & BLOCK_BITS_MASK;
        int dynBlockBits = (int) (best * 16.0);
        if (dynBlockBits <= origBlockBits) return original;
        if (dynBlockBits > BLOCK_BITS_MASK) dynBlockBits = BLOCK_BITS_MASK;
        return (original & SKY_BITS_MASK) | dynBlockBits;
    }

    // ---------- per-frame render position update ----------

    /** Called from {@code RenderFrameEvent.Pre}; refreshes interpolated source positions. */
    public static void updateRenderPositions(float partialTick) {
        if (SOURCES.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (DynamicLightSource source : SOURCES.values()) {
            Entity entity = level.getEntity(source.entityId());
            if (entity == null || entity.isRemoved()) continue;
            double rx = Mth.lerp((double) partialTick, entity.xOld, entity.getX());
            double ryBase = Mth.lerp((double) partialTick, entity.yOld, entity.getY());
            double rz = Mth.lerp((double) partialTick, entity.zOld, entity.getZ());
            double ry = (entity instanceof LivingEntity)
                ? ryBase + entity.getEyeHeight()
                : ryBase + entity.getBbHeight() * 0.5;
            source.setRenderPosition(rx, ry, rz);
        }
    }

    // ---------- per-tick bookkeeping ----------

    /** Reused across ticks (client tick thread only) to avoid a per-tick HashSet allocation. */
    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet SEEN =
        new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    public static void tick(ClientLevel level) {
        if (level == null) return;
        it.unimi.dsi.fastutil.ints.IntOpenHashSet seen = SEEN;
        seen.clear();
        boolean structureChanged = false;

        // Phase 1: visit live entities, refresh targets and tick positions.
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved()) continue;
            int target = computeLuminance(entity);
            DynamicLightSource existing = SOURCES.get(entity.getId());
            if (target <= 0 && existing == null) continue;
            seen.add(entity.getId());
            if (existing == null) {
                DynamicLightSource fresh = new DynamicLightSource(entity, target);
                SOURCES.put(entity.getId(), fresh);
                structureChanged = true;
            } else {
                existing.setTargetLuminance(target);
                if (existing.updateTickPosition(entity)) structureChanged = true;
            }
        }

        // Phase 2: ramp luminances, drive rebuild triggers, drop fully-faded sources.
        Iterator<Map.Entry<Integer, DynamicLightSource>> it = SOURCES.entrySet().iterator();
        while (it.hasNext()) {
            DynamicLightSource s = it.next().getValue();
            if (!seen.contains(s.entityId())) s.setTargetLuminance(0);
            s.rampLuminance(LUMINANCE_RAMP_PER_TICK);
            s.bumpTicksSinceRebuild();

            boolean throttleReady = s.ticksSinceRebuild() >= REBUILD_THROTTLE_TICKS;
            boolean shouldRebuild = throttleReady && (
                s.displayLuminanceFloorChanged()
                || s.moveDistSqSinceRebuild() >= REBUILD_MOVE_THRESHOLD * REBUILD_MOVE_THRESHOLD
            );

            if (shouldRebuild) {
                scheduleRebuildAround(s.sectionX(), s.sectionY(), s.sectionZ());
                if (s.lastRebuildSectionX() != s.sectionX()
                    || s.lastRebuildSectionY() != s.sectionY()
                    || s.lastRebuildSectionZ() != s.sectionZ()) {
                    scheduleRebuildAround(s.lastRebuildSectionX(),
                                          s.lastRebuildSectionY(),
                                          s.lastRebuildSectionZ());
                }
                s.markRebuilt();
                structureChanged = true;
            }

            if (s.displayLuminance() <= 0.0 && s.targetLuminance() == 0) {
                scheduleRebuildAround(s.sectionX(), s.sectionY(), s.sectionZ());
                it.remove();
                structureChanged = true;
            }
        }

        if (structureChanged) rebuildBucket();
    }

    public static void reset() {
        SOURCES.clear();
        bucket = new Bucket(new Long2ObjectOpenHashMap<>(), 0);
        bucketSeq++;
    }

    // ---------- internals ----------

    private static int computeLuminance(Entity entity) {
        int byType = EntityLightRegistry.luminanceOf(entity);
        int byItem = 0;
        if (entity instanceof LivingEntity living) {
            byItem = Math.max(
                ItemLightRegistry.luminanceOf(living.getMainHandItem()),
                ItemLightRegistry.luminanceOf(living.getOffhandItem())
            );
        } else if (entity instanceof ItemEntity itemEntity) {
            byItem = ItemLightRegistry.luminanceOf(itemEntity.getItem());
        } else if (entity instanceof ItemFrame frame) {
            ItemStack shown = frame.getItem();
            byItem = ItemLightRegistry.luminanceOf(shown);
        }
        return Math.max(byType, byItem);
    }

    private static void rebuildBucket() {
        // Build the primitive-keyed map directly (no Long boxing / intermediate HashMap).
        Long2ObjectOpenHashMap<List<DynamicLightSource>> tmp = new Long2ObjectOpenHashMap<>();
        int total = 0;
        for (DynamicLightSource s : SOURCES.values()) {
            if (s.displayLuminance() < 0.5) continue;
            long key = sectionKey(s.sectionX(), s.sectionY(), s.sectionZ());
            tmp.computeIfAbsent(key, k -> new ArrayList<>(2)).add(s);
            total++;
        }
        Long2ObjectOpenHashMap<DynamicLightSource[]> map = new Long2ObjectOpenHashMap<>(tmp.size());
        for (Long2ObjectMap.Entry<List<DynamicLightSource>> e : tmp.long2ObjectEntrySet()) {
            map.put(e.getLongKey(), e.getValue().toArray(new DynamicLightSource[0]));
        }
        bucket = new Bucket(map, total);
        bucketSeq++;
    }

    private static void scheduleRebuildAround(int sx, int sy, int sz) {
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer lr = mc.levelRenderer;
        if (lr == null) return;
        for (int x = sx - 1; x <= sx + 1; x++) {
            for (int y = sy - 1; y <= sy + 1; y++) {
                for (int z = sz - 1; z <= sz + 1; z++) {
                    lr.setSectionDirty(x, y, z);
                }
            }
        }
    }

    // 22|20|22 bit packing covers full overworld bounds (-30M..30M XZ, -2048..2048 Y after >>4).
    static long sectionKey(int sx, int sy, int sz) {
        return ((long) (sx & 0x3FFFFF) << 42) | ((long) (sy & 0xFFFFF) << 22) | (long) (sz & 0x3FFFFF);
    }

    /** Packs a raw block position. Shifted so negatives stay distinct under masking. */
    static long blockKey(int x, int y, int z) {
        long sx = (x + (1L << 25)) & 0x3FFFFFFL; // 26 bits
        long sy = (y + (1L << 11)) & 0xFFFL;     // 12 bits
        long sz = (z + (1L << 25)) & 0x3FFFFFFL; // 26 bits
        return (sx << 38) | (sy << 26) | sz;
    }

    // ---------- nested types ----------

    private record Bucket(Long2ObjectMap<DynamicLightSource[]> bySection, int totalSources) {}

    /**
     * Per-worker cache. Two parallel arrays (open-addressed, linear probe) keyed by packed block
     * pos. Stores the {@code (original, result)} pair so we don't mis-cache when vanilla returns
     * different values for the same pos across passes.
     */
    private static final class PatchCache {
        static final int MISS = Integer.MIN_VALUE;
        // 16K slots (a section is 4096 blocks; meshing also touches neighbours, so 4096
        // direct-mapped thrashed to ~0% hit rate). Bumped + linear probing below.
        private static final int SIZE = 1 << 14;
        private static final int MASK = SIZE - 1;
        /** Max linear probes before we give up and overwrite — bounds worst-case lookup cost. */
        private static final int MAX_PROBE = 8;

        private long bucketSeqSeen = -1L;
        private final long[] keys = new long[SIZE];
        private final int[] origs = new int[SIZE];
        private final int[] results = new int[SIZE];
        private final boolean[] occupied = new boolean[SIZE];

        int lookup(long key, long seq, int original) {
            if (bucketSeqSeen != seq) {
                java.util.Arrays.fill(occupied, false);
                bucketSeqSeen = seq;
            }
            int slot = (int) (key & MASK);
            for (int i = 0; i < MAX_PROBE; i++) {
                if (!occupied[slot]) return MISS;
                if (keys[slot] == key && origs[slot] == original) return results[slot];
                slot = (slot + 1) & MASK;
            }
            return MISS;
        }

        void store(long key, int original, int result) {
            int slot = (int) (key & MASK);
            for (int i = 0; i < MAX_PROBE; i++) {
                if (!occupied[slot] || (keys[slot] == key && origs[slot] == original)) break;
                slot = (slot + 1) & MASK;
            }
            keys[slot] = key;
            origs[slot] = original;
            results[slot] = result;
            occupied[slot] = true;
        }
    }
}
