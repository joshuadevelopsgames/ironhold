package kingdom.smp.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Wires quest tracking to gameplay events. Registered on the NeoForge event bus in
 * {@link kingdom.smp.Ironhold}.
 */
public final class QuestEventHandlers {
    private QuestEventHandlers() {}

    /** How often the per-player maintenance pass runs (ticks). */
    private static final int TICK_INTERVAL = 20;

    /** Credit SLAY objectives when a player kills a matching entity. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)) return;
        QuestService.recordKill(player,
                BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()));
    }

    /** Throttled expiry / inventory-completion / timer-bar refresh. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;
        QuestService.tick(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestService.rebuildBars(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestBossBars.clear(player);
        }
    }
}
