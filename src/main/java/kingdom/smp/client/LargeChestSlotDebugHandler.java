package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/** F4 / Tab / arrow / drag debug overlay for 6-row chest screens. */
public final class LargeChestSlotDebugHandler {
    private LargeChestSlotDebugHandler() {}

    private static boolean isSixRowChest(Object screen) {
        return screen instanceof ContainerScreen cs && cs.getMenu().getRowCount() == 6;
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!isSixRowChest(event.getScreen())) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {
            LargeChestSlotDebug.toggleEditMode();
            chat("§b[ironhold/largechest] edit mode " + (LargeChestSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }
        if (!LargeChestSlotDebug.isEditMode()) return;

        int step = shift ? 5 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                LargeChestSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(LargeChestSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { LargeChestSlotDebug.nudge(-step, 0); chat(LargeChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { LargeChestSlotDebug.nudge( step, 0); chat(LargeChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { LargeChestSlotDebug.nudge(0, -step); chat(LargeChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { LargeChestSlotDebug.nudge(0,  step); chat(LargeChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_P -> {
                String dump = LargeChestSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                LargeChestSlotDebug.resetAll();
                chat("§e[ironhold/largechest] reset to defaults");
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isSixRowChest(event.getScreen())) return;
        if (!LargeChestSlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (LargeChestSlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!isSixRowChest(event.getScreen())) return;
        if (!LargeChestSlotDebug.isDragging()) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        LargeChestSlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isSixRowChest(event.getScreen())) return;
        if (!LargeChestSlotDebug.isDragging()) return;
        LargeChestSlotDebug.endDrag();
        chat(LargeChestSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
