package kingdom.smp.client.hud;

import kingdom.smp.ModAttachments;
import kingdom.smp.game.EncumbranceHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HUD: carry weight (uses synced attachment + same formula as server).
 */
public final class CarryWeightHud {
    private CarryWeightHud() {}

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        var rpg = mc.player.getData(ModAttachments.PLAYER_RPG.get());
        int weight = EncumbranceHandler.weightForAnyPlayer(mc.player);
        int max = rpg.playerClass().maxCarryWeight();

        int color;
        if (max <= 0) {
            color = 0xFFAAAAAA;
        } else {
            float ratio = (float) weight / (float) max;
            if (ratio < 0.7f) {
                color = 0xFF55FF55;
            } else if (ratio < 0.9f) {
                color = 0xFFFFFF55;
            } else if (ratio <= 1.0f) {
                color = 0xFFFFAA00;
            } else {
                color = 0xFFFF5555;
            }
        }

        Font font = mc.font;
        // Right-align so we don’t stack on chat / system text on the left (same column as ClassInfoHud).
        int right = gfx.guiWidth() - 8;
        String text = weight > max ? "Weight: " + weight + "/" + max + " (!)" : "Weight: " + weight + "/" + max;
        int x = right - font.width(text);
        int y = KingdomHudAnchors.weightTextY(gfx);

        gfx.text(font, text, x + 1, y + 1, 0xFF000000, false);
        gfx.text(font, text, x, y, color, false);
    }
}
