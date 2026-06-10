package kingdom.smp.client;

/**
 * Live, client-only in-hand / in-GUI transform state for the Club family (plain / spiked /
 * ribbed share one renderer, so one set of poses drives all three). Driven by
 * {@code /clubdebug}. {@link kingdom.smp.client.entity.ClubRenderer} reads these every frame,
 * so the fields ARE the live values; their initial values are the compiled-in defaults.
 *
 * <p>Tuning workflow: in-game run e.g. {@code /clubdebug gui rot 30 225 0}, {@code /clubdebug
 * gui scale 0.5}, then {@code /clubdebug print} and paste the numbers back to bake them as the
 * defaults here. Mirrors the Battle Hammer's {@code /hammerdebug} tuner.</p>
 */
public final class ClubTransformDebug {
    private ClubTransformDebug() {}

    /** One per-display-context transform: translate, then scale, then Z·Y·X rotation. */
    public static final class Pose {
        public float tx, ty, tz, rx, ry, rz, scale;
        public Pose(float tx, float ty, float tz, float rx, float ry, float rz, float scale) {
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.rx = rx; this.ry = ry; this.rz = rz; this.scale = scale;
        }
        public String summary(String name) {
            return String.format("%s trans(%.2f,%.2f,%.2f) rot(%.1f,%.1f,%.1f) scale(%.2f)",
                name, tx, ty, tz, rx, ry, rz, scale);
        }
    }

    // First/third person mirror the old pitchfork-style poses (unchanged behaviour).
    public static final Pose FP     = new Pose(0.50f, 0.51f, 0.50f,   0f,   0f,   0f, 1.00f);
    public static final Pose TP     = new Pose(-0.70f, 0.20f, 0.50f,  0f,   1f, 270f, 1.50f);
    // Static-display contexts now show the 3D model. Block-ish starting poses — tune in-game.
    public static final Pose GUI    = new Pose(0.50f, 0.10f, 0.50f,  30f, 225f,   0f, 0.55f);
    public static final Pose GROUND = new Pose(0.50f, 0.20f, 0.50f,   0f,   0f,   0f, 0.40f);
    public static final Pose FIXED  = new Pose(0.50f, 0.50f, 0.50f,   0f, 180f,   0f, 0.70f);
}
