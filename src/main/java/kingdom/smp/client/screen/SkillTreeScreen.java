package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.RespecSkillPayload;
import kingdom.smp.net.SpendSkillPointPayload;
import kingdom.smp.skill.ClientSkillData;
import kingdom.smp.skill.ClientUseSkillData;
import kingdom.smp.skill.MiningGating;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.useskill.UseSkill;
import net.minecraft.world.level.ItemLike;
import net.minecraft.ChatFormatting;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Custom skills screen with two tabs:
 * <ul>
 *   <li><b>Mastery</b> — the 8 profession perk trees as linear 5-rank chains. Hover any node
 *       for a tooltip; click an affordable next-rank node to spend a point.</li>
 *   <li><b>Practice</b> — the use-to-level skills (Pickpocket / Sneak / Fishing) that grow
 *       through use, shown with level + XP progress. Display-only.</li>
 * </ul>
 *
 * Styled to match the rest of the mod's vanilla-look GUIs (beveled gray container panel).
 * Opened from the "Skills" button on the survival inventory screen.
 */
public class SkillTreeScreen extends Screen {

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 210;
    private static final int ROW_HEIGHT = 22;
    private static final int NODE_SIZE = 12;
    private static final int NODE_SPACING = 32;
    private static final int CHAIN_X_OFFSET = 110;

    /** Tabs occupy py+6..py+22; the divider sits at py+25; content starts py+30. */
    private static final int TAB_TOP = 6;
    private static final int TAB_HEIGHT = 16;
    private static final int CONTENT_TOP = 30;

    // ── Vanilla container-panel palette ──────────────────────────────────
    private static final int COL_PANEL_BG     = 0xFFC6C6C6;
    private static final int COL_PANEL_HI     = 0xFFFFFFFF;
    private static final int COL_PANEL_LO     = 0xFF555555;
    private static final int COL_PANEL_BORDER = 0xFF000000;
    private static final int COL_TAB_INACTIVE = 0xFF8B8B8B;

    // ── Text on the gray panel ───────────────────────────────────────────
    private static final int COLOR_TEXT        = 0xFF404040;
    private static final int COLOR_TEXT_DIM    = 0xFF6B6B6B;
    private static final int COLOR_HEADER_RULE = 0xFF555555;
    private static final int COLOR_ROW_ALT     = 0x18000000;
    private static final int COLOR_ROW_HOVER   = 0x55FFCC55;
    private static final int COLOR_OK          = 0xFF2E7D32;
    private static final int COLOR_NO          = 0xFFB23A3A;
    private static final int COLOR_MAXED       = 0xFFB8860B;

    // Tooltip stays dark (vanilla tooltips are dark regardless of panel).
    private static final int COLOR_TEXT_BRIGHT = 0xFFFFFFFF;
    private static final int COLOR_TIP_BG      = 0xF0050508;
    private static final int COLOR_TIP_BORDER  = 0xFF8A8AA8;

    private static final int NODE_LOCKED_FILL  = 0xFF26262E;
    private static final int NODE_LOCKED_HI    = 0xFF38384A;
    private static final int NODE_LOCKED_LO    = 0xFF14141A;
    private static final int NODE_UNLOCK_FILL  = 0xFFD9A53A;
    private static final int NODE_UNLOCK_HI    = 0xFFFFE07A;
    private static final int NODE_UNLOCK_LO    = 0xFF7A5A1A;
    private static final int NODE_NEXT_OK_FILL = 0xFF3FAE4D;
    private static final int NODE_NEXT_OK_HI   = 0xFF7BD884;
    private static final int NODE_NEXT_OK_LO   = 0xFF1F5C29;
    private static final int NODE_NEXT_NO_FILL = 0xFF8A2E2E;
    private static final int NODE_NEXT_NO_HI   = 0xFFB85959;
    private static final int NODE_NEXT_NO_LO   = 0xFF4A1818;

    private static final int LINE_LOCKED = 0xFF777777;
    private static final int LINE_DONE   = 0xFFD9A53A;

