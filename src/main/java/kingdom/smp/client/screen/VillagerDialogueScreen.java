package kingdom.smp.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Bottom-anchored RPG dialogue screen shown to the interacting player when they
 * talk to a Kingdom Villager. Features typewriter text reveal, profession portrait,
 * and a mood bar.
 */
public class VillagerDialogueScreen extends Screen {

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int SIDE_MARGIN  = 20;
    private static final int BOTTOM_GAP  = 14;
    private static final int PADDING     = 10;
    private static final int PORTRAIT_W  = 58;
    private static final int HEADER_H    = 34; // name + profession + separator
    private static final int BTN_H       = 18;
    private static final int BTN_W       = 76;
    private static final int LINE_SPACING = 2;

    // ── Data ─────────────────────────────────────────────────────────────────
    private final String villagerName;
    private final String profession;
    private final String dialogue;
    private final float mood;
    private final int entityId;

    /** World entity id of the villager this dialogue is with (see NpcNameTagHandler). */
    public int entityId() { return entityId; }

    // ── Cached layout (computed in init) ─────────────────────────────────────
    private int boxX, boxY, boxW, boxH;
    private int textX, textY, textW;
    private List<FormattedCharSequence> allLines;

    // ── Typewriter state ─────────────────────────────────────────────────────
    private int charsShown = 0;
    private int tick = 0;
    private static final int CHARS_PER_TICK = 3;

    private Button actionButton;

    public VillagerDialogueScreen(String villagerName, String profession, String dialogue, float mood, int entityId) {
        super(Component.empty());
        this.villagerName = villagerName;
        this.profession   = profession;
        this.dialogue     = dialogue;
        this.mood         = mood;
        this.entityId     = entityId;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        boxW = width - SIDE_MARGIN * 2;

        // Text area starts to the right of portrait + padding
        textX = SIDE_MARGIN + PADDING + PORTRAIT_W + PADDING;
        textW = boxW - PORTRAIT_W - PADDING * 3;

        // Pre-split all lines to measure total text height
        allLines = font.split(Component.literal(dialogue), textW);
        int textH = allLines.size() * (font.lineHeight + LINE_SPACING);

        // Portrait height = max(portrait min, header + text)
        int contentH = Math.max(PORTRAIT_W, HEADER_H + textH + PADDING);
        boxH = contentH + PADDING * 2 + BTN_H + 6;

        boxX = SIDE_MARGIN;
        boxY = height - boxH - BOTTOM_GAP;

        textY = boxY + PADDING + HEADER_H;

        // Action button — bottom right
        int btnX = boxX + boxW - BTN_W - PADDING;
        int btnY = boxY + boxH - BTN_H - 6;
        actionButton = Button.builder(Component.literal("Skip \u25BA"), btn -> handleAction())
            .bounds(btnX, btnY, BTN_W, BTN_H).build();
        addRenderableWidget(actionButton);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        tick++;
        if (charsShown < dialogue.length()) {
            charsShown = Math.min(charsShown + CHARS_PER_TICK, dialogue.length());
            if (charsShown >= dialogue.length()) {
                actionButton.setMessage(Component.literal("Close \u2715"));
            }
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int profColor = professionColor(profession);

        // ── Outer box ────────────────────────────────────────────────────────
        gfx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE5080810);
        // Double border: dim inner, bright accent outer
        gfx.outline(boxX, boxY, boxW, boxH, 0x44FFFFFF);
        gfx.outline(boxX - 1, boxY - 1, boxW + 2, boxH + 2, profColor);

        // ── Portrait box ─────────────────────────────────────────────────────
        int px = boxX + PADDING;
        int py = boxY + PADDING;
        gfx.fill(px, py, px + PORTRAIT_W, py + PORTRAIT_W, 0xBB000000);
        gfx.outline(px, py, PORTRAIT_W, PORTRAIT_W, profColor);

        // Entity face or fallback profession icon
        var level = Minecraft.getInstance().level;
        var entity = level != null ? level.getEntity(entityId) : null;
        if (entity instanceof LivingEntity living) {
            // Scissor to portrait area (minus the mood-bar strip at the bottom),
            // then extend the virtual bottom so the entity center drops below the
            // frame — only the head/shoulders stay in view. +28 matches the
            // lowered head framing of the voiced-NPC dialogue screen.
            gfx.enableScissor(px, py, px + PORTRAIT_W, py + PORTRAIT_W - 8);
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                gfx, px, py, px + PORTRAIT_W, py + PORTRAIT_W * 2 + 28,
                50, 0.0F, (float) mouseX, (float) mouseY, living);
            gfx.disableScissor();
        } else {
            String icon = professionIcon(profession);
            gfx.centeredText(font,
                Component.literal(icon).withStyle(Style.EMPTY.withBold(true)),
                px + PORTRAIT_W / 2, py + PORTRAIT_W / 2 - font.lineHeight / 2,
                profColor);
        }

