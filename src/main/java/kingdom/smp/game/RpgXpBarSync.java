package kingdom.smp.game;

import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Drives the vanilla XP bar from Kingdom SMP class level + bar XP (visual only; re-synced often).
 */
public final class RpgXpBarSync {
    private RpgXpBarSync() {}

    public static void sync(ServerPlayer player, PlayerKingdomRpgData rpg) {
        int level = Math.max(0, rpg.classLevel());
        int need = RpgProgression.xpToReachNextLevel(rpg.classLevel());
        float progress = need <= 0 ? 0f : Mth.clamp((float) rpg.xpIntoLevel() / (float) need, 0f, 1f);
        player.setExperienceLevels(level);
        player.experienceProgress = progress;
        // totalExperience=0: bar reflects class level + progress only (not vanilla point totals).
        player.connection.send(new ClientboundSetExperiencePacket(progress, 0, level));
    }
}
