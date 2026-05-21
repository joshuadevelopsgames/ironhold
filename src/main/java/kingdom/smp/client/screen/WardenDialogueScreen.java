package kingdom.smp.client.screen;

import kingdom.smp.ModAttachments;
import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.NpcMutePayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.net.WardenChatPayload;
import kingdom.smp.net.WardenPttTogglePayload;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.npc.PlayerNpcBonds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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
    /** How long a fully-revealed page lingers before auto-advancing to the next (ticks). */
    private static final int AUTO_ADVANCE_TICKS = 20 * 5; // 5 seconds
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

    // ── Typewriter + pagination state ────────────────────────────────────────
    private int tick = 0;
    /** The current dialogue split into pages, each a list of pre-wrapped line strings. */
    private List<List<String>> dialoguePages = new ArrayList<>();
    /** Which dialogue page is showing. */
    private int dialoguePageIndex = 0;
    /** Typewriter position within the current page (char count across its lines). */
    private int pageCharsShown = 0;
    /** Tick at which the current page finished revealing, or -1 if still revealing. */
    private int pageRevealedTick = -1;
    /** Auto-advance is disabled once the player manually pages back/forth. */
    private boolean autoAdvance = true;
    // Cache keys — pages are rebuilt only when one of these changes.
    private @Nullable String paginatedFor = null;
    private int paginatedMaxLines = -1;
    private int paginatedTextW = -1;
    // Pager arrow hitboxes (set during render when more than one page exists).
    private int prevArrowX0, prevArrowX1, nextArrowX0, nextArrowX1, arrowY0, arrowY1;
    private boolean pagerVisible = false;

    // ── Widgets ──────────────────────────────────────────────────────────────
    private EditBox inputBox;
    private Button sendButton;
    private Button talkButton;
    private Button muteButton;
    private Button closeButton;
    private Button talkTabBtn;
    private Button bondsTabBtn;

    /** Which page is showing: 0 = Talk (dialogue), 1 = Bonds (rapport, hearts, gift state). */
    private int activeTab = 0;

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

        // ── Tab strip — sits above the box, two chips: Talk / Bonds ─────────
        int tabY = boxY - BTN_H - 2;
        int tabW = 64;
        talkTabBtn = Button.builder(Component.literal("✍ Talk"), btn -> setActiveTab(0))
            .bounds(boxX + PADDING, tabY, tabW, BTN_H)
            .build();
        addRenderableWidget(talkTabBtn);
        bondsTabBtn = Button.builder(Component.literal("♥ Bonds"), btn -> setActiveTab(1))
            .bounds(boxX + PADDING + tabW + 4, tabY, tabW, BTN_H)
            .build();
        addRenderableWidget(bondsTabBtn);

        applyTabVisibility();
        refreshButtons();
    }

    private void setActiveTab(int tab) {
        if (tab == activeTab) return;
        activeTab = tab;
        applyTabVisibility();
        refreshButtons();
    }

    /** Hide the talk-only widgets when on the Bonds tab; restore them on Talk. */
    private void applyTabVisibility() {
        boolean talk = (activeTab == 0);
        if (inputBox  != null) inputBox.visible  = talk;
        if (sendButton != null) sendButton.visible = talk;
        if (talkButton != null) talkButton.visible = talk;
        // Mute + Close + tab buttons remain visible on both pages.
        if (talkTabBtn  != null) talkTabBtn.active  = !talk;   // dim active tab to show selection
        if (bondsTabBtn != null) bondsTabBtn.active = talk;
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
        rebuildPagesIfNeeded();
        if (dialoguePages.isEmpty()) return;

        int pageTotal = totalChars(dialoguePages.get(dialoguePageIndex));
        if (pageCharsShown < pageTotal) {
            pageCharsShown = Math.min(pageCharsShown + CHARS_PER_TICK, pageTotal);
        }
        if (pageCharsShown >= pageTotal && pageRevealedTick < 0) {
            pageRevealedTick = tick;
        }
        // Auto-advance to the next page after the current one has lingered.
        if (autoAdvance && pageCharsShown >= pageTotal
                && dialoguePageIndex < dialoguePages.size() - 1
                && pageRevealedTick >= 0
                && tick - pageRevealedTick >= AUTO_ADVANCE_TICKS) {
            dialoguePageIndex++;
            pageCharsShown = 0;
            pageRevealedTick = -1;
        }
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    /** Lines of dialogue text that fit in the text area above the input row.
     *  Mirrors the geometry render uses for the text-start baseline. */
    private int computeMaxLines() {
        int textStart = boxY + PADDING + 2 * font.lineHeight + 12;
        int echoTop = boxY + boxH - BTN_H - INPUT_H - 12 - font.lineHeight - 4;
        int availableHeight = echoTop - textStart;
        return Math.max(1, availableHeight / (font.lineHeight + LINE_SPACING));
    }

    private void rebuildPagesIfNeeded() {
        int maxLines = computeMaxLines();
        if (dialogue.equals(paginatedFor) && maxLines == paginatedMaxLines && textW == paginatedTextW) {
            return;
        }
        paginatedFor = dialogue;
        paginatedMaxLines = maxLines;
        paginatedTextW = textW;
        dialoguePages = buildPages(dialogue, textW, maxLines);
        dialoguePageIndex = 0;
        pageCharsShown = 0;
        pageRevealedTick = -1;
    }

    private List<List<String>> buildPages(String text, int width, int maxLines) {
        List<List<String>> pages = new ArrayList<>();
        if (text == null || text.isBlank()) return pages;
        List<String> wrapped = new ArrayList<>();
        for (FormattedText ft : font.getSplitter().splitLines(text, width, Style.EMPTY)) {
            wrapped.add(ft.getString());
        }
        for (int i = 0; i < wrapped.size(); i += maxLines) {
            pages.add(new ArrayList<>(wrapped.subList(i, Math.min(i + maxLines, wrapped.size()))));
        }
        return pages;
    }

    private static int totalChars(List<String> page) {
        int n = 0;
        for (String s : page) n += s.length();
        return n;
    }

    private void gotoPage(int index) {
        if (index < 0 || index >= dialoguePages.size() || index == dialoguePageIndex) return;
        autoAdvance = false; // player took control
        dialoguePageIndex = index;
        // Show the page in full immediately on manual navigation.
        pageCharsShown = totalChars(dialoguePages.get(index));
        pageRevealedTick = tick;
    }

    /** Right-aligned "◄ x/y ►" pager on the echo row; records arrow hitboxes. */
    private void drawPager(GuiGraphicsExtractor gfx) {
        int rowY = boxY + boxH - BTN_H - INPUT_H - 12 - font.lineHeight - 4;
        String prev = "◄";
        String next = "►";
        String mid = " " + (dialoguePageIndex + 1) + "/" + dialoguePages.size() + " ";
        int prevW = font.width(prev);
        int midW = font.width(mid);
        int nextW = font.width(next);
        int rightX = boxX + boxW - PADDING;
        int nextX = rightX - nextW;
        int midX = nextX - midW;
        int prevX = midX - prevW;

        boolean canPrev = dialoguePageIndex > 0;
        boolean canNext = dialoguePageIndex < dialoguePages.size() - 1;
        gfx.text(font, prev, prevX, rowY, canPrev ? 0xFFDDDDDD : 0xFF555555, false);
        gfx.text(font, mid, midX, rowY, 0xFFAAAAAA, false);
        gfx.text(font, next, nextX, rowY, canNext ? 0xFFDDDDDD : 0xFF555555, false);

        prevArrowX0 = prevX - 1; prevArrowX1 = prevX + prevW + 1;
        nextArrowX0 = nextX - 1; nextArrowX1 = nextX + nextW + 1;
        arrowY0 = rowY - 1; arrowY1 = rowY + font.lineHeight + 1;
        pagerVisible = true;
    }

    // ── Server -> client updates ─────────────────────────────────────────────

    /** Called from {@link kingdom.smp.client.VillagerDialogueCache#updateWardenScreen}. */
    public void handleServerUpdate(String status, String text) {
        switch (status) {
            case UpdateWardenScreenPayload.STATUS_REPLY -> {
                dialogue = text == null ? "" : text;
                // A new reply re-enables auto-advance; rebuildPagesIfNeeded resets
                // the page index + typewriter on the next tick/render.
                autoAdvance = true;
                paginatedFor = null;
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

        // ── Bonds page short-circuit ─────────────────────────────────────────
        if (activeTab == 1) {
            renderBondsPage(gfx, tx, ty);
            super.extractRenderState(gfx, mouseX, mouseY, partialTick);
            return;
        }

        // ── Dialogue text (paginated) ──────────────────────────────────────────
        pagerVisible = false;
        if (awaitingReply) {
            // Show a "thinking" indicator while we wait for the server.
            String dots = switch ((tick / 8) % 4) {
                case 0 -> "";
                case 1 -> ".";
                case 2 -> "..";
                default -> "...";
            };
            gfx.text(font, Component.literal("§7…thinking" + dots), tx, ty, 0xFFAAAAAA, false);
        } else {
            rebuildPagesIfNeeded();
            if (!dialoguePages.isEmpty()) {
                List<String> page = dialoguePages.get(dialoguePageIndex);
                int pageTotal = totalChars(page);
                boolean typing = pageCharsShown < pageTotal;

                int remaining = pageCharsShown;
                String lastVisibleLine = "";
                int lastLineY = ty;
                for (String line : page) {
                    String vis;
                    if (remaining >= line.length()) {
                        vis = line;
                        remaining -= line.length();
                    } else {
                        vis = line.substring(0, Math.max(0, remaining));
                        remaining = 0;
                    }
                    gfx.text(font, vis, tx, ty, 0xFFDDDDDD, false);
                    lastVisibleLine = vis;
                    lastLineY = ty;
                    ty += font.lineHeight + LINE_SPACING;
                    if (remaining <= 0 && typing) break; // stop at the typewriter head
                }

                // Blinking cursor while the page is still revealing.
                if (typing && (tick / 8) % 2 == 0) {
                    int lastW = font.width(lastVisibleLine);
                    int cursorX = lastW < textW ? tx + lastW : tx;
                    gfx.text(font, "█", cursorX, lastLineY, 0xAAFFFFFF, false);
                }

                // Page arrows + "x/y" indicator when the reply spans pages.
                if (dialoguePages.size() > 1) {
                    drawPager(gfx);
                }
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (pagerVisible && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double mx = event.x(), my = event.y();
            if (my >= arrowY0 && my <= arrowY1) {
                if (mx >= prevArrowX0 && mx <= prevArrowX1) {
                    gotoPage(dialoguePageIndex - 1);
                    return true;
                }
                if (mx >= nextArrowX0 && mx <= nextArrowX1) {
                    gotoPage(dialoguePageIndex + 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ── Bonds page ────────────────────────────────────────────────────────────

    private static final int MAX_RAPPORT       = NpcRapport.MAX_RAPPORT;
    private static final int RAPPORT_PER_HEART = NpcRapport.RAPPORT_PER_HEART;
    private static final int DAILY_GIFT_CAP    = 1;
    private static final long TICKS_PER_DAY    = 24000L;

    /** Right-of-portrait Bonds layout: hearts row, tier label, rapport count, today's gift dot. */
    private void renderBondsPage(GuiGraphicsExtractor gfx, int tx, int startY) {
        Minecraft mc = Minecraft.getInstance();
        PlayerNpcBonds bonds = (mc.player != null)
            ? mc.player.getData(ModAttachments.NPC_BONDS.get())
            : PlayerNpcBonds.empty();
        PlayerNpcBonds.Entry e = bonds.get(npcTag);

        int rapport = clamp(e.rapport(), 0, MAX_RAPPORT);
        int filled  = rapport / RAPPORT_PER_HEART;

        int y = startY;

        // ── Hearts row (10) ─────────────────────────────────────────────────
        int heartW = font.width("♥") + 1;
        int hx = tx;
        for (int i = 0; i < 10; i++) {
            boolean lit = i < filled;
            String glyph = lit ? "♥" : "♡";
            int color = lit ? ACCENT_COLOR : 0xFF555555;
            gfx.text(font, glyph, hx, y, color, false);
            hx += heartW;
        }
        // Numeric tail.
        String tail = "  " + rapport + " / " + MAX_RAPPORT;
        gfx.text(font, tail, hx + 4, y, 0xFFAAAAAA, false);
        y += font.lineHeight + 4;

        // ── Tier label ──────────────────────────────────────────────────────
        gfx.text(font, Component.literal(tierLabel(rapport))
                .withStyle(Style.EMPTY.withBold(true)),
            tx, y, ACCENT_COLOR, false);
        y += font.lineHeight + 6;

        // ── Today's gift indicator ──────────────────────────────────────────
        long today = (mc.level != null) ? (mc.level.getGameTime() / TICKS_PER_DAY) : 0L;
        int giftsToday = (e.lastGiftDay() == today) ? e.giftsToday() : 0;
        StringBuilder dots = new StringBuilder("Gifts today: ");
        for (int i = 0; i < DAILY_GIFT_CAP; i++) {
            dots.append(i < giftsToday ? "● " : "○ ");
        }
        gfx.text(font, dots.toString(), tx, y, 0xFFCCCCCC, false);
        y += font.lineHeight + 6;

        // ── Hint row ────────────────────────────────────────────────────────
        gfx.text(font, "§7Sneak + right-click them holding an item to gift.",
            tx, y, 0xFF888888, false);
        y += font.lineHeight + 2;
        gfx.text(font, "§7One gift per day grows your bond fastest.",
            tx, y, 0xFF888888, false);
    }

    private static String tierLabel(int rapport) {
        return NpcRapport.tierLabel(rapport);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
