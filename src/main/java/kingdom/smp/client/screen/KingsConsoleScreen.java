package kingdom.smp.client.screen;

import kingdom.smp.Ironhold;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The King's Console — opened via /console.
 * Stub buttons only; no behavior wired up yet.
 */
public class KingsConsoleScreen extends Screen {

    /**
     * When true, the screen blits sprites from {@link ConsoleAtlas#TEXTURE}
     * for chrome, headers, and buttons. When false, falls back to drawn
     * fills (legacy look).
     */
    public static final boolean USE_ATLAS = true;

    private static final int PANEL_W = 376;
    private static final int PANEL_H = 184;

    private static final int COL_LEFT_W  = 108;
    private static final int COL_RIGHT_W = 108;
    private static final int COL_GAP     = 4;
    private static final int COL_CENTER_W = PANEL_W - COL_LEFT_W - COL_RIGHT_W - COL_GAP * 4;

    private static final int TITLE_H = 18;

    private static final int BG          = 0xFF1A1A1A;
    private static final int PANEL_INNER = 0xFF1F1F1F;
    private static final int FRAME       = 0xFF8A8A8A;
    private static final int FRAME_DARK  = 0xFF555555;
    private static final int TITLE_BG    = 0xFF2D2D2D;
    private static final int GOLD        = 0xFFFFCC44;
    private static final int TEXT        = 0xFFE6E6E6;
    private static final int TEXT_DIM    = 0xFF888888;
    private static final int OK_GREEN    = 0xFF55DD55;
    private static final int WARN_AMBER  = 0xFFFFCC44;
    private static final int BAD_RED     = 0xFFFF5555;

    private record ClickRect(int x, int y, int w, int h, String tag) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private @Nullable ClickRect[] clickRects;

    // ── Mutable state — "pending" values (what stepper clicks mutate) ────────
    private int incomeTax  = 10;   // %
    private int landTax    = 2;    // coins per chunk
    private int tradeTax   = 5;    // %
    private int gateToll   = 2;    // coins
    private int capitalToll  = 10;
    private int northToll    = 4;
    private int southToll    = 3;
    private int frontierToll = 3;
    private boolean gateFreeCitizens     = true;
    private boolean gateAllyDiscount     = true;
    private boolean portalAllyDiscount   = true;
    private boolean portalEnemySurcharge = true;

    // ── Committed snapshot — last-stamped values, restored on cancel ─────────
    private int cIncomeTax = 10, cLandTax = 2, cTradeTax = 5;
    private int cGateToll = 2;
    private int cCapitalToll = 10, cNorthToll = 4, cSouthToll = 3, cFrontierToll = 3;
    private boolean cGateFree = true, cGateAlly = true;
    private boolean cPortalAlly = true, cPortalEnemy = true;

    private void commitDecree() {
        cIncomeTax = incomeTax; cLandTax = landTax; cTradeTax = tradeTax;
        cGateToll = gateToll;
        cCapitalToll = capitalToll; cNorthToll = northToll;
        cSouthToll = southToll; cFrontierToll = frontierToll;
        cGateFree = gateFreeCitizens; cGateAlly = gateAllyDiscount;
        cPortalAlly = portalAllyDiscount; cPortalEnemy = portalEnemySurcharge;
    }

    private void revertDecree() {
        incomeTax = cIncomeTax; landTax = cLandTax; tradeTax = cTradeTax;
        gateToll = cGateToll;
        capitalToll = cCapitalToll; northToll = cNorthToll;
        southToll = cSouthToll; frontierToll = cFrontierToll;
        gateFreeCitizens = cGateFree; gateAllyDiscount = cGateAlly;
        portalAllyDiscount = cPortalAlly; portalEnemySurcharge = cPortalEnemy;
    }

    private int pendingChangeCount() {
        int n = 0;
        if (incomeTax != cIncomeTax) n++;
        if (landTax != cLandTax) n++;
        if (tradeTax != cTradeTax) n++;
        if (gateToll != cGateToll) n++;
        if (capitalToll != cCapitalToll) n++;
        if (northToll != cNorthToll) n++;
        if (southToll != cSouthToll) n++;
        if (frontierToll != cFrontierToll) n++;
        if (gateFreeCitizens != cGateFree) n++;
        if (gateAllyDiscount != cGateAlly) n++;
        if (portalAllyDiscount != cPortalAlly) n++;
        if (portalEnemySurcharge != cPortalEnemy) n++;
        return n;
    }

    /** Daily income delta if pending values were committed. */
    private int pendingIncomeDelta() {
        int pending  = incomeTax * 96 + gateToll * 218 + tradeTax * 49;
        int committed = cIncomeTax * 96 + cGateToll * 218 + cTradeTax * 49;
        return pending - committed;
    }

    /** Crude risk heuristic: positive if tax/toll % went up significantly. */
    private String pendingRiskLevel() {
        int taxRise = Math.max(0, incomeTax - cIncomeTax)
                    + Math.max(0, tradeTax - cTradeTax);
        int tollRise = Math.max(0, gateToll - cGateToll)
                     + Math.max(0, capitalToll - cCapitalToll)
                     + Math.max(0, northToll - cNorthToll)
                     + Math.max(0, southToll - cSouthToll)
                     + Math.max(0, frontierToll - cFrontierToll);
        int score = taxRise * 2 + tollRise;
        if (score == 0) return "LOW";
        if (score <= 6) return "MED";
        return "HIGH";
    }

    public KingsConsoleScreen() {
        super(Component.literal("The King's Console"));
    }

