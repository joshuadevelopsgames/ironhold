package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Server-side apply class XP + kingdom pool + immediate sync (shared by commands and gameplay). */
public final class RpgProgressionActions {
    private RpgProgressionActions() {}

    /** @return RPG data after grant (unchanged if {@code amount <= 0}). */
    public static PlayerKingdomRpgData grantClassXp(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return player.getData(ModAttachments.PLAYER_RPG.get());
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData leveled = RpgProgression.addClassXp(cur, amount);
        player.setData(ModAttachments.PLAYER_RPG.get(), leveled);
        KingdomWorldData world = overworldData(player);
        world.addKingdomXp(leveled.kingdomIndexClamped(), amount);
        ClassStatHandler.apply(player, leveled);
        RpgXpBarSync.sync(player, leveled);
        ModNetworking.syncToClient(player);
        return leveled;
    }

    private static KingdomWorldData overworldData(ServerPlayer player) {
        ServerLevel here = (ServerLevel) player.level();
        ServerLevel ow = here.getServer().getLevel(Level.OVERWORLD);
        return ow.getDataStorage().computeIfAbsent(KingdomWorldData.TYPE);
    }
}
