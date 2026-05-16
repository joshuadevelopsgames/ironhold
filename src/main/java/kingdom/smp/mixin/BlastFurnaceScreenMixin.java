package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;
import kingdom.smp.Ironhold;
import kingdom.smp.client.BlastFurnaceSlotDebug;
import kingdom.smp.client.ScreenWidgetTuner;
import kingdom.smp.inventory.BlastFurnaceLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reskin of the vanilla BlastFurnace screen. Targets AbstractFurnaceScreen because
 * extractBackground is declared on the parent — coexists with FurnaceScreenMixin via
 * an instanceof check (regular FurnaceScreen / SmokerScreen pass through unchanged).
 *
 * <p>Slot positions are rough; drag-tune in-game with F4.
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class BlastFurnaceScreenMixin extends AbstractContainerScreen<AbstractFurnaceMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/blast_furnace/blast_furnace_main.png");

    /** Synthesized constructor — never invoked. */
    protected BlastFurnaceScreenMixin(AbstractFurnaceMenu menu, net.minecraft.world.entity.player.Inventory inv,
                                      net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    private boolean ironhold$isBlastFurnace() {
        return ((Object) this) instanceof BlastFurnaceScreen;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(CallbackInfo ci) {
        if (!ironhold$isBlastFurnace()) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(BlastFurnaceLayout.MAIN_W);
        acc.ironhold$setImageHeight(BlastFurnaceLayout.MAIN_H);
        // Labels tunable via F4 overlay — see BlastFurnaceSlotDebug label range.
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        if (!ironhold$isBlastFurnace()) return;

        ironhold$applyDebugPositions();
        ScreenWidgetTuner.ensureCaptured(BlastFurnaceSlotDebug.SCREEN_KEY, this, this.leftPos, this.topPos);
        ScreenWidgetTuner.applyPositions(BlastFurnaceSlotDebug.SCREEN_KEY, this.leftPos, this.topPos);

        ScreenWidgetTuner.ensureLabelsCaptured(BlastFurnaceSlotDebug.SCREEN_KEY, this,
                this.titleLabelX, this.titleLabelY,
                this.inventoryLabelX, this.inventoryLabelY);
        int[] titleXY = ScreenWidgetTuner.labelPos(BlastFurnaceSlotDebug.SCREEN_KEY, 0);
        int[] invXY   = ScreenWidgetTuner.labelPos(BlastFurnaceSlotDebug.SCREEN_KEY, 1);
        this.titleLabelX = titleXY[0];
        this.titleLabelY = titleXY[1];
        this.inventoryLabelX = invXY[0];
        this.inventoryLabelY = invXY[1];

        int left = this.leftPos;
        int top = this.topPos;

        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                BlastFurnaceLayout.MAIN_W, BlastFurnaceLayout.MAIN_H,
                BlastFurnaceLayout.SRC_W,  BlastFurnaceLayout.SRC_H,
                BlastFurnaceLayout.SRC_W,  BlastFurnaceLayout.SRC_H);

        if (BlastFurnaceSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }
        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(BlastFurnaceSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = BlastFurnaceSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + BlastFurnaceSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int sel = BlastFurnaceSlotDebug.selectedIndex();
        int[] xy = BlastFurnaceSlotDebug.slotPos(sel);
        int widgetCount = ScreenWidgetTuner.widgetCount(BlastFurnaceSlotDebug.SCREEN_KEY);
        if (sel < BlastFurnaceSlotDebug.SLOT_COUNT) {
            ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
        } else if (sel < BlastFurnaceSlotDebug.SLOT_COUNT + widgetCount) {
            int wi = sel - BlastFurnaceSlotDebug.SLOT_COUNT;
            int[] box = ScreenWidgetTuner.widgetBox(BlastFurnaceSlotDebug.SCREEN_KEY, wi);
            ironhold$drawOutline(gfx, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, 0xFFFFAA00);
        } else {
            int li = sel - BlastFurnaceSlotDebug.SLOT_COUNT - widgetCount;
            int[] box = ScreenWidgetTuner.labelBox(BlastFurnaceSlotDebug.SCREEN_KEY, li, left, top);
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
