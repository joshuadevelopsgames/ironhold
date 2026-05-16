package kingdom.smp.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live tunable display transforms for Halric's Staff item, per display context.
 *
 * <p>Defaults must mirror the {@code display} block in
 * {@code assets/ironhold/models/item/halric_staff.json} — the mixin overwrites
 * the JSON-loaded transform with these values at render time, so a mismatch
 * silently changes how the item looks even though the JSON appears correct.
 *
 * <p>Adjusted via the {@code /halricstaffdebug} command. Print with
 * {@code /halricstaffdebug print} — copy the output into the model JSON.
 */
public final class HalricStaffTransformDebug {
    private HalricStaffTransformDebug() {}

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
            if (f == (int) f) return Integer.toString((int) f);
            return String.format("%.3f", f).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    public static final String[] CONTEXTS = {
        "thirdperson_righthand", "thirdperson_lefthand",
        "firstperson_righthand", "firstperson_lefthand",
        "ground", "gui", "fixed", "head"
    };

    private static Map<String, Transform> makeDefaults() {
        Map<String, Transform> m = new LinkedHashMap<>();
        m.put("thirdperson_righthand", new Transform(50,  -40,   0,  37,   10,    7,    2.5f));
        m.put("thirdperson_lefthand",  new Transform(30, -160,   0,  -3,  -10,   45,    3.0f));
        m.put("firstperson_righthand", new Transform(0,   90,   0,   0,   10,    0,    1.0f));
        m.put("firstperson_lefthand",  new Transform(0,   90,   0,  -2,   10,    1,    1.0f));
        m.put("ground",                new Transform(0,    0,   0,   0,    4,    0,    0.3f));
        m.put("gui",                   new Transform(0,  180,   0,   0,    0,    0,    5.0f));
        m.put("fixed",                 new Transform(0,    0,   0,   0,    0,    0,    0.5f));
        m.put("head",                  new Transform(0,    0,   0,   0,    0,    0,    0.5f));
        return m;
    }

    private static Map<String, Transform> values = makeDefaults();
    private static String activeContext = "thirdperson_righthand";

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
