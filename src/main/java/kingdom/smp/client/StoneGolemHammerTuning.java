package kingdom.smp.client;

/**
 * Live, client-side tuning values for where the stone golem grips the battle hammer.
 * Applied to the {@code weapon_mount} bone each frame in
 * {@link kingdom.smp.client.entity.StoneGolemRenderer}, so changes show instantly with no recompile.
 *
 * <p>Adjust via the {@code -}/{@code =} keys (nudge the active field) and {@code ;} (cycle field),
 * or precisely via {@code /golemhammer}. {@code /golemhammer print} dumps the numbers to bake into
 * the bone. Translation is in model pixels, rotation in degrees, scale is a uniform multiplier.
 */
public final class StoneGolemHammerTuning {
    private StoneGolemHammerTuning() {}

    // Baked idle grip — the golem's two-handed hold of the battle hammer (tuned in-game).
    public static float posX = 0.95f, posY = 0.30f, posZ = -1.05f;
    public static float rotX = -50f, rotY = -85f, rotZ = -30f;
    public static float scale = 1.60f;
    public static boolean hudVisible = false;

    private static final String[] NAMES = {"posX", "posY", "posZ", "rotX", "rotY", "rotZ", "scale"};
    public static int active = 0;

    public static void cycle(int dir) {
        active = (active + dir + NAMES.length) % NAMES.length;
        hudVisible = true;
    }

    /** Nudge the active field; step scales per field type (px / deg / scale-mul). */
    public static void nudge(float dir) {
        float step = active < 3 ? 0.05f : active < 6 ? 2.5f : 0.1f;  // pos: blocks, rot: deg, scale: mul
        switch (active) {
            case 0 -> posX += dir * step;
            case 1 -> posY += dir * step;
            case 2 -> posZ += dir * step;
            case 3 -> rotX += dir * step;
            case 4 -> rotY += dir * step;
            case 5 -> rotZ += dir * step;
            case 6 -> scale = Math.max(0.05f, scale + dir * step);
        }
        hudVisible = true;
    }

    public static String activeName() {
        return NAMES[active];
    }

    public static String summary() {
        return String.format("pos(%.2f, %.2f, %.2f)  rot(%.1f, %.1f, %.1f)  scale(%.2f)",
            posX, posY, posZ, rotX, rotY, rotZ, scale);
    }
}
