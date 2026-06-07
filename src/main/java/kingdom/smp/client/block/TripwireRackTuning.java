package kingdom.smp.client.block;

import java.util.List;
import java.util.Locale;

/**
 * Live-tunable geometry dials for {@link TripwireRackRenderer}, so the item a tripwire hook holds
 * can be nudged into the hook's ring in-game (via {@code /rackdebug}) without recompiling.
 *
 * <p>Defaults mirror the renderer's original constants. Once a good pose is found, run
 * {@code /rackdebug print} and bake the {@code DEF_*} values below back into source.
 */
public final class TripwireRackTuning {
    private TripwireRackTuning() {}

    // Values below were dialled in live with /rackdebug (golden-sword test): the item sits flush in
    // the hook's ring rather than jutting off a peg — pitch ~flat and no outward push.

    /** Height up the block of the hook's ring/hole — the item's hang point. */
    public static final float DEF_HOOK_Y = 0.66f;
    /** Base spin (deg) about the wall normal so orientation 0 reads as "hanging down". */
    public static final float DEF_ROLL = 0.0f;
    /** Tilt (deg) out of the wall plane so the item juts off the hook like a peg. */
    public static final float DEF_PITCH = 0.1f;
    /** How far the item dangles below the ring (after the hang rotation). Small = nestled in the hole. */
    public static final float DEF_HANG = 0.1f;
    /** How far to push the item out from the wall into open space. */
    public static final float DEF_OUT_LIFT = 0.0f;
    /** Item render scale (1.0 ≈ how big an armor stand holds it). */
    public static final float DEF_SCALE = 0.92f;

    public static volatile float hookY = DEF_HOOK_Y;
    public static volatile float hangRollBase = DEF_ROLL;
    public static volatile float jutPitch = DEF_PITCH;
    public static volatile float hang = DEF_HANG;
    public static volatile float outLift = DEF_OUT_LIFT;
    public static volatile float scale = DEF_SCALE;

    /** Dial names accepted by {@link #set} (also used for tab-completion). */
    public static List<String> dials() {
        return List.of("hooky", "hang", "outlift", "pitch", "roll", "scale");
    }

    /** @return false if {@code dial} isn't a known name. */
    public static boolean set(String dial, float v) {
        switch (dial.toLowerCase(Locale.ROOT)) {
            case "hooky" -> hookY = v;
            case "hang" -> hang = v;
            case "outlift" -> outLift = v;
            case "pitch" -> jutPitch = v;
            case "roll" -> hangRollBase = v;
            case "scale" -> scale = v;
            default -> { return false; }
        }
        return true;
    }

    public static void reset() {
        hookY = DEF_HOOK_Y;
        hangRollBase = DEF_ROLL;
        jutPitch = DEF_PITCH;
        hang = DEF_HANG;
        outLift = DEF_OUT_LIFT;
        scale = DEF_SCALE;
    }

    public static String summary() {
        return String.format(Locale.ROOT,
            "hooky=%.3f  hang=%.3f  outlift=%.3f  pitch=%.1f  roll=%.1f  scale=%.3f",
            hookY, hang, outLift, jutPitch, hangRollBase, scale);
    }

    /** Paste-ready Java for the {@code DEF_*} constants above, reflecting the current live values. */
    public static String printJava() {
        return String.format(Locale.ROOT,
            "    public static final float DEF_HOOK_Y = %sf;%n"
          + "    public static final float DEF_ROLL = %sf;%n"
          + "    public static final float DEF_PITCH = %sf;%n"
          + "    public static final float DEF_HANG = %sf;%n"
          + "    public static final float DEF_OUT_LIFT = %sf;%n"
          + "    public static final float DEF_SCALE = %sf;",
            trim(hookY), trim(hangRollBase), trim(jutPitch), trim(hang), trim(outLift), trim(scale));
    }

    private static String trim(float v) {
        String s = String.format(Locale.ROOT, "%.4f", v);
        // drop trailing zeros but keep at least one decimal
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }
}
