package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.Ironhold;
import kingdom.smp.client.CraftingSlotDebug;
import kingdom.smp.inventory.CraftingTableLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reskin of the standalone crafting-table screen with the Ironhold custom panel:
 * <ul>
 *   <li>Resizes the panel to {@link CraftingTableLayout#MAIN_W}×{@link CraftingTableLayout#MAIN_H}.</li>
 *   <li>Replaces vanilla's stone bg with our wood-frame texture.</li>
 *   <li>Hides the vanilla recipe book button + component (Ironhold recommends JEI/REI/EMI).</li>
 *   <li>Suppresses vanilla "Crafting" / "Inventory" labels — texture has them painted in.</li>
 *   <li>Live-applies {@link CraftingSlotDebug} positions so F4 drag-tuning works.</li>
 * </ul>
 */
@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends AbstractContainerScreen<CraftingMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/crafting/crafting_main.png");

    /** Synthesized constructor — never invoked. */
    protected CraftingScreenMixin(CraftingMenu menu, Inventory inv, net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(CraftingMenu menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(CraftingTableLayout.MAIN_W);
        acc.ironhold$setImageHeight(CraftingTableLayout.MAIN_H);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$dropRecipeBook(CallbackInfo ci) {
        // CraftingScreen.init() overrides titleLabelX = 29 — wipe AFTER vanilla's set.
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;

        // Remove the recipe book toggle button.
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

        // Hide the recipe book panel itself.
        RecipeBookComponent<?> book = ((AbstractRecipeBookScreenAccessor) this).ironhold$getRecipeBookComponent();
        if (book != null && book.isVisible()) {
            book.toggleVisibility();
        }

        // Recipe book updateScreenPosition shifted leftPos; re-center.
        this.leftPos = (this.width - this.imageWidth) / 2;
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        ironhold$applyDebugPositions();

        int left = this.leftPos;
        int top = this.topPos;

        // Full-screen dim covers in-world HUD bleeding through around the panel.
        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        // Main panel
        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                CraftingTableLayout.MAIN_W, CraftingTableLayout.MAIN_H,
                CraftingTableLayout.MAIN_W, CraftingTableLayout.MAIN_H,
                CraftingTableLayout.MAIN_W, CraftingTableLayout.MAIN_H);

        if (CraftingSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }

        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(CraftingSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = CraftingSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + CraftingSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int[] xy = CraftingSlotDebug.slotPos(CraftingSlotDebug.selectedIndex());
        ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }
}
