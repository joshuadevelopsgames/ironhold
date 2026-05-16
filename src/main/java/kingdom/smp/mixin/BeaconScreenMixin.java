package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;
import kingdom.smp.Ironhold;
import kingdom.smp.client.BeaconSlotDebug;
import kingdom.smp.client.ScreenWidgetTuner;
import kingdom.smp.inventory.BeaconLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Reskin of the vanilla Beacon screen — HAND-EDITED (do not regenerate).
 *
 * <p>BeaconScreen overrides {@code extractLabels} and ignores {@code titleLabelX/Y} entirely.
 * Instead it draws "Primary Power" at (62, 10) and "Secondary Power" at (169, 10) via centeredText.
 * We disable the generic labels and expose those two as named anchors ("primary_label",
 * "secondary_label"), then @ModifyArgs on the two centeredText calls to honor the tuned positions.
 */
@Mixin(BeaconScreen.class)
public abstract class BeaconScreenMixin extends AbstractContainerScreen<BeaconMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/beacon/beacon_main.png");

    /** Synthesized constructor — never invoked. */
    protected BeaconScreenMixin(BeaconMenu menu, Inventory inv, net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(BeaconMenu menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(BeaconLayout.MAIN_W);
        acc.ironhold$setImageHeight(BeaconLayout.MAIN_H);
        // Labels are NOT force-hidden here — they're tunable via the F4 overlay.
        // To hide a label, drag it off-screen or set its tuned (x, y) to large negatives.
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        ironhold$applyDebugPositions();
        ScreenWidgetTuner.ensureCaptured(BeaconSlotDebug.SCREEN_KEY, this, this.leftPos, this.topPos);
        ScreenWidgetTuner.applyPositions(BeaconSlotDebug.SCREEN_KEY, this.leftPos, this.topPos);
        // Labels are disabled for beacon (see BeaconLayout static init) — BeaconScreen.extractLabels
        // ignores titleLabelX/Y. The two real labels are exposed as anchors and tuned below.

        int left = this.leftPos;
        int top = this.topPos;

        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        // Stretch the hi-res source (BeaconLayout.SRC_W×SRC_H) to in-game MAIN_W×MAIN_H.
        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                BeaconLayout.MAIN_W, BeaconLayout.MAIN_H,
                BeaconLayout.SRC_W,  BeaconLayout.SRC_H,
                BeaconLayout.SRC_W,  BeaconLayout.SRC_H);

        if (BeaconSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }
        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(BeaconSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = BeaconSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + BeaconSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int sel = BeaconSlotDebug.selectedIndex();
        int[] xy = BeaconSlotDebug.slotPos(sel);
        int widgetCount = ScreenWidgetTuner.widgetCount(BeaconSlotDebug.SCREEN_KEY);
        if (sel < BeaconSlotDebug.SLOT_COUNT) {
            // Slot — 16×16 cyan box.
            ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
        } else if (sel < BeaconSlotDebug.SLOT_COUNT + widgetCount) {
            // Widget — orange box from live AbstractWidget bbox.
            int wi = sel - BeaconSlotDebug.SLOT_COUNT;
            int[] box = ScreenWidgetTuner.widgetBox(BeaconSlotDebug.SCREEN_KEY, wi);
            ironhold$drawOutline(gfx, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, 0xFFFFAA00);
        } else if (sel < BeaconSlotDebug.SLOT_COUNT + widgetCount + ScreenWidgetTuner.labelCount(BeaconSlotDebug.SCREEN_KEY)) {
            // Label — magenta box (synthetic 60×12 around label origin).
            int li = sel - BeaconSlotDebug.SLOT_COUNT - widgetCount;
            int[] box = ScreenWidgetTuner.labelBox(BeaconSlotDebug.SCREEN_KEY, li, left, top);
            ironhold$drawOutline(gfx, box[0], box[1], box[2], box[3], 0xFFFF55FF);
        } else {
            // Anchor — yellow box (synthetic 24×24 around anchor origin).
            int ai = sel - BeaconSlotDebug.SLOT_COUNT - widgetCount - ScreenWidgetTuner.labelCount(BeaconSlotDebug.SCREEN_KEY);
            int[] box = ScreenWidgetTuner.anchorBox(BeaconSlotDebug.SCREEN_KEY, ai, left, top);
            ironhold$drawOutline(gfx, box[0], box[1], box[2], box[3], 0xFFFFFF00);
        }
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }

    // ── Anchor-driven label re-positioning ─────────────────────────────────────
    // BeaconScreen.extractLabels makes two centeredText calls, drawing PRIMARY_EFFECT_LABEL
    // at (62, 10) and SECONDARY_EFFECT_LABEL at (169, 10) within the (leftPos, topPos)-translated
    // matrix. We expose those origins as the "primary_label" and "secondary_label" anchors,
    // registered in BeaconLayout's static initializer with their vanilla originals.

    /** centeredText signature: (Font, Component, int x, int y, int color). x=args[2], y=args[3]. */
    @ModifyArgs(method = "extractLabels",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
                         ordinal = 0))
    private void ironhold$tunePrimaryLabel(Args args) {
        int origX = args.<Integer>get(2);
        int origY = args.<Integer>get(3);
        int[] anchor = ScreenWidgetTuner.anchorPos(BeaconSlotDebug.SCREEN_KEY, "primary_label");
        args.set(2, origX - 62 + anchor[0]);
        args.set(3, origY - 10 + anchor[1]);
    }

    @ModifyArgs(method = "extractLabels",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
                         ordinal = 1))
    private void ironhold$tuneSecondaryLabel(Args args) {
        int origX = args.<Integer>get(2);
        int origY = args.<Integer>get(3);
        int[] anchor = ScreenWidgetTuner.anchorPos(BeaconSlotDebug.SCREEN_KEY, "secondary_label");
        args.set(2, origX - 169 + anchor[0]);
        args.set(3, origY - 10 + anchor[1]);
    }
}
