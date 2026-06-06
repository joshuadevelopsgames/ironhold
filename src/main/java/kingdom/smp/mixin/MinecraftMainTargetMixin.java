package kingdom.smp.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import kingdom.smp.client.MirrorReflection;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While the mirror reflection is being captured, redirect {@code getMainRenderTarget()} to the
 * mirror's off-screen framebuffer. The level render pipeline keys its output target and viewport
 * off this call, so the whole world pass lands in our buffer instead of the screen.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMainTargetMixin {
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void ironhold$redirectToMirror(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget mirror = MirrorReflection.activeCaptureTarget();
        if (mirror != null) {
            cir.setReturnValue(mirror);
            return;
        }
        RenderTarget portal = kingdom.smp.portal.client.PortalRenderer.activeCaptureTarget();
        if (portal != null) {
            cir.setReturnValue(portal);
        }
    }
}
