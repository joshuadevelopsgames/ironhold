package kingdom.smp.dynlight;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Game-bus plumbing for Ironhold's built-in dynamic lights:
 * <ul>
 *   <li>Walks the entity list every tick to keep the active-source set fresh.</li>
 *   <li>Drops all sources when the player disconnects so a fresh world starts clean.</li>
 * </ul>
 *
 * <p>The {@link DynamicLightLoader} is registered separately via
 * {@link #onAddClientReloadListeners(AddClientReloadListenersEvent)} on the mod bus —
 * keeping the buses split keeps NeoForge from rejecting either side at registration.
 */
public final class DynamicLightsClientEvents {
    private DynamicLightsClientEvents() {}

    /** Mod-bus event handler — invoked via {@code modEventBus.addListener(...)} from {@code IronholdClient}. */
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
