package kingdom.smp.client.screen;

import kingdom.smp.ModAttachments;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.rpg.ClassSkills;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Player profile screen — shows class, level, XP progress, kingdom, carry weight, and skills.
 * Opened via /profile, from the main menu, or keybind.
 */
public class ProfileScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 290;

    private static final String[] KINGDOM_NAMES = {
            "The Iron Hold", "The Verdant Court", "The Ember Throne", "The Abyssal Pact"
    };
    private static final int[] KINGDOM_COLORS = {
            0xFFBBBBBB, 0xFF44BB44, 0xFFDD6622, 0xFF8855CC
    };

    private final @Nullable Screen returnTo;

    public ProfileScreen() {
        this(null);
    }

    public ProfileScreen(@Nullable Screen returnTo) {
        super(Component.literal("Player Profile"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(width / 2 - 40, height / 2 + PANEL_HEIGHT / 2 + 5, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            super.extractRenderState(gfx, mouseX, mouseY, partialTick);
            return;
        }
        var rpg = player.getData(ModAttachments.PLAYER_RPG.get());

        int px = width / 2 - PANEL_WIDTH / 2;
        int py = height / 2 - PANEL_HEIGHT / 2;

        // Panel background
        gfx.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, 0xDD111111);
        gfx.outline(px, py, PANEL_WIDTH, PANEL_HEIGHT, 0xFF555555);

        int tx = px + 12;
        int ty = py + 12;

        // Player name
        String playerName = Minecraft.getInstance().getUser().getName();
        gfx.text(font, Component.literal(playerName)
                        .withStyle(Style.EMPTY.withBold(true)),
                tx, ty, 0xFFFFFFFF, true);
        ty += 16;

        // Kingdom
        int ki = rpg.kingdomIndexClamped();
        String kingdomName = KINGDOM_NAMES[ki];
        int kingdomColor = KINGDOM_COLORS[ki];
        gfx.text(font, "Kingdom:", tx, ty, 0xFF888888, false);
        gfx.text(font, kingdomName, tx + 55, ty, kingdomColor, false);
        ty += 14;

        // Separator
        gfx.fill(tx, ty, px + PANEL_WIDTH - 12, ty + 1, 0xFF444444);
        ty += 8;

        // Class
        PlayerClass pc = rpg.playerClass();
        int classColor = classColor(pc);
        gfx.text(font, "Class:", tx, ty, 0xFF888888, false);
        gfx.text(font, Component.literal(pc.id())
                        .withStyle(Style.EMPTY.withBold(true)),
                tx + 55, ty, classColor, true);
        ty += 14;

        // Level
        int level = rpg.classLevel();
        gfx.text(font, "Level:", tx, ty, 0xFF888888, false);
        gfx.text(font, String.valueOf(level), tx + 55, ty, 0xFFFFFFFF, false);
        ty += 14;

        // XP Progress bar
        gfx.text(font, "XP:", tx, ty, 0xFF888888, false);
        int barX = tx + 55;
        int barW = PANEL_WIDTH - 80;
        int barH = 8;
        // Background
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF222222);
        // Fill
        int xpNeed = RpgProgression.xpToReachNextLevel(level);
        float progress = xpNeed <= 0 ? 0f : (float) rpg.xpIntoLevel() / (float) xpNeed;
        int fillW = Mth.clamp((int) (progress * barW), 0, barW);
        if (fillW > 0) {
            gfx.fill(barX, ty, barX + fillW, ty + barH, classColor | 0xFF000000);
        }
        gfx.outline(barX - 1, ty - 1, barW + 2, barH + 2, 0xFF555555);
        // XP text on bar
        String xpText = rpg.xpIntoLevel() + " / " + xpNeed;
        int xpTextX = barX + barW / 2 - font.width(xpText) / 2;
        gfx.text(font, xpText, xpTextX, ty, 0xFFFFFFFF, true);
        ty += 16;

        // Separator
        gfx.fill(tx, ty, px + PANEL_WIDTH - 12, ty + 1, 0xFF444444);
        ty += 8;

        // Carry Weight
        int weight = EncumbranceHandler.weightForAnyPlayer(player);
        int maxWeight = rpg.playerClass().maxCarryWeight();
        boolean over = weight > maxWeight;
        gfx.text(font, "Carry:", tx, ty, 0xFF888888, false);
        int weightColor = over ? 0xFFFF5555 : 0xFFFFFFFF;
        gfx.text(font, weight + " / " + maxWeight, tx + 55, ty, weightColor, false);
        if (over) {
            gfx.text(font, " (Encumbered!)", tx + 55 + font.width(weight + " / " + maxWeight), ty,
                    0xFFFF5555, false);
        }
        ty += 14;

        // Carry bar
        barX = tx + 55;
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF222222);
        float weightRatio = maxWeight <= 0 ? 0f : Mth.clamp((float) weight / maxWeight, 0f, 1f);
        int wFill = (int) (weightRatio * barW);
        if (wFill > 0) {
            int wColor = over ? 0xFFFF5555 : (weightRatio > 0.9f ? 0xFFFFAA00 :
                    (weightRatio > 0.7f ? 0xFFFFFF55 : 0xFF55FF55));
            gfx.fill(barX, ty, barX + wFill, ty + barH, wColor);
        }
        gfx.outline(barX - 1, ty - 1, barW + 2, barH + 2, 0xFF555555);
        ty += 16;

        // Separator
        gfx.fill(tx, ty, px + PANEL_WIDTH - 12, ty + 1, 0xFF444444);
        ty += 8;

        // Skills (catalog + status; keybinds / server effects later)
        gfx.text(font, "Skills", tx, ty, 0xFFDDAA66, false);
        ty += 11;
        for (var skill : ClassSkills.profileEntries(pc)) {
            gfx.text(font, skill.title(), tx + 2, ty, 0xFFFFFFFF, false);
            ty += 10;
            gfx.text(font, skill.category() + " · " + skill.status(), tx + 6, ty, 0xFF888888, false);
            ty += 9;
            gfx.text(font, skill.description(), tx + 6, ty, 0xFFAAAAAA, false);
            ty += 11;
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(returnTo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
