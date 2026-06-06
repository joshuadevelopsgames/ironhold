package kingdom.smp.portal.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Game-bus hooks that drive {@link PortalDetector} once per client tick (the detector self-throttles
 * its actual scans) and tear down secondary views on disconnect. Registered from {@code IronholdClient},
 * matching the {@code MirrorReflectionEvents} pattern.
 */
public final class PortalClientEvents {
    private PortalClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PortalDetector.tick();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDimensionStack.clear();
        PortalDetector.reset();
    }
}
