package kingdom.smp.portal.client;

import java.util.List;
import kingdom.smp.portal.PortalLink;

/**
 * Render-thread-confined snapshot of the cross-dimensional portals currently visible to the client.
 * {@link PortalDetector} refreshes it; the (future) portal render pass reads it each frame — the same
 * producer/consumer split {@code MirrorReflection} uses for its per-frame mirror list.
 */
public final class ClientPortalRegistry {
    private ClientPortalRegistry() {}

    private static volatile List<PortalLink> links = List.of();

    public static void set(List<PortalLink> next) {
        links = next;
    }

    public static List<PortalLink> get() {
        return links;
    }

    public static void clear() {
        links = List.of();
    }
}
