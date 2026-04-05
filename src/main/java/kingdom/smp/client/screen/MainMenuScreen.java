package kingdom.smp.client.screen;

import kingdom.smp.net.ClientRpgData;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Unified player menu — opened via /menu.
 * Tabs: Home · Profile · Skills · Kingdom · Rankings · Settings
 */
public class MainMenuScreen extends Screen {

    // ── Panel ────────────────────────────────────────────────────────────────
    private static final int PANEL_W   = 330;
    private static final int PANEL_H   = 224;
    private static final int TAB_H     = 20;
    // content area starts this many px below panel top
    private static final int CONTENT_Y = TAB_H + 4;
    private static final int PAD       = 10;

    // ── Tabs ─────────────────────────────────────────────────────────────────
    private enum Tab {
        HOME     ("Home",     new ItemStack(Items.COMPASS)),
        PROFILE  ("Profile",  new ItemStack(Items.PAPER)),
        SKILLS   ("Skills",   new ItemStack(Items.ENCHANTED_BOOK)),
        KINGDOM  ("Kingdom",  new ItemStack(Items.SHIELD)),
        RANKINGS ("Rankings", new ItemStack(Items.NETHER_STAR)),
        SETTINGS ("Settings", new ItemStack(Items.COMPARATOR));

        final String     label;
        final ItemStack  icon;
        Tab(String l, ItemStack i) { label = l; icon = i; }
    }

    private Tab currentTab = Tab.HOME;

    /** Hitboxes for quick-action row on Home (set each frame in {@link #renderHome}). */
    private @Nullable ClickRect[] homeActionRects;
    private @Nullable ClickRect profileTabOpenRect;
    private @Nullable ClickRect kingdomTabOpenRect;
    private @Nullable ClickRect skillsTabOpenRect;

    private static final int ACT_W = 76;
    private static final int ACT_H = 16;
    private static final int ACT_GAP = 4;

    private record ClickRect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    // ── Kingdom data ─────────────────────────────────────────────────────────
    private static final String[] K_NAMES    = {"The Iron Hold","The Verdant Court","The Ember Throne","The Abyssal Pact"};
    private static final int[]    K_COLORS   = {0xFFBBBBBB, 0xFF44BB44, 0xFFDD6622, 0xFF8855CC};
    private static final String[] K_TAGLINES = {"Discipline, duty, and steel.","Growth, wisdom, and patience.","Ambition, fire, and conquest.","Shadow, cunning, and secrets."};
    private static final String[] K_INITIALS = {"I","V","E","A"};

    // ── Skills ───────────────────────────────────────────────────────────────
    private static final String[] SKILL_NAMES = {
        "Blacksmithing","Farming","Cooking","Alchemy",
        "Fishing","Enchanting","Mining","Trading"
    };

    public MainMenuScreen() {
        super(Component.literal("Menu"));
    }

    // ── Layout helpers ────────────────────────────────────────────────────────
    private int px() { return (width  - PANEL_W) / 2; }
    private int py() { return (height - PANEL_H) / 2; }

    // Content-area top-left and dimensions
    private int cx() { return px() + PAD; }
    private int cy() { return py() + CONTENT_Y + PAD; }
    private int cw() { return PANEL_W - PAD * 2; }
    private int ch() { return PANEL_H - CONTENT_Y - PAD * 2; }

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        addRenderableWidget(
            Button.builder(Component.literal("Close"), b -> onClose())
                  .bounds(width / 2 - 40, py() + PANEL_H + 4, 80, 18)
                  .build());
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int px = px(), py = py();

