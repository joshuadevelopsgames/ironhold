package kingdom.smp.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Click target with no painted chrome — used when the surrounding texture supplies
 * the visual (label, frame, etc.) and the button only needs to provide a hit zone.
 *
 * <p>{@link Button#extractContents} is the 1.26 hook that paints the default sprite +
 * label; overriding it as a no-op leaves only the click + focus behavior intact.
 */
public class InvisibleButton extends Button {

    public InvisibleButton(int x, int y, int w, int h, OnPress onPress) {
        super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float alpha) {
        // No-op — host screen paints the panel art.
    }
}
