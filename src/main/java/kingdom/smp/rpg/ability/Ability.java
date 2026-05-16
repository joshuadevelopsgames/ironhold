package kingdom.smp.rpg.ability;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * One active class ability (Iron Word, Guardian's Vow, etc.). Implementations are
 * registered in {@link AbilityRegistry}; the registry maps each {@link PlayerClass}
 * to up to four ability slots (Z/X/C/V).
 */
public interface Ability {

    Identifier id();

    /** Cooldown in game ticks (20 = 1 second). */
    int cooldownTicks();

    /** Class level required before the player can cast this. */
    int unlockLevel();

    /** Classes for which this ability appears in the kit. */
    Set<PlayerClass> classes();

    /** Translation key root, e.g. {@code "ability.ironhold.iron_ward"}. Used for HUD tooltips. */
    String translationKey();

    /**
     * Server-side cast. Return {@code true} on success (cooldown is then started by the caller).
     * Return {@code false} on a soft failure (no valid target / wrong stance) so the cooldown is
     * NOT consumed — the HUD will flash red instead.
     */
    boolean cast(ServerPlayer player);
}
