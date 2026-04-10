package kingdom.smp.mixin;

import kingdom.smp.game.DeathMessageHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla death message with a random Terraria-style one.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {

    @Unique
    private DamageSource ironhold$lastDeathCause;

    @Inject(method = "die", at = @At("HEAD"))
    private void ironhold$captureDeathCause(DamageSource cause, CallbackInfo ci) {
        this.ironhold$lastDeathCause = cause;
    }

    @Redirect(
        method = "die",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;")
    )
    private Component ironhold$replaceDeathMessage(CombatTracker tracker) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        return DeathMessageHandler.buildDeathMessage(self, this.ironhold$lastDeathCause);
    }
}
