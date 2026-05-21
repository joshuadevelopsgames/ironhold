package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Per-frame physics for Halric's Staff hanging chain. Drives each of the 6
 * chain_link bones with a damped-spring response to the local player's
 * horizontal velocity, so the chain trails behind motion and overshoots on
 * stops. Cheap (no allocations on the hot path), single-threaded — runs only
 * for the local player's held staff.
 *
 * <p>Each bone's rotation is set as a small relative delta; with the chain
 * parented in the bone hierarchy (chain_link_1 → … → chain_link_6) these
 * deltas compound into a curving chain. Direction:
 * <ul>
 *   <li>Z rotation ← sideways (left/right) velocity in player-local space</li>
 *   <li>X rotation ← forward/back velocity in player-local space</li>
 * </ul>
 *
 * <p>The physics state is updated once per render tick (deduped via game time)
 * — multiple GeckoLib render passes per frame all share the same snapshot.
 */
public final class HalricStaffChainPhysics {
    private HalricStaffChainPhysics() {}

    public static final int LINKS = 6;

    /** Per-link "additional bend beyond the parent" — chain parenting compounds these. */
    private static final float LAG_PER_LINK_DEG = 7f;
    /** Spring stiffness toward target (0 = no pull, 1 = snap instantly). */
    private static final float SPRING = 0.18f;
    /** Velocity damping (0 = stop, 1 = no damping). */
    private static final float DAMPING = 0.82f;
    /** Sensitivity of target angle to player velocity. */
    private static final float VEL_TO_DEG = 120f;

    private static final float[] thetaZ = new float[LINKS];   // current Z rot (deg)
    private static final float[] omegaZ = new float[LINKS];   // angular vel Z
    private static final float[] thetaX = new float[LINKS];   // current X rot (deg)
    private static final float[] omegaX = new float[LINKS];   // angular vel X

    private static long lastUpdateTick = -1;
    private static double prevX, prevY, prevZ;
    private static float prevYaw;
    private static boolean primed;

    // Swing-reversal detection state for the chain-clink sound.
    private static final float[] prevOmegaZ = new float[LINKS];
    private static long lastClinkTick = Long.MIN_VALUE;
    /** Minimum |omega| at reversal to count as a real clink (filters tiny tremors). */
    private static final float CLINK_OMEGA_THRESHOLD = 0.25f;
    /** Min ticks between clinks so we don't spam the channel. */
    private static final int CLINK_COOLDOWN_TICKS = 3;
    /** Distance-attenuation reference range — chain is held, so volume is dominant. */
    private static final float CLINK_VOLUME = 0.5f;

    /**
     * How strongly camera/body yaw rotation contributes to chain swing.
     * The staff is held to the right of the player center, so a yaw rotation
     * makes the staff's grip travel through an arc — that arc velocity adds to
     * the same "right-velocity" input that translation uses. Larger = chain
     * reacts more dramatically to turning.
     */
    private static final float YAW_LAG_SCALE = 0.04f;

    /**
     * Total bias applied along the chain (distributed evenly across the 6 links).
     * Gravity correction: when the staff is held at an angle, the chain should
     * still hang straight down in world space, so the chain bones need a
     * counter-rotation that sums to this total. Set per-frame by the renderer,
     * computed from the current display context's rotation.
     */
    private static float gravityBiasXDeg = 0f;
    private static float gravityBiasZDeg = 0f;

    /**
     * Sets gravity bias for both axes. {@code xDeg} bends the chain forward/back
     * relative to the staff, {@code zDeg} bends it left/right. Together they
     * orient the chain along world-down regardless of staff tilt.
     */
    public static void setGravityBiasDegrees(float xDeg, float zDeg) {
        gravityBiasXDeg = xDeg;
        gravityBiasZDeg = zDeg;
    }