    // ── Practice (use-skill) bar colors ──────────────────────────────────
    private static final int BAR_BORDER = 0xFF373737;
    private static final int BAR_BG     = 0xFF1A1A1A;
    private static final int BAR_FILL   = 0xFF5FCF66;
    private static final int BAR_MAXED  = 0xFFE0B83A;

    // ── Gate-ladder colors ───────────────────────────────────────────────
    private static final int GATE_RAIL_DONE   = 0xFFD9A53A;
    private static final int GATE_RAIL_LOCKED = 0xFF777777;
    private static final int GATE_ROW_CURRENT = 0x553FAE4D;
    private static final int GATE_VEIL        = 0xC8101014;

    private static final String[] TAB_LABELS = { "Mastery", "Practice", "Gates" };
    private static final int TAB_MASTERY = 0;
    private static final int TAB_PRACTICE = 1;
    private static final int TAB_GATES = 2;

    private final Map<Profession, ItemStack> icons = new EnumMap<>(Profession.class);
    private final Map<UseSkill, ItemStack> useSkillIcons = new EnumMap<>(UseSkill.class);
    private final List<List<ItemStack>> gateIcons = new ArrayList<>();
    private final @Nullable Screen returnTo;

    private int activeTab = TAB_MASTERY;
    private @Nullable Button respecButton;

    public SkillTreeScreen() { this(null); }

