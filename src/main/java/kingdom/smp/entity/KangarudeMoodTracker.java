package kingdom.smp.entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-session per-player mood tracker for {@link KangarudeEntity}. Range
 * {@link #MIN_MOOD} (furious) to {@link #MAX_MOOD} (delighted), default
 * {@link #DEFAULT_MOOD} (warm — Kanga starts every conversation in a good mood).
 *
 * <p>State is in-memory and resets on server restart. Promote to a NeoForge
 * attachment if cross-restart persistence is needed.
 */
public final class KangarudeMoodTracker {
    private KangarudeMoodTracker() {}

    public static final int MIN_MOOD = -100;
    public static final int MAX_MOOD = 100;
    /** Default starting mood — Kanga is warm and likes meeting new people. */
    public static final int DEFAULT_MOOD = 30;
    /** Mood at or below this triggers Kangabrine haunting. */
    public static final int HAUNT_THRESHOLD = -75;

    private static final Map<UUID, Integer> MOOD = new ConcurrentHashMap<>();

    public static int getMood(UUID playerId) {
        return MOOD.getOrDefault(playerId, DEFAULT_MOOD);
    }

    /** Adjust mood by delta and return the new value (clamped). */
    public static int adjust(UUID playerId, int delta) {
        return MOOD.compute(playerId, (k, cur) -> {
            int base = (cur == null) ? DEFAULT_MOOD : cur;
            return Math.max(MIN_MOOD, Math.min(MAX_MOOD, base + delta));
        });
    }

    public static void reset(UUID playerId) {
        MOOD.remove(playerId);
    }

    /** Human-readable label for the current mood band; used in the system prompt. */
    public static String moodBand(int mood) {
        if (mood >= 60)  return "delighted";
        if (mood >= 20)  return "warm";
        if (mood >= -19) return "neutral";
        if (mood >= -59) return "snippy";
        return "hostile";
    }
}
