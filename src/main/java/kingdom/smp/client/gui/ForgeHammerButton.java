package kingdom.smp.client.gui;

import kingdom.smp.blacksmithing.ForgeEligibility;
import kingdom.smp.client.ForgeButtonDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The anvil forge-hammer click target. Transparent in normal play (the host
 * screen / texture supplies the visual), but:
 * <ul>
 *   <li>draws a labelled outline while {@link ForgeButtonDebug#isEditMode()} is
 *       on, so it can be dragged into place; and</li>
 *   <li>draws a short "use the hammer" prompt when the anvil holds a forge-
 *       eligible repair (whose normal output is suppressed by the gate mixin),
 *       explaining why the output shows an X.</li>
 * </ul>
 */
public class ForgeHammerButton extends InvisibleButton {

    public ForgeHammerButton(int x, int y, int w, int h, OnPress onPress) {
        super(x, y, w, h, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float alpha) {
        if (ForgeButtonDebug.isEditMode()) {
            drawEditOutline(graphics);
            return;
        }
        if (isForgeRepairOpen()) {
            drawForgePrompt(graphics);
        }
    }

    private void drawEditOutline(GuiGraphicsExtractor graphics) {
        int x0 = getX(), y0 = getY();
        int x1 = x0 + getWidth(), y1 = y0 + getHeight();
        int fill    = ForgeButtonDebug.isDragging() ? 0x66FF55FF : 0x4455CCFF;
        int outline = ForgeButtonDebug.isDragging() ? 0xFFFF55FF : 0xFF55CCFF;
        graphics.fill(x0, y0, x1, y1, fill);
        graphics.fill(x0, y0, x1, y0 + 1, outline);
        graphics.fill(x0, y1 - 1, x1, y1, outline);
        graphics.fill(x0, y0, x0 + 1, y1, outline);
        graphics.fill(x1 - 1, y0, x1, y1, outline);
    }

    private void drawForgePrompt(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        // A soft glow on the hammer zone to draw the eye, plus a label below it.
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);
        int a = 0x33 + (int) (0x44 * pulse);
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                (a << 24) | 0xFFD060);

        String hint = "Click the hammer";
        int tx = getX() + getWidth() / 2 - font.width(hint) / 2;
        int ty = getY() + getHeight() + 2;
        graphics.text(font, hint, tx, ty, 0xFFFFE070, true);
    }

    private static boolean isForgeRepairOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.containerMenu instanceof AnvilMenu am)) return false;
        ItemStack gear = am.getSlot(0).getItem();
        ItemStack material = am.getSlot(1).getItem();
        return ForgeEligibility.isForgeRepair(gear, material);
    }
}
