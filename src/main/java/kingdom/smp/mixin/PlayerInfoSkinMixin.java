package kingdom.smp.mixin;

import com.mojang.authlib.GameProfile;
import kingdom.smp.client.KangarudeSkins;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the default-Steve face shown in the tab list with the bundled
 * Kangarude / Kangabrine textures whenever the {@link PlayerInfo} represents
 * one of our synthetic NPC entries.
 *
 * <p>The server announces these entries via {@link kingdom.smp.entity.KangarudePlayerListSync}
 * with profile names {@code "Kangarude"} or {@code "Kangabrine"}. Without
 * this mixin the client falls back to the default skin because the fake
 * GameProfile carries no Mojang-signed textures property.
 */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoSkinMixin {
    @Shadow @Final private GameProfile profile;

    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void ironhold$overrideKangarudeSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        String name = profile.name();
        if (name == null) return;
        if ("Kangarude".equals(name)) {
            cir.setReturnValue(KangarudeSkins.KANGARUDE_SKIN);
        } else if ("Kangabrine".equals(name)) {
            cir.setReturnValue(KangarudeSkins.KANGABRINE_SKIN);
        }
    }
}
