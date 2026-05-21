package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * In-screen editor for the anvil forge-hammer button — needed because chat
 * commands ({@code /forgebutton}) can't be used while a container screen is
 * open. Works on the live (vanilla) anvil screen via {@link ScreenEvent} input
 * hooks, mirroring {@link AnvilSlotDebugHandler}.
 *
 * <p>Keys (anvil screen open):
 * <ul>
 *   <li><b>F6</b> — toggle edit mode (button shows a draggable outline)</li>
 *   <li><b>Arrows</b> — nudge position (Shift = ×5)</li>
 *   <li><b>[ ]</b> — shrink / grow width &nbsp; <b>- =</b> — shrink / grow height</li>
 *   <li><b>P</b> — print bakeable coords to chat + log</li>
 *   <li><b>Drag</b> — move the button with the mouse</li>
 * </ul>
 */
public final class ForgeButtonDebugHandler {
    private ForgeButtonDebugHandler() {}

    private static boolean isAnvil(Object screen) {
        return screen instanceof AnvilScreen;
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!isAnvil(event.getScreen())) return;
        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F6) {
            ForgeButtonDebug.toggleEditMode();
            chat("§b[forgebutton] edit mode " + (ForgeButtonDebug.isEditMode() ? "ON" : "OFF")
                    + (ForgeButtonDebug.isEditMode() ? " — drag the box / arrows to move, [ ] - = resize, P to print" : ""));
            event.setCanceled(true);
            return;
        }
        if (!ForgeButtonDebug.isEditMode()) return;
        int step = shift ? 5 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT  -> { ForgeButtonDebug.nudge(-step, 0); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { ForgeButtonDebug.nudge( step, 0); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { ForgeButtonDebug.nudge(0, -step); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { ForgeButtonDebug.nudge(0,  step); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_LEFT_BRACKET  -> { ForgeButtonDebug.resize(-step, 0); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> { ForgeButtonDebug.resize( step, 0); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_MINUS -> { ForgeButtonDebug.resize(0, -step); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_EQUAL -> { ForgeButtonDebug.resize(0,  step); chat(ForgeButtonDebug.summary()); }
            case GLFW.GLFW_KEY_P -> {
                String dump = ForgeButtonDebug.printJava();
                System.out.println("[forgebutton]\n" + dump);
                chat("§a[forgebutton] baked values:");
                for (String line : dump.split("\n")) chat("§7" + line.trim());
            }
            default -> { return; }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!isAnvil(event.getScreen())) return;
        if (!ForgeButtonDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;
        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (ForgeButtonDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!isAnvil(event.getScreen())) return;
        if (!ForgeButtonDebug.isDragging()) return;
        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        ForgeButtonDebug.updateDrag((int) event.getMouseX(), (int) event.getMouseY(),
                s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isAnvil(event.getScreen())) return;
        if (!ForgeButtonDebug.isDragging()) return;
        ForgeButtonDebug.endDrag();
        chat(ForgeButtonDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
    }
}
