package kingdom.smp.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import kingdom.smp.access.SlotAccessor;
import kingdom.smp.Ironhold;
import kingdom.smp.client.StonecutterSlotDebug;
import kingdom.smp.client.ScreenWidgetTuner;
import kingdom.smp.inventory.StonecutterLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Reskin of the vanilla Stonecutter screen — HAND-EDITED (do not regenerate).
 *
 * <p>Unlike most reskins (which {@code ci.cancel()} {@code extractBackground} and substitute their
 * own panel draw wholesale), the stonecutter screen draws its scroller sprite AND the recipe-icon
 * grid INSIDE {@code extractBackground}. Cancelling would hide the recipe items (stone brick stairs,
 * walls, etc.) that the player needs to click on.
 *
 * <p>So we use a different strategy:
 * <ul>
 *   <li>{@code @Inject(HEAD)} (NOT cancellable): full-screen dim + slot/label/widget bookkeeping.</li>
 *   <li>{@code @Redirect} on the first {@code blit(...)} call in {@code extractBackground} (the BG_LOCATION
 *       panel blit): replace vanilla's 256×256 stone-panel blit with our hi-res wood-frame texture
 *       sized to MAIN_W×MAIN_H. The subsequent scroller sprite and recipe-icon draws proceed
 *       unmolested and appear on top of our texture.</li>
 *   <li>{@code @Inject(RETURN)}: edit-mode overlay (drawn last so the F4 outlines sit on top of everything).</li>
 * </ul>
 */
