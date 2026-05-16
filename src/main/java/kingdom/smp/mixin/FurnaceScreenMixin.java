package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.Ironhold;
import kingdom.smp.client.FurnaceSlotDebug;
import kingdom.smp.inventory.FurnaceLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Reskin of the regular furnace screen (not blast furnace or smoker — those keep vanilla).
 *
 * <p>The flame area is replaced with a 3-frame texture cycle:
 * <ul>
 *   <li>{@code no-flame} — fuel slot empty / not burning</li>
 *   <li>{@code half-flame} / {@code full-flame} — alternated every 10 ticks while lit (flicker)</li>
 * </ul>
 *
 * <p>Vanilla's vertical flame sprite is suppressed (our texture supplies it). The
 * burn-progress arrow is currently NOT overlaid — the painted arrow in the texture
 * is decorative; flip {@link #DRAW_PROGRESS_ARROW} on if you want vanilla's animated
 * fill on top.
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceScreenMixin extends AbstractContainerScreen<AbstractFurnaceMenu> {

    private static final Identifier TEX_NO_FLAME =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/furnace/no-flame.png");
    private static final Identifier TEX_HALF_FLAME =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/furnace/half-flame.png");
    private static final Identifier TEX_FULL_FLAME =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/furnace/full-flame.png");

    /** If true, render vanilla's burn-progress sprite over the painted arrow. */
    private static final boolean DRAW_PROGRESS_ARROW = false;

    /** Synthesized constructor — never invoked. */
    protected FurnaceScreenMixin(AbstractFurnaceMenu menu, net.minecraft.world.entity.player.Inventory inv,
                                 net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(CallbackInfo ci) {
        if (!((Object) this instanceof FurnaceScreen)) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(FurnaceLayout.MAIN_W);
        acc.ironhold$setImageHeight(FurnaceLayout.MAIN_H);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$dropRecipeBook(CallbackInfo ci) {
        if (!((Object) this instanceof FurnaceScreen)) return;
        // AbstractFurnaceScreen.init() centers titleLabelX based on imageWidth — wipe AFTER vanilla.
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;

        // Remove recipe book toggle button
        List<GuiEventListener> toRemove = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof ImageButton) toRemove.add(child);
        }
        for (GuiEventListener child : toRemove) {
            if (child instanceof AbstractWidget w) this.removeWidget(w);
        }

        // Hide the recipe book panel
        RecipeBookComponent<?> book = ((AbstractRecipeBookScreenAccessor) this).ironhold$getRecipeBookComponent();
        if (book != null && book.isVisible()) book.toggleVisibility();

        this.leftPos = (this.width - this.imageWidth) / 2;
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof FurnaceScreen)) return;

        ironhold$applyDebugPositions();

        int left = this.leftPos;
        int top = this.topPos;

        // Full-screen dim covers in-world HUD bleeding through
        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        // Pick flame state texture
        Identifier tex;
        if (!this.menu.isLit()) {
            tex = TEX_NO_FLAME;
        } else {
            // 2 Hz flicker while burning (10-tick toggle)
            long t = System.currentTimeMillis() / 250L;  // 0.25s per frame
            tex = (t % 2L == 0L) ? TEX_HALF_FLAME : TEX_FULL_FLAME;
        }

        gfx.blit(RenderPipelines.GUI_TEXTURED, tex, left, top, 0f, 0f,
                FurnaceLayout.MAIN_W, FurnaceLayout.MAIN_H,
                FurnaceLayout.MAIN_W, FurnaceLayout.MAIN_H,
                FurnaceLayout.MAIN_W, FurnaceLayout.MAIN_H);

        // Optionally overlay the vanilla animated cook-progress sprite over the painted arrow.
        if (DRAW_PROGRESS_ARROW) {
            int filled = net.minecraft.util.Mth.ceil(this.menu.getBurnProgress() * FurnaceLayout.ARROW_W);
            if (filled > 0) {
                gfx.fill(left + FurnaceLayout.ARROW_X,
                         top + FurnaceLayout.ARROW_Y,
                         left + FurnaceLayout.ARROW_X + filled,
                         top + FurnaceLayout.ARROW_Y + FurnaceLayout.ARROW_H,
                         0x80FFFFFF);  // subtle white overlay; tweak as desired
            }
        }

        if (FurnaceSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }

        ci.cancel();  // suppress vanilla bg + vanilla burn flame sprite
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(FurnaceSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = FurnaceSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + FurnaceSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int[] xy = FurnaceSlotDebug.slotPos(FurnaceSlotDebug.selectedIndex());
        ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }
}
