package kingdom.smp.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player mic-suppression window so Kangarude's own TTS audio (echoing
 * back through the player's speakers into their mic) doesn't get transcribed
 * and fed back as "user input" — a classic feedback loop without this gate.
 *
 * <p>Stateless aside from the muted-until map; safe to call from any thread.
 * Lives in {@code kingdom.smp.ai} (not the {@code svc/} subpackage) so it has
 * zero Simple Voice Chat type references — both the entity and the mic
 * listener depend on it without dragging in SVC classes.
 */
public final class MicGate {

    private static final Map<UUID, Long> MUTED_UNTIL = new ConcurrentHashMap<>();

    private MicGate() {}

    /** Extend the mute window for {@code playerId} by at least {@code ms} from now. */
    public static void muteFor(UUID playerId, long ms) {
        if (playerId == null || ms <= 0) return;
        long target = System.currentTimeMillis() + ms;
        MUTED_UNTIL.merge(playerId, target, Math::max);
    }

    /** True iff the player's mic should be ignored right now. */
    public static boolean isMuted(UUID playerId) {
        if (playerId == null) return false;
        Long until = MUTED_UNTIL.get(playerId);
        if (until == null) return false;
        if (System.currentTimeMillis() < until) return true;
        // Lazily evict expired entries so the map doesn't grow unbounded.
        MUTED_UNTIL.remove(playerId, until);
        return false;
    }
}
