package kingdom.smp.client;

/**
 * Live, client-side tuning for the butterflies rendered inside a placed Butterfly Jar block,
 * driven by {@code /jardebug}. Lets the jar display size/position be dialled in at runtime
 * (separate from the in-world mob scale, which is {@link ButterflyScaleDebug}).
 */
public final class ButterflyJarScaleDebug {

    public static final float DEFAULT_SCALE = 0.6F;

    private static volatile float scale = DEFAULT_SCALE;
    private static volatile float offsetX = 0.0F;
    private static volatile float offsetY = 0.0F;
    private static volatile float offsetZ = 0.0F;

    private ButterflyJarScaleDebug() {}

    public static float scale() { return scale; }
    public static void setScale(float value) { scale = value; }

    public static float offsetX() { return offsetX; }
    public static float offsetY() { return offsetY; }
    public static float offsetZ() { return offsetZ; }

    public static void setOffset(float x, float y, float z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }

    public static void reset() {
        scale = DEFAULT_SCALE;
        offsetX = offsetY = offsetZ = 0.0F;
    }

    public static String summary() {
        return String.format("scale %.3f, offset [%.3f, %.3f, %.3f]", scale, offsetX, offsetY, offsetZ);
    }
}
