package kingdom.smp.dynlight;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Plumbing for Ironhold's built-in dynamic lights:
 * <ul>
 *   <li>Registers the {@link DynamicLightLoader} on the client resource manager.</li>
 *   <li>Walks the entity list every tick to keep the active-source set fresh.</li>
 *   <li>Drops all sources when the player disconnects so a fresh world starts clean.</li>
 * </ul>
 */
public final class DynamicLightsClientEvents {
    private DynamicLightsClientEvents() {}

    /** Fired on the mod bus during client construction. */
    @SubscribeEvent
    public static void onAddClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(DynamicLightLoader.LISTENER_ID, new DynamicLightLoader());
    }

    /** Fired on the game bus. */
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        DynamicLights.tick(mc.level);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DynamicLights.reset();
    }
}