    private int px() { return (width  - PANEL_W) / 2; }
    private int py() { return (height - PANEL_H) / 2; }

    private static ItemStack coin() { return new ItemStack(kingdom.smp.ModItems.GOLD_COIN.get()); }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(
            Button.builder(Component.literal("Close"), b -> onClose())
                  .bounds(width / 2 - 40, py() + PANEL_H + 4, 80, 18)
                  .build());
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int px = px(), py = py();
        List<ClickRect> rects = new ArrayList<>();

        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.OUTER_FRAME, px, py, PANEL_W, PANEL_H, 8);
        } else {
            gfx.fill(px, py, px + PANEL_W, py + PANEL_H, BG);
            gfx.outline(px, py, PANEL_W, PANEL_H, FRAME);
            gfx.outline(px + 1, py + 1, PANEL_W - 2, PANEL_H - 2, FRAME_DARK);
        }

        renderTitleBar(gfx, px, py);

        int colY = py + TITLE_H + 4;
        int leftX   = px + COL_GAP;
        int centerX = leftX + COL_LEFT_W + COL_GAP;
        int rightX  = centerX + COL_CENTER_W + COL_GAP;
        int colH    = PANEL_H - TITLE_H - 6;

        renderTaxesColumn  (gfx, leftX,   colY, COL_LEFT_W,   colH, mouseX, mouseY, rects);
        renderCenterColumn (gfx, centerX, colY, COL_CENTER_W, colH, mouseX, mouseY, rects);
        renderTollsColumn  (gfx, rightX,  colY, COL_RIGHT_W,  colH, mouseX, mouseY, rects);

        clickRects = rects.toArray(new ClickRect[0]);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTitleBar(GuiGraphicsExtractor gfx, int px, int py) {
        if (USE_ATLAS) {
            // Stretch the title-bar 9-slice across the top
            blit9Slice(gfx, ConsoleAtlas.TITLE_BAR, px + 2, py + 2, PANEL_W - 4, TITLE_H, 8);
        } else {
            gfx.fill(px + 2, py + 2, px + PANEL_W - 2, py + 2 + TITLE_H, TITLE_BG);
            gfx.outline(px + 2, py + 2, PANEL_W - 4, TITLE_H, FRAME_DARK);
            drawCrownGlyph(gfx, px + 8,            py + 6);
            drawCrownGlyph(gfx, px + PANEL_W - 16, py + 6);
        }

        Component title = Component.literal("THE KING'S CONSOLE").withStyle(Style.EMPTY.withBold(true));
        gfx.centeredText(font, title, px + PANEL_W / 2, py + 6, GOLD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEFT COLUMN — Taxes
    // ─────────────────────────────────────────────────────────────────────────
    private void renderTaxesColumn(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                   int mx, int my, List<ClickRect> rects) {
        renderColumnHeader(gfx, x, y, w, "TAXES");
        int boxY = y + 14;

        renderTaxBox(gfx, x, boxY, w, "INCOME", incomeTax + "%",
                statusFor(incomeTax, 10, 20), "coin",
                "tax_income", mx, my, rects);
        boxY += 38;

        renderTaxBox(gfx, x, boxY, w, "LAND", String.valueOf(landTax),
                statusFor(landTax, 2, 5), "land",
                "tax_land", mx, my, rects);
        boxY += 38;

        renderTaxBox(gfx, x, boxY, w, "TRADE", tradeTax + "%",
                statusFor(tradeTax, 5, 10), "emerald",
                "tax_trade", mx, my, rects);
        boxY += 38;

        // Total tax burden footer — sum of the three taxes as displayed
        int burden = incomeTax + landTax + tradeTax;
        float burdenFill = Math.min(1f, burden / 50f);
        int burdenColor = burden <= 20 ? OK_GREEN : burden <= 35 ? WARN_AMBER : BAD_RED;

        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.INNER_BOX, x, boxY, w, 30, 8);
        } else {
            gfx.fill(x, boxY, x + w, boxY + 30, PANEL_INNER);
            gfx.outline(x, boxY, w, 30, FRAME_DARK);
        }
        gfx.centeredText(font, "TOTAL BURDEN", x + w / 2, boxY + 3, TEXT_DIM);
        renderTrafficBar(gfx, x + 4, boxY + 13, w - 8, 4, burdenFill);
        gfx.centeredText(font, Component.literal(burden + "%").withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, boxY + 19, burdenColor);
    }

    private void renderTaxBox(GuiGraphicsExtractor gfx, int x, int y, int w,
                              String title, String value, String status, String iconKind,
                              String tagPrefix, int mx, int my, List<ClickRect> rects) {
        int boxH = 38;
        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.INNER_BOX, x, y, w, boxH, 8);
        } else {
            gfx.fill(x, y, x + w, y + boxH, PANEL_INNER);
            gfx.outline(x, y, w, boxH, FRAME_DARK);
        }

        if (USE_ATLAS) {
            switch (iconKind) {
                case "coin"    -> gfx.item(new ItemStack(kingdom.smp.ModItems.GOLD_COIN.get()), x + 2, y + 2);
                case "emerald" -> gfx.item(new ItemStack(Items.EMERALD),            x + 2, y + 2);
                case "land"    -> gfx.item(new ItemStack(Items.GRASS_BLOCK),        x + 2, y + 2);
                default        -> blitSlot(gfx, ConsoleAtlas.ICON_COIN, x + 2, y + 2);
            }
        } else {
            drawTinyIcon(gfx, iconKind, x + 2, y + 3);
        }

        int textIndent = USE_ATLAS ? 20 : 12;
        int statusColor = status.equalsIgnoreCase("FAIR") ? OK_GREEN
                : status.equalsIgnoreCase("STEEP") ? WARN_AMBER : BAD_RED;
        int statusW = font.width(status);
        int titleMaxW = w - textIndent - statusW - 5;
        clipText(gfx, title, x + textIndent, y + 4, titleMaxW, TEXT, true);
        gfx.text(font, status, x + w - statusW - 3, y + 4, statusColor, false);

        // Stepper row
        int stepperY = y + boxH - 14;
        int btnW = 12, btnH = 12;
        int valW = w - btnW * 2 - 8;
        int sx = x + 2;

        ClickRect minus = new ClickRect(sx, stepperY, btnW, btnH, tagPrefix + "_minus");
        renderStepperButton(gfx, minus, "-", mx, my);
        rects.add(minus);

        gfx.fill(sx + btnW + 2, stepperY, sx + btnW + 2 + valW, stepperY + btnH, BG);
        gfx.outline(sx + btnW + 2, stepperY, valW, btnH, FRAME_DARK);
        gfx.centeredText(font, value, sx + btnW + 2 + valW / 2, stepperY + 2, TEXT);

        ClickRect plus = new ClickRect(sx + btnW + 4 + valW, stepperY, btnW, btnH, tagPrefix + "_plus");
        renderStepperButton(gfx, plus, "+", mx, my);
        rects.add(plus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CENTER COLUMN — overview, realm map, decree
    // ─────────────────────────────────────────────────────────────────────────
    private void renderCenterColumn(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                    int mx, int my, List<ClickRect> rects) {
        renderColumnHeader(gfx, x, y, w, "OVERVIEW");
        int by = y + 14;

        // Treasury + Daily Income side-by-side
        int statsH = 64;
        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.INNER_BOX, x, by, w, statsH, 8);
        } else {
            gfx.fill(x, by, x + w, by + statsH, PANEL_INNER);
            gfx.outline(x, by, w, statsH, FRAME_DARK);
        }
        int half = w / 2;
        renderStatTreasury(gfx, x + 2,    by + 4, half - 2);
        renderStatIncome  (gfx, x + half, by + 4, half - 2);
        by += statsH + 4;

        // Royal Decree Preview — fixed height, leaves space below blank
        int decreeH = 64;
        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.PARCHMENT, x, by, w, decreeH, 8);
        } else {
            gfx.fill(x, by, x + w, by + decreeH, 0xFFC8B07A);
            gfx.outline(x, by, w, decreeH, 0xFF5C4A2B);
        }
        // Header centered + decorative rule beneath
        gfx.centeredText(font, Component.literal("ROYAL DECREE").withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, by + 5, 0xFF3A2D14);
        // Inked rule under the header (dark ink with a 1px highlight above)
        gfx.fill(x + 8, by + 14, x + w - 8, by + 15, 0xFF5C4A2B);
        gfx.fill(x + 8, by + 15, x + w - 8, by + 16, 0xFF7A6444);

        // Body — left margin, right margin reserved for the wax seal
        int textLeft = x + 6;
        int sealW = 32;
        int textRight = x + w - sealW - 4;
        int textWidth = textRight - textLeft;

        int changes = pendingChangeCount();
        int delta   = pendingIncomeDelta();
        String risk = pendingRiskLevel();

        // Line 1 — pending change summary
        String summary;
        if (changes == 0)      summary = "No changes pending.";
        else if (changes == 1) summary = decreeFirstChangeLine();
        else                   summary = changes + " changes pending.";
        clipText(gfx, summary, textLeft, by + 19, textWidth, 0xFF3A2D14);

        // Line 2 — net daily income delta
        String deltaStr = (delta >= 0 ? "+" : "") + commaFmt(delta) + " / day";
        clipText(gfx, deltaStr, textLeft, by + 28, textWidth, 0xFF3A2D14);

        // Line 3 — risk level (color depends on tier)
        gfx.text(font, "Risk: ", textLeft, by + 37, 0xFF3A2D14, false);
        int riskColor = risk.equals("LOW") ? OK_GREEN
                      : risk.equals("MED") ? WARN_AMBER : BAD_RED;
        gfx.text(font, Component.literal(risk).withStyle(Style.EMPTY.withBold(true)),
                textLeft + font.width("Risk: "), by + 37, riskColor, false);

        // Wax seal — hand-painted 64×64 PNG, rendered at 32×32 lower on the parchment
        int sealX = x + w - sealW - 2;
        int sealY = by + decreeH - 38;
        gfx.blit(RenderPipelines.GUI_TEXTURED, ConsoleAtlas.WAX_SEAL_TEXTURE,
                sealX, sealY, 0f, 0f, 32, 32,
                ConsoleAtlas.WAX_SEAL_NATIVE, ConsoleAtlas.WAX_SEAL_NATIVE,
                ConsoleAtlas.WAX_SEAL_NATIVE, ConsoleAtlas.WAX_SEAL_NATIVE);

        // Action bar: Cancel (X) on the left, Confirm (✓) on the right
        int slotY = by + decreeH - 18;
        ClickRect cancel = new ClickRect(x + w / 2 - 24, slotY, 20, 16, "decree_cancel");
        renderXButton(gfx, cancel, mx, my);
        rects.add(cancel);

        ClickRect stamp = new ClickRect(x + w / 2 + 4, slotY, 20, 16, "decree_stamp");
        renderCheckButton(gfx, stamp, mx, my);
        rects.add(stamp);
    }

    private void renderStatTreasury(GuiGraphicsExtractor gfx, int x, int y, int w) {
        gfx.text(font, Component.literal("TREASURY").withStyle(Style.EMPTY.withBold(true)),
                x, y, GOLD, false);
        gfx.item(new ItemStack(Items.GOLD_INGOT), x, y + 14);
        clipText(gfx, "12,458", x + 18, y + 18, w - 18, GOLD);
        gfx.item(new ItemStack(Items.EMERALD), x, y + 36);
        clipText(gfx, "1,287", x + 18, y + 40, w - 18, OK_GREEN);
    }

    private void renderStatIncome(GuiGraphicsExtractor gfx, int x, int y, int w) {
        int taxRev   = incomeTax * 96;
        int gateRev  = gateToll * 218;
        int tradeRev = tradeTax * 49;
        int total    = taxRev + gateRev + tradeRev;

        gfx.text(font, Component.literal("DAILY").withStyle(Style.EMPTY.withBold(true)),
                x, y, GOLD, false);
        renderIncomeRow(gfx, x, y + 14, w, "Tax",   "+" + commaFmt(taxRev),   OK_GREEN);
        renderIncomeRow(gfx, x, y + 24, w, "Gate",  "+" + commaFmt(gateRev),  OK_GREEN);
        renderIncomeRow(gfx, x, y + 34, w, "Trade", "+" + commaFmt(tradeRev), OK_GREEN);
        gfx.fill(x, y + 44, x + w - 2, y + 45, FRAME_DARK);
        gfx.text(font, Component.literal("+" + commaFmt(total)).withStyle(Style.EMPTY.withBold(true)),
                x, y + 48, GOLD, false);
    }

    private static String commaFmt(int n) {
        return String.format("%,d", n);
    }

    private static String statusFor(int value, int fairCap, int steepCap) {
        if (value <= fairCap) return "FAIR";
        if (value <= steepCap) return "STEEP";
        return "HARSH";
    }

    /** When exactly one field has a pending change, format it for the decree. */
    private String decreeFirstChangeLine() {
        if (incomeTax != cIncomeTax) return "Income: " + cIncomeTax + "% \u2192 " + incomeTax + "%";
        if (landTax != cLandTax)     return "Land: "   + cLandTax   + " \u2192 " + landTax;
        if (tradeTax != cTradeTax)   return "Trade: "  + cTradeTax  + "% \u2192 " + tradeTax + "%";
        if (gateToll != cGateToll)   return "Gate: "   + cGateToll  + " \u2192 " + gateToll;
        if (capitalToll != cCapitalToll)   return "Capital: "  + cCapitalToll  + " \u2192 " + capitalToll;
        if (northToll != cNorthToll)       return "North: "    + cNorthToll    + " \u2192 " + northToll;
        if (southToll != cSouthToll)       return "South: "    + cSouthToll    + " \u2192 " + southToll;
        if (frontierToll != cFrontierToll) return "Frontier: " + cFrontierToll + " \u2192 " + frontierToll;
        if (gateFreeCitizens != cGateFree)         return "Free citizens: " + (gateFreeCitizens ? "on" : "off");
        if (gateAllyDiscount != cGateAlly)         return "Ally discount: " + (gateAllyDiscount ? "on" : "off");
        if (portalAllyDiscount != cPortalAlly)     return "Portal ally: "   + (portalAllyDiscount ? "on" : "off");
        if (portalEnemySurcharge != cPortalEnemy)  return "Enemy charge: "  + (portalEnemySurcharge ? "on" : "off");
        return "";
    }

    private void renderIncomeRow(GuiGraphicsExtractor gfx, int x, int y, int w, String label, String value, int color) {
        gfx.text(font, label, x, y, TEXT, false);
        int valW = font.width(value);
        gfx.text(font, value, x + w - valW - 2, y, color, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIGHT COLUMN — Tolls
    // ─────────────────────────────────────────────────────────────────────────
    private void renderTollsColumn(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                   int mx, int my, List<ClickRect> rects) {
        renderColumnHeader(gfx, x, y, w, "TOLLS");
        int by = y + 14;

        // Gate Toll box — 56 tall: header row (18) + stepper row (12) + toggle row (14) + padding
        int gateBoxH = 56;
        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.INNER_BOX, x, by, w, gateBoxH, 8);
        } else {
            gfx.fill(x, by, x + w, by + gateBoxH, PANEL_INNER);
            gfx.outline(x, by, w, gateBoxH, FRAME_DARK);
        }
        // Header
        gfx.item(new ItemStack(Items.IRON_BARS), x + 4, by + 4);
        gfx.text(font, Component.literal("GATE TOLL").withStyle(Style.EMPTY.withBold(true)),
                x + 22, by + 8, TEXT, false);

        // Stepper row — below the icon, with 4px clear space on each side from the rim
        int gsy = by + 22;
        ClickRect gMinus = new ClickRect(x + 4, gsy, 12, 12, "gate_minus");
        renderStepperButton(gfx, gMinus, "-", mx, my);
        rects.add(gMinus);
        int gValX  = x + 4 + 12 + 2;            // after minus + 2 px gap
        int gValRX = x + w - 4 - 12 - 2;        // before plus + 2 px gap
        int gValW  = gValRX - gValX;
        gfx.fill(gValX, gsy, gValRX, gsy + 12, BG);
        gfx.outline(gValX, gsy, gValW, 12, FRAME_DARK);
        // Centered value with coin to its right
        String gateVal = String.valueOf(gateToll);
        int gateValTextW = font.width(gateVal);
        int coinW = 6;
        int contentW = gateValTextW + 3 + coinW;
        int contentX = gValX + (gValW - contentW) / 2;
        gfx.text(font, gateVal, contentX, gsy + 2, TEXT, false);
        drawTinyIcon(gfx, "coin", contentX + gateValTextW + 3, gsy + 3);
        ClickRect gPlus = new ClickRect(gValRX + 2, gsy, 12, 12, "gate_plus");
        renderStepperButton(gfx, gPlus, "+", mx, my);
        rects.add(gPlus);

        // Toggle row — below the stepper
        int tY = by + 38;
        int tW = (w - 12) / 2;
        ClickRect t1 = new ClickRect(x + 4, tY, tW, 14, "gate_free_citizens");
        renderToggleButton(gfx, t1, "Citizens", gateFreeCitizens, mx, my);
        rects.add(t1);
        ClickRect t2 = new ClickRect(x + 8 + tW, tY, tW, 14, "gate_ally_discount");
        renderToggleButton(gfx, t2, "Allies", gateAllyDiscount, mx, my);
        rects.add(t2);

        by += gateBoxH + 4;

        // Portal Toll box — fills remainder
        int pBoxH = h - (by - y) - 2;
        if (USE_ATLAS) {
            blit9Slice(gfx, ConsoleAtlas.INNER_BOX, x, by, w, pBoxH, 8);
        } else {
            gfx.fill(x, by, x + w, by + pBoxH, PANEL_INNER);
            gfx.outline(x, by, w, pBoxH, FRAME_DARK);
        }
        gfx.text(font, Component.literal("PORTAL TOLL").withStyle(Style.EMPTY.withBold(true)),
                x + 4, by + 4, TEXT, false);

        String[] portals = {"Capital", "North", "South", "Frontier"};
        int[]    values  = {capitalToll, northToll, southToll, frontierToll};
        int rowY = by + 13;
        for (int i = 0; i < portals.length; i++) {
            renderPortalRow(gfx, x + 2, rowY, w - 4, portals[i], values[i],
                    "portal_" + portals[i].toLowerCase(), mx, my, rects);
            rowY += 13;
        }

        int pTY = rowY + 3;
        int pTW = (w - 6) / 2;
        ClickRect pT1 = new ClickRect(x + 2, pTY, pTW, 14, "portal_ally_discount");
        renderToggleButton(gfx, pT1, "Allies", portalAllyDiscount, mx, my);
        rects.add(pT1);
        ClickRect pT2 = new ClickRect(x + 4 + pTW, pTY, pTW, 14, "portal_enemy_surcharge");
        renderToggleButton(gfx, pT2, "Enemies", portalEnemySurcharge, mx, my);
        rects.add(pT2);
    }

    private void renderPortalRow(GuiGraphicsExtractor gfx, int x, int y, int w,
                                 String name, int value, String tagPrefix,
                                 int mx, int my, List<ClickRect> rects) {
        gfx.item(new ItemStack(Items.ENDER_EYE), x - 1, y - 3);
        int btnW = 10;
        int valBoxW = 24;
        int nameMaxW = w - 14 - valBoxW - btnW * 2 - 2;
        clipText(gfx, name, x + 14, y + 2, nameMaxW, TEXT, false);
        int rightX = x + w;

        ClickRect minus = new ClickRect(rightX - btnW * 2 - valBoxW - 2, y, btnW, 11,
                tagPrefix + "_minus");
        renderStepperButton(gfx, minus, "-", mx, my);
        rects.add(minus);

        int valX = rightX - btnW - valBoxW - 1;
        gfx.fill(valX, y, valX + valBoxW, y + 11, BG);
        gfx.outline(valX, y, valBoxW, 11, FRAME_DARK);
        // Value text right-aligned; coin pinned to the right edge with 1px pad
        String valStr = String.valueOf(value);
        int coinW = 6;
        int rightPad = 1;
        int valTextW = font.width(valStr);
        int valTextX = valX + valBoxW - rightPad - coinW - 2 - valTextW;
        gfx.text(font, valStr, valTextX, y + 2, TEXT, false);
        drawTinyIcon(gfx, "coin", valX + valBoxW - rightPad - coinW, y + 3);

        ClickRect plus = new ClickRect(rightX - btnW, y, btnW, 11, tagPrefix + "_plus");
        renderStepperButton(gfx, plus, "+", mx, my);
        rects.add(plus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared
    // ─────────────────────────────────────────────────────────────────────────
    private void clipText(GuiGraphicsExtractor gfx, String text, int x, int y, int maxW, int color) {
        clipText(gfx, text, x, y, maxW, color, false);
    }

    private void clipText(GuiGraphicsExtractor gfx, String text, int x, int y, int maxW, int color, boolean bold) {
        String safe = font.width(text) <= maxW ? text : font.plainSubstrByWidth(text, maxW);
        if (bold) {
            gfx.text(font, Component.literal(safe).withStyle(Style.EMPTY.withBold(true)), x, y, color, false);
        } else {
            gfx.text(font, safe, x, y, color, false);
        }
    }

    // ── Tiny pixel glyphs (used in narrow contexts where 16x16 items would overlap) ──
    private void drawCrownGlyph(GuiGraphicsExtractor gfx, int x, int y) {
        // 7x5 crown silhouette
        gfx.fill(x,     y,     x + 1, y + 1, GOLD);
        gfx.fill(x + 3, y,     x + 4, y + 1, GOLD);
        gfx.fill(x + 6, y,     x + 7, y + 1, GOLD);
        gfx.fill(x,     y + 1, x + 7, y + 4, GOLD);
        gfx.fill(x + 1, y + 2, x + 2, y + 3, 0xFFCC9933);
        gfx.fill(x + 5, y + 2, x + 6, y + 3, 0xFFCC9933);
    }

    private void drawTinyIcon(GuiGraphicsExtractor gfx, String kind, int x, int y) {
        switch (kind) {
            case "coin" -> {
                // 6x6 gold coin
                gfx.fill(x + 1, y,     x + 5, y + 1, 0xFFB8860B);
                gfx.fill(x,     y + 1, x + 6, y + 5, 0xFFFFCC44);
                gfx.fill(x + 1, y + 5, x + 5, y + 6, 0xFFB8860B);
                gfx.fill(x + 1, y + 1, x + 2, y + 2, 0xFFFFEE99);
                gfx.fill(x + 3, y + 2, x + 4, y + 4, 0xFFCC9933);
            }
            case "land" -> {
                // 6x6 grass/dirt cube face
                gfx.fill(x, y,     x + 6, y + 2, 0xFF55AA33);
                gfx.fill(x, y + 2, x + 6, y + 6, 0xFF8B6414);
                gfx.fill(x + 1, y, x + 2, y + 1, 0xFF77CC55);
                gfx.fill(x + 4, y, x + 5, y + 1, 0xFF77CC55);
            }
            case "emerald" -> {
                // 6x6 emerald
                gfx.fill(x + 2, y,     x + 4, y + 1, 0xFF22DD66);
                gfx.fill(x + 1, y + 1, x + 5, y + 5, 0xFF22DD66);
                gfx.fill(x + 2, y + 5, x + 4, y + 6, 0xFF22DD66);
                gfx.fill(x + 2, y + 1, x + 3, y + 3, 0xFFAAFFCC);
            }
            case "gate" -> {
                // 6x6 portcullis (3 vertical bars + crossbar)
                gfx.fill(x,     y,     x + 6, y + 1, 0xFF888888);
                gfx.fill(x + 1, y + 1, x + 2, y + 6, 0xFFAAAAAA);
                gfx.fill(x + 3, y + 1, x + 4, y + 6, 0xFFAAAAAA);
                gfx.fill(x + 5, y + 1, x + 6, y + 6, 0xFFAAAAAA);
            }
            case "portal" -> {
                // 6x6 ender ring
                gfx.fill(x + 1, y,     x + 5, y + 1, 0xFF7755CC);
                gfx.fill(x,     y + 1, x + 1, y + 5, 0xFF7755CC);
                gfx.fill(x + 5, y + 1, x + 6, y + 5, 0xFF7755CC);
                gfx.fill(x + 1, y + 5, x + 5, y + 6, 0xFF7755CC);
                gfx.fill(x + 2, y + 2, x + 4, y + 4, 0xFF110A2E);
            }
        }
    }

    private void renderColumnHeader(GuiGraphicsExtractor gfx, int x, int y, int w, String label) {
        if (USE_ATLAS) {
            blitSlotStretch(gfx, ConsoleAtlas.BANNER_RIBBON, x, y - 2, w, 14);
        } else {
            gfx.fill(x, y, x + w, y + 12, TITLE_BG);
            gfx.outline(x, y, w, 12, FRAME_DARK);
        }
        String safe = font.width(label) <= w - 4 ? label : font.plainSubstrByWidth(label, w - 4);
        gfx.centeredText(font, Component.literal(safe).withStyle(Style.EMPTY.withBold(true)),
                x + w / 2, y + 2, USE_ATLAS ? GOLD_LIGHT : GOLD);
    }

    private static final int GOLD_LIGHT = 0xFFFFEE99;

    private void renderStepperButton(GuiGraphicsExtractor gfx, ClickRect r, String glyph, int mx, int my) {
        boolean hover = r.contains(mx, my);
        if (USE_ATLAS) {
            ConsoleAtlas.Slot slot = hover ? ConsoleAtlas.STEPPER_HOVER : ConsoleAtlas.STEPPER_IDLE;
            blitSlotStretch(gfx, slot, r.x, r.y, r.w, r.h);
        } else {
            int bg = hover ? 0xFF4A4A4A : 0xFF303030;
            gfx.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
            gfx.outline(r.x, r.y, r.w, r.h, hover ? FRAME : FRAME_DARK);
            gfx.fill(r.x + 1, r.y + 1, r.x + r.w - 1, r.y + 2, hover ? 0xFF666666 : 0xFF454545);
        }
        // Hand-drawn glyphs for pixel-perfect centering (vanilla font's "-"/"+" sit off-center)
        int cx = r.x + r.w / 2;
        int cy = r.y + r.h / 2;
        if ("-".equals(glyph)) {
            gfx.fill(cx - 3, cy - 1, cx + 3, cy + 1, TEXT);
        } else if ("+".equals(glyph)) {
            gfx.fill(cx - 3, cy - 1, cx + 3, cy + 1, TEXT);
            gfx.fill(cx - 1, cy - 3, cx + 1, cy + 3, TEXT);
        } else {
            gfx.centeredText(font, glyph, cx, r.y + (r.h - 8) / 2, TEXT);
        }
    }

    private void renderToggleButton(GuiGraphicsExtractor gfx, ClickRect r, String label,
                                    boolean on, int mx, int my) {
        boolean hover = r.contains(mx, my);
        if (USE_ATLAS) {
            ConsoleAtlas.Slot slot;
            if (on) slot = hover ? ConsoleAtlas.TOGGLE_ON_HOVER : ConsoleAtlas.TOGGLE_ON_IDLE;
            else    slot = hover ? ConsoleAtlas.TOGGLE_OFF_HOVER : ConsoleAtlas.TOGGLE_OFF_IDLE;
            blitSlotStretch(gfx, slot, r.x, r.y, r.w, r.h);
        } else {
            int bg = on ? (hover ? 0xFF3D5A2C : 0xFF304521)
                        : (hover ? 0xFF3A3A3A : 0xFF2A2A2A);
            gfx.fill(r.x, r.y, r.x + r.w, r.y + r.h, bg);
            gfx.outline(r.x, r.y, r.w, r.h, on ? OK_GREEN : FRAME_DARK);
        }
        int textColor = on ? 0xFFCCFFCC : TEXT_DIM;
        int innerW = r.w - 12;
        String safe = font.width(label) <= innerW ? label : font.plainSubstrByWidth(label, innerW);
        gfx.text(font, safe, r.x + 3, r.y + 3, textColor, false);
        if (on) gfx.text(font, "\u2713", r.x + r.w - 8, r.y + 3, OK_GREEN, false);
    }

    private void renderXButton(GuiGraphicsExtractor gfx, ClickRect r, int mx, int my) {
        boolean hover = r.contains(mx, my);
        gfx.fill(r.x, r.y, r.x + r.w, r.y + r.h, hover ? 0xFF6A2A2A : 0xFF3A1A1A);
        gfx.outline(r.x, r.y, r.w, r.h, hover ? FRAME : FRAME_DARK);

        int cx = r.x + r.w / 2;
        int cy = r.y + r.h / 2;
        int main   = 0xFFFF5555;     // bright wax red
        int shadow = 0xFF8B1F1F;     // deep red shadow
        int hi     = 0xFFFFCCCC;     // pink highlight
        int reach  = 4;

        // Pass 1 — shadow halo (3×3 blocks, offset down-right by 1 px)
        for (int i = -reach; i <= reach; i++) {
            gfx.fill(cx + i,     cy + i,     cx + i + 3, cy + i + 3, shadow);
            gfx.fill(cx + i,     cy - i,     cx + i + 3, cy - i + 3, shadow);
        }
        // Pass 2 — main strokes (3×3 blocks centered on each diagonal step)
        for (int i = -reach; i <= reach; i++) {
            gfx.fill(cx + i - 1, cy + i - 1, cx + i + 2, cy + i + 2, main);
            gfx.fill(cx + i - 1, cy - i - 1, cx + i + 2, cy - i + 2, main);
        }
        // Pass 3 — 1-pixel highlight along the centerline of each stroke
        for (int i = -reach + 1; i <= reach - 1; i++) {
            gfx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, hi);
            gfx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, hi);
        }
    }

    private void renderCheckButton(GuiGraphicsExtractor gfx, ClickRect r, int mx, int my) {
        boolean hover = r.contains(mx, my);
        gfx.fill(r.x, r.y, r.x + r.w, r.y + r.h, hover ? 0xFF2A6A2A : 0xFF1A3A1A);
        gfx.outline(r.x, r.y, r.w, r.h, hover ? FRAME : FRAME_DARK);

        int cx = r.x + r.w / 2;
        int cy = r.y + r.h / 2;
        int main   = 0xFF55DD55;
        int shadow = 0xFF1F6E1F;
        int hi     = 0xFFCCFFCC;

        // Elbow at (cx-1, cy+2). Short leg 4 steps, long leg 6 steps.
        // Pass 1 — shadow halo
        for (int i = 0; i <= 3; i++) {
            int dx = -4 + i, dy = -1 + i;
            gfx.fill(cx + dx,     cy + dy,     cx + dx + 3, cy + dy + 3, shadow);
        }
        for (int i = 0; i <= 5; i++) {
            int dx = -1 + i, dy = 2 - i;
            gfx.fill(cx + dx,     cy + dy,     cx + dx + 3, cy + dy + 3, shadow);
        }
        // Pass 2 — main strokes (3×3 centered)
        for (int i = 0; i <= 3; i++) {
            int dx = -4 + i, dy = -1 + i;
            gfx.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, main);
        }
        for (int i = 0; i <= 5; i++) {
            int dx = -1 + i, dy = 2 - i;
            gfx.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, main);
        }
        // Pass 3 — 1-pixel highlight along the centerline of each leg
        for (int i = 0; i <= 3; i++) {
            int dx = -4 + i, dy = -1 + i;
            gfx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, hi);
        }
        for (int i = 0; i <= 5; i++) {
            int dx = -1 + i, dy = 2 - i;
            gfx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, hi);
        }
    }

    /** Render a sprite from the atlas at (x, y), at the slot's native size. */
    private void blitSlot(GuiGraphicsExtractor gfx, ConsoleAtlas.Slot s, int x, int y) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, ConsoleAtlas.TEXTURE,
                x, y, (float) s.u(), (float) s.v(),
                s.w(), s.h(), s.w(), s.h(),
                ConsoleAtlas.ATLAS_W, ConsoleAtlas.ATLAS_H);
    }

    /** Stretch a sprite from the atlas to a target size. Used for 9-slice middles. */
    private void blitSlotStretch(GuiGraphicsExtractor gfx, ConsoleAtlas.Slot s, int x, int y, int w, int h) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, ConsoleAtlas.TEXTURE,
                x, y, (float) s.u(), (float) s.v(),
                w, h, s.w(), s.h(),
                ConsoleAtlas.ATLAS_W, ConsoleAtlas.ATLAS_H);
    }

    /** 9-slice blit: corners stay fixed, edges stretch 1D, center stretches 2D. */
    private void blit9Slice(GuiGraphicsExtractor gfx, ConsoleAtlas.Slot s, int x, int y, int w, int h, int c) {
        int sx = s.u(), sy = s.v(), sw = s.w(), sh = s.h();
        int midSrcW = sw - c * 2;
        int midSrcH = sh - c * 2;
        int midDstW = w - c * 2;
        int midDstH = h - c * 2;
        // Corners
        blitRaw(gfx, sx,             sy,             c,        c,        x,             y,             c,        c);
        blitRaw(gfx, sx + sw - c,    sy,             c,        c,        x + w - c,     y,             c,        c);
        blitRaw(gfx, sx,             sy + sh - c,    c,        c,        x,             y + h - c,     c,        c);
        blitRaw(gfx, sx + sw - c,    sy + sh - c,    c,        c,        x + w - c,     y + h - c,     c,        c);
        // Top + bottom edges
        if (midDstW > 0 && midSrcW > 0) {
            blitRaw(gfx, sx + c, sy,             midSrcW, c, x + c, y,             midDstW, c);
            blitRaw(gfx, sx + c, sy + sh - c,    midSrcW, c, x + c, y + h - c,     midDstW, c);
        }
        // Left + right edges
        if (midDstH > 0 && midSrcH > 0) {
            blitRaw(gfx, sx,             sy + c, c, midSrcH, x,             y + c, c, midDstH);
            blitRaw(gfx, sx + sw - c,    sy + c, c, midSrcH, x + w - c,     y + c, c, midDstH);
        }
        // Center
        if (midDstW > 0 && midDstH > 0 && midSrcW > 0 && midSrcH > 0) {
            blitRaw(gfx, sx + c, sy + c, midSrcW, midSrcH, x + c, y + c, midDstW, midDstH);
        }
    }

    private void blitRaw(GuiGraphicsExtractor gfx, int u, int v, int srcW, int srcH,
                         int x, int y, int dstW, int dstH) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, ConsoleAtlas.TEXTURE,
                x, y, (float) u, (float) v,
                dstW, dstH, srcW, srcH,
                ConsoleAtlas.ATLAS_W, ConsoleAtlas.ATLAS_H);
    }

    private void renderTrafficBar(GuiGraphicsExtractor gfx, int x, int y, int w, int h, float fillPct) {
        int seg = w / 4;
        gfx.fill(x,           y, x + seg,     y + h, 0xFF55BB55);
        gfx.fill(x + seg,     y, x + seg * 2, y + h, 0xFFAACC44);
        gfx.fill(x + seg * 2, y, x + seg * 3, y + h, 0xFFDD9933);
        gfx.fill(x + seg * 3, y, x + w,       y + h, 0xFFCC3333);
        int markerX = x + Math.round(fillPct * w);
        gfx.fill(markerX - 1, y - 1, markerX + 1, y + h + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0 && clickRects != null) {
            for (ClickRect r : clickRects) {
                if (r.contains(event.x(), event.y())) {
                    handleClick(r.tag());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    private void handleClick(String tag) {
        switch (tag) {
            case "tax_income_minus"        -> incomeTax  = Math.max(0,  incomeTax  - 1);
            case "tax_income_plus"         -> incomeTax  = Math.min(100, incomeTax  + 1);
            case "tax_land_minus"          -> landTax    = Math.max(0,  landTax    - 1);
            case "tax_land_plus"           -> landTax    = Math.min(99, landTax    + 1);
            case "tax_trade_minus"         -> tradeTax   = Math.max(0,  tradeTax   - 1);
            case "tax_trade_plus"          -> tradeTax   = Math.min(100, tradeTax  + 1);
            case "gate_minus"              -> gateToll   = Math.max(0,  gateToll   - 1);
            case "gate_plus"               -> gateToll   = Math.min(99, gateToll   + 1);
            case "portal_capital_minus"    -> capitalToll  = Math.max(0,  capitalToll  - 1);
            case "portal_capital_plus"     -> capitalToll  = Math.min(99, capitalToll  + 1);
            case "portal_north_minus"      -> northToll    = Math.max(0,  northToll    - 1);
            case "portal_north_plus"       -> northToll    = Math.min(99, northToll    + 1);
            case "portal_south_minus"      -> southToll    = Math.max(0,  southToll    - 1);
            case "portal_south_plus"       -> southToll    = Math.min(99, southToll    + 1);
            case "portal_frontier_minus"   -> frontierToll = Math.max(0,  frontierToll - 1);
            case "portal_frontier_plus"    -> frontierToll = Math.min(99, frontierToll + 1);
            case "gate_free_citizens"      -> gateFreeCitizens     = !gateFreeCitizens;
            case "gate_ally_discount"      -> gateAllyDiscount     = !gateAllyDiscount;
            case "portal_ally_discount"    -> portalAllyDiscount   = !portalAllyDiscount;
            case "portal_enemy_surcharge"  -> portalEnemySurcharge = !portalEnemySurcharge;
            case "decree_stamp"  -> commitDecree();
            case "decree_cancel" -> revertDecree();
            default -> {}
        }
    }
}
