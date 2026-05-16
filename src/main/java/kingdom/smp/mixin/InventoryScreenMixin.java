package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.Ironhold;
import kingdom.smp.client.IronholdSlotDebug;
import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reskin of the survival inventory screen with the Ironhold custom layout:
 * <ul>
 *   <li>Resizes the main panel to {@link IronholdInventoryLayout#MAIN_W}×{@link IronholdInventoryLayout#MAIN_H}.</li>
 *   <li>Replaces vanilla's stone bg + paperdoll blit entirely with our own textures and paperdoll position.</li>
 *   <li>Hides the vanilla recipe book button + component (Ironhold recommends JEI/REI/EMI).</li>
 *   <li>Suppresses vanilla "Crafting" / "Inventory" labels — the texture has them painted in.</li>
 *   <li>Live-applies {@link IronholdSlotDebug} positions every frame (F4 in-game toggles edit mode).</li>
 * </ul>
 *
 * <p>Slot positions are first set by {@link InventoryMenuMixin}; this mixin overrides them
 * each frame from the debug state so they can be tuned live without restarting.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<net.minecraft.world.inventory.InventoryMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/inventory/inventory_main.png");
    private static final Identifier VANITY_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/inventory/vanity_panel.png");
    private static final Identifier SKILLS_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/inventory/skills_panel.png");

    /** Synthesized constructor — never invoked. The mixin code is merged into InventoryScreen. */
    protected InventoryScreenMixin(net.minecraft.world.inventory.InventoryMenu menu,
                                   net.minecraft.world.entity.player.Inventory inv,
                                   net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(Player player, CallbackInfo ci) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(IronholdInventoryLayout.MAIN_W);
        acc.ironhold$setImageHeight(IronholdInventoryLayout.MAIN_H);
        // Texture has its own painted "CRAFTING" / "ACCESSORIES" labels.
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$dropRecipeBook(CallbackInfo ci) {
        // The recipe-book toggle ImageButton lives in children — remove it so users can't reopen.
        List<GuiEventListener> toRemove = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof ImageButton) {
                toRemove.add(child);
            }
        }
        for (GuiEventListener child : toRemove) {
            if (child instanceof AbstractWidget w) {
                this.removeWidget(w);
            }
        }

        // The RecipeBookComponent itself is a private field on AbstractRecipeBookScreen,
        // rendered explicitly in extractRenderState. Toggling its visibility off prevents
        // the panel from drawing.
        RecipeBookComponent<?> book = ((AbstractRecipeBookScreenAccessor) this).ironhold$getRecipeBookComponent();
        if (book != null && book.isVisible()) {
            book.toggleVisibility();
        }

        // Recipe book updateScreenPosition() shifted leftPos; restore centering.
        this.leftPos = (this.width - this.imageWidth) / 2;
    }

    /**
     * Replace vanilla's extractBackground entirely. Vanilla blits the 176×166 stone GUI
     * (which leaves transparent gaps around our larger custom panel) and renders the player
     * paperdoll at hardcoded coords. We draw our own textures + paperdoll instead.
     */
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        // Apply live debug positions to every slot before they render.
        ironhold$applyDebugPositions();

        int left = this.leftPos;
        int top = this.topPos;

        // Full-screen dim covers in-world HUD bleeding through around the custom panel.
        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        // Main panel
        ironhold$blit(gfx, MAIN_TEXTURE, left, top,
                IronholdInventoryLayout.MAIN_W, IronholdInventoryLayout.MAIN_H);

        // Vanity panel — left of main (offset is debug-tunable)
        int[] vp = IronholdSlotDebug.vanityPanelOffset();
        ironhold$blit(gfx, VANITY_TEXTURE, left + vp[0], top + vp[1],
                IronholdInventoryLayout.VANITY_PANEL_W, IronholdInventoryLayout.VANITY_PANEL_H);

        // Skills panel — right of main (offset is debug-tunable)
        int[] sp = IronholdSlotDebug.skillsPanelOffset();
        ironhold$blit(gfx, SKILLS_TEXTURE, left + sp[0], top + sp[1],
                IronholdInventoryLayout.SKILLS_PANEL_W, IronholdInventoryLayout.SKILLS_PANEL_H);

        // Player paperdoll — bounds debug-tunable
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int[] pd = IronholdSlotDebug.paperdoll();
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    gfx,
                    left + pd[0], top + pd[1],
                    left + pd[2], top + pd[3],
                    pd[4], 0.0625F, mouseX, mouseY, player);
        }

        // Edit-mode overlay: outline the selected slot in cyan, plus a header.
        if (IronholdSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }

        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(IronholdSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = IronholdSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        // Header strip at the top of the screen
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + IronholdSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  1234=group  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        // Outline whatever is currently selected
        int left = this.leftPos;
        int top = this.topPos;
        switch (IronholdSlotDebug.group()) {
            case SLOT -> {
                int[] xy = IronholdSlotDebug.slotPos(IronholdSlotDebug.selectedIndex());
                ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
            }
            case PAPERDOLL -> {
                int[] pd = IronholdSlotDebug.paperdoll();
                ironhold$drawOutline(gfx, left + pd[0], top + pd[1],
                        pd[2] - pd[0], pd[3] - pd[1], 0xFFFF55FF);
            }
            case VANITY_PANEL -> {
                int[] vp2 = IronholdSlotDebug.vanityPanelOffset();
                ironhold$drawOutline(gfx, left + vp2[0], top + vp2[1],
                        IronholdInventoryLayout.VANITY_PANEL_W, IronholdInventoryLayout.VANITY_PANEL_H, 0xFF55FF55);
            }
            case SKILLS_PANEL -> {
                int[] sp2 = IronholdSlotDebug.skillsPanelOffset();
                ironhold$drawOutline(gfx, left + sp2[0], top + sp2[1],
                        IronholdInventoryLayout.SKILLS_PANEL_W, IronholdInventoryLayout.SKILLS_PANEL_H, 0xFFFFFF55);
            }
        }
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);  // top
        gfx.fill(x,         y + h - 1, x + w, y + h, color);  // bottom
        gfx.fill(x,         y,         x + 1, y + h, color);  // left
        gfx.fill(x + w - 1, y,         x + w, y + h, color);  // right
    }

    private static void ironhold$blit(GuiGraphicsExtractor gfx, Identifier tex,
                                      int x, int y, int w, int h) {
        gfx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, w, h, w, h, w, h);
    }
}
