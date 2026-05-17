package kingdom.smp.dynlight;

/**
 * A point in the world that contributes to the dynamic light buffer.
 *
 * <p>Tracked per-entity. {@link #luminance()} is the peak block-light level (0–15) at the origin;
 * the falloff to neighboring blocks is computed by {@link DynamicLights}.
 */
public final class DynamicLightSource {
    private final int entityId;
    private double x;
    private double y;
    private double z;
    private int luminance;
    private int sectionX;
    private int sectionY;
    private int sectionZ;

    DynamicLightSource(int entityId, double x, double y, double z, int luminance) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.luminance = luminance;
        this.sectionX = floorSection(x);
        this.sectionY = floorSection(y);
        this.sectionZ = floorSection(z);
    }

    private static int floorSection(double world) {
        return ((int) Math.floor(world)) >> 4;
    }

    public int entityId() { return entityId; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public int luminance() { return luminance; }
    public int sectionX() { return sectionX; }
    public int sectionY() { return sectionY; }
    public int sectionZ() { return sectionZ; }

    /** Returns true when the section coordinates changed (caller should reschedule chunk rebuilds). */
    boolean updatePosition(double nx, double ny, double nz) {
        this.x = nx;
        this.y = ny;
        this.z = nz;
        int nsx = floorSection(nx);
        int nsy = floorSection(ny);
        int nsz = floorSection(nz);
        if (nsx == sectionX && nsy == sectionY && nsz == sectionZ) return false;
        this.sectionX = nsx;
        this.sectionY = nsy;
        this.sectionZ = nsz;
        return true;
    }

    void setLuminance(int luminance) { this.luminance = luminance; }
}
