package kingdom.smp.client.screen;

import kingdom.smp.entity.BabyMimicMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple 5-slot chest screen for the baby mimic inventory.
 * Uses the standard extractContents/extractRenderState pipeline.
 */
public class BabyMimicScreen extends AbstractContainerScreen<BabyMimicMenu> {

    private static final int BG = 0xFFC6C6C6;
    private static final int BORDER_HI = 0xFFFFFFFF;
    private static final int BORDER_LO = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_HI = 0xFFFFFFFF;
    private static final int SLOT_LO = 0xFF373737;

    public BabyMimicScreen(BabyMimicMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, 133);
    }

    /** Draws the GUI background (slot frames, panel). Runs BEFORE item rendering. */
    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;

        // Stone panel background
        gfx.fill(x, y, x + w, y + h, BG);
        gfx.fill(x, y, x + w, y + 1, BORDER_HI);
        gfx.fill(x, y, x + 1, y + h, BORDER_HI);
        gfx.fill(x + w - 1, y, x + w, y + h, BORDER_LO);
        gfx.fill(x, y + h - 1, x + w, y + h, BORDER_LO);

        // Draw slot backgrounds for all slots (18×18 border around 16×16 item area)
        for (var slot : this.menu.slots) {
            drawSlot(gfx, x + slot.x - 1, y + slot.y - 1);
        }
    }

    /** Renders items in slots, highlights, carried item, and tooltips. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // super renders: slot items, highlights, carried item, labels
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        // Tooltips on hover
        extractTooltip(gfx, mouseX, mouseY);
    }

    private void drawSlot(GuiGraphicsExtractor gfx, int sx, int sy) {
        gfx.fill(sx, sy, sx + 18, sy + 1, SLOT_LO);
        gfx.fill(sx, sy, sx + 1, sy + 18, SLOT_LO);
        gfx.fill(sx + 17, sy, sx + 18, sy + 18, SLOT_HI);
        gfx.fill(sx, sy + 17, sx + 18, sy + 18, SLOT_HI);
        gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
    }
}
