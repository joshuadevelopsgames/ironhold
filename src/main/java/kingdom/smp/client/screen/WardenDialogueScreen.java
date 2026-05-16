package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.NpcMutePayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.net.WardenChatPayload;
import kingdom.smp.net.WardenPttTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Interactive RPG dialogue screen for Warden Halric (and future voiced AI
 * NPCs). Bottom-anchored panel with portrait, typewriter-revealed NPC line,
 * a text input box, a Send button, and a Talk push-to-talk button. The
 * player can keep replying — server pushes new lines via
 * {@link UpdateWardenScreenPayload}, which the screen renders by restarting
 * the typewriter.
 */
public class WardenDialogueScreen extends Screen {

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int SIDE_MARGIN  = 20;
    private static final int BOTTOM_GAP   = 14;
    private static final int PADDING      = 10;
    private static final int PORTRAIT_W   = 58;
    private static final int HEADER_H     = 28;
    private static final int BTN_H        = 18;
    private static final int INPUT_H      = 18;
    private static final int LINE_SPACING = 2;
    private static final int CHARS_PER_TICK = 3;
    private static final int ACCENT_COLOR = 0xFFBB9944; // weathered bronze — matches "warden"

    // ── Data ─────────────────────────────────────────────────────────────────
    private final int entityId;
    private final String npcName;
    private final String npcTag;
    private final String subtitle;

    private String dialogue;
    private @Nullable String lastHeardEcho;
    /** True after Send/Talk submission, until server replies. */
    private boolean awaitingReply;
    /** True while the mic is recording (between tap-on and tap-off). */
    private boolean recording;
    /** Persisted per-player mute setting for this NPC tag. */
    private boolean muted;

    // ── Cached layout ────────────────────────────────────────────────────────
    private int boxX, boxY, boxW, boxH;
    private int textX, textY, textW;

    // ── Typewriter state ─────────────────────────────────────────────────────
    private int charsShown = 0;
    private int tick = 0;

    // ── Widgets ──────────────────────────────────────────────────────────────
    private EditBox inputBox;
    private Button sendButton;
    private Button talkButton;
    private Button muteButton;
    private Button closeButton;

    public WardenDialogueScreen(int entityId, String npcName, String npcTag,
                                String subtitle, String openingLine, boolean muted) {
        super(Component.empty());
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcTag = npcTag;
        this.subtitle = subtitle == null || subtitle.isBlank()
            ? "Warden  •  Wayfarer's Hollow"
            : subtitle;
        this.dialogue = openingLine == null ? "" : openingLine;
        this.muted = muted;
    }

    public int entityId() { return entityId; }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        boxW = width - SIDE_MARGIN * 2;

        textX = SIDE_MARGIN + PADDING + PORTRAIT_W + PADDING;
        textW = boxW - PORTRAIT_W - PADDING * 3;

        // Box height: header + 4 lines of dialogue + input row + button row + padding
        int textH = font.lineHeight * 4 + LINE_SPACING * 3;
        int contentH = Math.max(PORTRAIT_W, HEADER_H + textH + PADDING);
        boxH = contentH + PADDING * 2 + INPUT_H + BTN_H + 14;

        boxX = SIDE_MARGIN;
        boxY = height - boxH - BOTTOM_GAP;

        textY = boxY + PADDING + HEADER_H;

        // ── Input row (text field + Send button) ─────────────────────────────
        int inputY = boxY + boxH - BTN_H - INPUT_H - 12;
        int sendW = 60;
        int inputW = boxW - PADDING * 2 - sendW - 6;

        inputBox = new EditBox(font, boxX + PADDING, inputY, inputW, INPUT_H,
            Component.literal("Type a reply…"));
        inputBox.setMaxLength(500);
        inputBox.setBordered(true);
        inputBox.setHint(Component.literal("Type a reply, or press Talk to speak…")
            .withStyle(Style.EMPTY.withColor(0xFF888888)));
        addRenderableWidget(inputBox);
        setInitialFocus(inputBox);

        sendButton = Button.builder(Component.literal("Send ▶"), btn -> sendTyped())
            .bounds(boxX + boxW - PADDING - sendW, inputY, sendW, INPUT_H)
            .build();
        addRenderableWidget(sendButton);

        // ── Bottom row (Talk + Mute + Close) ─────────────────────────────────
        int btnY = boxY + boxH - BTN_H - 6;
        int talkW  = 110;
        int muteW  = 80;
        int closeW = 70;

        talkButton = Button.builder(Component.literal("🎤 Hold to Talk"),
                btn -> toggleTalk())
            .bounds(boxX + PADDING, btnY, talkW, BTN_H)
            .build();
        addRenderableWidget(talkButton);

