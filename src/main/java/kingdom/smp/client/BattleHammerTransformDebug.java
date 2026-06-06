package kingdom.smp.client;

/**
 * Live, client-only debug state for tuning the Battle Hammer's in-hand transforms
 * and its forge-glow layer. Driven by {@code /hammerdebug}. Defaults match the
 * values currently compiled into the renderer/layer.
 */
public final class BattleHammerTransformDebug {
    private BattleHammerTransformDebug() {}

    public static boolean enabled = false; // when true, renderer uses these values

    // First person (tuned in-game)
    public static float fpTransX = -0.50F, fpTransY = 0.50F, fpTransZ = 0.50F;
    public static float fpRotX = 0F, fpRotY = 90F, fpRotZ = 0F;
    public static float fpScale = 1.50F;

    // Third person (tuned in-game)
    public static float tpTransX = 0.0F, tpTransY = 0.80F, tpTransZ = 0.97F;
    public static float tpRotX = 0F, tpRotY = 90F, tpRotZ = 270F;
    public static float tpScale = 1.50F;

    // Off-hand first person (tuned in-game)
    public static float ohFpTransX = -0.60F, ohFpTransY = 0.50F, ohFpTransZ = 1.30F;
    public static float ohFpRotX = 0F, ohFpRotY = 90F, ohFpRotZ = 0F;
    public static float ohFpScale = 1.50F;

    // Off-hand third person (defaults mirror main-hand TP)
    public static float ohTpTransX = 0.0F, ohTpTransY = 0.80F, ohTpTransZ = 0.97F;
    public static float ohTpRotX = 0F, ohTpRotY = 90F, ohTpRotZ = 270F;
    public static float ohTpScale = 1.50F;

    /** Glow bone-render method: 0 = render() (cubes at absolute coords),
     *  1 = positionAndRender() (applies bone pivot transform). Toggle to find the
     *  one that lands the glow on the rings. */
    public static int glowMethod = 0;

    // Glow: mode 0 = off, 1 = normal (glow texture / orange / charge alpha),
    //       2 = DEBUG (ring bones drawn with the BASE texture, debug colour, forced alpha)
    public static int glowMode = 1;
    public static float glowAlpha = -1F;          // <0 = follow charge; >=0 = forced
    public static int glowR = 255, glowG = 255, glowB = 255; // tint (mode 1 texture is orange)
    public static float glowScale = 1.08F;        // inflate factor of the glow shell

    public static String fpSummary() {
        return String.format("FP trans(%.2f,%.2f,%.2f) rot(%.1f,%.1f,%.1f) scale(%.2f)",
            fpTransX, fpTransY, fpTransZ, fpRotX, fpRotY, fpRotZ, fpScale);
    }
    public static String tpSummary() {
        return String.format("TP trans(%.2f,%.2f,%.2f) rot(%.1f,%.1f,%.1f) scale(%.2f)",
            tpTransX, tpTransY, tpTransZ, tpRotX, tpRotY, tpRotZ, tpScale);
    }
    public static String ohFpSummary() {
        return String.format("OHFP trans(%.2f,%.2f,%.2f) rot(%.1f,%.1f,%.1f) scale(%.2f)",
            ohFpTransX, ohFpTransY, ohFpTransZ, ohFpRotX, ohFpRotY, ohFpRotZ, ohFpScale);
    }
    public static String ohTpSummary() {
        return String.format("OHTP trans(%.2f,%.2f,%.2f) rot(%.1f,%.1f,%.1f) scale(%.2f)",
            ohTpTransX, ohTpTransY, ohTpTransZ, ohTpRotX, ohTpRotY, ohTpRotZ, ohTpScale);
    }
    public static String glowSummary() {
        return String.format("glow mode=%d alpha=%.2f color(%d,%d,%d) scale(%.2f)",
            glowMode, glowAlpha, glowR, glowG, glowB, glowScale);
    }
}
