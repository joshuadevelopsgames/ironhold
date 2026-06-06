package kingdom.smp.mixin;

import kingdom.smp.perms.IronholdPermissionSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overlays Ironhold's permission store onto every player's {@link PermissionSet}. MC 1.26 resolves all
 * permission checks (vanilla commands and modded alike) through {@code ServerPlayer.permissions()}, so
 * wrapping its return value is what turns Ironhold into a server-wide permissions manager. The overlay
 * defers to the vanilla base whenever the store has nothing to say, so unmanaged players are unaffected.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerPermissionsMixin {

    @Inject(method = "permissions", at = @At("RETURN"), cancellable = true)
    private void ironhold$overlayPermissions(CallbackInfoReturnable<PermissionSet> cir) {
        PermissionSet base = cir.getReturnValue();
        if (base instanceof IronholdPermissionSet) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        cir.setReturnValue(new IronholdPermissionSet(self.level().getServer().overworld(), self.getUUID(), base));
    }
}
