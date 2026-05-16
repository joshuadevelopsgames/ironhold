package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;

import kingdom.smp.Ironhold;
import kingdom.smp.client.ChestKind;
import kingdom.smp.client.EnderChestSlotDebug;
import kingdom.smp.inventory.EnderChestLayout;
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
 * Reskin of the ender-chest screen ({@link ContainerScreen} with a 3-row {@link ChestMenu}
 * backed by a {@link PlayerEnderChestContainer}).
 *
 * <p>Distinguished from regular 3-row chests via the menu's container type — see
 * {@link ChestMenuAccessor}.
 */
@Mixin(ContainerScreen.class)
public abstract class EnderChestScreenMixin extends AbstractContainerScreen<ChestMenu> {

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/chest/ender_chest_main.png");

    /** Synthesized constructor — never invoked. */
    protected EnderChestScreenMixin(ChestMenu menu, Inventory inv, net.minecraft.network.chat.Component title) {
        super(menu, inv, title);
    }

    private boolean ironhold$isEnderChest() {
        return ChestKind.isEnderChest((ContainerScreen) (Object) this);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel(ChestMenu menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {
        ironhold$debugFrameCount = 0;
        System.out.println("[ironhold/ender-init] rows=" + menu.getRowCount()
                + " titleClass=" + title.getContents().getClass().getSimpleName()
                + " title=\"" + title.getString() + "\"");
        if (menu.getRowCount() != 3) return;
        // Detect ender chest by title key (server-side container type doesn't reach the client).
        if (!(title.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && "container.enderchest".equals(tc.getKey()))) return;

        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth(EnderChestLayout.MAIN_W);
        acc.ironhold$setImageHeight(EnderChestLayout.MAIN_H);
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    private static int ironhold$debugFrameCount = 0;
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        boolean ender = ironhold$isEnderChest();
        if (ironhold$debugFrameCount++ < 3) {
            System.out.println("[ironhold/ender-bg] frame=" + ironhold$debugFrameCount
                    + " isEnderChest=" + ender
                    + " imageWidth=" + this.imageWidth + " imageHeight=" + this.imageHeight);
        }
        if (!ender) return;

        // DEBUG MARKER: solid PURPLE bar at top-left if ender chest mixin draws
        gfx.fill(0, 0, 200, 16, 0xFFFF00FF);
        gfx.text(this.font, "ENDER MIXIN", 4, 4, 0xFF000000, false);

        ironhold$applyDebugPositions();

        int left = this.leftPos;
        int top = this.topPos;

        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                EnderChestLayout.MAIN_W, EnderChestLayout.MAIN_H,
                EnderChestLayout.MAIN_W, EnderChestLayout.MAIN_H,
                EnderChestLayout.MAIN_W, EnderChestLayout.MAIN_H);

        if (EnderChestSlotDebug.isEditMode()) {
            ironhold$drawEditModeOverlay(gfx);
        }

        ci.cancel();
    }

    private void ironhold$applyDebugPositions() {
        int n = Math.min(EnderChestSlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {
            int[] xy = EnderChestSlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }
    }

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + EnderChestSlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int[] xy = EnderChestSlotDebug.slotPos(EnderChestSlotDebug.selectedIndex());
        ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
    }

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }
}