        muteButton = Button.builder(Component.literal(muted ? "🔈 Unmute" : "🔇 Mute"),
                btn -> toggleMute())
            .bounds(boxX + boxW - PADDING - closeW - 6 - muteW, btnY, muteW, BTN_H)
            .build();
        addRenderableWidget(muteButton);

        closeButton = Button.builder(Component.literal("Close ✕"), btn -> onClose())
            .bounds(boxX + boxW - PADDING - closeW, btnY, closeW, BTN_H)
            .build();
        addRenderableWidget(closeButton);

        refreshButtons();
    }

    private void toggleMute() {
        muted = !muted;
        refreshButtons();
        ClientPayloads.sendToServer(new NpcMutePayload(npcTag, muted));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        tick++;
        if (charsShown < dialogue.length()) {
            charsShown = Math.min(charsShown + CHARS_PER_TICK, dialogue.length());
        }
    }

    // ── Server -> client updates ─────────────────────────────────────────────

    /** Called from {@link kingdom.smp.client.VillagerDialogueCache#updateWardenScreen}. */
    public void handleServerUpdate(String status, String text) {
        switch (status) {
            case UpdateWardenScreenPayload.STATUS_REPLY -> {
                dialogue = text == null ? "" : text;
                charsShown = 0;
                awaitingReply = false;
                lastHeardEcho = null;
                refreshButtons();
            }
            case UpdateWardenScreenPayload.STATUS_HEARD -> lastHeardEcho = text;
            case UpdateWardenScreenPayload.STATUS_THINKING -> {
                awaitingReply = true;
                refreshButtons();
            }
            case UpdateWardenScreenPayload.STATUS_CLOSE -> onClose();
            default -> { /* ignore unknown */ }
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void sendTyped() {
        if (awaitingReply) return;
        String value = inputBox.getValue().trim();
        if (value.isEmpty()) return;
        inputBox.setValue("");
        lastHeardEcho = value;
        awaitingReply = true;
        refreshButtons();
        ClientPayloads.sendToServer(new WardenChatPayload(entityId, value));
    }

    private void toggleTalk() {
        recording = !recording;
        refreshButtons();
        // If we're flipping off, the server will flush the mic buffer to Whisper
        // and (eventually) come back with a STATUS_HEARD + STATUS_REPLY.
        if (!recording) awaitingReply = true;
        ClientPayloads.sendToServer(new WardenPttTogglePayload(entityId));
    }

    private void refreshButtons() {
        boolean canSend = !awaitingReply && inputBox != null && !inputBox.getValue().isBlank();
        sendButton.active = canSend;
        if (recording) {
            talkButton.setMessage(Component.literal("■ Stop & Send")
                .withStyle(Style.EMPTY.withColor(0xFFFF4444)));
        } else {
            talkButton.setMessage(Component.literal("🎤 Hold to Talk"));
        }
        talkButton.active = !awaitingReply;
        if (muteButton != null) {
            muteButton.setMessage(Component.literal(muted ? "🔈 Unmute" : "🔇 Mute"));
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // ── Outer box ────────────────────────────────────────────────────────
        gfx.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE5080810);
        gfx.outline(boxX, boxY, boxW, boxH, 0x44FFFFFF);
        gfx.outline(boxX - 1, boxY - 1, boxW + 2, boxH + 2, ACCENT_COLOR);

        // ── Portrait ─────────────────────────────────────────────────────────
        int px = boxX + PADDING;
        int py = boxY + PADDING;
        gfx.fill(px, py, px + PORTRAIT_W, py + PORTRAIT_W, 0xBB000000);
        gfx.outline(px, py, PORTRAIT_W, PORTRAIT_W, ACCENT_COLOR);

        var level = Minecraft.getInstance().level;
        var entity = level != null ? level.getEntity(entityId) : null;
        if (entity instanceof LivingEntity living) {
            // Suppress the nameplate just for the portrait pass — the world's
            // entity render has already happened for this frame, so flipping
            // the visibility flag here only affects the inventory-style
            // extraction below. We restore it before this method returns so
            // the next world frame still shows the floating nameplate above
            // the NPC's head.
            boolean wasNameVisible = living.isCustomNameVisible();
            living.setCustomNameVisible(false);
            try {
                // Adult NPCs render at scale 50; child entities (Pippa) are
                // ~70% the size of an adult + use the bobblehead baby model,
                // so a scale-50 render leaves only the top of their head in
                // the visible scissor area. Bump the scale + tighten the
                // render rect so the child's head ends up at roughly the
                // same screen position as an adult's.
                int scale = living.isBaby() ? 85 : 50;
                int rectBottom = living.isBaby()
                    ? py + PORTRAIT_W + (PORTRAIT_W / 2) + 16
                    : py + PORTRAIT_W * 2 + 16;
                gfx.enableScissor(px, py, px + PORTRAIT_W, py + PORTRAIT_W - 8);
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    gfx, px, py, px + PORTRAIT_W, rectBottom,
                    scale, 0.0F, (float) mouseX, (float) mouseY, living);
                gfx.disableScissor();
            } finally {
                living.setCustomNameVisible(wasNameVisible);
            }
        } else {
            gfx.centeredText(font,
                Component.literal("★").withStyle(Style.EMPTY.withBold(true)),
                px + PORTRAIT_W / 2, py + PORTRAIT_W / 2 - font.lineHeight / 2,
                ACCENT_COLOR);
        }

        // ── Name + subtitle ──────────────────────────────────────────────────
        int tx = textX;
        int ty = boxY + PADDING;
        gfx.text(font, Component.literal(npcName).withStyle(Style.EMPTY.withBold(true)),
            tx, ty, ACCENT_COLOR, true);
        ty += font.lineHeight + 3;
        gfx.text(font, subtitle, tx, ty, 0xFF888888, false);
        ty += font.lineHeight + 4;
        gfx.fill(tx, ty, boxX + boxW - PADDING, ty + 1, 0xFF332211);
        ty += 5;

        // ── Dialogue text ────────────────────────────────────────────────────
        if (awaitingReply && (dialogue.isEmpty() || charsShown >= dialogue.length())) {
            // Show a "thinking" indicator while we wait for the server.
            String dots = switch ((tick / 8) % 4) {
                case 0 -> "";
                case 1 -> ".";
                case 2 -> "..";
                default -> "...";
            };
            gfx.text(font, Component.literal("§7…thinking" + dots), tx, ty, 0xFFAAAAAA, false);
            ty += font.lineHeight + LINE_SPACING;
        } else {
            String visible = dialogue.substring(0, Math.min(charsShown, dialogue.length()));
            List<FormattedCharSequence> allLines = font.split(Component.literal(visible), textW);

            // Hard cap on how many lines we draw. Lower bound of the text
            // area = where the "You said:" echo row begins (computed below as
            // echoTop). Anything below that would visually crash into the
            // input row. Previous version subtracted an extra "safety" line
            // on top of an already-over-reserved budget, which was clipping
            // text at 2 lines when 4 actually fit cleanly.
            int echoTop = boxY + boxH - BTN_H - INPUT_H - 12 - font.lineHeight - 4;
            int availableHeight = echoTop - ty;
            int maxLines = Math.max(1,
                availableHeight / (font.lineHeight + LINE_SPACING));

            int linesToDraw = Math.min(allLines.size(), maxLines);
            for (int i = 0; i < linesToDraw; i++) {
                gfx.text(font, allLines.get(i), tx, ty, 0xFFDDDDDD, false);
                ty += font.lineHeight + LINE_SPACING;
            }

            // If we clipped, draw a dim "…" right after the last visible
            // line so the player can tell there's more they didn't see.
            boolean clipped = allLines.size() > maxLines;
            if (clipped) {
                FormattedCharSequence lastLine = allLines.get(linesToDraw - 1);
                int lastW = font.width(lastLine);
                int dotsX = (lastW < textW - 12)
                    ? tx + lastW + 2
                    : tx + textW - font.width("…") - 1;
                int dotsY = ty - font.lineHeight - LINE_SPACING;
                gfx.text(font, "…", dotsX, dotsY, 0xFFAAAAAA, false);
            }

            // Typewriter cursor — only when the visible content is still
            // unfolding AND we haven't already clipped (no point blinking at
            // text the player can't see).
            boolean typing = charsShown < dialogue.length();
            if (typing && !clipped && linesToDraw > 0 && (tick / 8) % 2 == 0) {
                FormattedCharSequence lastLine = allLines.get(linesToDraw - 1);
                int lastW = font.width(lastLine);
                int cursorX = lastW < textW ? tx + lastW : tx;
                int cursorY = ty - font.lineHeight - LINE_SPACING;
                gfx.text(font, "█", cursorX, cursorY, 0xAAFFFFFF, false);
            }
        }

        // ── Last-heard echo (above input row, dimmed) ─────────────────────────
        if (lastHeardEcho != null && !lastHeardEcho.isBlank()) {
            int echoY = boxY + boxH - BTN_H - INPUT_H - 12 - font.lineHeight - 4;
            String trimmed = lastHeardEcho.length() > 80
                ? lastHeardEcho.substring(0, 80) + "…"
                : lastHeardEcho;
            gfx.text(font, "§8You: §7" + trimmed,
                boxX + PADDING, echoY, 0xFF888888, false);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (inputBox.isFocused()) {
                sendTyped();
                return true;
            }
        }
        // Update Send button enabled state on every keystroke.
        boolean handled = super.keyPressed(event);
        refreshButtons();
        return handled;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean handled = super.charTyped(event);
        refreshButtons();
        return handled;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
