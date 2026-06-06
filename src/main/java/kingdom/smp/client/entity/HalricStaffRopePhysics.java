package kingdom.smp.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * World-space verlet rope physics for Halric's Staff chain.
 *
 * <p>Six chain-link particles, each tracked as a world-space {@link Vec3}.
 * Anchor (particle 0) snaps to the player's reference position each tick;
 * subsequent particles evolve under verlet integration with constant world
 * gravity and distance constraints back to the previous link. After enough
 * relaxation passes the chain hangs straight down in world coords when at
 * rest, swings naturally when the anchor moves, and overshoots on stops.
 *
 * <p>Why world space (not local): the bone-rotation Euler decomposition
 * couldn't reliably handle third-person camera/arm transforms, so we
 * simulate in world coords (where gravity is unambiguously {@code (0, -1, 0)})
 * and convert to bone rotations at render time using the actual pose-stack
 * matrix, sidestepping every Euler-convention question.
 *
 * <p>Singleton state: assumes one local player holds at most one staff at
 * a time. Multi-player visual fidelity is not a goal for this iteration.
 */
public final class HalricStaffRopePhysics {
    private HalricStaffRopePhysics() {}

    public static final int LINKS = 6;

    /** Per-tick gravity acceleration (world blocks/tick²). */
    private static final double GRAVITY = 0.025;
    /** Velocity damping per tick — 1.0 = no damping, smaller = settles faster.
     *  Heavier damping (~0.82) prevents jump-induced overshoot that briefly
     *  inverts the chain through the anchor. */
    private static final double DAMPING = 0.82;
    /** Distance between adjacent chain links (world blocks). Total chain ≈ 0.27 blocks. */
    public static final double SEG_LENGTH = 0.045;
    /** Constraint-relaxation passes per tick. More = stiffer rope. */
    private static final int CONSTRAINT_ITERS = 8;

    private static final Vec3[] particles = new Vec3[LINKS];
    private static final Vec3[] prevParticles = new Vec3[LINKS];
    /** Pre-tick snapshot for render interpolation. Captured at the start of each
     *  tick so the renderer can lerp from renderPrev → particles using partial tick. */
    private static final Vec3[] renderPrev = new Vec3[LINKS];
    private static boolean primed;
    private static long lastTick = -1;

    /**
     * Ticks the rope once per game tick (deduped). Should be called every
     * render frame; the dedup makes repeated calls in the same tick free.
     *
     * @param anchorWorld world position to anchor particle 0 to (player + offset)
     */
    public static void tickIfDue(Vec3 anchorWorld) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now == lastTick) return;
        lastTick = now;

        if (!primed) {
            for (int i = 0; i < LINKS; i++) {
                particles[i] = anchorWorld.add(0, -SEG_LENGTH * (i + 1), 0);
                prevParticles[i] = particles[i];
                renderPrev[i] = particles[i];
            }
            primed = true;
            return;
        }

        // Snapshot current positions BEFORE integration for render interpolation.
        // The renderer will lerp from renderPrev → particles using partial tick.
        for (int i = 0; i < LINKS; i++) {
            renderPrev[i] = particles[i];
        }

        // Verlet integration: implicit velocity from (current - prev), with damping
        // and constant world gravity.
        for (int i = 0; i < LINKS; i++) {
            Vec3 vel = particles[i].subtract(prevParticles[i]).scale(DAMPING);
            Vec3 next = particles[i].add(vel).add(0, -GRAVITY, 0);
            prevParticles[i] = particles[i];
            particles[i] = next;
        }

        // Distance constraints: relax repeatedly so the chain reaches a stable
        // configuration each tick. Anchor stays pinned; other particles get
        // pulled toward their fixed segment lengths.
        for (int iter = 0; iter < CONSTRAINT_ITERS; iter++) {
            particles[0] = anchorWorld;
            for (int i = 0; i < LINKS - 1; i++) {
                Vec3 delta = particles[i + 1].subtract(particles[i]);
                double dist = delta.length();
                if (dist < 1e-9) continue;
                double correction = (dist - SEG_LENGTH) / dist;
                if (i == 0) {
                    // particle[0] is anchored — only particle[1] moves
                    particles[i + 1] = particles[i + 1].subtract(delta.scale(correction));
                } else {
                    Vec3 c = delta.scale(correction * 0.5);
                    particles[i] = particles[i].add(c);
                    particles[i + 1] = particles[i + 1].subtract(c);
                }
            }
        }
    }

    public static Vec3 getParticleWorld(int i) {
        return primed ? particles[i] : Vec3.ZERO;
    }

    /**
     * Returns the particle position interpolated between the previous tick and
     * the current tick. Use this for rendering to avoid snapping between ticks
     * (e.g. during jumps where the anchor moves ~0.42 blocks in one tick).
     *
     * @param i           particle index (0 = anchor, LINKS-1 = tip)
     * @param partialTick fraction of the current tick elapsed (0 = tick start, 1 = tick end)
     */
    public static Vec3 getParticleWorldLerped(int i, float partialTick) {
        if (!primed) return Vec3.ZERO;
        return renderPrev[i].lerp(particles[i], partialTick);
    }

    /** Convenience: anchor position for the local player (head height roughly). */
    public static Vec3 defaultAnchorFor(Player player) {
        return player.getEyePosition();
    }

    /**
     * Anchor position using partial-tick interpolation, matching Minecraft's
     * entity rendering interpolation so the chain anchor tracks the visually
     * rendered player position (not the tick-snapped position).
     */
    public static Vec3 interpolatedAnchorFor(Player player, float partialTick) {
        double x = player.xo + (player.getX() - player.xo) * partialTick;
        double y = player.yo + (player.getY() - player.yo) * partialTick;
        double z = player.zo + (player.getZ() - player.zo) * partialTick;
        return new Vec3(x, y + player.getEyeHeight(), z);
    }
}
