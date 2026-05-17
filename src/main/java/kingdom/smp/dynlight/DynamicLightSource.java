package kingdom.smp.dynlight;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One active dynamic light source. Holds two position pairs:
 *
 * <ul>
 *   <li>{@code tickX/Y/Z} — sampled once per client tick. Drives section bucketing and the
 *       rebuild-trigger movement check.</li>
 *   <li>{@code renderX/Y/Z} — sampled once per render frame from the entity's interpolated
 *       position. Drives the per-frame {@code patchLightmap} reads so entity/HUD lighting tracks
 *       the model smoothly at full framerate.</li>
 * </ul>
 *
 * <p>The visible {@code displayLuminance} lerps toward {@code targetLuminance} so equipping or
 * sheathing a light source fades in/out instead of snapping. The integer floor of
 * {@code displayLuminance} drives chunk-rebuild scheduling — every time it crosses a whole
 * level, the affected sections re-mesh.
 */
public final class DynamicLightSource {

    private final int entityId;

    // --- tick-sampled (updated in DynamicLights.tick) ---
    private double tickX;
    private double tickY;
    private double tickZ;
    private int sectionX;
    private int sectionY;
    private int sectionZ;

    // --- render-sampled (updated each frame in DynamicLights.updateRenderPositions) ---
    // Volatile so render-thread writes are immediately visible to chunk-build worker threads.
    private volatile double renderX;
    private volatile double renderY;
    private volatile double renderZ;
    private volatile double displayLuminance;

    // --- ramp state ---
    private int targetLuminance;
    private int lastRebuildLuminanceFloor = -1;

    // --- rebuild-trigger state ---
    private double lastRebuildX;
    private double lastRebuildY;
    private double lastRebuildZ;
    private int lastRebuildSectionX;
    private int lastRebuildSectionY;
    private int lastRebuildSectionZ;
    private int ticksSinceRebuild = Integer.MAX_VALUE / 2;

    DynamicLightSource(Entity entity, int initialTarget) {
        this.entityId = entity.getId();
        this.tickX = entity.getX();
        this.tickY = lightYFor(entity);
        this.tickZ = entity.getZ();
        this.renderX = tickX;
        this.renderY = tickY;
        this.renderZ = tickZ;
        this.sectionX = ((int) Math.floor(tickX)) >> 4;
        this.sectionY = ((int) Math.floor(tickY)) >> 4;
        this.sectionZ = ((int) Math.floor(tickZ)) >> 4;
        this.targetLuminance = initialTarget;
        this.displayLuminance = 0.0; // fade in from dark
        this.lastRebuildX = tickX;
        this.lastRebuildY = tickY;
        this.lastRebuildZ = tickZ;
        this.lastRebuildSectionX = sectionX;
        this.lastRebuildSectionY = sectionY;
        this.lastRebuildSectionZ = sectionZ;
    }

    public int entityId() { return entityId; }
    public double tickX() { return tickX; }
    public double tickY() { return tickY; }
    public double tickZ() { return tickZ; }
    public double renderX() { return renderX; }
    public double renderY() { return renderY; }
    public double renderZ() { return renderZ; }
    public int sectionX() { return sectionX; }
    public int sectionY() { return sectionY; }
    public int sectionZ() { return sectionZ; }
    public int targetLuminance() { return targetLuminance; }
    public double displayLuminance() { return displayLuminance; }
    public int lastRebuildSectionX() { return lastRebuildSectionX; }
    public int lastRebuildSectionY() { return lastRebuildSectionY; }
    public int lastRebuildSectionZ() { return lastRebuildSectionZ; }

    void setTargetLuminance(int v) { this.targetLuminance = Math.max(0, Math.min(15, v)); }

    /** Updates {@link #tickX}, {@link #tickY}, {@link #tickZ}; returns true when the section coords changed. */
    boolean updateTickPosition(Entity entity) {
        this.tickX = entity.getX();
        this.tickY = lightYFor(entity);
        this.tickZ = entity.getZ();
        int nsx = ((int) Math.floor(tickX)) >> 4;
        int nsy = ((int) Math.floor(tickY)) >> 4;
        int nsz = ((int) Math.floor(tickZ)) >> 4;
        if (nsx == sectionX && nsy == sectionY && nsz == sectionZ) return false;
        this.sectionX = nsx;
        this.sectionY = nsy;
        this.sectionZ = nsz;
        return true;
    }

    void setRenderPosition(double rx, double ry, double rz) {
        this.renderX = rx;
        this.renderY = ry;
        this.renderZ = rz;
    }

    /** Lerps {@link #displayLuminance} toward {@link #targetLuminance} by {@code step}. Returns true on change. */
    boolean rampLuminance(double step) {
        double t = targetLuminance;
        double d = displayLuminance;
        if (d == t) return false;
        if (d < t) {
            d = Math.min(t, d + step);
        } else {
            d = Math.max(t, d - step);
        }
        this.displayLuminance = d;
        return true;
    }

    public int ticksSinceRebuild() { return ticksSinceRebuild; }

    /** Increments the throttle counter and returns its new value. */
    int bumpTicksSinceRebuild() {
        if (ticksSinceRebuild < Integer.MAX_VALUE - 1) ticksSinceRebuild++;
        return ticksSinceRebuild;
    }

    /** Distance² between the current tick position and the position at the last rebuild trigger. */
    double moveDistSqSinceRebuild() {
        double dx = tickX - lastRebuildX;
        double dy = tickY - lastRebuildY;
        double dz = tickZ - lastRebuildZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** True if {@link #displayLuminance}'s integer floor has shifted since the last rebuild. */
    boolean displayLuminanceFloorChanged() {
        return ((int) Math.floor(displayLuminance)) != lastRebuildLuminanceFloor;
    }

    /** Captures the current state as the new rebuild baseline. */
    void markRebuilt() {
        this.lastRebuildX = tickX;
        this.lastRebuildY = tickY;
        this.lastRebuildZ = tickZ;
        this.lastRebuildSectionX = sectionX;
        this.lastRebuildSectionY = sectionY;
        this.lastRebuildSectionZ = sectionZ;
        this.lastRebuildLuminanceFloor = (int) Math.floor(displayLuminance);
        this.ticksSinceRebuild = 0;
    }

    /** Y coord of the light source — eye-height for mobs, body center for items/frames. */
    private static double lightYFor(Entity entity) {
        if (entity instanceof LivingEntity) return entity.getEyeY();
        return entity.getY() + entity.getBbHeight() * 0.5;
    }
}
