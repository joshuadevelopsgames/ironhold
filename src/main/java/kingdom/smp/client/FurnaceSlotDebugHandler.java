package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * F4 / Tab / arrow / drag debug overlay for {@link FurnaceScreen}. Mirrors the
 * inventory + crafting variants, but scoped to the furnace screen + 39 slots.
 */
public final class FurnaceSlotDebugHandler {
    private FurnaceSlotDebugHandler() {}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof FurnaceScreen)) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {
            FurnaceSlotDebug.toggleEditMode();
            chat("§b[ironhold/furnace] edit mode " + (FurnaceSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }
        if (!FurnaceSlotDebug.isEditMode()) return;

        int step = shift ? 5 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                FurnaceSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(FurnaceSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { FurnaceSlotDebug.nudge(-step, 0); chat(FurnaceSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { FurnaceSlotDebug.nudge( step, 0); chat(FurnaceSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { FurnaceSlotDebug.nudge(0, -step); chat(FurnaceSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { FurnaceSlotDebug.nudge(0,  step); chat(FurnaceSlotDebug.summary()); }
            case GLFW.GLFW_KEY_P -> {
                String dump = FurnaceSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                FurnaceSlotDebug.resetAll();
                chat("§e[ironhold/furnace] reset to defaults");
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof FurnaceScreen screen)) return;
        if (!FurnaceSlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;

        AbstractContainerScreen<?> s = screen;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (FurnaceSlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof FurnaceScreen screen)) return;
        if (!FurnaceSlotDebug.isDragging()) return;

        AbstractContainerScreen<?> s = screen;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        FurnaceSlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof FurnaceScreen)) return;
        if (!FurnaceSlotDebug.isDragging()) return;
        FurnaceSlotDebug.endDrag();
        chat(FurnaceSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
