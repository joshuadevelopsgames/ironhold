package kingdom.smp.client.screen;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.net.RespecSkillPayload;
import kingdom.smp.net.SpendSkillPointPayload;
import kingdom.smp.skill.ClientSkillData;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
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
 * Custom screen showing all 8 profession trees as linear 5-rank chains.
 * Hover any node for a tooltip; click an affordable next-rank node to spend a point.
 *
 * Sized to fit at GUI scale 4 / 1080p windows (small panel, tight rows).
 *
 * Opened from the "Skills" button on the survival inventory screen.
 */
public class SkillTreeScreen extends Screen {

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 208;
    private static final int ROW_HEIGHT = 22;
    private static final int NODE_SIZE = 12;
    private static final int NODE_SPACING = 32;
    private static final int CHAIN_X_OFFSET = 110;

    private static final int COLOR_BG          = 0xEE0E0E14;
    private static final int COLOR_BORDER      = 0xFF6B6B85;
    private static final int COLOR_HEADER_RULE = 0xFF44445A;
    private static final int COLOR_ROW_ALT     = 0x22FFFFFF;
    private static final int COLOR_ROW_HOVER   = 0x33FFCC55;
    private static final int COLOR_TEXT        = 0xFFEEEEFF;
    private static final int COLOR_TEXT_DIM    = 0xFF7A7A8F;
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

    private static final int LINE_LOCKED = 0xFF333344;
    private static final int LINE_DONE   = 0xFFD9A53A;

    private final Map<Profession, ItemStack> icons = new EnumMap<>(Profession.class);
    private final @Nullable Screen returnTo;

    public SkillTreeScreen() { this(null); }

    public SkillTreeScreen(@Nullable Screen returnTo) {
        super(Component.literal("Skill Tree"));
        this.returnTo = returnTo;
        icons.put(Profession.BLACKSMITHING, new ItemStack(Items.ANVIL));
        icons.put(Profession.FARMING,       new ItemStack(Items.WHEAT));
        icons.put(Profession.COOKING,       new ItemStack(Items.BREAD));
        icons.put(Profession.ALCHEMY,       new ItemStack(Items.BREWING_STAND));
        icons.put(Profession.FISHING,       new ItemStack(Items.FISHING_ROD));
        icons.put(Profession.ENCHANTING,    new ItemStack(Items.ENCHANTED_BOOK));
        icons.put(Profession.MINING,        new ItemStack(Items.DIAMOND_PICKAXE));
        icons.put(Profession.TRADING,       new ItemStack(Items.EMERALD));
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
        addRenderableWidget(Button.builder(Component.literal("Respec All"), b -> requestRespecAll())
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

        gfx.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, COLOR_BG);
        gfx.outline(px, py, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        // Header
        gfx.text(font,
                Component.literal("Skill Tree").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)),
                px + 12, py + 8, COLOR_TEXT_BRIGHT, true);

        String unspentLabel = ClientSkillData.hasReceived()
                ? ClientSkillData.unspentPoints() + " unspent"
                : "loading...";
        int unspentColor = ClientSkillData.hasReceived() && ClientSkillData.unspentPoints() > 0
                ? 0xFF7BD884 : COLOR_TEXT_DIM;
        int unspentX = px + PANEL_WIDTH - 12 - font.width(unspentLabel);
        gfx.text(font, unspentLabel, unspentX, py + 9, unspentColor, false);

        gfx.fill(px + 10, py + 22, px + PANEL_WIDTH - 10, py + 23, COLOR_HEADER_RULE);

        // Rows
        int rowY = py + 28;
        HoverTarget hover = null;
        int rowIndex = 0;
        for (Profession profession : Profession.values()) {
            HoverTarget rowHover = renderRow(gfx, px, rowY, profession, rowIndex, mouseX, mouseY);
            if (rowHover != null) hover = rowHover;
            rowY += ROW_HEIGHT;
            rowIndex++;
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        // Tooltip pass — drawn on top of everything else
        if (hover != null) {
            renderNodeTooltip(gfx, hover, mouseX, mouseY);
        }
    }

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
            int hintColor = canAfford ? 0xFF7BD884 : 0xFFB85959;
            gfx.text(font, hint, rightX, y + 4, hintColor, false);
        } else {
            gfx.text(font, "MAXED", rightX, y + 4, 0xFFD9A53A, false);
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

    private int pickHoveredRow(int mouseX, int mouseY, int py) {
        int rowsTop = py + 28 - 1;
        if (mouseY < rowsTop) return -1;
        int idx = (mouseY - rowsTop) / ROW_HEIGHT;
        if (idx < 0 || idx >= Profession.values().length) return -1;
        int px = panelX();
        if (mouseX < px + 6 || mouseX > px + PANEL_WIDTH - 6) return -1;
        return idx;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        if (event.button() == 0 && ClientSkillData.hasReceived()) {
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
                case NOVICE     -> List.of("Field Patching", "60% repair efficiency, full fatigue gain");
                case APPRENTICE -> List.of("Proper Tools", "75% repair efficiency, −10% fatigue gain");
                case JOURNEYMAN -> List.of("Fine Preservation", "85% efficiency, −20% fatigue gain", "Refine Fine gear to Pristine");
                case EXPERT     -> List.of("Mint Handling", "92% efficiency, −30% fatigue gain", "Refine Mint gear; Royal Forge access");
                case MASTER     -> List.of("Masterwork Conservation", "95% efficiency, −40% fatigue gain", "No quality loss with correct mats");
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
