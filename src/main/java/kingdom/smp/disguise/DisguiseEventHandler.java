package kingdom.smp.disguise;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server-side rules for the Master of Disguise: any incoming damage instantly drops the
 * disguise, and a player that starts tracking a disguised player is told about the disguise
 * so it renders correctly on their client.
 */
public final class DisguiseEventHandler {
    private DisguiseEventHandler() {}

    /** Any damage reveals you — clear the disguise the moment something hits you. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            DisguiseManager.clear(sp);
        }
    }

    /** When a player comes into view, send them the tracked player's current disguise. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target
                && event.getEntity() instanceof ServerPlayer tracker) {
            DisguiseManager.sendTo(target, tracker);
        }
    }
}
