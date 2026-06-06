package kingdom.smp.mixin;

import kingdom.smp.client.MirrorReflection;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While capturing the mirror view, skip first-person-only effects: the held-item render and
 *  view-bob. The reflection is a fixed world camera, not the player's first-person view. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMirrorHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void ironhold$skipHandInMirror(CallbackInfo ci) {
        if (MirrorReflection.isCapturing() || kingdom.smp.portal.client.PortalRenderer.isCapturing()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void ironhold$skipBobView(CallbackInfo ci) {
        if (MirrorReflection.isCapturing() || kingdom.smp.portal.client.PortalRenderer.isCapturing()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void ironhold$skipBobHurt(CallbackInfo ci) {
        if (MirrorReflection.isCapturing() || kingdom.smp.portal.client.PortalRenderer.isCapturing()) {
            ci.cancel();
        }
    }
}
