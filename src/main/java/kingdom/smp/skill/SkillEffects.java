package kingdom.smp.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side helper for reading a player's profession ranks. Used by the various event
 * handlers in {@link SkillEventHandlers} that apply gameplay effects from those ranks.
 *
 * Read-only — server is the source of truth for skill state.
 */
public final class SkillEffects {
    private SkillEffects() {}

    /** Read the player's current rank in the given profession. Returns {@code null} if no progress. */
    public static ProfessionRank rankFor(Player player, Profession profession) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel sl)) return null;
        SkillSavedData data = SkillSavedData.get(sl);
        return data.stateFor(sp.getUUID()).rankFor(profession);
    }

    /** True if the player has reached at least the given rank in the profession. */
    public static boolean hasAtLeast(Player player, Profession profession, ProfessionRank rank) {
        ProfessionRank current = rankFor(player, profession);
        return current != null && current.order() >= rank.order();
    }

    /**
     * Bonus-drop chance percentage based on rank order. Used by Mining/Farming/Fishing.
     * <pre>
     *   Novice:     5%
     *   Apprentice: 10%
     *   Journeyman: 20%
     *   Expert:     30%
     *   Master:     40%
     * </pre>
     * Returns 0 if the player has no progress in the profession.
     */
    public static int extraDropChancePercent(Player player, Profession profession) {
        ProfessionRank rank = rankFor(player, profession);
        if (rank == null) return 0;
        return switch (rank) {
            case NOVICE -> 5;
            case APPRENTICE -> 10;
            case JOURNEYMAN -> 20;
            case EXPERT -> 30;
            case MASTER -> 40;
        };
    }
}
