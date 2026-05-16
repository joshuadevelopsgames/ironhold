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
 * F4 / Tab / arrow / drag debug overlay for the ender-chest screen
 * (3-row {@link net.minecraft.world.inventory.ChestMenu} backed by
 * {@link PlayerEnderChestContainer}).
 */
public final class EnderChestSlotDebugHandler {
    private EnderChestSlotDebugHandler() {}

    private static boolean isEnderChest(Object screen) {
        return screen instanceof ContainerScreen cs && ChestKind.isEnderChest(cs);
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!isEnderChest(event.getScreen())) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {
            EnderChestSlotDebug.toggleEditMode();
            chat("§b[ironhold/enderchest] edit mode " + (EnderChestSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }
        if (!EnderChestSlotDebug.isEditMode()) return;

        int step = shift ? 5 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                EnderChestSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(EnderChestSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { EnderChestSlotDebug.nudge(-step, 0); chat(EnderChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { EnderChestSlotDebug.nudge( step, 0); chat(EnderChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { EnderChestSlotDebug.nudge(0, -step); chat(EnderChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { EnderChestSlotDebug.nudge(0,  step); chat(EnderChestSlotDebug.summary()); }
            case GLFW.GLFW_KEY_P -> {
                String dump = EnderChestSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                EnderChestSlotDebug.resetAll();
                chat("§e[ironhold/enderchest] reset to defaults");
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isEnderChest(event.getScreen())) return;
        if (!EnderChestSlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (EnderChestSlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!isEnderChest(event.getScreen())) return;
        if (!EnderChestSlotDebug.isDragging()) return;

        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        EnderChestSlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isEnderChest(event.getScreen())) return;
        if (!EnderChestSlotDebug.isDragging()) return;
        EnderChestSlotDebug.endDrag();
        chat(EnderChestSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
