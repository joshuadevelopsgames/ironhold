package kingdom.smp.rpg.ability;

import kingdom.smp.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side entry point that validates a cast request, runs the ability, and starts cooldown. */
public final class AbilityDispatch {
    private AbilityDispatch() {}

    /** Called by the network handler when a client requests to cast slot {@code slot}. */
    public static void tryCast(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= AbilityRegistry.SLOT_COUNT) {
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        Ability ability = AbilityRegistry.forSlot(rpg.playerClass(), slot);
        if (ability == null) {
            return;
        }
        if (rpg.classLevel() < ability.unlockLevel()) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        AbilityCooldowns cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get());
        String key = ability.id().toString();
        if (cds.isOnCooldown(key, now)) {
            return;
        }

        boolean fired = false;
        try {
            fired = ability.cast(player);
        } catch (Throwable t) {
            // Don't let a bug in one ability poison the dispatcher.
            kingdom.smp.Ironhold.LOGGER.error("Ability cast threw", t);
            fired = false;
        }
        if (!fired) {
            return;
        }
        AbilityCooldowns next = cds.withCooldown(key, now + ability.cooldownTicks());
        player.setData(ModAttachments.ABILITY_COOLDOWNS.get(), next);
    }
}
