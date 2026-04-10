package kingdom.smp.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Terraria-style random death messages. Called by {@code ServerPlayerDeathMixin}
 * to replace the vanilla death message with a randomly selected one.
 */
public final class DeathMessageHandler {
    private DeathMessageHandler() {}

    private static final int GENERIC_COUNT = 70;
    private static final int KILLED_COUNT = 30;

    /**
     * Build a random death message for the given player.
     * Called from the mixin that replaces {@code CombatTracker.getDeathMessage()}.
     */
    public static Component buildDeathMessage(ServerPlayer player, DamageSource source) {
        String playerName = player.getDisplayName().getString();

        Entity killer = source != null ? source.getEntity() : null;

        if (killer != null && killer != player) {
            String killerName = killer.getDisplayName().getString();
            int index = player.getRandom().nextInt(KILLED_COUNT);
            return Component.translatable("ironhold.death.killed." + index, playerName, killerName);
        } else {
            int index = player.getRandom().nextInt(GENERIC_COUNT);
            return Component.translatable("ironhold.death." + index, playerName);
        }
    }
}