        // Mood bar (below portrait icon, 6px tall)
        int barY    = py + PORTRAIT_W - 8;
        int barX0   = px + 4;
        int barXMax = px + PORTRAIT_W - 4;
        int barMid  = (barX0 + barXMax) / 2;
        gfx.fill(barX0, barY, barXMax, barY + 5, 0xFF222222);
        int moodColor = moodColor(mood);
        float clamped = Math.max(-1f, Math.min(1f, mood));
        int halfW = barMid - barX0;
        if (clamped >= 0) {
            int fill = (int) (halfW * clamped);
            gfx.fill(barMid, barY, barMid + fill, barY + 5, moodColor);
        } else {
            int fill = (int) (halfW * -clamped);
            gfx.fill(barMid - fill, barY, barMid, barY + 5, moodColor);
        }
        // Center tick
        gfx.fill(barMid - 1, barY - 1, barMid + 1, barY + 6, 0xFFAAAAAA);

        // ── Name & Profession ─────────────────────────────────────────────────
        int tx = textX;
        int ty = boxY + PADDING;

        gfx.text(font, Component.literal(villagerName).withStyle(Style.EMPTY.withBold(true)),
            tx, ty, profColor, true);
        ty += font.lineHeight + 3;

        String profLabel = capitalize(profession) + (isTalkingProfession(profession) ? "  \u2022  Talker" : "");
        gfx.text(font, profLabel, tx, ty, 0xFF888888, false);
        ty += font.lineHeight + 4;

        // Separator
        gfx.fill(tx, ty, boxX + boxW - PADDING, ty + 1, 0xFF223344);
        ty += 5;

        // ── Dialogue text (typewriter) ────────────────────────────────────────
        String visible = dialogue.substring(0, charsShown);
        List<FormattedCharSequence> visibleLines = font.split(Component.literal(visible), textW);
        for (var line : visibleLines) {
            gfx.text(font, line, tx, ty, 0xFFDDDDDD, false);
            ty += font.lineHeight + LINE_SPACING;
        }

        // Blinking cursor while still typing
        boolean typing = charsShown < dialogue.length();
        if (typing && !visibleLines.isEmpty() && (tick / 8) % 2 == 0) {
            FormattedCharSequence lastLine = visibleLines.get(visibleLines.size() - 1);
            int lastW = font.width(lastLine);
            int cursorX = lastW < textW ? tx + lastW : tx;
            int cursorY = ty - font.lineHeight - LINE_SPACING;
            gfx.text(font, "\u2588", cursorX, cursorY, 0xAAFFFFFF, false);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_E) {
            handleAction();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            // Click outside the button area: skip if typing, ignore if done
            if (charsShown < dialogue.length()) {
                charsShown = dialogue.length();
                actionButton.setMessage(Component.literal("Close \u2715"));
                return true;
            }
        }
        return super.mouseClicked(event, clicked);
    }

    private void handleAction() {
        if (charsShown < dialogue.length()) {
            charsShown = dialogue.length();
            actionButton.setMessage(Component.literal("Close \u2715"));
        } else {
            onClose();
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String professionIcon(String profession) {
        return switch (profession.toLowerCase()) {
            case "wizard"     -> "\u2736"; // ✶
            case "priest"     -> "\u271D"; // ✝
            case "bard"       -> "\u266B"; // ♫
            case "librarian"  -> "\u2756"; // ❖
            case "blacksmith" -> "\u2692"; // ⚒
            case "farmer"     -> "\u2618"; // ☘
            case "guard"      -> "\u2694"; // ⚔
            case "merchant"   -> "\u25C6"; // ◆
            case "alchemist"  -> "\u2697"; // ⚗
            case "warden"     -> "\u2605"; // ★
            default -> profession.substring(0, 1).toUpperCase();
        };
    }

    private static int professionColor(String profession) {
        return switch (profession.toLowerCase()) {
            case "wizard"    -> 0xFF9966FF;
            case "priest"    -> 0xFFFFDD55;
            case "bard"      -> 0xFF55FF88;
            case "librarian" -> 0xFF55CCFF;
            case "blacksmith"-> 0xFFBBBBBB;
            case "farmer"    -> 0xFF88CC55;
            case "guard"     -> 0xFFCC4444;
            case "merchant"  -> 0xFFFFAA22;
            case "alchemist" -> 0xFF44DDCC;
            case "warden"    -> 0xFFBB9944; // weathered bronze
            default          -> 0xFFFFFFFF;
        };
    }

    private static int moodColor(float mood) {
        if (mood >= 0.5f)  return 0xFF44FF44; // bright green — happy
        if (mood >= 0.1f)  return 0xFF88CC44; // yellow-green
        if (mood >= -0.1f) return 0xFFCCCC44; // neutral yellow
        if (mood >= -0.5f) return 0xFFCC6633; // orange — grumpy
        return 0xFFFF4444;                      // red — angry
    }

    private static boolean isTalkingProfession(String profession) {
        return switch (profession.toLowerCase()) {
            case "wizard", "priest", "bard", "librarian" -> true;
            default -> false;
        };
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
