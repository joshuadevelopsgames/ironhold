package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.ClassChoicePayload;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Full-screen class selection GUI. Shows all 5 choosable classes with their
 * fantasy description, stat trade-offs, and starting ability.
 * Card width is computed dynamically so all 5 cards always fit the screen.
 */
public class ClassSelectionScreen extends Screen {

    private static final int CARD_GAP = 6;
    private static final int MARGIN = 8;
    private static final int CARD_HEIGHT = 175;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 40;

    private int selected = -1;

    private final @Nullable Screen returnTo;

    private static final ClassInfo[] CLASSES = {
            new ClassInfo(PlayerClass.KNIGHT,
                    "Steel and shield.",
                    new String[]{"+4 max health", "+15% melee dmg", "+20% KB resist", "-10% movement"},
                    "Iron Will: 10% to negate damage",
                    0xFFCCCCCC,
                    new ItemStack(Items.SHIELD)),
            new ClassInfo(PlayerClass.RANGER,
                    "Fast, far, and first.",
                    new String[]{"+15% movement", "+20% proj dmg", "+30% bow draw", "-1 heart"},
                    "Tracker: See footprints nearby",
                    0xFF55AA55,
                    new ItemStack(Items.BOW)),
            new ClassInfo(PlayerClass.WIZARD,
                    "Fragile. Devastating.",
                    new String[]{"+40% spell dmg", "+25% XP gain", "-20% cooldowns", "-2 hearts"},
                    "Arcane Sight: See class & level",
                    0xFF7755FF,
                    new ItemStack(Items.ENCHANTED_BOOK)),
            new ClassInfo(PlayerClass.CLERIC,
                    "The reason you win.",
                    new String[]{"+3 hearts", "+30% healing", "-10% damage", "-30% debuff dur."},
                    "Mend: Heal ally for 4 hearts",
                    0xFFFFDD55,
                    new ItemStack(Items.GOLDEN_APPLE)),
            new ClassInfo(PlayerClass.ROGUE,
                    "Patient. Precise. Gone.",
                    new String[]{"+25% movement", "+50% backstab", "+20% atk speed", "-1.5 hearts"},
                    "Shadow Step: Teleport 8 blocks",
                    0xFFAA3333,
                    new ItemStack(Items.ENDER_PEARL)),
    };

    public ClassSelectionScreen() {
        this(null);
    }

    public ClassSelectionScreen(@Nullable Screen returnTo) {
        super(Component.literal("Choose Your Path"));
        this.returnTo = returnTo;
    }

    /** Card width computed fresh each frame from current screen width. */
    private int cardWidth() {
        int available = width - MARGIN * 2 - CARD_GAP * (CLASSES.length - 1);
        return Math.max(80, available / CLASSES.length);
    }

    private int startX() {
        int totalWidth = CLASSES.length * cardWidth() + (CLASSES.length - 1) * CARD_GAP;
        return (width - totalWidth) / 2;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            if (selected >= 0 && selected < CLASSES.length) {
                ClientPayloads.sendToServer(new ClassChoicePayload(CLASSES[selected].pc.ordinal()));
                onClose();
            }
        }).bounds(width / 2 - 50, height - FOOTER_HEIGHT + 10, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int cw = cardWidth();
        int sx = startX();

        // Title
        gfx.centeredText(font, Component.literal("Choose Your Path")
                        .withStyle(Style.EMPTY.withBold(true)),
                width / 2, 12, 0xFFFFDD55);
        gfx.centeredText(font, "Select a class to begin your journey.",
                width / 2, 26, 0xFFAAAAAA);

        int startY = HEADER_HEIGHT;

        for (int i = 0; i < CLASSES.length; i++) {
            int cx = sx + i * (cw + CARD_GAP);
            boolean hovered = mouseX >= cx && mouseX < cx + cw
                    && mouseY >= startY && mouseY < startY + CARD_HEIGHT;
            drawClassCard(gfx, CLASSES[i], cx, startY, cw, hovered, i == selected);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            int cw = cardWidth();
            int sx = startX();
            for (int i = 0; i < CLASSES.length; i++) {
                int cx = sx + i * (cw + CARD_GAP);
                if (mx >= cx && mx < cx + cw
                        && my >= HEADER_HEIGHT && my < HEADER_HEIGHT + CARD_HEIGHT) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(returnTo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawClassCard(GuiGraphicsExtractor gfx, ClassInfo info, int x, int y,
                               int cw, boolean hovered, boolean selected) {
        int bgColor = selected ? 0xCC333333 : (hovered ? 0xBB222222 : 0xAA111111);
        gfx.fill(x, y, x + cw, y + CARD_HEIGHT, bgColor);

        int borderColor = selected ? info.color : (hovered ? 0xFFAAAAAA : 0xFF555555);
        gfx.outline(x, y, cw, CARD_HEIGHT, borderColor);
        if (selected) {
            gfx.outline(x - 1, y - 1, cw + 2, CARD_HEIGHT + 2, info.color);
        }

        // Scissor-clip all content to the card boundary so nothing bleeds over
        gfx.enableScissor(x + 1, y + 1, x + cw - 1, y + CARD_HEIGHT - 1);

        int tx = x + 5;
        int ty = y + 6;

        // Item icon — centered at the top
        gfx.item(info.icon, x + cw / 2 - 8, ty);
        ty += 22;

        // Class name
        gfx.text(font, Component.literal(info.pc.id())
                        .withStyle(Style.EMPTY.withBold(true)),
                tx, ty, info.color, true);
        ty += 12;

        // Tagline
        gfx.text(font, info.tagline, tx, ty, 0xFFAAAAAA, false);
        ty += 14;

        // Separator
        gfx.fill(tx, ty, x + cw - 5, ty + 1, 0xFF444444);
        ty += 5;

        // Stats
        for (String stat : info.stats) {
            int statColor = stat.startsWith("+") ? 0xFF55FF55 : 0xFFFF5555;
            gfx.text(font, stat, tx, ty, statColor, false);
            ty += 10;
        }
        ty += 4;

        // Separator
        gfx.fill(tx, ty, x + cw - 5, ty + 1, 0xFF444444);
        ty += 5;

        // Starting ability (word-wrapped to card width)
        gfx.text(font, "Lv.1:", tx, ty, 0xFF888888, false);
        ty += 10;
        for (var line : font.split(Component.literal(info.ability), cw - 12)) {
            gfx.text(font, line, tx, ty, 0xFFDDDDDD, false);
            ty += 10;
        }

        gfx.disableScissor();
    }

    private record ClassInfo(PlayerClass pc, String tagline, String[] stats, String ability, int color, ItemStack icon) {}
}
