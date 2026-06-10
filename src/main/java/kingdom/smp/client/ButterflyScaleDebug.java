package kingdom.smp.client;

import kingdom.smp.entity.ButterflySpecies.ModelShape;

import java.util.EnumMap;

/**
 * Live client-side model scales driven by {@code /butterflydebug}.
 */
public final class ButterflyScaleDebug {
    private static final EnumMap<ModelShape, Float> SCALES = new EnumMap<>(ModelShape.class);

    static {
        reset();
    }

    private ButterflyScaleDebug() {}

    public static float scaleFor(ModelShape shape) {
        return SCALES.get(shape);
    }

    public static void set(ModelShape shape, float scale) {
        SCALES.put(shape, scale);
    }

    public static void setAll(float scale) {
        for (ModelShape shape : ModelShape.values()) {
            set(shape, scale);
        }
    }

    public static void reset() {
        setAll(0.25F);
    }

    public static String summary(ModelShape shape) {
        return shape.id() + " scale " + String.format("%.3f", scaleFor(shape));
    }
}
