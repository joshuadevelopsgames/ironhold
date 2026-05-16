package kingdom.smp.ai;

import kingdom.smp.Ironhold;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflective shim into {@code kingdom.smp.ai.svc.MicrophoneListener} so the
 * non-SVC parts of the mod (networking handlers, the entity itself) can drive
 * push-to-talk without dragging Simple Voice Chat types into the verifier
 * when SVC isn't on the classpath.
 *
 * <p>Same pattern as {@link SvcVoiceBridge}: caller checks {@link #isAvailable()},
 * impl is loaded by name only when SVC is present.
 */
public final class KangaPttBridge {

    private KangaPttBridge() {}

    public static boolean isAvailable() {
        return SvcVoiceBridge.isAvailable();
    }

    /** Tap-on / tap-off for the player's PTT state. No-op if SVC is missing. */
    public static void togglePtt(ServerPlayer player) {
        if (!isAvailable()) return;
        try {
            Class<?> impl = Class.forName("kingdom.smp.ai.svc.MicrophoneListener");
            Method m = impl.getMethod("togglePtt", ServerPlayer.class);
            m.invoke(null, player);
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] PTT toggle dispatch failed: {}", t.getMessage());
        }
    }

    /** Drop any PTT/buffer state for a player. No-op if SVC is missing. */
    public static void clearForPlayer(UUID playerId) {
        if (!isAvailable()) return;
        try {
            Class<?> impl = Class.forName("kingdom.smp.ai.svc.MicrophoneListener");
            Method m = impl.getMethod("clearForPlayer", UUID.class);
            m.invoke(null, playerId);
        } catch (Throwable t) {
            Ironhold.LOGGER.warn("[Kangarude] PTT clear dispatch failed: {}", t.getMessage());
        }
    }
}
