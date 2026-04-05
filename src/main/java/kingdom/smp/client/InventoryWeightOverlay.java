package kingdom.smp.client;

import kingdom.smp.ModAttachments;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Carry weight beside container GUIs (screen space, after render). */
public final class InventoryWeightOverlay {
    private InventoryWeightOverlay() {}

    public static void renderAfterScreen(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        // Top-right beside the panel overlaps vanilla status-effect icons; anchor to bottom-right of GUI.
        int x = container.getGuiLeft() + container.getXSize() + 6;
        int y = container.getGuiTop() + container.getYSize() - 28;
        drawWeightText(player, event.getGuiGraphics(), mc.font, x, y);
    }

    private static void drawWeightText(Player player, GuiGraphicsExtractor gfx, Font font, int x, int y) {
        int weight = EncumbranceHandler.weightForAnyPlayer(player);
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        int max = rpg.playerClass().maxCarryWeight();
        boolean over = weight > max;
        int color = over ? 0xFFFF5555 : 0xFFE0E0E0;
        String line1 = "Weight: " + weight + "/" + max;
        gfx.text(font, line1, x, y, color, true);
        if (over) {
            gfx.text(font, "Encumbered!", x, y + 12, 0xFFFF5555, true);
        }
    }
}
