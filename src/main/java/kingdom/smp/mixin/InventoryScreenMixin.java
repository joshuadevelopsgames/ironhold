package kingdom.smp.mixin;

import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders accessory and vanity panels on the survival inventory screen. Slots are real
 * {@link net.minecraft.world.inventory.Slot}s from {@link InventoryMenuMixin}.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    /** Matches vanilla container title / label gray (e.g. "Crafting"). */
    private static final int VANILLA_TITLE    = 0xFF404040;
    private static final int PANEL_INNER_LINE = 0xFF555555;
    private static final int SLOT_OUTLINE     = 0xFF373737;
    private static final int STONE_FACE       = 0xFFC6C6C6;
    private static final int STONE_SHADOW     = 0xFF555555;
    private static final int STONE_HIGHLIGHT  = 0xFFFFFFFF;
    private static final int OUTER_EDGE       = 0xFF222222;

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void ironhold$renderAccessoryIslands(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                                   float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) (Object) this;
        int left = gui.getGuiLeft();
        int top  = gui.getGuiTop();
        var font = ((Screen) (Object) this).getFont();

        // ── Compact bar: only as wide as 5 slots + padding (centered under inventory)
        int ax0 = left + IronholdInventoryLayout.ACCESSORY_SLOT_X0;
        int ay  = top + IronholdInventoryLayout.ACCESSORY_SLOT_Y;
        int pad = IronholdInventoryLayout.ACCESSORY_PANEL_PAD_X;
        int rowW = IronholdInventoryLayout.ACCESSORY_SLOT_ROW_WIDTH;
        int barW = rowW + 2 * pad;
        int barX = ax0 - pad;
        int barTop = ay - 14;
        int barH = 18 + 16;

        drawFramedPanel(gfx, barX, barTop, barW, barH, OUTER_EDGE, STONE_FACE, STONE_HIGHLIGHT, STONE_SHADOW);
        gfx.outline(barX + 1, barTop + 1, barW - 2, barH - 2, PANEL_INNER_LINE);

        ironhold$drawCenteredStringNoShadow(gfx, font, Component.translatable("gui.ironhold.accessories"),
            barX + barW / 2, barTop + 3, VANILLA_TITLE);

        for (int i = 0; i < 5; i++) {
            drawAccessorySlot(gfx, ax0 + i * 18 - 1, ay - 1);
        }

        // ── Left column: vanity (wider island, slots centered; matches armor column height) ──
        int lw = 32;
        int lh = 86;
        int lx = left - lw;
        int ly = top + 2;

        drawFramedPanel(gfx, lx, ly, lw, lh, OUTER_EDGE, STONE_FACE, STONE_HIGHLIGHT, STONE_SHADOW);
        gfx.outline(lx + 1, ly + 1, lw - 2, lh - 2, PANEL_INNER_LINE);

        ironhold$drawCenteredStringNoShadow(gfx, font, Component.translatable("gui.ironhold.vanity"),
            lx + lw / 2, ly - 9, VANILLA_TITLE);

        for (int i = 0; i < 4; i++) {
            drawVanitySlot(gfx, left + IronholdInventoryLayout.VANITY_SLOT_X - 1, top + IronholdInventoryLayout.VANITY_SLOT_Y0 + i * 18 - 1);
        }
    }

    /** {@link GuiGraphicsExtractor#centeredText} always enables shadow; this matches flat survival labels. */
    private static void ironhold$drawCenteredStringNoShadow(
        GuiGraphicsExtractor gfx, Font font, Component text, int centerX, int y, int color
    ) {
        FormattedCharSequence seq = text.getVisualOrderText();
        gfx.text(font, seq, centerX - font.width(seq) / 2, y, color, false);
    }

    /** Vanilla-style raised panel (inventory stone look). */
    private static void drawFramedPanel(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                        int outer, int face, int hi, int lo) {
        gfx.fill(x, y, x + w, y + h, outer);
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, hi);
        gfx.fill(x + 1, y + 1, x + 2, y + h - 1, hi);
        gfx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, lo);
        gfx.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, lo);
    }

    private static void drawAccessorySlot(GuiGraphicsExtractor gfx, int x, int y) {
        drawVanillaStyleSlot(gfx, x, y);
    }

    private static void drawVanitySlot(GuiGraphicsExtractor gfx, int x, int y) {
        drawVanillaStyleSlot(gfx, x, y);
    }

    private static void drawVanillaStyleSlot(GuiGraphicsExtractor gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 18, 0xFF373737);
        gfx.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gfx.outline(x, y, 18, 18, SLOT_OUTLINE);
    }
}
