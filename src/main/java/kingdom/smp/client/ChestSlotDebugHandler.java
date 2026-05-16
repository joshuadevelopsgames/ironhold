package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * F4 / Tab / arrow / drag debug overlay for 3-row chest screens
 * (single chests + barrels). Other row counts pass through to vanilla.
 */
public final class ChestSlotDebugHandler {
    private ChestSlotDebugHandler() {}

    private static boolean isThreeRowChest(Object screen) {
        return screen instanceof ContainerScreen cs && ChestKind.isRegularThreeRow(cs);
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!isThreeRowChest(event.getScreen())) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {
            ChestSlotDebug.toggleEditMode();
            chat("§b[ironhold/chest] edit mode " + (ChestSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }
        if (!ChestSlotDebug.isEditMode()) return;

        int step = shift ? 5 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                ChestSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(ChestSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { ChestSlotDebug.nudge(-step, 0); chat(ChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { ChestSlotDebug.nudge( step, 0); chat(ChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { ChestSlotDebug.nudge(0, -step); chat(ChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { ChestSlotDebug.nudge(0,  step); chat(ChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_P -> {
                String dump = ChestSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                ChestSlotDebug.resetAll();
                chat("§e[ironhold/chest] reset to defaults");
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isThreeRowChest(event.getScreen())) return;
        if (!ChestSlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (ChestSlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!isThreeRowChest(event.getScreen())) return;
        if (!ChestSlotDebug.isDragging()) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        ChestSlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isThreeRowChest(event.getScreen())) return;
        if (!ChestSlotDebug.isDragging()) return;
        ChestSlotDebug.endDrag();
        chat(ChestSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