    public SkillTreeScreen(@Nullable Screen returnTo) {
        super(Component.literal("Skills"));
        this.returnTo = returnTo;
        icons.put(Profession.BLACKSMITHING, new ItemStack(Items.ANVIL));
        icons.put(Profession.FARMING,       new ItemStack(Items.WHEAT));
        icons.put(Profession.COOKING,       new ItemStack(Items.BREAD));
        icons.put(Profession.ALCHEMY,       new ItemStack(Items.BREWING_STAND));
        icons.put(Profession.FISHING,       new ItemStack(Items.FISHING_ROD));
        icons.put(Profession.ENCHANTING,    new ItemStack(Items.ENCHANTED_BOOK));
        icons.put(Profession.MINING,        new ItemStack(Items.DIAMOND_PICKAXE));
        icons.put(Profession.TRADING,       new ItemStack(Items.EMERALD));

        useSkillIcons.put(UseSkill.PICKPOCKET, new ItemStack(Items.GOLD_INGOT));
        useSkillIcons.put(UseSkill.SNEAK,      new ItemStack(Items.LEATHER_BOOTS));
        useSkillIcons.put(UseSkill.FISHING,    new ItemStack(Items.FISHING_ROD));

        for (MiningGating.GateTier tier : MiningGating.tiers()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemLike it : tier.icons()) stacks.add(new ItemStack(it));
            gateIcons.add(stacks);
        }
    }

    private int panelX() { return (width - PANEL_WIDTH) / 2; }

    private int panelY() {
        // Clamp so the panel never renders off the top of the screen.
        return Math.max(4, (height - PANEL_HEIGHT - 30) / 2);
    }

    @Override
    protected void init() {
        super.init();
        int btnY = panelY() + PANEL_HEIGHT + 6;
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(width / 2 - 84, btnY, 80, 20)
                .build());
        respecButton = addRenderableWidget(Button.builder(Component.literal("Respec All"), b -> requestRespecAll())
                .bounds(width / 2 + 4, btnY, 80, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Refunds every spent profession point — minus 1 as the cost. "
                        + "Clears all profession progress.")))
                .build());
    }

    private void requestRespecAll() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.gui.screens.ConfirmScreen confirm = new net.minecraft.client.gui.screens.ConfirmScreen(
                accepted -> {
                    if (accepted) {
                        ClientPayloads.sendToServer(new RespecSkillPayload());
                    }
                    mc.setScreen(this);
                },
                Component.literal("Respec All Skills?").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)),
                Component.literal("Refunds every spent profession point — minus 1 as the cost. "
                        + "All profession progress will be cleared.")
        );
        mc.setScreen(confirm);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int px = panelX();
        int py = panelY();

        // Respec only applies to the Mastery (profession) tab.
        if (respecButton != null) respecButton.visible = activeTab == TAB_MASTERY;

        // Vanilla container panel
        drawVanillaPanel(gfx, px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT);

        // Tabs
        int[] tabX = tabXs(px);
        for (int i = 0; i < TAB_LABELS.length; i++) {
            drawTab(gfx, tabX[i], py + TAB_TOP, font.width(TAB_LABELS[i]) + 16, TAB_HEIGHT,
                    TAB_LABELS[i], i == activeTab);
        }

        // Mastery shows the unspent-points readout on the right of the tab row.
        if (activeTab == TAB_MASTERY) {
            String unspentLabel = ClientSkillData.hasReceived()
                    ? ClientSkillData.unspentPoints() + " unspent"
                    : "loading...";
            int unspentColor = ClientSkillData.hasReceived() && ClientSkillData.unspentPoints() > 0
                    ? COLOR_OK : COLOR_TEXT_DIM;
            int unspentX = px + PANEL_WIDTH - 12 - font.width(unspentLabel);
            gfx.text(font, unspentLabel, unspentX, py + TAB_TOP + 4, unspentColor, false);
        } else if (activeTab == TAB_GATES) {
            ProfessionRank mining = ClientSkillData.rankFor(Profession.MINING);
            String label = "Mining: " + (mining == null ? "Untrained" : mining.displayName());
            int labelX = px + PANEL_WIDTH - 12 - font.width(label);
            gfx.text(font, label, labelX, py + TAB_TOP + 4,
                    mining == null ? COLOR_TEXT_DIM : COLOR_MAXED, false);
        }

        gfx.fill(px + 8, py + 25, px + PANEL_WIDTH - 8, py + 26, COLOR_HEADER_RULE);

        HoverTarget hover = null;
        List<Component> gateTip = null;
        if (activeTab == TAB_MASTERY) {
            int rowY = py + CONTENT_TOP;
            int rowIndex = 0;
            for (Profession profession : Profession.values()) {
                HoverTarget rowHover = renderRow(gfx, px, rowY, profession, rowIndex, mouseX, mouseY);
                if (rowHover != null) hover = rowHover;
                rowY += ROW_HEIGHT;
                rowIndex++;
            }
        } else if (activeTab == TAB_PRACTICE) {
            renderPractice(gfx, px, py + CONTENT_TOP);
        } else {
            gateTip = renderGates(gfx, px, py + CONTENT_TOP, mouseX, mouseY);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        // Tooltip pass — drawn on top of everything else
        if (hover != null) {
            renderNodeTooltip(gfx, hover, mouseX, mouseY);
        } else if (gateTip != null) {
            drawTooltipBox(gfx, gateTip, mouseX + 12, mouseY - 6);
        }
    }

    // ── Practice tab (use-to-level skills) ─────────────────────────────────

    private void renderPractice(GuiGraphicsExtractor gfx, int px, int top) {
        if (!ClientUseSkillData.hasReceived()) {
            gfx.text(font, "loading...", px + 12, top + 4, COLOR_TEXT_DIM, false);
            return;
        }
        int rowH = 34;
        int y = top + 2;
        for (UseSkill skill : UseSkill.values()) {
            int level = ClientUseSkillData.levelFor(skill);
            boolean maxed = level >= skill.maxLevel();
            float progress = ClientUseSkillData.progressFor(skill);

            gfx.item(useSkillIcons.get(skill), px + 10, y);
            gfx.text(font, skill.displayName(), px + 32, y + 2, COLOR_TEXT, false);

            String lvl = maxed ? "MAX" : "Lv " + level;
            gfx.text(font, lvl, px + PANEL_WIDTH - 12 - font.width(lvl), y + 2,
                    maxed ? COLOR_MAXED : COLOR_TEXT, false);

            // XP progress bar (vanilla inset)
            int barX0 = px + 32, barX1 = px + PANEL_WIDTH - 12, barY = y + 15, barH = 8;
            gfx.fill(barX0 - 1, barY - 1, barX1 + 1, barY + barH + 1, BAR_BORDER);
            gfx.fill(barX0, barY, barX1, barY + barH, BAR_BG);
            int fillW = Math.round((barX1 - barX0) * progress);
            gfx.fill(barX0, barY, barX0 + fillW, barY + barH, maxed ? BAR_MAXED : BAR_FILL);

            y += rowH;
        }
    }

    // ── Gates tab (mining ore ladder) ──────────────────────────────────────

    /**
     * Vertical "gate ladder": one rung per mining tier, from the free baseline up to the
     * Veinbreaker capstone. Reached rungs show bright ore icons + a green requirement label;
     * locked rungs dim the ore icons behind a gold padlock and show the rank still needed.
     * The player's current rung is highlighted. Returns the tooltip for a hovered rung, if any.
     */
    private List<Component> renderGates(GuiGraphicsExtractor gfx, int px, int top, int mouseX, int mouseY) {
        if (!ClientSkillData.hasReceived()) {
            gfx.text(font, "loading...", px + 12, top + 4, COLOR_TEXT_DIM, false);
            return null;
        }
        List<MiningGating.GateTier> tiers = MiningGating.tiers();
        ProfessionRank mining = ClientSkillData.rankFor(Profession.MINING);
        int miningOrder = mining == null ? -1 : mining.order();

        int rowH = 29;
        int railX = px + 22;
        int rowLeft = px + 6;
        int rowRight = px + PANEL_WIDTH - 6;

        // ── Pass 1: row backgrounds + connecting rails ──
        // Drawn first so the rail lines sit *behind* the rung nodes that follow.
        int prevCenterY = -1;
        for (int i = 0; i < tiers.size(); i++) {
            MiningGating.GateTier tier = tiers.get(i);
            int y = top + 2 + i * rowH;
            int centerY = y + 14;

            boolean unlocked = tier.isFree() || (mining != null && miningOrder >= tier.rank().order());
            boolean isCurrent = tier.isFree() ? mining == null : mining == tier.rank();
            boolean rowHovered = mouseY >= y && mouseY < y + rowH - 1
                    && mouseX >= rowLeft && mouseX < rowRight;

            if (isCurrent)        gfx.fill(rowLeft, y, rowRight, y + rowH - 1, GATE_ROW_CURRENT);
            else if (rowHovered)  gfx.fill(rowLeft, y, rowRight, y + rowH - 1, COLOR_ROW_ALT);

            // Rail segment leading up from the previous rung — gold once this rung is reached.
            if (prevCenterY >= 0) {
                gfx.fill(railX + 4, prevCenterY, railX + 6, centerY,
                        unlocked ? GATE_RAIL_DONE : GATE_RAIL_LOCKED);
            }
            prevCenterY = centerY;
        }

        // ── Pass 2: rung nodes, labels, ore icons (drawn on top of the rails) ──
        List<Component> tip = null;
        for (int i = 0; i < tiers.size(); i++) {
            MiningGating.GateTier tier = tiers.get(i);
            int y = top + 2 + i * rowH;
            int nodeY = y + 9;

            boolean unlocked = tier.isFree() || (mining != null && miningOrder >= tier.rank().order());
            boolean isCurrent = tier.isFree() ? mining == null : mining == tier.rank();
            boolean rowHovered = mouseY >= y && mouseY < y + rowH - 1
                    && mouseX >= rowLeft && mouseX < rowRight;

            if (unlocked) drawBeveledNode(gfx, railX, nodeY, 11, NODE_UNLOCK_FILL, NODE_UNLOCK_HI, NODE_UNLOCK_LO);
            else          drawBeveledNode(gfx, railX, nodeY, 11, NODE_LOCKED_FILL, NODE_LOCKED_HI, NODE_LOCKED_LO);

            String title = tier.isFree() ? "Open" : tier.rank().displayName();
            int titleColor = isCurrent ? COLOR_MAXED : (unlocked ? COLOR_OK : COLOR_NO);
            gfx.text(font, title, railX + 18, y + 4, titleColor, false);
            gfx.text(font, tier.label(), railX + 18, y + 15, COLOR_TEXT_DIM, false);

            // Ore icons, right-aligned.
            List<ItemStack> stacks = gateIcons.get(i);
            int iconY = y + (rowH - 16) / 2;
            int ix = rowRight - 8 - (stacks.size() * 18 - 2);
            for (ItemStack stack : stacks) {
                gfx.item(stack, ix, iconY);
                if (!unlocked) {
                    gfx.fill(ix, iconY, ix + 16, iconY + 16, GATE_VEIL);
                    drawPadlock(gfx, ix + 4, iconY + 4);
                }
                ix += 18;
            }

            if (rowHovered) tip = gateTooltip(tier, unlocked);
        }
        return tip;
    }

    private static List<Component> gateTooltip(MiningGating.GateTier tier, boolean unlocked) {
        List<Component> lines = new ArrayList<>();
        String head = tier.isFree() ? "Open Mining"
                : tier.isPerk() ? "Veinbreaker"
                : tier.rank().displayName() + " Gate";
        lines.add(Component.literal(head)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));
        if (unlocked) {
            lines.add(Component.literal(tier.isPerk() ? "Active — veins chain-mine." : "Unlocked — you can mine these.")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        } else {
            lines.add(Component.literal("Locked — reach Mining " + tier.rank().displayName())
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }
        lines.add(Component.literal(tier.label()).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));
        return lines;
    }

    /** Small gold padlock badge (~8×8) drawn over a locked ore icon. */
    private static void drawPadlock(GuiGraphicsExtractor gfx, int x, int y) {
        int shackle = 0xFFE8E8E8;
        gfx.fill(x + 2, y, x + 6, y + 1, shackle);        // top bar
        gfx.fill(x + 2, y, x + 3, y + 3, shackle);        // left arm
        gfx.fill(x + 5, y, x + 6, y + 3, shackle);        // right arm
        gfx.outline(x, y + 3, 8, 5, 0xFF000000);          // body border
        gfx.fill(x + 1, y + 4, x + 7, y + 7, 0xFFE0B83A); // gold body
        gfx.fill(x + 3, y + 5, x + 5, y + 7, 0xFF5A4510); // keyhole
    }

    // ── Mastery tab (profession perk trees) ────────────────────────────────

    private HoverTarget renderRow(GuiGraphicsExtractor gfx, int px, int y, Profession profession,
                                  int rowIndex, int mouseX, int mouseY) {
        int rowLeft = px + 6;
        int rowRight = px + PANEL_WIDTH - 6;
        boolean rowHovered = mouseY >= y - 1 && mouseY < y + ROW_HEIGHT - 3
                && mouseX >= rowLeft && mouseX < rowRight;

        if (rowIndex % 2 == 1) {
            gfx.fill(rowLeft, y - 1, rowRight, y + ROW_HEIGHT - 3, COLOR_ROW_ALT);
        }
        if (rowHovered && ClientSkillData.canAffordNext(profession)) {
            gfx.fill(rowLeft, y - 1, rowRight, y + ROW_HEIGHT - 3, COLOR_ROW_HOVER);
        }

        gfx.item(icons.get(profession), rowLeft + 4, y);
        gfx.text(font, profession.displayName(), rowLeft + 26, y + 4, COLOR_TEXT, false);

        // Node chain
        int chainX0 = px + CHAIN_X_OFFSET;
        ProfessionRank current = ClientSkillData.rankFor(profession);
        ProfessionRank next = ClientSkillData.nextRankFor(profession);
        boolean canAfford = ClientSkillData.canAffordNext(profession);
        int currentOrder = current == null ? -1 : current.order();

        for (int i = 0; i < ProfessionRank.values().length - 1; i++) {
            int x1 = chainX0 + i * NODE_SPACING + NODE_SIZE / 2 + 1;
            int x2 = chainX0 + (i + 1) * NODE_SPACING - NODE_SIZE / 2;
            int lineY = y + NODE_SIZE / 2 + 1;
            int lineColor = i < currentOrder ? LINE_DONE : LINE_LOCKED;
            gfx.fill(x1, lineY - 1, x2, lineY + 1, lineColor);
        }

        HoverTarget hovered = null;
        for (ProfessionRank rank : ProfessionRank.values()) {
            int nx = chainX0 + rank.order() * NODE_SPACING - NODE_SIZE / 2;
            int ny = y + 1;
            boolean unlocked = currentOrder >= rank.order();
            boolean isNext = next != null && rank == next;

            int fill, hi, lo;
            if (unlocked) { fill = NODE_UNLOCK_FILL; hi = NODE_UNLOCK_HI; lo = NODE_UNLOCK_LO; }
            else if (isNext && canAfford) { fill = NODE_NEXT_OK_FILL; hi = NODE_NEXT_OK_HI; lo = NODE_NEXT_OK_LO; }
            else if (isNext) { fill = NODE_NEXT_NO_FILL; hi = NODE_NEXT_NO_HI; lo = NODE_NEXT_NO_LO; }
            else { fill = NODE_LOCKED_FILL; hi = NODE_LOCKED_HI; lo = NODE_LOCKED_LO; }
            drawBeveledNode(gfx, nx, ny, NODE_SIZE, fill, hi, lo);

            if (mouseX >= nx && mouseX < nx + NODE_SIZE && mouseY >= ny && mouseY < ny + NODE_SIZE) {
                hovered = new HoverTarget(profession, rank, unlocked, isNext, canAfford);
            }
        }

        // Right-side hint
        int rightX = chainX0 + ProfessionRank.values().length * NODE_SPACING - NODE_SIZE / 2 + 8;
        if (next != null) {
            String hint = "→ " + next.displayName() + " (" + next.pointCost() + "pt)";
            int hintColor = canAfford ? COLOR_OK : COLOR_NO;
            gfx.text(font, hint, rightX, y + 4, hintColor, false);
        } else {
            gfx.text(font, "MAXED", rightX, y + 4, COLOR_MAXED, false);
        }

        return hovered;
    }

    private void renderNodeTooltip(GuiGraphicsExtractor gfx, HoverTarget hover, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        // Title: profession + rank
        lines.add(Component.literal(hover.profession.displayName() + " — " + hover.rank.displayName())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));

        // Status line
        if (hover.unlocked) {
            lines.add(Component.literal("Unlocked").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        } else if (hover.isNext && hover.canAfford) {
            lines.add(Component.literal("Cost: " + hover.rank.pointCost() + " pt — click to spend")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        } else if (hover.isNext) {
            lines.add(Component.literal("Cost: " + hover.rank.pointCost() + " pt — not enough points")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        } else {
            lines.add(Component.literal("Locked — earlier ranks required")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
        }

        // Effect description
        for (String line : effectDescription(hover.profession, hover.rank)) {
            lines.add(Component.literal(line).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)));
        }

        drawTooltipBox(gfx, lines, mouseX + 12, mouseY - 6);
    }

    private void drawTooltipBox(GuiGraphicsExtractor gfx, List<Component> lines, int x, int y) {
        int maxW = 0;
        for (Component line : lines) {
            int w = font.width(line);
            if (w > maxW) maxW = w;
        }
        int padding = 4;
        int boxW = maxW + padding * 2;
        int boxH = lines.size() * 10 + padding * 2;
        // Constrain to screen
        if (x + boxW > width - 4) x = width - 4 - boxW;
        if (y + boxH > height - 4) y = height - 4 - boxH;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        gfx.fill(x, y, x + boxW, y + boxH, COLOR_TIP_BG);
        gfx.outline(x, y, boxW, boxH, COLOR_TIP_BORDER);

        int textY = y + padding;
        for (Component line : lines) {
            gfx.text(font, line, x + padding, textY, COLOR_TEXT_BRIGHT, true);
            textY += 10;
        }
    }

    private static void drawBeveledNode(GuiGraphicsExtractor gfx, int x, int y, int size,
                                        int fill, int hi, int lo) {
        gfx.outline(x, y, size, size, 0xFF000000);
        gfx.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
        gfx.fill(x + 1, y + 1, x + size - 1, y + 2, hi);
        gfx.fill(x + 1, y + 1, x + 2, y + size - 1, hi);
        gfx.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, lo);
        gfx.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, lo);
    }

    /** Draws the classic beveled gray Minecraft container panel. */
    private void drawVanillaPanel(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, COL_PANEL_BORDER);
        gfx.fill(x0, y0, x1, y1, COL_PANEL_BG);
        gfx.fill(x0, y0, x1, y0 + 2, COL_PANEL_HI);
        gfx.fill(x0, y0, x0 + 2, y1, COL_PANEL_HI);
        gfx.fill(x0, y1 - 2, x1, y1, COL_PANEL_LO);
        gfx.fill(x1 - 2, y0, x1, y1, COL_PANEL_LO);
    }

    /** Left x of each tab, laid out left-to-right from the panel's left edge. */
    private int[] tabXs(int px) {
        int[] xs = new int[TAB_LABELS.length];
        int x = px + 8;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            xs[i] = x;
            x += font.width(TAB_LABELS[i]) + 16 + 4;
        }
        return xs;
    }

    private void drawTab(GuiGraphicsExtractor gfx, int x, int y, int w, int h, String label, boolean active) {
        int bg = active ? COL_PANEL_BG : COL_TAB_INACTIVE;
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_PANEL_BORDER);
        gfx.fill(x, y, x + w, y + h, bg);
        gfx.fill(x, y, x + w, y + 1, COL_PANEL_HI);
        gfx.fill(x, y, x + 1, y + h, COL_PANEL_HI);
        if (!active) {
            gfx.fill(x, y + h - 1, x + w, y + h, COL_PANEL_LO);
            gfx.fill(x + w - 1, y, x + w, y + h, COL_PANEL_LO);
        }
        int tx = x + (w - font.width(label)) / 2;
        gfx.text(font, label, tx, y + 4, active ? COLOR_TEXT : COLOR_TEXT_DIM, false);
    }

    /** Returns the tab index under the cursor, or -1. */
    private int pickTab(int mouseX, int mouseY, int px, int py) {
        if (mouseY < py + TAB_TOP || mouseY >= py + TAB_TOP + TAB_HEIGHT) return -1;
        int[] xs = tabXs(px);
        for (int i = 0; i < TAB_LABELS.length; i++) {
            int w = font.width(TAB_LABELS[i]) + 16;
            if (mouseX >= xs[i] && mouseX < xs[i] + w) return i;
        }
        return -1;
    }

    private int pickHoveredRow(int mouseX, int mouseY, int py) {
        int rowsTop = py + CONTENT_TOP - 1;
        if (mouseY < rowsTop) return -1;
        int idx = (mouseY - rowsTop) / ROW_HEIGHT;
        if (idx < 0 || idx >= Profession.values().length) return -1;
        int px = panelX();
        if (mouseX < px + 6 || mouseX > px + PANEL_WIDTH - 6) return -1;
        return idx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0) {
            int tab = pickTab((int) event.x(), (int) event.y(), panelX(), panelY());
            if (tab >= 0) {
                activeTab = tab;
                return true;
            }
        }
        if (event.button() == 0 && activeTab == TAB_MASTERY && ClientSkillData.hasReceived()) {
            int hovered = pickHoveredRow((int) event.x(), (int) event.y(), panelY());
            if (hovered >= 0) {
                Profession profession = Profession.values()[hovered];
                if (ClientSkillData.canAffordNext(profession)) {
                    ClientPayloads.sendToServer(new SpendSkillPointPayload(profession));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(returnTo); }

    @Override
    public boolean isPauseScreen() { return false; }

    /** A node the mouse is currently over — used to render its tooltip. */
    private record HoverTarget(Profession profession, ProfessionRank rank,
                               boolean unlocked, boolean isNext, boolean canAfford) {}

    /** Effect description per profession × rank. v1 — Blacksmithing and Mining have full text from spec; others are stubs. */
    private static List<String> effectDescription(Profession profession, ProfessionRank rank) {
        return switch (profession) {
            case BLACKSMITHING -> switch (rank) {
                case NOVICE     -> List.of("Field Patching", "Cheaper anvil repairs; basic forge control");
                case APPRENTICE -> List.of("Proper Tools", "Cheaper anvil repairs; steadier forge");
                case JOURNEYMAN -> List.of("Fine Preservation", "Cheaper anvil repairs; wider forge timing");
                case EXPERT     -> List.of("Mint Handling", "Cheap anvil repairs; forgiving forge window");
                case MASTER     -> List.of("Masterwork Conservation", "Cheapest anvil repairs; master forge control");
            };
            case MINING -> switch (rank) {
                case NOVICE     -> List.of("Prospecting", "Visible ore-density hint within 8 blocks");
                case APPRENTICE -> List.of("Claimed Mine Yield", "+10% drop count from Claimed mines");
                case JOURNEYMAN -> List.of("Deep Mine Access", "Mine Deep tier without speed penalty");
                case EXPERT     -> List.of("Royal Surveying", "Bonus Mint-quality drop chance from Royal mines");
                case MASTER     -> List.of("Veinbreaker", "Vein-mine adjacent ore on first hit");
            };
            case FARMING -> switch (rank) {
                case NOVICE     -> List.of("Yield bonus on harvested crops");
                case APPRENTICE -> List.of("Faster crop growth nearby");
                case JOURNEYMAN -> List.of("Crop quality propagation");
                case EXPERT     -> List.of("Animal husbandry — better breeding outcomes");
                case MASTER     -> List.of("Master Farmer — full v2 calibration TBD");
            };
            case COOKING -> switch (rank) {
                case NOVICE     -> List.of("Basic meal quality bonus");
                case APPRENTICE -> List.of("Reduced food spoilage");
                case JOURNEYMAN -> List.of("Feast buffs at the king's table");
                case EXPERT     -> List.of("Advanced recipes unlocked");
                case MASTER     -> List.of("Master Chef — full v2 calibration TBD");
            };
            case ALCHEMY -> switch (rank) {
                case NOVICE     -> List.of("Basic potion crafting");
                case APPRENTICE -> List.of("Reduced reagent waste on failed brews");
                case JOURNEYMAN -> List.of("Tier-2 potion access");
                case EXPERT     -> List.of("Tier-3 potions; failure rate cut");
                case MASTER     -> List.of("Master Alchemist — full v2 calibration TBD");
            };
            case FISHING -> switch (rank) {
                case NOVICE     -> List.of("Slight rare-catch chance bonus");
                case APPRENTICE -> List.of("Bait conservation — uses last longer");
                case JOURNEYMAN -> List.of("Sea economy access");
                case EXPERT     -> List.of("Tropical fish in deep ocean");
                case MASTER     -> List.of("Master Fisher — full v2 calibration TBD");
            };
            case ENCHANTING -> switch (rank) {
                case NOVICE     -> List.of("Basic enchant stability");
                case APPRENTICE -> List.of("Gold synergy — +enchant strength on gold tools");
                case JOURNEYMAN -> List.of("Reroll handling — better second-roll outcomes");
                case EXPERT     -> List.of("Advanced enchant unlocks");
                case MASTER     -> List.of("Master Enchanter — full v2 calibration TBD");
            };
            case TRADING -> switch (rank) {
                case NOVICE     -> List.of("Reduced toll cost at portals");
                case APPRENTICE -> List.of("Market discount — citizen rates");
                case JOURNEYMAN -> List.of("Trade tax efficiency for kingdom");
                case EXPERT     -> List.of("Contract fulfillment bonuses");
                case MASTER     -> List.of("Master Trader — full v2 calibration TBD");
            };
        };
    }
}