    /** Update the rope state once per tick for the local player's held staff. */
    public static void tickIfDue() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now == lastUpdateTick) return;
        lastUpdateTick = now;

        // Derive velocity from position delta (more reliable than getDeltaMovement which
        // can be zero mid-tick during render).
        // Use BODY yaw, not head yaw. The staff is held in the arm bone, which
        // is parented to the body — so the staff's world-space rotation tracks
        // yBodyRot. Reading getYRot() (head/camera yaw) would make the chain
        // swing when the player just looks around with the mouse, even though
        // the staff itself hasn't moved in the world. Third-person viewers and
        // other players would see ghost-swing on a stationary staff.
        float bodyYawNow = player.yBodyRot;

        if (!primed) {
            prevX = player.getX(); prevY = player.getY(); prevZ = player.getZ();
            prevYaw = bodyYawNow;
            primed = true;
            return;
        }
        double dx = player.getX() - prevX;
        double dy = player.getY() - prevY;
        double dz = player.getZ() - prevZ;
        prevX = player.getX(); prevY = player.getY(); prevZ = player.getZ();

        // Body yaw delta with wrap-around handling (yaw can jump 180 ↔ -180).
        float yawDelta = Mth.wrapDegrees(bodyYawNow - prevYaw);
        prevYaw = bodyYawNow;

        // Transform world velocity into player-local space (using body yaw —
        // that's the frame the staff is held in).
        float yawRad = bodyYawNow * Mth.DEG_TO_RAD;
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        // Player forward in world XZ = (-sin(yaw), 0, cos(yaw)); right = (cos(yaw), 0, sin(yaw)).
        double rightVel   = dx * cosY + dz * sinY;
        double forwardVel = -dx * sinY + dz * cosY;
        // Vertical "bounce" — jumping/falling adds a forward kick.
        double verticalVel = dy;

        // Add yaw rotation as a lateral velocity contribution: turning the body
        // rotates the staff's grip through an arc, which feels (to the chain)
        // like a sideways push at the anchor point. Sign is inverted relative
        // to translation — the staff's local axes after the third-person pose
        // make yaw and rightward translation push the chain in opposite ways.
        rightVel -= yawDelta * YAW_LAG_SCALE;

        // Gravity correction applied to the TOP chain link only (chain_link_1),
        // not split evenly across all 6 links. With chain parenting, distributing
        // the bias across multiple links would compound non-commutatively (a
        // sequence of mixed X+Z rotations is not equivalent to one large X+Z
        // rotation) and the chain would end up pointing in the wrong direction.
        // Rotating just the top link makes the entire chain hang straight in
        // the target direction — physically what a real chain under gravity
        // should do at rest anyway.

        // Per-link relative target = bias + velocity lag, modulated by depth.
        // Deeper links get a slightly larger velocity share (heavier tail).
        for (int i = 0; i < LINKS; i++) {
            float depth = (i + 1f) / LINKS;

            // Sign flipped (again) — the third-person pose changed (rot [270,0,180]),
            // which inverts the chain's local axes relative to player motion, so we
            // flip back to the original sign convention.
            float velTargetZ = Mth.clamp(
                (float) (rightVel * VEL_TO_DEG) * (0.6f + 0.4f * depth),
                -LAG_PER_LINK_DEG, LAG_PER_LINK_DEG);
            float velTargetX = Mth.clamp(
                (float) (-forwardVel * VEL_TO_DEG) * (0.6f + 0.4f * depth)
                + (float) (verticalVel * VEL_TO_DEG * 0.4f),
                -LAG_PER_LINK_DEG, LAG_PER_LINK_DEG);

            // Only chain_link_1 (i==0) carries the gravity bias.
            float biasZForLink = (i == 0) ? gravityBiasZDeg : 0f;
            float biasXForLink = (i == 0) ? gravityBiasXDeg : 0f;

            float targetZ = biasZForLink + velTargetZ;
            float targetX = biasXForLink + velTargetX;

            omegaZ[i] = (omegaZ[i] + (targetZ - thetaZ[i]) * SPRING) * DAMPING;
            omegaX[i] = (omegaX[i] + (targetX - thetaX[i]) * SPRING) * DAMPING;
            thetaZ[i] += omegaZ[i];
            thetaX[i] += omegaX[i];
        }

        // Chain-clink detection: a "clink" is when a link's swing reverses
        // direction (omega sign flips) with enough energy. Cooldown prevents
        // the sound from firing on every link in the same swing.
        if (now - lastClinkTick >= CLINK_COOLDOWN_TICKS) {
            for (int i = 0; i < LINKS; i++) {
                boolean reversed = Math.signum(omegaZ[i]) != Math.signum(prevOmegaZ[i])
                    && prevOmegaZ[i] != 0f;
                if (reversed && Math.abs(prevOmegaZ[i]) >= CLINK_OMEGA_THRESHOLD) {
                    // Pitch varies slightly with which link reversed (deeper =
                    // higher pitch, like smaller links) and with swing energy.
                    float pitch = 0.85f + 0.05f * i
                        + Math.min(0.25f, Math.abs(prevOmegaZ[i]) * 0.02f);
                    System.out.println("[HalricStaffChainPhysics] CLINK fired: link=" + i
                        + " prevOmega=" + prevOmegaZ[i] + " sound=" + kingdom.smp.ModSounds.HALRIC_STAFF_CHAIN_CLINK.get());
                    player.level().playLocalSound(
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        kingdom.smp.ModSounds.HALRIC_STAFF_CHAIN_CLINK.get(),
                        SoundSource.PLAYERS,
                        CLINK_VOLUME, pitch, false);
                    lastClinkTick = now;
                    break;
                }
            }
        }
        for (int i = 0; i < LINKS; i++) {
            prevOmegaZ[i] = omegaZ[i];
        }
    }

    /** Returns the bone rotation in RADIANS for chain_link_(linkIndex+1). */
    public static float getRotZRadians(int linkIndex) {
        if (linkIndex < 0 || linkIndex >= LINKS) return 0f;
        return thetaZ[linkIndex] * Mth.DEG_TO_RAD;
    }

    public static float getRotXRadians(int linkIndex) {
        if (linkIndex < 0 || linkIndex >= LINKS) return 0f;
        return thetaX[linkIndex] * Mth.DEG_TO_RAD;
    }

    /**
     * Per-link gravity bias in radians. Only chain_link_1 (linkIndex=0) carries
     * the full bias; the rest return 0. The renderer subtracts this from
     * actual theta to isolate the velocity-driven swing, then scales just the
     * swing portion (so first-person can dampen swing without breaking rest pose).
     */
    public static float getBiasZForLinkRadians(int linkIndex) {
        return (linkIndex == 0 ? gravityBiasZDeg : 0f) * Mth.DEG_TO_RAD;
    }

    public static float getBiasXForLinkRadians(int linkIndex) {
        return (linkIndex == 0 ? gravityBiasXDeg : 0f) * Mth.DEG_TO_RAD;
    }
}
