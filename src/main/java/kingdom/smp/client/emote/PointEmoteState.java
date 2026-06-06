package kingdom.smp.client.emote;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of which players are currently playing the "point"
 * emote, keyed by player UUID, storing the game tick the emote began. This is a
 * clean-room replacement for a full animation library: a single procedural pose
 * blended in/out over a fixed duration, no keyframe format and no third-party
 * dependency.
 *
 * <p>The local player adds itself on keypress; remote players are added when a
 * broadcast packet arrives. The render layer reads {@link #amount} every frame.
 */
public final class PointEmoteState {
    private PointEmoteState() {}

    /** Ticks the arm takes to rise to the point. */
    private static final float RISE_TICKS = 6f;
    /** Tick the lower-back begins. */
    private static final float LOWER_START = 50f;
    /** Total length; entry is dropped after this. */
    private static final float DURATION = 56f;

    private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();

    /** Begin (or restart) the point emote for the given player. */
    public static void start(UUID player, long gameTime) {
        ACTIVE.put(player, gameTime);
    }

    /** Client packet entry point — begins the emote at the current client tick. */
    public static void receive(UUID player) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            ACTIVE.put(player, level.getGameTime());
        }
    }

    public static void clear(UUID player) {
        ACTIVE.remove(player);
    }

    /**
     * Pointing blend for a player: 0 = arm at rest, 1 = fully pointing.
     * Eases in over {@link #RISE_TICKS}, holds, then eases back out. Returns 0
     * and forgets the player once the emote has fully finished.
     */
    public static float amount(UUID player, long gameTime, float partialTick) {
        Long start = ACTIVE.get(player);
        if (start == null) {
            return 0f;
        }
        float elapsed = (gameTime - start) + partialTick;
        if (elapsed < 0f) {
            return 0f;
        }
        if (elapsed >= DURATION) {
            ACTIVE.remove(player);
            return 0f;
        }
        float t;
        if (elapsed <= RISE_TICKS) {
            t = elapsed / RISE_TICKS;              // 0 -> 1 rising
        } else if (elapsed >= LOWER_START) {
            t = (DURATION - elapsed) / (DURATION - LOWER_START); // 1 -> 0 lowering
        } else {
            t = 1f;                                // hold
        }
        return easeInOut(Math.max(0f, Math.min(1f, t)));
    }

    /** Smoothstep so the arm accelerates and settles instead of snapping. */
    private static float easeInOut(float x) {
        return x * x * (3f - 2f * x);
    }
}
