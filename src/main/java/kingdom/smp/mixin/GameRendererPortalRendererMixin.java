package kingdom.smp.mixin;

import kingdom.smp.portal.client.PortalRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While {@link PortalRenderer} is capturing a portal's see-through view, swap {@code minecraft.levelRenderer}
 * for the destination dimension's dedicated renderer in {@code GameRenderer.renderLevel} (terrain/entity
 * draw) and {@code GameRenderer.extract} (visible-section + render-state extraction). {@code mc.level} is
 * swapped directly by PortalRenderer (it is a public field), so this only needs to redirect the renderer.
 * Outside a capture both redirects return the real renderer unchanged.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPortalRendererMixin {

    @Redirect(
        method = "renderLevel",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;levelRenderer:Lnet/minecraft/client/renderer/LevelRenderer;",
            opcode = Opcodes.GETFIELD))
    private LevelRenderer ironhold$portalRendererInRender(Minecraft mc) {
        LevelRenderer dedicated = PortalRenderer.activeRenderer();
        return dedicated != null ? dedicated : mc.levelRenderer;
    }

    @Redirect(
        method = "extract",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;levelRenderer:Lnet/minecraft/client/renderer/LevelRenderer;",
            opcode = Opcodes.GETFIELD))
    private LevelRenderer ironhold$portalRendererInExtract(Minecraft mc) {
        LevelRenderer dedicated = PortalRenderer.activeRenderer();
        return dedicated != null ? dedicated : mc.levelRenderer;
    }
}
