package kingdom.smp.client;

/**
 * Live debug values for the arcane scepter third-person transform.
 * Adjusted via client commands: /scepterdebug
 */
public final class ScepterTransformDebug {
    private ScepterTransformDebug() {}

    public static float rotX = 0, rotY = 90, rotZ = -25;
    public static float transX = 0.5F, transY = 0.5F, transZ = 0.5F;
    public static float scale = 1.0F;

    public static String summary() {
        return String.format("rot(%.1f, %.1f, %.1f) trans(%.2f, %.2f, %.2f) scale(%.2f)",
            rotX, rotY, rotZ, transX, transY, transZ, scale);
    }
}
