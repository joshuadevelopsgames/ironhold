package kingdom.smp.mixin;

import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the vanilla-native accessory + vanity side panels to the otherwise-stock
 * survival inventory.
 *
 * <p>The inventory itself is left fully vanilla (stone GUI, paperdoll, labels and
 * recipe book all unmodified). {@link InventoryMenuMixin} appends the 5 accessory
 * and 4 vanity slots; this mixin draws beveled gray panels and sunken slot cells
 * behind them — a vanity panel docked to the left and an accessory bar below.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    private static final int PANEL_BG     = 0xFFC6C6C6;
    private static final int PANEL_HI     = 0xFFFFFFFF;
    private static final int PANEL_LO     = 0xFF555555;
    private static final int PANEL_BORDER = 0xFF000000;
    private static final int SLOT_DARK    = 0xFF373737;
    private static final int SLOT_LIGHT   = 0xFFFFFFFF;
    private static final int SLOT_FACE    = 0xFF8B8B8B;
    private static final int LABEL_COLOR  = 0xFF404040;

    /** Recipe-book button's natural Y offset below the GUI top (captured at init, before the shift). */
    private int ironhold$recipeBtnOffsetY = 34;

    /** Synthesized constructor — never invoked. The mixin code is merged into InventoryScreen. */
    protected InventoryScreenMixin(InventoryMenu menu,
                                   net.minecraft.world.entity.player.Inventory inv,
                                   net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    /**
     * Nudge the whole inventory up so the accessory bar + label fit below it. Record the recipe-book
     * button's natural offset from the GUI top first; it's re-pinned every frame in
     * {@link #ironhold$drawDockedPanels} because vanilla resets its Y whenever the book is toggled.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$initLayout(CallbackInfo ci) {
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.ImageButton btn) {
                ironhold$recipeBtnOffsetY = btn.getY() - this.topPos;
            }
        }
        this.topPos -= IronholdInventoryLayout.GUI_SHIFT_UP;
    }

    /** Draw the docked panels after vanilla's background (slot items render later, on top). */
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void ironhold$drawDockedPanels(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        int left = this.leftPos;
        int top = this.topPos;

        // Keep the recipe-book button pinned to the shifted GUI (vanilla resets its Y on toggle).
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.ImageButton btn) {
                btn.setY(top + ironhold$recipeBtnOffsetY);
            }
        }

        boolean bookOpen = ironhold$recipeBookOpen();

        // Vanity slots live in the left dock; the open recipe book covers that area, so hide them
        // (move the slots off-screen) and skip drawing the panel while the book is open.
        for (int i = 0; i < IronholdInventoryLayout.VANITY_DOCK_SLOT_Y.length; i++) {
            var acc = (kingdom.smp.access.SlotAccessor) this.menu.slots.get(IronholdInventoryLayout.ACCESSORY_SLOT_END + i);
            if (bookOpen) {
                acc.ironhold$setX(-9999);
                acc.ironhold$setY(-9999);
            } else {
                acc.ironhold$setX(IronholdInventoryLayout.VANITY_DOCK_SLOT_X);
                acc.ironhold$setY(IronholdInventoryLayout.VANITY_DOCK_SLOT_Y[i]);
            }
        }

        if (!bookOpen) {
            // Vanity panel — docked left of the inventory: "Vanity" header above 4 vertical slots.
            int vx0 = left + IronholdInventoryLayout.VANITY_DOCK_X;
            int vy0 = top + IronholdInventoryLayout.VANITY_DOCK_Y;
            ironhold$panel(gfx, vx0, vy0,
                    vx0 + IronholdInventoryLayout.VANITY_DOCK_W, vy0 + IronholdInventoryLayout.VANITY_DOCK_H);
            gfx.text(this.font, "Vanity",
                    left + IronholdInventoryLayout.VANITY_DOCK_CENTER_X - this.font.width("Vanity") / 2,
                    top + IronholdInventoryLayout.VANITY_DOCK_TITLE_Y, LABEL_COLOR, false);
            for (int i = 0; i < IronholdInventoryLayout.VANITY_DOCK_SLOT_Y.length; i++) {
                ironhold$slotCell(gfx,
                        left + IronholdInventoryLayout.VANITY_DOCK_SLOT_X - 1,
                        top + IronholdInventoryLayout.VANITY_DOCK_SLOT_Y[i] - 1);
            }
        }

        // Accessory bar — docked below: 5 centered slots with "Accessories" label beneath.
        int ax0 = left + IronholdInventoryLayout.ACC_PANEL_X;
        int ay0 = top + IronholdInventoryLayout.ACC_PANEL_Y;
        ironhold$panel(gfx, ax0, ay0,
                ax0 + IronholdInventoryLayout.ACC_PANEL_W, ay0 + IronholdInventoryLayout.ACC_PANEL_H);
        for (int i = 0; i < 5; i++) {
            ironhold$slotCell(gfx,
                    left + IronholdInventoryLayout.ACC_SLOT_X0 + i * IronholdInventoryLayout.COSMETIC_SLOT_PITCH - 1,
                    top + IronholdInventoryLayout.ACC_ROW_Y - 1);
        }
        gfx.text(this.font, "Accessories",
                left + IronholdInventoryLayout.ACC_CENTER_X - this.font.width("Accessories") / 2,
                top + IronholdInventoryLayout.ACC_LABEL_Y, LABEL_COLOR, false);
    }

    private boolean ironhold$recipeBookOpen() {
        if (!(this instanceof AbstractRecipeBookScreenAccessor acc)) return false;
        var book = acc.ironhold$getRecipeBookComponent();
        return book != null && book.isVisible();
    }

    /** Classic beveled gray Minecraft container panel. */
    private static void ironhold$panel(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, PANEL_BORDER);
        gfx.fill(x0, y0, x1, y1, PANEL_BG);
        gfx.fill(x0, y0, x1, y0 + 2, PANEL_HI);
        gfx.fill(x0, y0, x0 + 2, y1, PANEL_HI);
        gfx.fill(x0, y1 - 2, x1, y1, PANEL_LO);
        gfx.fill(x1 - 2, y0, x1, y1, PANEL_LO);
    }

    /** Vanilla sunken 18×18 slot cell (dark top-left, light bottom-right). */
    private static void ironhold$slotCell(GuiGraphicsExtractor gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, SLOT_DARK);
        gfx.fill(x + 1, y + 1, x + 18, y + 18, SLOT_LIGHT);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FACE);
    }
}
