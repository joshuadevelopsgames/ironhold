package kingdom.smp.dynlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndLightGetter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central client-only manager for Ironhold's built-in dynamic lighting.
 *
 * <p><b>Scope</b> (mirrors the user-facing rule "only light sources / wands / glowing mobs"):
 * <ul>
 *   <li>Held items: contribute light only if registered in {@link ItemLightRegistry} OR they are a
 *       {@link net.minecraft.world.item.BlockItem} whose block has positive light emission.</li>
 *   <li>Entities: contribute light only if their entity type is registered in {@link EntityLightRegistry}
 *       (the "glowing mobs" axis).</li>
 *   <li>Dropped {@link ItemEntity} and items shown in {@link ItemFrame}s are evaluated as held items.</li>
 * </ul>
 *
 * <p><b>Algorithm</b>. Each client tick {@link #tick(ClientLevel)} walks the rendered entity list,
 * computes peak luminance per entity, and bookkeeps a sparse set of {@link DynamicLightSource}s.
 * When a source is added, removed, moves between chunk sections, or changes luminance, the
 * affected sections are queued for a renderer rebuild (3×3×3 around the source — the light radius
 * is 7.75 blocks so neighboring sections may be touched).
 *
 * <p>During rendering the level mixins call {@link #patchLightmap(BlockAndLightGetter, BlockPos, int)},
 * which compares the vanilla block-light to the dynamic falloff from each source and returns the
 * brighter of the two — written back into bits 0–19 of the packed lightmap value (the upper bits,
 * which carry sky light, are preserved).
 *
 * <p>This is a clean-room reimplementation; the underlying lightmap-injection technique is
 * documented openly by LambdAurora (see {@code THIRD_PARTY.md}).
 */
public final class DynamicLights {
    private DynamicLights() {}

    /** Max distance, in blocks, a dynamic light source affects. Matches the standard 7.75 cap. */
    public static final double RADIUS = 7.75;

    /** Bits 0–19 carry the (potentially fractional) block-light contribution as {@code (int)(level * 16)}. */
    private static final int BLOCK_BITS_MASK = 0x000FFFFF;
    /** Bits 20–31 carry sky light + any reserved/upper bits. Always preserved when we patch. */
    private static final int SKY_BITS_MASK = 0xFFF00000;

    /** Keyed by {@link Entity#getId()} for O(1) entity → source lookup. */
    private static final Map<Integer, DynamicLightSource> SOURCES = new ConcurrentHashMap<>();

    /** Read by the renderer threads; replaced atomically when the active set changes. */
    private static volatile DynamicLightSource[] snapshot = new DynamicLightSource[0];

    public static int activeCount() { return snapshot.length; }

    /** Called from the chunk-build and main render threads. Must be hot-path cheap. */
    public static int patchLightmap(BlockAndLightGetter level, BlockPos pos, int original) {
        DynamicLightSource[] sources = snapshot;
        if (sources.length == 0) return original;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        double bestLevel = 0.0;
        for (DynamicLightSource s : sources) {
            double dx = s.x() - cx;
            double dy = s.y() - cy;
            double dz = s.z() - cz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq >= RADIUS * RADIUS) continue;
            double dist = Math.sqrt(distSq);
            double contribution = (1.0 - dist / RADIUS) * s.luminance();
            if (contribution > bestLevel) bestLevel = contribution;
        }
        if (bestLevel <= 0.0) return original;
        int origBlockBits = original & BLOCK_BITS_MASK;
        int dynBlockBits = (int) (bestLevel * 16.0);
        if (dynBlockBits <= origBlockBits) return original;
        if (dynBlockBits > BLOCK_BITS_MASK) dynBlockBits = BLOCK_BITS_MASK;
        return (original & SKY_BITS_MASK) | dynBlockBits;
    }

    /** Client tick entry point. Walks entities and bookkeeps the active source set. */
    public static void tick(ClientLevel level) {
        if (level == null) return;
        Set<Integer> seen = new HashSet<>();
        boolean changed = false;

        for (Entity entity : level.entitiesForRendering()) {
            if (entity == null || entity.isRemoved()) continue;
            int luminance = computeLuminance(entity);
            if (luminance <= 0) continue;
            seen.add(entity.getId());
            DynamicLightSource existing = SOURCES.get(entity.getId());
            double ex = entity.getX();
            double ey = entity.getEyeY();
            double ez = entity.getZ();
            if (existing == null) {
                DynamicLightSource fresh = new DynamicLightSource(entity.getId(), ex, ey, ez, luminance);
                SOURCES.put(entity.getId(), fresh);
                scheduleRebuildAround(fresh.sectionX(), fresh.sectionY(), fresh.sectionZ());
                changed = true;
            } else {
                boolean sectionMoved = existing.updatePosition(ex, ey, ez);
                boolean luminanceChanged = existing.luminance() != luminance;
                if (luminanceChanged) existing.setLuminance(luminance);
                if (sectionMoved || luminanceChanged) {
                    scheduleRebuildAround(existing.sectionX(), existing.sectionY(), existing.sectionZ());
                    changed = true;
                }
            }
        }

        Iterator<Map.Entry<Integer, DynamicLightSource>> it = SOURCES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, DynamicLightSource> e = it.next();
            if (!seen.contains(e.getKey())) {
                DynamicLightSource removed = e.getValue();
                scheduleRebuildAround(removed.sectionX(), removed.sectionY(), removed.sectionZ());
                it.remove();
                changed = true;
            }
        }

        if (changed) snapshot = SOURCES.values().toArray(new DynamicLightSource[0]);
    }

    /** Clears all active sources (called when the client level unloads). */
    public static void reset() {
        SOURCES.clear();
        snapshot = new DynamicLightSource[0];
    }

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
}
