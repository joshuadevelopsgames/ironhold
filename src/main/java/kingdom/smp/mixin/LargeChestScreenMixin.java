package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.Ironhold;
import kingdom.smp.client.LargeChestSlotDebug;
import kingdom.smp.inventory.LargeChestLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reskin of the 6-row (large) chest screen ({@link ContainerScreen} with a 6-row {@link ChestMenu}).
 * Other row counts (1/2/3/4/5) keep vanilla rendering or are handled by {@link ChestScreenMixin}.
 */
@Mixin(ContainerScreen.class)
public abstract class LargeChestScreenMixin extends AbstractContainerScreen<ChestMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/chest/large_chest_main.png");

    /** Synthesized constructor — never invoked. */
    protected LargeChestScreenMixin(ChestMenu menu, Inventory inv, net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    private boolean ironhold$isSixRow() {
        return this.menu.getRowCount() == 6;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(ChestMenu menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {
        ironhold$debugFrameCount = 0;  // reset debug counter on each new screen open
        // DEBUG: log every chest open so we can see large chest vs ender chest
        System.out.println("[ironhold/large-init] rows=" + menu.getRowCount()
                + " titleClass=" + title.getContents().getClass().getSimpleName()
                + " title=\"" + title.getString() + "\"");
        if (menu.getRowCount() != 6) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(LargeChestLayout.MAIN_W);
        acc.ironhold$setImageHeight(LargeChestLayout.MAIN_H);
        // ContainerScreen has no init() override — set labels here, in the constructor RETURN.
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    private static int ironhold$debugFrameCount = 0;
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        boolean six = ironhold$isSixRow();
        if (ironhold$debugFrameCount++ < 3) {
            System.out.println("[ironhold/large-bg] frame=" + ironhold$debugFrameCount
                    + " isSixRow=" + six
                    + " rowCount=" + this.menu.getRowCount()
                    + " imageWidth=" + this.imageWidth + " imageHeight=" + this.imageHeight);
        }
        if (!six) return;

        // DEBUG MARKER: solid YELLOW bar at top-left if large chest mixin draws
        gfx.fill(0, 0, 200, 16, 0xFFFFFF00);
        gfx.text(this.font, "LARGE MIXIN", 4, 4, 0xFF000000, false);

        ironhold$applyDebugPositions();

        int left = this.leftPos;
        int top = this.topPos;

        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                LargeChestLayout.MAIN_W, LargeChestLayout.MAIN_H,
                LargeChestLayout.MAIN_W, LargeChestLayout.MAIN_H,
                LargeChestLayout.MAIN_W, LargeChestLayout.MAIN_H);

        if (LargeChestSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }

        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(LargeChestSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = LargeChestSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + LargeChestSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int[] xy = LargeChestSlotDebug.slotPos(LargeChestSlotDebug.selectedIndex());
        ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }
}
