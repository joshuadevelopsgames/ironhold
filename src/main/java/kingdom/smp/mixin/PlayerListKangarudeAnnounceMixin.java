package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Suppresses the vanilla "joined the game" chat broadcast for a real human
 * player whose name is {@value #SUPPRESSED_NAME} — it's logged to the server
 * console instead. The Kangarude / Kangabrine <em>NPC</em> keeps its own
 * theatrical join chat line (broadcast explicitly elsewhere); this mixin only
 * silences the automatic vanilla announcement for a player sharing the name.
 *
 * <p>The matching "left the game" suppression lives in
 * {@link ServerGameLeaveAnnounceMixin}: as of MC 26.1.2 the leave broadcast
 * moved out of {@code PlayerList.remove} into
 * {@code ServerGamePacketListenerImpl.removePlayerFromWorld}.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListKangarudeAnnounceMixin {

    private static final String SUPPRESSED_NAME = "Kangarude";

    @Redirect(
        method = "placeNewPlayer",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void ironhold$suppressJoinMessage(
            PlayerList self, Component message, boolean overlay,
            Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        if (SUPPRESSED_NAME.equals(player.getGameProfile().name())) {
            Ironhold.LOGGER.info("{} joined the game (player join chat suppressed)", SUPPRESSED_NAME);
            return;
        }
        self.broadcastSystemMessage(message, overlay);
    }
}
