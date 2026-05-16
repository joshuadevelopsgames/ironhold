package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * F4 / Tab / arrow / drag debug overlay for {@link CraftingScreen}. Mirrors
 * {@link IronholdSlotDebugHandler} but targets the crafting-table screen and
 * its 46-slot data in {@link CraftingSlotDebug}.
 */
public final class CraftingSlotDebugHandler {
    private CraftingSlotDebugHandler() {}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof CraftingScreen)) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {
            CraftingSlotDebug.toggleEditMode();
            chat("§b[ironhold/craft] edit mode " + (CraftingSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }

        if (!CraftingSlotDebug.isEditMode()) return;
        int step = shift ? 5 : 1;

        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                CraftingSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(CraftingSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { CraftingSlotDebug.nudge(-step, 0); chat(CraftingSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { CraftingSlotDebug.nudge( step, 0); chat(CraftingSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { CraftingSlotDebug.nudge(0, -step); chat(CraftingSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { CraftingSlotDebug.nudge(0,  step); chat(CraftingSlotDebug.summary()); }

            case GLFW.GLFW_KEY_P -> {
                String dump = CraftingSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                CraftingSlotDebug.resetAll();
                chat("§e[ironhold/craft] reset to defaults");
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CraftingScreen screen)) return;
        if (!CraftingSlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;

        AbstractContainerScreen<?> s = screen;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (CraftingSlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof CraftingScreen screen)) return;
        if (!CraftingSlotDebug.isDragging()) return;

        AbstractContainerScreen<?> s = screen;
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        CraftingSlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof CraftingScreen)) return;
        if (!CraftingSlotDebug.isDragging()) return;
        CraftingSlotDebug.endDrag();
        chat(CraftingSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
