package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Leave-message half of the Kangarude announce suppression. As of MC 26.1.2 the
 * vanilla "left the game" broadcast moved out of {@code PlayerList.remove} and
 * into {@code ServerGamePacketListenerImpl.removePlayerFromWorld}, so the redirect
 * has to live here. The join half stays in
 * {@link PlayerListKangarudeAnnounceMixin}.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGameLeaveAnnounceMixin {

    // Mixin requires static fields to be private; this mirrors the same constant
    // in PlayerListKangarudeAnnounceMixin (the join half).
    private static final String SUPPRESSED_NAME = "Kangarude";

    @Shadow public ServerPlayer player;

    @Redirect(
        method = "removePlayerFromWorld",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void ironhold$suppressLeaveMessage(PlayerList self, Component message, boolean overlay) {
        if (player != null && SUPPRESSED_NAME.equals(player.getGameProfile().name())) {
            Ironhold.LOGGER.info("{} left the game (player leave chat suppressed)", SUPPRESSED_NAME);
            return;
        }
        self.broadcastSystemMessage(message, overlay);
    }
}
