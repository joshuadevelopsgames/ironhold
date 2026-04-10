package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.ClassChoicePayload;
import kingdom.smp.net.ClientRpgData;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dynamic class selection / promotion screen with scrolling.
 * Only shows classes on the player's path (connected via prerequisites
 * to their current or completed classes).
 */
public class ClassSelectionScreen extends Screen {

    private static final int CARD_GAP = 6;
    private static final int MARGIN = 12;
    private static final int CARD_HEIGHT = 150;
    private static final int CARD_WIDTH = 190;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 34;
    private static final int MAX_COLS = 3;

    private int selected = -1;
    private double scrollY = 0;

    private final @Nullable Screen returnTo;
    private List<ClassEntry> entries = List.of();

    public ClassSelectionScreen() {
        this(null);
    }

    public ClassSelectionScreen(@Nullable Screen returnTo) {
        super(Component.literal("Choose Your Path"));
        this.returnTo = returnTo;
    }

    @Override
    protected void init() {
        super.init();
        entries = buildEntries();
        selected = -1;
        scrollY = 0;

        addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> {
            if (selected >= 0 && selected < entries.size() && entries.get(selected).unlocked) {
                ClientPayloads.sendToServer(new ClassChoicePayload(entries.get(selected).pc.ordinal()));
                onClose();
            }
        }).bounds(width / 2 - 50, height - FOOTER_HEIGHT + 4, 100, 20).build());
    }

    // ── Entry building — only classes on the player's path ───────────────────

    private List<ClassEntry> buildEntries() {
        PlayerClass current = ClientRpgData.playerClass();
        Set<PlayerClass> completed = ClientRpgData.completedClassSet();
        int targetTier = current.tier() + 1;

        Set<PlayerClass> withCurrent = new HashSet<>(completed);
        withCurrent.add(current);

        List<ClassEntry> result = new ArrayList<>();
        for (PlayerClass pc : PlayerClass.values()) {
            if (pc.tier() != targetTier) continue;
            if (!isOnPlayerPath(pc, withCurrent)) continue;

            boolean unlocked = pc.canUnlock(withCurrent);
            String reason = unlocked ? null : buildLockedReason(pc, withCurrent);
            result.add(new ClassEntry(pc, unlocked, reason));
        }
        return result;
    }

    /**
     * A class is "on the player's path" if:
     * - It has no prerequisites (Tier 1 from Peasant), OR
     * - At least one of its prerequisites is in the player's completed set
     *   (including current class)
     */
    private static boolean isOnPlayerPath(PlayerClass candidate, Set<PlayerClass> completedWithCurrent) {
        List<PlayerClass> prereqs = candidate.prerequisites();
        if (prereqs.isEmpty()) return true;
        for (PlayerClass req : prereqs) {
            if (completedWithCurrent.contains(req)) return true;
        }
        return false;
    }

    private static String buildLockedReason(PlayerClass pc, Set<PlayerClass> completed) {
        List<PlayerClass> prereqs = pc.prerequisites();
        List<String> missing = new ArrayList<>();
        for (PlayerClass req : prereqs) {
            if (!completed.contains(req)) {
                missing.add(req.id());
            }
        }
        if (missing.isEmpty()) return "Locked";
        return "Also requires: " + String.join(" + ", missing);
    }

    // ── Grid layout ──────────────────────────────────────────────────────────

    private int cols() {
        return Math.min(entries.size(), MAX_COLS);
    }

    private int rows() {
        int c = cols();
        if (c == 0) return 0;
        return (entries.size() + c - 1) / c;
    }

    private int cardWidth() {
        int c = cols();
        int fitWidth = (width - MARGIN * 2 - CARD_GAP * (c - 1)) / c;
        return Math.min(CARD_WIDTH, fitWidth);
    }

    private int itemsInRow(int row) {
        int c = cols();
        int remaining = entries.size() - row * c;
        return Math.min(remaining, c);
    }

    private int cardX(int col, int rowCount) {
        int cw = cardWidth();
        int totalWidth = rowCount * cw + (rowCount - 1) * CARD_GAP;
        int rowStartX = (width - totalWidth) / 2;
        return rowStartX + col * (cw + CARD_GAP);
    }

    private int cardY(int row) {
        return HEADER_HEIGHT + row * (CARD_HEIGHT + CARD_GAP) - (int) scrollY;
    }

    private int totalGridHeight() {
        int r = rows();
        return r * CARD_HEIGHT + (r - 1) * CARD_GAP;
    }

    private int viewportHeight() {
        return height - HEADER_HEIGHT - FOOTER_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, totalGridHeight() - viewportHeight());
    }

    // ── Scrolling ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY -= scrollY * 20;
        this.scrollY = Math.max(0, Math.min(this.scrollY, maxScroll()));
        return true;
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        PlayerClass current = ClientRpgData.playerClass();
        int targetTier = current.tier() + 1;

        // Title
        String titleText = current == PlayerClass.PEASANT
            ? "Choose Your Path"
            : "Promotion — Tier " + targetTier;
        gfx.centeredText(font, Component.literal(titleText)
                        .withStyle(Style.EMPTY.withBold(true)),
                width / 2, 12, 0xFFFFDD55);

        int promoLevel = RpgProgression.promotionLevelForTier(current.tier());
        String subtitle = current == PlayerClass.PEASANT
            ? "Select a class to begin your journey."
            : "You have mastered " + current.id() + " at Level " + promoLevel + ".";
        gfx.centeredText(font, subtitle, width / 2, 26, 0xFFAAAAAA);

        // Clip the card area so cards don't bleed into header/footer
        gfx.enableScissor(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT);

        int cw = cardWidth();
        int c = cols();

        for (int i = 0; i < entries.size(); i++) {
            int row = i / c;
            int col = i % c;
            int rowItems = itemsInRow(row);
            int cx = cardX(col, rowItems);
            int cy = cardY(row);

            // Skip cards entirely off-screen
            if (cy + CARD_HEIGHT < HEADER_HEIGHT || cy > height - FOOTER_HEIGHT) continue;

            boolean hovered = mouseX >= cx && mouseX < cx + cw
                    && mouseY >= cy && mouseY < cy + CARD_HEIGHT
                    && mouseY >= HEADER_HEIGHT && mouseY < height - FOOTER_HEIGHT;
            drawClassCard(gfx, entries.get(i), cx, cy, cw, hovered, i == selected);
        }

        gfx.disableScissor();

        // Scroll indicator
        if (maxScroll() > 0) {
            drawScrollbar(gfx);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx) {
        int trackX = width - 6;
        int trackTop = HEADER_HEIGHT;
        int trackH = viewportHeight();

        // Track background
        gfx.fill(trackX, trackTop, trackX + 4, trackTop + trackH, 0x44FFFFFF);

        // Thumb
        int totalH = totalGridHeight();
        int thumbH = Math.max(10, trackH * viewportHeight() / totalH);
        int thumbY = trackTop + (int) ((trackH - thumbH) * scrollY / maxScroll());
        gfx.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xAAFFFFFF);
    }

    // ── Click ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            if (my >= HEADER_HEIGHT && my < height - FOOTER_HEIGHT) {
                int cw = cardWidth();
                int c = cols();
                for (int i = 0; i < entries.size(); i++) {
                    int row = i / c;
                    int col = i % c;
                    int rowItems = itemsInRow(row);
                    int cx = cardX(col, rowItems);
                    int cy = cardY(row);
                    if (mx >= cx && mx < cx + cw
                            && my >= cy && my < cy + CARD_HEIGHT) {
                        if (entries.get(i).unlocked) {
                            selected = i;
                        }
                        return true;
                    }
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

    // ── Card drawing ─────────────────────────────────────────────────────────

    private void drawClassCard(GuiGraphicsExtractor gfx, ClassEntry entry, int x, int y,
                               int cw, boolean hovered, boolean selected) {
        PlayerClass pc = entry.pc;
        boolean locked = !entry.unlocked;
        int color = locked ? 0xFF555555 : classColor(pc);

        int bgColor = locked ? 0xAA0A0A0A
            : (selected ? 0xCC333333 : (hovered ? 0xBB222222 : 0xAA111111));
        gfx.fill(x, y, x + cw, y + CARD_HEIGHT, bgColor);

        int borderColor = locked ? 0xFF333333
            : (selected ? color : (hovered ? 0xFFAAAAAA : 0xFF555555));
        gfx.outline(x, y, cw, CARD_HEIGHT, borderColor);
        if (selected && !locked) {
            gfx.outline(x - 1, y - 1, cw + 2, CARD_HEIGHT + 2, color);
        }

        // Clip card content (nested scissor within the viewport scissor)
        gfx.enableScissor(x + 1, Math.max(y + 1, HEADER_HEIGHT),
                          x + cw - 1, Math.min(y + CARD_HEIGHT - 1, height - FOOTER_HEIGHT));

        int tx = x + 6;
        int ty = y + 8;

        // Class name
        gfx.text(font, Component.literal(pc.id())
                        .withStyle(Style.EMPTY.withBold(true)),
                tx, ty, color, true);
        ty += 14;

        // Role + Tier
        gfx.text(font, pc.role() + " — Tier " + pc.tier(), tx, ty, 0xFF888888, false);
        ty += 12;

        // Separator
        gfx.fill(tx, ty, x + cw - 6, ty + 1, 0xFF444444);
        ty += 5;

        if (locked) {
            gfx.text(font, "\u274C Locked", tx, ty, 0xFFFF5555, false);
            ty += 14;
            if (entry.lockReason != null) {
                for (var line : font.split(Component.literal(entry.lockReason), cw - 14)) {
                    gfx.text(font, line, tx, ty, 0xFFAA6666, false);
                    ty += 10;
                }
            }
        } else {
            // Stat lines
            gfx.text(font, "HP " + pc.statHealth() + "  ATK " + pc.statAttackDamage()
                + "  SPD " + pc.statSpeed(), tx, ty, 0xFF55FF55, false);
            ty += 12;
            gfx.text(font, "DEF " + pc.statDefense() + "  MANA " + pc.statMana()
                + "  LUCK " + pc.statLuck(), tx, ty, 0xFF55CCFF, false);
            ty += 12;
            gfx.text(font, "ATK SPD " + pc.statAttackSpeed()
                + "  CARRY " + pc.maxCarryWeight(), tx, ty, 0xFFFFDD55, false);
            ty += 14;

            // Separator
            gfx.fill(tx, ty, x + cw - 6, ty + 1, 0xFF444444);
            ty += 5;

            // Prerequisites
            List<PlayerClass> prereqs = pc.prerequisites();
            if (!prereqs.isEmpty()) {
                StringBuilder sb = new StringBuilder("\u2713 From: ");
                for (int i = 0; i < prereqs.size(); i++) {
                    if (i > 0) sb.append(" + ");
                    sb.append(prereqs.get(i).id());
                }
                gfx.text(font, sb.toString(), tx, ty, 0xFF55AA55, false);
            } else {
                gfx.text(font, "No prerequisites", tx, ty, 0xFF888888, false);
            }
        }

        gfx.disableScissor();
    }

    private static int classColor(PlayerClass pc) {
        return switch (pc) {
            case KNIGHT  -> 0xFFCCCCCC;
            case RANGER  -> 0xFF55AA55;
            case WIZARD  -> 0xFF7755FF;
            case CLERIC  -> 0xFFFFDD55;
            case ROGUE   -> 0xFFAA3333;
            case PEASANT -> 0xFF888888;
            default      -> 0xFFFFFFFF;
        };
    }

    private record ClassEntry(PlayerClass pc, boolean unlocked, @Nullable String lockReason) {}
}
