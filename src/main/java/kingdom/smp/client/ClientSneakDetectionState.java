package kingdom.smp.client;

import kingdom.smp.net.SneakDetectionPayload;

/**
 * Client-side cache for the latest sneak-detection state received from the server.
 * Read by {@code SneakEyeHud}, written by the {@link SneakDetectionPayload} handler.
 */
public final class ClientSneakDetectionState {
    private ClientSneakDetectionState() {}

    private static volatile byte state = 0;

    public static byte get() { return state; }

    public static void receive(SneakDetectionPayload payload) {
        state = payload.state();
    }
}
