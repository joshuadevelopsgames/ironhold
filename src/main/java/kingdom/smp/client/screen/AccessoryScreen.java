package kingdom.smp.client.screen;

import kingdom.smp.accessory.AccessoryMenu;
import kingdom.smp.accessory.AccessorySlot;
import kingdom.smp.accessory.VanitySlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Terraria-inspired equipment screen with vanity armor slots (left),
 * a rotating player preview (centre), and accessory slots (right).
 * Reached via the ✦ tab button on the vanilla inventory.
 */
public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {

    // ── Colour palette ────────────────────────────────────────────────────────

    private static final int BG              = 0xEE0D0D1A;
    private static final int BORDER_GOLD     = 0xFFB8860B;
    private static final int BORDER_INNER    = 0xFF8B6914;
    private static final int SLOT_ACC        = 0xFF2E2E2E;
    private static final int SLOT_VAN        = 0xFF2E2E2E;
    private static final int SLOT_ARMOR      = 0xFF2E2E2E;
    private static final int SLOT_INV        = 0xFF1A1A1A;
    private static final int SLOT_INV_BORDER = 0xFF3A3A3A;
    private static final int SLOT_BORDER     = 0xFF555555;
    private static final int LABEL_GOLD      = 0xFFDDAA44;
    private static final int LABEL_DIM       = 0xFF888888;
    /** Vanilla-style container title gray (matches survival inventory labels). */
    private static final int LABEL_VANILLA   = 0xFF404040;
    private static final int SEPARATOR       = 0xFF333344;
    /** Survival-style stone frame (matches {@link kingdom.smp.mixin.InventoryScreenMixin}). */
    private static final int STONE_OUTER     = 0xFF222222;
    private static final int STONE_FACE      = 0xFFC6C6C6;
    private static final int STONE_HI        = 0xFFFFFFFF;
    private static final int STONE_LO        = 0xFF555555;

    // ── Construction ──────────────────────────────────────────────────────────

    public AccessoryScreen(AccessoryMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 230, 200);
    }

    // ── Background rendering ──────────────────────────────────────────────────

    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        // Panel fill
        gfx.fill(x, y, x + w, y + h, BG);

        // Double gold border
        gfx.outline(x, y, w, h, BORDER_GOLD);
        gfx.outline(x + 1, y + 1, w - 2, h - 2, BORDER_INNER);

        // Corner accents (3×3 filled squares)
        cornerFill(gfx, x, y, 3, BORDER_GOLD);
        cornerFill(gfx, x + w - 3, y, 3, BORDER_GOLD);
        cornerFill(gfx, x, y + h - 3, 3, BORDER_GOLD);
        cornerFill(gfx, x + w - 3, y + h - 3, 3, BORDER_GOLD);

        // Vanity column: full-width island (slots at menu x=52, 18px wide, centered in 32px)
        int vpx = x + 45;
        int vpy = y + 8;
        int vlw = 32;
        int vlh = 82;
        drawStonePanel(gfx, vpx, vpy, vlw, vlh);
        gfx.outline(vpx + 1, vpy + 1, vlw - 2, vlh - 2, STONE_LO);

        // Section headers
        gfx.text(font, "Armor",  x + 8,   y + 2, LABEL_DIM,    false);
        drawCenteredNoShadow(gfx, font, Component.translatable("gui.ironhold.vanity"),
            vpx + vlw / 2, y + 2, LABEL_VANILLA);
        gfx.text(font, "\u2726 Accessories", x + 168, y + 2, LABEL_VANILLA, false);

        // Separator line between equipment and inventory
        int sepY = y + 103;
        gfx.fill(x + 6, sepY, x + w - 6, sepY + 1, SEPARATOR);
        // Diamond ornament at centre
        int cx = x + w / 2;
        gfx.fill(cx - 2, sepY - 1, cx + 2, sepY + 2, BORDER_GOLD);
        gfx.fill(cx - 1, sepY - 2, cx + 1, sepY + 3, BORDER_GOLD);

        // Slot backgrounds
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;

            if (slot instanceof AccessorySlot) {
                gfx.fill(sx, sy, sx + 18, sy + 18, SLOT_ACC);
                gfx.outline(sx, sy, 18, 18, SLOT_BORDER);
            } else if (slot instanceof VanitySlot) {
                gfx.fill(sx, sy, sx + 18, sy + 18, SLOT_VAN);
                gfx.outline(sx, sy, 18, 18, SLOT_BORDER);
            } else if (isArmorOrOffhand(slot)) {
                gfx.fill(sx, sy, sx + 18, sy + 18, SLOT_ARMOR);
                gfx.outline(sx, sy, 18, 18, SLOT_BORDER);
            } else {
                gfx.fill(sx, sy, sx + 18, sy + 18, SLOT_INV);
                gfx.outline(sx, sy, 18, 18, SLOT_INV_BORDER);
            }
        }

        // Arrow indicators between armor and vanity columns
        for (int i = 0; i < 4; i++) {
            gfx.text(font, "\u2192", x + 33, y + 13 + i * 18, LABEL_DIM, false);
        }

        // Player model preview
        var player = Minecraft.getInstance().player;
        if (player != null) {
            int mx1 = x + 80;
            int my1 = y + 12;
            int mx2 = x + 152;
            int my2 = y + 96;

            // Dark backdrop
            gfx.fill(mx1, my1, mx2, my2, 0xFF111122);
            gfx.outline(mx1, my1, mx2 - mx1, my2 - my1, 0xFF2A2A44);

            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    gfx, mx1, my1, mx2, my2,
                    38, 0.0625F,
                    (float) mouseX, (float) mouseY,
                    player);
        }
    }

    // ── Label rendering ───────────────────────────────────────────────────────

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        // Centred title above the panel
        String title = "\u2726 Equipment \u2726";
        gfx.text(font,
                Component.literal(title).withStyle(ChatFormatting.GOLD),
                imageWidth / 2 - font.width(title) / 2, -12,
                LABEL_GOLD, false);

        // "Inventory" label above the bottom section
        gfx.text(font, this.playerInventoryTitle, 34, imageHeight - 93, LABEL_DIM, false);
    }

    // ── Standard boilerplate ──────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        extractTooltip(gfx, mouseX, mouseY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isArmorOrOffhand(Slot slot) {
        if (!(slot.container instanceof Inventory)) return false;
        int idx = slot.getSlotIndex();
        return idx >= 36 && idx <= 40;
    }

    private static void drawStonePanel(GuiGraphicsExtractor gfx, int px, int py, int w, int h) {
        gfx.fill(px, py, px + w, py + h, STONE_OUTER);
        gfx.fill(px + 1, py + 1, px + w - 1, py + h - 1, STONE_FACE);
        gfx.fill(px + 1, py + 1, px + w - 1, py + 2, STONE_HI);
        gfx.fill(px + 1, py + 1, px + 2, py + h - 1, STONE_HI);
        gfx.fill(px + 1, py + h - 2, px + w - 1, py + h - 1, STONE_LO);
        gfx.fill(px + w - 2, py + 1, px + w - 1, py + h - 1, STONE_LO);
    }

    private static void cornerFill(GuiGraphicsExtractor gfx, int x, int y, int size, int color) {
        gfx.fill(x, y, x + size, y + size, color);
    }

    private static void drawCenteredNoShadow(GuiGraphicsExtractor gfx, Font font, Component text, int centerX, int y, int color) {
        FormattedCharSequence seq = text.getVisualOrderText();
        gfx.text(font, seq, centerX - font.width(seq) / 2, y, color, false);
    }
}
