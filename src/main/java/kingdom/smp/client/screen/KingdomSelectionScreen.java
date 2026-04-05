package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.KingdomChoicePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Kingdom selection screen — shows the four kingdoms with name, identity, and color.
 * Displayed after class selection (or on first join).
 */
public class KingdomSelectionScreen extends Screen {

    private static final int PAD  = 8;   // inner card padding
    private static final int GAP  = 10;  // gap between cards

    private int selected = -1;

    private final @Nullable Screen returnTo;

    private static final KingdomInfo[] KINGDOMS = {
            new KingdomInfo(0, "The Iron Hold",
                    "Discipline, duty, and steel.",
                    "A fortress-kingdom built on military strength and unbreakable oaths. The anvil of war.",
                    0xFFBBBBBB),
            new KingdomInfo(1, "The Verdant Court",
                    "Growth, wisdom, and patience.",
                    "A woodland realm of scholars and druids. Power through knowledge and nature's bounty.",
                    0xFF44BB44),
            new KingdomInfo(2, "The Ember Throne",
                    "Ambition, fire, and conquest.",
                    "A volcanic kingdom of conquerors and smiths. Forged in flame, feared by all.",
                    0xFFDD6622),
            new KingdomInfo(3, "The Abyssal Pact",
                    "Shadow, cunning, and secrets.",
                    "A hidden kingdom of spies and merchants. They own what others don't even know exists.",
                    0xFF8855CC),
    };

    public KingdomSelectionScreen() {
        this(null);
    }

    public KingdomSelectionScreen(@Nullable Screen returnTo) {
        super(Component.literal("Pledge Your Allegiance"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Pledge"), btn -> {
            if (selected >= 0 && selected < KINGDOMS.length) {
                ClientPayloads.sendToServer(new KingdomChoicePayload(KINGDOMS[selected].index));
                onClose();
            }
        }).bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Title
        gfx.centeredText(font, Component.literal("Pledge Your Allegiance")
                        .withStyle(Style.EMPTY.withBold(true)),
                width / 2, 10, 0xFFFFDD55);
        gfx.centeredText(font, "Choose the kingdom you will serve.",
                width / 2, 22, 0xFFAAAAAA);

        // 2×2 grid of kingdom cards (dynamic sizing)
        for (int i = 0; i < KINGDOMS.length; i++) {
            int[] b = cardBounds(i);
            boolean hovered = mouseX >= b[0] && mouseX < b[0] + b[2]
                    && mouseY >= b[1] && mouseY < b[1] + b[3];
            drawKingdomCard(gfx, KINGDOMS[i], b[0], b[1], b[2], b[3], hovered, i == selected);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            for (int i = 0; i < KINGDOMS.length; i++) {
                int[] b = cardBounds(i);
                if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    /** Returns {x, y, cardW, cardH} for card i, computed from current screen dimensions. */
    private int[] cardBounds(int i) {
        int titleBottom = 34;
        int buttonArea  = 30;
        int gridX = PAD;
        int gridY = titleBottom + GAP;
        int gridW = width  - 2 * PAD;
        int gridH = height - gridY - buttonArea - GAP;
        int cardW = (gridW - GAP) / 2;
        int cardH = (gridH - GAP) / 2;
        int col = i % 2, row = i / 2;
        int cx = gridX + col * (cardW + GAP);
        int cy = gridY + row * (cardH + GAP);
        return new int[]{cx, cy, cardW, cardH};
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(returnTo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawKingdomCard(GuiGraphicsExtractor gfx, KingdomInfo info,
                                 int x, int y, int w, int h,
                                 boolean hovered, boolean selected) {
        int bgColor     = selected ? 0xCC333333 : (hovered ? 0xBB222222 : 0xAA111111);
        int borderColor = selected ? info.color  : (hovered ? 0xFFAAAAAA : 0xFF555555);

        gfx.fill(x, y, x + w, y + h, bgColor);
        gfx.outline(x, y, w, h, borderColor);
        if (selected) {
            gfx.outline(x - 1, y - 1, w + 2, h + 2, info.color);
        }

        int tx = x + PAD;
        int ty = y + PAD;

        // Kingdom name
        gfx.text(font, Component.literal(info.name).withStyle(Style.EMPTY.withBold(true)),
                tx, ty, info.color, true);
        ty += 13;

        // Tagline
        gfx.text(font, info.tagline, tx, ty, 0xFFAAAAAA, false);
        ty += 12;

        // Separator
        gfx.fill(tx, ty, x + w - PAD, ty + 1, 0xFF444444);
        ty += 5;

        // Description — word-wrapped to card interior
        int wrapW = w - 2 * PAD;
        List<FormattedCharSequence> lines = font.split(Component.literal(info.description), wrapW);
        for (FormattedCharSequence line : lines) {
            if (ty + 9 > y + h - PAD) break; // don't overflow the card bottom
            gfx.text(font, line, tx, ty, 0xFFCCCCCC, false);
            ty += 10;
        }
    }

    private record KingdomInfo(int index, String name, String tagline, String description, int color) {}
}
