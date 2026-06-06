package kingdom.smp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * On-screen readout for the stone-golem hammer-grip tuner. Shows each field and highlights the one the
 * {@code -}/{@code =} keys currently nudge. Hidden unless {@link StoneGolemHammerTuning#hudVisible}
 * (toggled by tuning, or {@code /golemhammer hud}).
 */
public final class StoneGolemHammerHud {
    private StoneGolemHammerHud() {}

    public static void render(GuiGraphicsExtractor gfx) {
        if (!StoneGolemHammerTuning.hudVisible) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = 4;
        int y = gfx.guiHeight() / 2 - 48;
        gfx.text(font, "[Golem Hammer]  \";\" cycle   \"-\"/\"=\" nudge   /golemhammer print", x, y, 0xFFFFFF55, false);
        y += 11;

        String[] names = {"posX", "posY", "posZ", "rotX", "rotY", "rotZ", "scale"};
        float[] vals = {
            StoneGolemHammerTuning.posX, StoneGolemHammerTuning.posY, StoneGolemHammerTuning.posZ,
            StoneGolemHammerTuning.rotX, StoneGolemHammerTuning.rotY, StoneGolemHammerTuning.rotZ,
            StoneGolemHammerTuning.scale
        };
        for (int i = 0; i < names.length; i++) {
            boolean act = i == StoneGolemHammerTuning.active;
            String line = (act ? "> " : "  ") + names[i] + " = " + String.format("%.2f", vals[i]);
            gfx.text(font, line, x, y, act ? 0xFF55FF55 : 0xFFB0B0B0, false);
            y += 10;
        }
    }
}
