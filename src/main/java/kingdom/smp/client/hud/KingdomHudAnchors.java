package kingdom.smp.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared Y positions so class / weight HUD don’t drift apart. */
public final class KingdomHudAnchors {
    /** Distance from bottom of screen to weight text baseline (right-aligned block). */
    public static final int FROM_BOTTOM_WEIGHT_LINE = 18;

    public static int weightTextY(GuiGraphicsExtractor gfx) {
        return gfx.guiHeight() - FROM_BOTTOM_WEIGHT_LINE;
    }

    private KingdomHudAnchors() {}
}
