package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ButterflyDex;
import kingdom.smp.entity.ButterflySpecies;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.skill.PlayerSkillState;
import kingdom.smp.skill.SkillSavedData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Awards the "discover every butterfly" achievement plus one skill point once a player's
 * Butterfly Encyclopedia is complete.
 *
 * <p>Both rewards are idempotent — the advancement won't re-fire once earned, and the skill
 * point is granted through a {@link PlayerSkillState#withMilestone milestone} so it can only
 * be received once. That lets us call {@link #checkComplete} freely: on every new discovery
 * and on login (the login check retroactively rewards players who already filled the dex).
 */
public final class ButterflyCollectionRewards {

    private static final Identifier ADVANCEMENT =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "butterfly_collector");
    private static final String MILESTONE = "butterfly_collector";
    private static final int SKILL_POINTS = 1;

    private ButterflyCollectionRewards() {}

    /** If the player has discovered every species, grant the achievement + skill point. */
    public static void checkComplete(ServerPlayer player) {
        if (ButterflyDex.of(player).count() < ButterflySpecies.values().length) {
            return;
        }
        grantAdvancement(player);
        awardSkillPoint(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            checkComplete(player);
        }
    }

    private static void grantAdvancement(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        AdvancementHolder advancement = server.getAdvancements().get(ADVANCEMENT);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static void awardSkillPoint(ServerPlayer player) {
        SkillSavedData skills = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = skills.stateFor(player.getUUID());
        PlayerSkillState updated = state.withMilestone(MILESTONE, SKILL_POINTS);
        if (updated != state) {
            skills.setState(player.getUUID(), updated);
            ModNetworking.syncSkillsToClient(player);
        }
    }
}
