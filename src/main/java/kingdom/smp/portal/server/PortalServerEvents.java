package kingdom.smp.portal.server;

import kingdom.smp.Ironhold;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Server game-bus hooks for the immersive-portal streaming bridge. Auto-registered via annotation. */
@EventBusSubscriber(modid = Ironhold.MODID)
public final class PortalServerEvents {
    private PortalServerEvents() {}

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PortalViewTracker.forget(sp);
        }
    }
}
