package kingdom.smp.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Customizes how slots render on Ironhold-reskinned container screens
 * ({@link InventoryScreen}, {@link CraftingScreen}, {@link FurnaceScreen}):
 * <ul>
 *   <li>Suppresses vanilla's empty-slot silhouettes (helmet/chestplate/leggings/boots/shield/accessory)
 *       — Ironhold textures have those icons painted into the wells already.</li>
 *   <li>Replaces the gray slot-hover highlight sprite with a translucent light-brown fill
 *       that matches the wood / metal aesthetic.</li>
 * </ul>
 *
 * <p>Other container screens (chests, anvils, etc.) keep vanilla behavior.
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenSlotIconMixin {

    /** Translucent warm brown — matches Ironhold wood-frame palette. ARGB. */
    private static final int IRONHOLD_HIGHLIGHT_COLOR = 0xCCB87333;

    /** True if this screen instance is one of the Ironhold-reskinned screens. */
    private boolean ironhold$isReskinnedScreen() {
        if ((Object) this instanceof InventoryScreen) return true;
        if ((Object) this instanceof CraftingScreen) return true;
        if ((Object) this instanceof FurnaceScreen) return true;
        // ContainerScreen handles all chest sizes — only reskin 3-row + 6-row variants.
        if ((Object) this instanceof ContainerScreen cs) {
            int rows = cs.getMenu().getRowCount();
            return rows == 3 || rows == 6;
        }
        return false;
    }

    @Redirect(
            method = "extractSlot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getNoItemIcon()Lnet/minecraft/resources/Identifier;"))
    private Identifier ironhold$suppressEmptySlotIcons(Slot slot) {
        if (ironhold$isReskinnedScreen()) {
            return null;
        }
        return slot.getNoItemIcon();
    }

    /**
     * Replace the back-layer highlight sprite (drawn under slot contents) with our brown fill.
     * Sprite blit is at (slot.x − 4, slot.y − 4) sized 24×24; we draw a 16×16 fill on the slot proper.
     */
    @Redirect(
            method = "extractSlotHighlightBack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void ironhold$replaceBackHighlight(GuiGraphicsExtractor gfx,
                                               RenderPipeline pipeline, Identifier sprite,
                                               int x, int y, int w, int h) {
        if (ironhold$isReskinnedScreen()) {
            // Skip the back layer — the front fill is enough.
            return;
        }
        gfx.blitSprite(pipeline, sprite, x, y, w, h);
    }

    /** Replace the front-layer highlight sprite with our brown fill. */
    @Redirect(
            method = "extractSlotHighlightFront",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void ironhold$replaceFrontHighlight(GuiGraphicsExtractor gfx,
                                                RenderPipeline pipeline, Identifier sprite,
                                                int x, int y, int w, int h) {
        if (ironhold$isReskinnedScreen()) {
            // x,y are 4 px outside the 16×16 slot — fill the slot interior in brown.
            gfx.fill(x + 4, y + 4, x + 4 + 16, y + 4 + 16, IRONHOLD_HIGHLIGHT_COLOR);
            return;
        }
        gfx.blitSprite(pipeline, sprite, x, y, w, h);
    }
}
