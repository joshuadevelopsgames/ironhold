package kingdom.smp.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live tunable display transforms for the Wizard Stick item, per display context.
 * Defaults match the {@code item/handheld} parent so the starting visual is the
 * same as what the model file would render.
 *
 * <p>Live preview is wired only for {@code firstperson_righthand} (via
 * {@link WizardStickClientExtensions#applyForgeHandTransform}). The other
 * contexts are tracked here so {@link #printJson()} can emit a paste-ready
 * {@code "display"} block for the model JSON.
 *
 * <p>Adjusted via the {@code /wizardstickdebug} command. Print with
 * {@code /wizardstickdebug print} — copy the output into
 * {@code assets/ironhold/models/item/wizard_stick_3d.json}.
 */
public final class WizardStickTransformDebug {
    private WizardStickTransformDebug() {}

    public static final class Transform {
        public float rotX, rotY, rotZ;
        public float transX, transY, transZ;
        public float scale = 1.0F;

        Transform(float rx, float ry, float rz, float tx, float ty, float tz, float s) {
            rotX = rx; rotY = ry; rotZ = rz;
            transX = tx; transY = ty; transZ = tz;
            scale = s;
        }

        String toJson(String key, String indent) {
            return indent + "\"" + key + "\": {\n"
                + indent + "  \"rotation\": [" + fmt(rotX) + ", " + fmt(rotY) + ", " + fmt(rotZ) + "],\n"
                + indent + "  \"translation\": [" + fmt(transX) + ", " + fmt(transY) + ", " + fmt(transZ) + "],\n"
                + indent + "  \"scale\": [" + fmt(scale) + ", " + fmt(scale) + ", " + fmt(scale) + "]\n"
                + indent + "}";
        }

        String summary() {
            return String.format(
                "rot(%.1f, %.1f, %.1f) trans(%.2f, %.2f, %.2f) scale(%.2f)",
                rotX, rotY, rotZ, transX, transY, transZ, scale);
        }

        private static String fmt(float f) {
            // Emit ints when whole, otherwise up to 3 decimals
            if (f == (int) f) return Integer.toString((int) f);
            return String.format("%.3f", f).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    /** Display-context keys, in the order they should appear in the JSON. */
    public static final String[] CONTEXTS = {
        "thirdperson_righthand", "thirdperson_lefthand",
        "firstperson_righthand", "firstperson_lefthand",
        "ground", "gui", "fixed", "head"
    };

    /**
     * Defaults match the locked-in {@code display} block in
     * {@code wizard_stick_3d.json}. Keep these in sync — the mixin overrides
     * the JSON with these values, so a mismatch would silently change how the
     * item looks at runtime even though the JSON appears correct.
     */
    private static Map<String, Transform> makeDefaults() {
        Map<String, Transform> m = new LinkedHashMap<>();
        m.put("thirdperson_righthand", new Transform(1,     1,   1,    0,    4,    2,    0.85f));
        m.put("thirdperson_lefthand",  new Transform(0,    90, -35,    0,    4,    2,    0.85f));
        m.put("firstperson_righthand", new Transform(0,   -90,  25,    1.13f, 3.2f, 1.13f, 0.68f));
        m.put("firstperson_lefthand",  new Transform(0,   -90,  25,    1.13f, 3.2f, 1.13f, 0.68f));
        m.put("ground",                new Transform(0,     0,   0,    0,    2,    0,    0.5f));
        m.put("gui",                   new Transform(0,     0,   0,    0,    0,    0,    1.0f));
        m.put("fixed",                 new Transform(0,     0,   0,    0,    0,    0,    1.0f));
        m.put("head",                  new Transform(0,     0,   0,    0,    0,    0,    1.0f));
        return m;
    }

    private static Map<String, Transform> values = makeDefaults();
    private static String activeContext = "firstperson_righthand";

    public static Transform active() { return values.get(activeContext); }
    public static Transform forContext(String ctx) { return values.get(ctx); }
    public static String activeName() { return activeContext; }

    public static boolean setActive(String ctx) {
        if (!values.containsKey(ctx)) return false;
        activeContext = ctx;
        return true;
    }

    public static void resetActive() {
        values.put(activeContext, makeDefaults().get(activeContext));
    }

    public static void resetAll() {
        values = makeDefaults();
    }

    /** Pretty-printed JSON for the entire {@code display} block, ready to paste. */
    public static String printJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"display\": {\n");
        for (int i = 0; i < CONTEXTS.length; i++) {
            sb.append(values.get(CONTEXTS[i]).toJson(CONTEXTS[i], "  "));
            sb.append(i == CONTEXTS.length - 1 ? "\n" : ",\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