@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/stonecutter/stonecutter_main.png");

    /** Synthesized constructor — never invoked. */
    protected StonecutterScreenMixin(StonecutterMenu menu, Inventory inv, net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(StonecutterMenu menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(StonecutterLayout.MAIN_W);
        acc.ironhold$setImageHeight(StonecutterLayout.MAIN_H);
    }

    /**
     * Runs BEFORE vanilla's extractBackground body. Sets up all the bookkeeping (slots, widgets,
     * labels) and draws our full-screen dim. We do NOT cancel — vanilla then draws its (redirected)
     * bg blit, scroller, and recipe icons on top.
     */
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void ironhold$preBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        ironhold$applyDebugPositions();
        ScreenWidgetTuner.ensureCaptured(StonecutterSlotDebug.SCREEN_KEY, this, this.leftPos, this.topPos);
        ScreenWidgetTuner.applyPositions(StonecutterSlotDebug.SCREEN_KEY, this.leftPos, this.topPos);

        ScreenWidgetTuner.ensureLabelsCaptured(StonecutterSlotDebug.SCREEN_KEY, this,
                this.titleLabelX, this.titleLabelY,
                this.inventoryLabelX, this.inventoryLabelY);
        int[] titleXY = ScreenWidgetTuner.labelPos(StonecutterSlotDebug.SCREEN_KEY, 0);
        int[] invXY   = ScreenWidgetTuner.labelPos(StonecutterSlotDebug.SCREEN_KEY, 1);
        this.titleLabelX = titleXY[0];
        this.titleLabelY = titleXY[1];
        this.inventoryLabelX = invXY[0];
        this.inventoryLabelY = invXY[1];

        // Full-screen dim to cover in-world HUD bleed around the panel.
        gfx.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    /**
     * Intercept vanilla's panel-bg blit (the first 10-arg blit in extractBackground, which targets
     * BG_LOCATION at 256×256 source) and substitute our hi-res wood-frame texture instead.
     * Vanilla's subsequent draws (scroller sprite + recipe icons) are unaffected.
     */
    @Redirect(method = "extractBackground",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
                       ordinal = 0))
    private void ironhold$replaceBgBlit(GuiGraphicsExtractor gfx,
                                        RenderPipeline pipeline, Identifier vanillaTex,
                                        int x, int y, float u, float v,
                                        int destW, int destH, int srcW, int srcH) {
        // Use the 12-arg blit so we can pass a different source size (our texture is ~1150×1150, not 256×256).
        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, x, y, 0f, 0f,
                StonecutterLayout.MAIN_W, StonecutterLayout.MAIN_H,
                StonecutterLayout.SRC_W,  StonecutterLayout.SRC_H,
                StonecutterLayout.SRC_W,  StonecutterLayout.SRC_H);
    }

    // ── Anchor-driven re-positioning of vanilla draw calls ─────────────────────
    // These args-modifiers run BETWEEN our HEAD inject and vanilla's blits, so when vanilla
    // calls blitSprite(scroller) and extractButtons/extractRecipes, the (x, y) args have
    // already been swapped to anchor positions. Anchor originals (119,15 for scroller and
    // 52,14 for recipe_grid) are registered in StonecutterLayout's static initializer.

    /** Scroller sprite — vanilla draws at (leftPos + 119, topPos + 15 + scrollOffset). */
    @ModifyArgs(method = "extractBackground",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                         ordinal = 0))
    private void ironhold$tuneScroller(Args args) {
        int origX = args.<Integer>get(2);
        int origY = args.<Integer>get(3);
        int[] anchor = ScreenWidgetTuner.anchorPos(StonecutterSlotDebug.SCREEN_KEY, "scroller");
        // vanilla uses (leftPos+119, topPos+15+scrollOffset) — preserve the dynamic offset by
        // re-basing the constants only (119→anchor[0], 15→anchor[1]).
        args.set(2, origX - 119 + anchor[0]);
        args.set(3, origY - 15 + anchor[1]);
    }

    /** Recipe button frames — vanilla calls extractButtons(gfx, mx, my, recipeX, recipeY, lastVisible). */
    @ModifyArgs(method = "extractBackground",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/gui/screens/inventory/StonecutterScreen;extractButtons(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIII)V"))
    private void ironhold$tuneButtonsAnchor(Args args) {
        int origRecipeX = args.<Integer>get(3);
        int origRecipeY = args.<Integer>get(4);
        int[] anchor = ScreenWidgetTuner.anchorPos(StonecutterSlotDebug.SCREEN_KEY, "recipe_grid");
        args.set(3, origRecipeX - 52 + anchor[0]);
        args.set(4, origRecipeY - 14 + anchor[1]);
    }

    /** Recipe item icons — vanilla calls extractRecipes(gfx, recipeX, recipeY, lastVisible). */
    @ModifyArgs(method = "extractBackground",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/gui/screens/inventory/StonecutterScreen;extractRecipes(Lnet/minecraft/client/gui/GuiGraphicsExtractor;III)V"))
    private void ironhold$tuneRecipesAnchor(Args args) {
        int origRecipeX = args.<Integer>get(1);
        int origRecipeY = args.<Integer>get(2);
        int[] anchor = ScreenWidgetTuner.anchorPos(StonecutterSlotDebug.SCREEN_KEY, "recipe_grid");
        args.set(1, origRecipeX - 52 + anchor[0]);
        args.set(2, origRecipeY - 14 + anchor[1]);
    }

    /** Edit-mode F4 overlay — drawn AFTER vanilla's bg + scroller + recipes so it sits on top. */
    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void ironhold$postBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        if (StonecutterSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(StonecutterSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = StonecutterSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + StonecutterSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int sel = StonecutterSlotDebug.selectedIndex();
        int[] xy = StonecutterSlotDebug.slotPos(sel);
        int widgetCount = ScreenWidgetTuner.widgetCount(StonecutterSlotDebug.SCREEN_KEY);
        if (sel < StonecutterSlotDebug.SLOT_COUNT) {
            ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
        } else if (sel < StonecutterSlotDebug.SLOT_COUNT + widgetCount) {
            int wi = sel - StonecutterSlotDebug.SLOT_COUNT;
            int[] box = ScreenWidgetTuner.widgetBox(StonecutterSlotDebug.SCREEN_KEY, wi);
            ironhold$drawOutline(gfx, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, 0xFFFFAA00);
        } else {
            int li = sel - StonecutterSlotDebug.SLOT_COUNT - widgetCount;
            int[] box = ScreenWidgetTuner.labelBox(StonecutterSlotDebug.SCREEN_KEY, li, left, top);
            ironhold$drawOutline(gfx, box[0], box[1], box[2], box[3], 0xFFFF55FF);
        }
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }
}
