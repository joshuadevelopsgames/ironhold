package kingdom.smp.client.hud;

import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** HUD: class + level + XP bar (from synced attachment). */
public final class ClassInfoHud {
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 3;

    private ClassInfoHud() {}

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        var rpg = mc.player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass pc = rpg.playerClass();
        int level = rpg.classLevel();
        int need = RpgProgression.xpToReachNextLevel(level);
        float progress = need <= 0 ? 0f : (float) rpg.xpIntoLevel() / (float) need;

        Font font = mc.font;
        // Stack above weight line (CarryWeightHud), right-aligned — avoids chat column on the left.
        int right = gfx.guiWidth() - 8;
        int weightLineY = KingdomHudAnchors.weightTextY(gfx);
        int barY = weightLineY - 5 - BAR_HEIGHT;
        int y = barY - 2 - font.lineHeight;

        int classColor = classColor(pc);
        String classText = pc.id() + " Lv." + level;
        int x = right - font.width(classText);
        gfx.text(font, classText, x + 1, y + 1, 0xFF000000, false);
        gfx.text(font, classText, x, y, classColor, false);

        int barLeft = right - BAR_WIDTH;
        gfx.fill(barLeft, barY, barLeft + BAR_WIDTH, barY + BAR_HEIGHT, 0x88000000);
        int fillWidth = Mth.clamp((int) (progress * BAR_WIDTH), 0, BAR_WIDTH);
        if (fillWidth > 0) {
            gfx.fill(barLeft, barY, barLeft + fillWidth, barY + BAR_HEIGHT, classColor | 0xFF000000);
        }
        gfx.outline(barLeft - 1, barY - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 0x66FFFFFF);
    }

    private static int classColor(PlayerClass pc) {
        return switch (pc) {
            case KNIGHT -> 0xFFCCCCCC;
            case RANGER -> 0xFF55AA55;
            case WIZARD -> 0xFF7755FF;
            case CLERIC -> 0xFFFFDD55;
            case PEASANT -> 0xFF888888;
            default -> 0xFFFFFFFF;
        };
    }
}