        // Panel
        gfx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xDD111111);
        gfx.outline(px, py, PANEL_W, PANEL_H, 0xFF555555);

        // Tab bar
        renderTabs(gfx, px, py, mouseX, mouseY);

        // Separator below tabs
        gfx.fill(px, py + CONTENT_Y - 1, px + PANEL_W, py + CONTENT_Y, 0xFF444444);

        // Content (scissored)
        gfx.enableScissor(px + 1, py + CONTENT_Y, px + PANEL_W - 1, py + PANEL_H - 1);
        int cx = cx(), cy = cy(), cw = cw(), ch = ch();
        switch (currentTab) {
            case HOME     -> renderHome    (gfx, cx, cy, cw, ch, mouseX, mouseY);
            case PROFILE  -> renderProfile (gfx, cx, cy, cw, ch, mouseX, mouseY);
            case SKILLS   -> renderSkills  (gfx, cx, cy, cw, ch, mouseX, mouseY);
            case KINGDOM  -> renderKingdom (gfx, cx, cy, cw, ch, mouseX, mouseY);
            case RANKINGS -> renderRankings(gfx, cx, cy, cw, ch);
            case SETTINGS -> renderSettings(gfx, cx, cy, cw, ch);
        }
        gfx.disableScissor();

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────
    private void renderTabs(GuiGraphicsExtractor gfx, int px, int py, int mx, int my) {
        Tab[] tabs  = Tab.values();
        int   tabW  = (PANEL_W - 4) / tabs.length;   // ≈54 px each
        int   startX = px + 2;

        for (int i = 0; i < tabs.length; i++) {
            Tab tab    = tabs[i];
            int tx     = startX + i * tabW;
            int ty     = py + 1;
            boolean active  = tab == currentTab;
            boolean hovered = !active && mx >= tx && mx < tx + tabW && my >= ty && my < ty + TAB_H - 1;

            int bg     = active ? 0xFF2A2A2A : (hovered ? 0xFF202020 : 0xFF181818);
            int border = active ? 0xFF777777 : 0xFF333333;

            gfx.fill(tx, ty, tx + tabW - 1, ty + TAB_H - 1, bg);
            gfx.outline(tx, ty, tabW - 1, TAB_H - 1, border);

            // Icon (16×16) centred vertically, left side
            gfx.item(tab.icon, tx + 2, ty + 2);

            // Label
            int textColor = active ? 0xFFFFFFFF : 0xFF777777;
            int maxTextW  = tabW - 22;
            String label  = font.width(tab.label) <= maxTextW
                    ? tab.label
                    : font.plainSubstrByWidth(tab.label, maxTextW);
            gfx.text(font, label, tx + 20, ty + 6, textColor, false);
        }
    }

    // ── Click handling ────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            // Tab clicks
            Tab[] tabs   = Tab.values();
            int   tabW   = (PANEL_W - 4) / tabs.length;
            int   startX = px() + 2;
            int   py     = py();
            for (int i = 0; i < tabs.length; i++) {
                int tx = startX + i * tabW;
                if (event.x() >= tx && event.x() < tx + tabW
                        && event.y() >= py + 1 && event.y() < py + TAB_H) {
                    currentTab = tabs[i];
                    return true;
                }
            }

            var mc = Minecraft.getInstance();
            if (currentTab == Tab.HOME && homeActionRects != null) {
                for (int i = 0; i < homeActionRects.length; i++) {
                    if (homeActionRects[i].contains(event.x(), event.y())) {
                        switch (i) {
                            case 0 -> mc.setScreen(new ClassSelectionScreen(this));
                            case 1 -> mc.setScreen(new ProfileScreen(this));
                            case 2 -> mc.setScreen(new KingdomSelectionScreen(this));
                        }
                        return true;
                    }
                }
            }
            if (currentTab == Tab.PROFILE && profileTabOpenRect != null
                    && profileTabOpenRect.contains(event.x(), event.y())) {
                mc.setScreen(new ProfileScreen(this));
                return true;
            }
            if (currentTab == Tab.KINGDOM && kingdomTabOpenRect != null
                    && kingdomTabOpenRect.contains(event.x(), event.y())) {
                mc.setScreen(new KingdomSelectionScreen(this));
                return true;
            }
            if (currentTab == Tab.SKILLS && skillsTabOpenRect != null
                    && skillsTabOpenRect.contains(event.x(), event.y())) {
                mc.setScreen(new ProfileScreen(this));
                return true;
            }

            // Settings toggle clicks
            if (currentTab == Tab.SETTINGS) {
                int cx = cx(), cy = cy();
                int box = 9;
                // Mirror the y-offsets from renderSettings
                int[] toggleY = { cy + 28, cy + 42, cy + 82 };
                for (int i = 0; i < toggleY.length; i++) {
                    if (event.x() >= cx && event.x() < cx + box
                            && event.y() >= toggleY[i] && event.y() < toggleY[i] + box) {
                        switch (i) {
                            case 0 -> ClientRpgData.setClassHudEnabled (!ClientRpgData.classHudEnabled());
                            case 1 -> ClientRpgData.setCarryHudEnabled (!ClientRpgData.carryHudEnabled());
                            case 2 -> ClientRpgData.setBroadcastLevelUps(!ClientRpgData.broadcastLevelUps());
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    @Override public boolean isPauseScreen() { return false; }

    // =========================================================================
    // HOME TAB
    // =========================================================================
    private static ClickRect[] homeActionRow(int x, int w, int rowY) {
        int total = 3 * ACT_W + 2 * ACT_GAP;
        int sx = x + (w - total) / 2;
        return new ClickRect[]{
                new ClickRect(sx, rowY, ACT_W, ACT_H),
                new ClickRect(sx + ACT_W + ACT_GAP, rowY, ACT_W, ACT_H),
                new ClickRect(sx + 2 * (ACT_W + ACT_GAP), rowY, ACT_W, ACT_H)
        };
    }

    private void drawActionButton(GuiGraphicsExtractor gfx, ClickRect r, String label, int mx, int my) {
        boolean hover = r.contains(mx, my);
        int bg = hover ? 0xFF3A3A3A : 0xFF252525;
        gfx.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
        gfx.outline(r.x, r.y, r.w, r.h, 0xFF666666);
        gfx.centeredText(font, label, r.x + r.w / 2, r.y + (r.h - 8) / 2, 0xFFFFFFFF);
    }

    private void renderHome(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int mouseX, int mouseY) {
        var mc     = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // Player head — centred, 48 × 48
        int headSize = 48;
        int headX    = x + w / 2 - headSize / 2;
        renderPlayerHead(gfx, skinTextureId((AbstractClientPlayer) player), headX, y, headSize);

        int ty = y + headSize + 6;

        // Name
        String name = mc.getUser().getName();
        gfx.centeredText(font,
                Component.literal(name).withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, ty, 0xFFFFFFFF);
        ty += 14;

        // Class + level
        PlayerClass pc         = ClientRpgData.playerClass();
        int         classColor = classColor(pc);
        gfx.centeredText(font,
                Component.literal(pc.id() + "  ·  Lv." + ClientRpgData.classLevel())
                         .withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, ty, classColor);
        ty += 13;

        // Kingdom
        int    ki = ClientRpgData.kingdomIndex();
        gfx.centeredText(font, K_NAMES[ki], x + w / 2, ty, K_COLORS[ki]);
        ty += 16;

        // XP bar
        int barW = Math.min(160, w - 20);
        int barX = x + w / 2 - barW / 2;
        renderXpBar(gfx, barX, ty, barW, 6, classColor,
                ClientRpgData.xpIntoLevel(), ClientRpgData.xpToNext());
        ty += 16;

        // Carry weight (one line)
        int  cw  = ClientRpgData.carryWeight();
        int  max = ClientRpgData.maxCarryWeight();
        boolean over = cw > max;
        String carryStr = "Carry: " + cw + " / " + max + (over ? "   ENCUMBERED" : "");
        gfx.centeredText(font, carryStr, x + w / 2, ty, over ? 0xFFFF5555 : 0xFF888888);
        ty += 16;
        gfx.centeredText(font, "Quick open (Esc or Close returns here)", x + w / 2, ty, 0xFF555555);
        ty += 11;
        homeActionRects = homeActionRow(x, w, ty);
        String[] labels = {"Classes", "Profile", "Kingdom"};
        for (int i = 0; i < 3; i++) {
            drawActionButton(gfx, homeActionRects[i], labels[i], mouseX, mouseY);
        }
    }

    // =========================================================================
    // PROFILE TAB
    // =========================================================================
    private void renderProfile(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int mouseX, int mouseY) {
        var mc     = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // Head — 32 × 32, top-left
        int headSize = 32;
        renderPlayerHead(gfx, skinTextureId((AbstractClientPlayer) player), x, y, headSize);

        // Name + class next to head
        String name = mc.getUser().getName();
        gfx.text(font,
                Component.literal(name).withStyle(Style.EMPTY.withBold(true)),
                x + headSize + 6, y + 2, 0xFFFFFFFF, true);
        PlayerClass pc         = ClientRpgData.playerClass();
        int         classColor = classColor(pc);
        gfx.text(font, pc.id() + "  ·  Lv." + ClientRpgData.classLevel(),
                x + headSize + 6, y + 14, classColor, false);

        int ty = y + headSize + 8;

        sep(gfx, x, ty, w); ty += 7;

        // Kingdom
        int ki = ClientRpgData.kingdomIndex();
        label(gfx, "Kingdom", x, ty); gfx.text(font, K_NAMES[ki], x + 62, ty, K_COLORS[ki], false);
        ty += 12;

        // Level
        label(gfx, "Level", x, ty);
        gfx.text(font, String.valueOf(ClientRpgData.classLevel()), x + 62, ty, 0xFFFFFFFF, false);
        ty += 12;

        // XP bar
        label(gfx, "XP", x, ty);
        int barX = x + 62, barW = w - 66, barH = 8;
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF222222);
        int fillW = Mth.clamp((int) (ClientRpgData.xpProgress() * barW), 0, barW);
        if (fillW > 0) gfx.fill(barX, ty, barX + fillW, ty + barH, classColor | 0xFF000000);
        gfx.outline(barX - 1, ty - 1, barW + 2, barH + 2, 0xFF444444);
        gfx.text(font, ClientRpgData.xpIntoLevel() + " / " + ClientRpgData.xpToNext(),
                barX + 2, ty, 0xFFFFFFFF, true);
        ty += barH + 8;

        sep(gfx, x, ty, w); ty += 7;

        // Carry weight
        int  cw  = ClientRpgData.carryWeight();
        int  max = ClientRpgData.maxCarryWeight();
        boolean over = cw > max;
        label(gfx, "Carry", x, ty);
        gfx.text(font, cw + " / " + max, x + 62, ty, over ? 0xFFFF5555 : 0xFFFFFFFF, false);
        if (over) gfx.text(font, " (Encumbered!)",
                x + 62 + font.width(cw + " / " + max), ty, 0xFFFF5555, false);
        ty += 12;

        // Carry bar
        barX = x + 62;
        gfx.fill(barX, ty, barX + barW, ty + barH, 0xFF222222);
        float wRatio = max <= 0 ? 0f : Mth.clamp((float) cw / max, 0f, 1f);
        int   wFill  = (int) (wRatio * barW);
        if (wFill > 0) {
            int wc = over ? 0xFFFF5555
                    : (wRatio > 0.9f ? 0xFFFFAA00 : (wRatio > 0.7f ? 0xFFFFFF55 : 0xFF55FF55));
            gfx.fill(barX, ty, barX + wFill, ty + barH, wc);
        }
        gfx.outline(barX - 1, ty - 1, barW + 2, barH + 2, 0xFF444444);

        int openY = y + h - ACT_H - 4;
        profileTabOpenRect = new ClickRect(x + w / 2 - 84, openY, 168, ACT_H);
        drawActionButton(gfx, profileTabOpenRect, "Full profile & skills", mouseX, mouseY);
    }

    // =========================================================================
    // SKILLS TAB
    // =========================================================================
    private void renderSkills(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int mouseX, int mouseY) {
        gfx.text(font,
                Component.literal("Skills").withStyle(Style.EMPTY.withBold(true)),
                x, y, 0xFFFFDD55, true);
        int ty   = y + 14;
        int colW = w / 2;

        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int col = i % 2, row = i / 2;
            int sx = x + col * colW;
            int sy = ty + row * 32;

            gfx.text(font, SKILL_NAMES[i], sx, sy, 0xFFCCCCCC, false);
            gfx.text(font, "Lv. 0", sx, sy + 10, 0xFF555555, false);

            // Empty progress bar
            int bw = colW - 12;
            gfx.fill(sx, sy + 21, sx + bw, sy + 23, 0xFF222222);
            gfx.outline(sx - 1, sy + 20, bw + 2, 4, 0xFF333333);
        }

        int btnY = ty + 4 * 32 + 4;
        skillsTabOpenRect = new ClickRect(x + w / 2 - 84, btnY, 168, ACT_H);
        drawActionButton(gfx, skillsTabOpenRect, "Abilities on profile", mouseX, mouseY);
        int noteY = btnY + ACT_H + 6;
        gfx.centeredText(font, "Skill progression coming soon.",
                x + w / 2, noteY, 0xFF555555);
    }

    // =========================================================================
    // KINGDOM TAB
    // =========================================================================
    private void renderKingdom(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int mouseX, int mouseY) {
        int ki     = Mth.clamp(ClientRpgData.kingdomIndex(), 0, 3);
        int kColor = K_COLORS[ki];

        gfx.centeredText(font,
                Component.literal("Your Kingdom").withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, y, 0xFFFFDD55);
        int ty = y + 16;

        // Banner placeholder — coloured rectangle with initial letter
        int bx = x + w / 2 - 16;
        gfx.fill(bx, ty, bx + 32, ty + 40, kColor);
        gfx.outline(bx, ty, 32, 40, 0xFF888888);
        gfx.centeredText(font,
                Component.literal(K_INITIALS[ki]).withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, ty + 15, 0xFF111111);
        ty += 48;

        // Name + tagline
        gfx.centeredText(font,
                Component.literal(K_NAMES[ki]).withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, ty, kColor);
        ty += 13;
        gfx.centeredText(font, K_TAGLINES[ki], x + w / 2, ty, 0xFF888888);
        ty += 18;

        sep(gfx, x, ty, w); ty += 8;

        gfx.centeredText(font, "Territory & war features coming soon.",
                x + w / 2, ty, 0xFF555555);

        int openY = y + h - ACT_H - 4;
        kingdomTabOpenRect = new ClickRect(x + w / 2 - 84, openY, 168, ACT_H);
        drawActionButton(gfx, kingdomTabOpenRect, "Kingdom allegiance", mouseX, mouseY);
    }

    // =========================================================================
    // RANKINGS TAB
    // =========================================================================
    private void renderRankings(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.centeredText(font,
                Component.literal("Rankings").withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, y, 0xFFFFDD55);
        int ty = y + 22;

        gfx.centeredText(font, "Rankings are being forged...", x + w / 2, ty, 0xFF888888);
        ty += 14;
        gfx.centeredText(font, "Check back once more players have", x + w / 2, ty, 0xFF555555);
        ty += 10;
        gfx.centeredText(font, "chosen their path.", x + w / 2, ty, 0xFF555555);

        gfx.item(new ItemStack(Items.NETHER_STAR), x + w / 2 - 8, ty + 18);
    }

    // =========================================================================
    // SETTINGS TAB
    // =========================================================================
    private void renderSettings(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.text(font,
                Component.literal("Settings").withStyle(Style.EMPTY.withBold(true)),
                x, y, 0xFFFFDD55, true);
        // y offsets MUST match the toggleY[] array in mouseClicked
        int ty = y + 16;

        gfx.text(font, "HUD Overlays", x, ty, 0xFFCCCCCC, false);
        ty += 12;                                               // ty = y+28 → toggleY[0]
        renderToggle(gfx, x, ty, "Class & XP HUD",             ClientRpgData.classHudEnabled());
        ty += 14;                                               // ty = y+42 → toggleY[1]
        renderToggle(gfx, x, ty, "Carry Weight HUD",           ClientRpgData.carryHudEnabled());
        ty += 20;                                               // ty = y+62

        sep(gfx, x, ty, w); ty += 8;                           // ty = y+70

        gfx.text(font, "Privacy", x, ty, 0xFFCCCCCC, false);
        ty += 12;                                               // ty = y+82 → toggleY[2]
        renderToggle(gfx, x, ty, "Broadcast level-ups to all players",
                ClientRpgData.broadcastLevelUps());
    }

    // ── Shared render helpers ─────────────────────────────────────────────────

    /** 9×9 checkbox with label. */
    private void renderToggle(GuiGraphicsExtractor gfx, int x, int y, String label, boolean value) {
        int box = 9;
        gfx.fill(x, y, x + box, y + box, 0xFF1A1A1A);
        gfx.outline(x, y, box, box, value ? 0xFF55FF55 : 0xFF555555);
        if (value) gfx.fill(x + 2, y + 2, x + box - 2, y + box - 2, 0xFF55FF55);
        gfx.text(font, label, x + box + 4, y + 1, 0xFFAAAAAA, false);
    }

    /** Thin horizontal separator. */
    private void sep(GuiGraphicsExtractor gfx, int x, int y, int w) {
        gfx.fill(x, y, x + w, y + 1, 0xFF444444);
    }

    /** Grey "key:" label. */
    private void label(GuiGraphicsExtractor gfx, String text, int x, int y) {
        gfx.text(font, text + ":", x, y, 0xFF888888, false);
    }

    /** Mini XP bar with centred text. */
    private void renderXpBar(GuiGraphicsExtractor gfx, int bx, int by, int bw, int bh,
                             int color, int xpIn, int xpMax) {
        gfx.fill(bx, by, bx + bw, by + bh, 0xFF222222);
        int fill = xpMax <= 0 ? 0 : Mth.clamp((int) ((float) xpIn / xpMax * bw), 0, bw);
        if (fill > 0) gfx.fill(bx, by, bx + fill, by + bh, color | 0xFF000000);
        gfx.outline(bx - 1, by - 1, bw + 2, bh + 2, 0xFF444444);
        String txt = xpIn + " / " + xpMax + " XP";
        gfx.centeredText(font, txt, bx + bw / 2, by + bh + 2, 0xFF888888);
    }

    private static Identifier skinTextureId(AbstractClientPlayer player) {
        ClientAsset.Texture body = player.getSkin().body();
        return body.texturePath();
    }

    /** Renders the 2-layer player face at (x, y) scaled to size×size pixels. */
    private static void renderPlayerHead(GuiGraphicsExtractor gfx, Identifier skin, int x, int y, int size) {
        // Face layer  — skin UV: (8,8), region 8×8 on a 64×64 texture
        gfx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, size, size, 8, 8, 64, 64);
        // Hat overlay — skin UV: (40,8)
        gfx.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, size, size, 8, 8, 64, 64);
    }

    // ── Class / kingdom color helpers ─────────────────────────────────────────
    private static int classColor(PlayerClass pc) {
        return switch (pc) {
            case KNIGHT  -> 0xFFCCCCCC;
            case RANGER  -> 0xFF55AA55;
            case WIZARD  -> 0xFF7755FF;
            case CLERIC  -> 0xFFFFDD55;
            case ROGUE   -> 0xFFAA3333;
            case PEASANT -> 0xFF888888;
        };
    }
}
