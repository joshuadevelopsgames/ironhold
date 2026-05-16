package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Live debug overlay for {@link InventoryScreen}. Triggered with F4 while the
 * vanilla inventory is open. See {@link IronholdSlotDebug} for the data layer.
 *
 * <pre>
 *   F4              toggle edit mode
 *   Tab / Shift+Tab cycle selected slot
 *   Arrows          nudge selected by 1px (Shift = 5px)
 *   1               group: SLOT  (default)
 *   2               group: PAPERDOLL
 *   3               group: VANITY_PANEL
 *   4               group: SKILLS_PANEL
 *   [, ]            paperdoll size −/+ (only when group=PAPERDOLL)
 *   ;, '            paperdoll width −/+
 *   ,, .            paperdoll height −/+
 *   P               print all values to chat
 *   R               reset to defaults
 * </pre>
 */
public final class IronholdSlotDebugHandler {
    private IronholdSlotDebugHandler() {}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        // F4 always works (toggle edit mode)
        if (key == GLFW.GLFW_KEY_F4) {
            IronholdSlotDebug.toggleEditMode();
            chat("§b[ironhold] edit mode " + (IronholdSlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }

        // Other keys only fire in edit mode
        if (!IronholdSlotDebug.isEditMode()) return;

        int step = shift ? 5 : 1;

        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                IronholdSlotDebug.cycleSelection(shift ? -1 : 1);
                chat(IronholdSlotDebug.summary());
            }
            case GLFW.GLFW_KEY_LEFT  -> { IronholdSlotDebug.nudge(-step, 0); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT -> { IronholdSlotDebug.nudge( step, 0); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_UP    -> { IronholdSlotDebug.nudge(0, -step); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_DOWN  -> { IronholdSlotDebug.nudge(0,  step); chat(IronholdSlotDebug.summary()); }

            case GLFW.GLFW_KEY_1 -> { IronholdSlotDebug.setGroup(IronholdSlotDebug.Group.SLOT);          chat("group → SLOT (Tab cycles)"); }
            case GLFW.GLFW_KEY_2 -> { IronholdSlotDebug.setGroup(IronholdSlotDebug.Group.PAPERDOLL);     chat("group → PAPERDOLL"); }
            case GLFW.GLFW_KEY_3 -> { IronholdSlotDebug.setGroup(IronholdSlotDebug.Group.VANITY_PANEL);  chat("group → VANITY_PANEL"); }
            case GLFW.GLFW_KEY_4 -> { IronholdSlotDebug.setGroup(IronholdSlotDebug.Group.SKILLS_PANEL);  chat("group → SKILLS_PANEL"); }

            // Paperdoll-specific resize
            case GLFW.GLFW_KEY_LEFT_BRACKET  -> { IronholdSlotDebug.resizePaperdoll(0, 0, -step); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> { IronholdSlotDebug.resizePaperdoll(0, 0,  step); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_SEMICOLON     -> { IronholdSlotDebug.resizePaperdoll(-step, 0, 0); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_APOSTROPHE    -> { IronholdSlotDebug.resizePaperdoll( step, 0, 0); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_COMMA         -> { IronholdSlotDebug.resizePaperdoll(0, -step, 0); chat(IronholdSlotDebug.summary()); }
            case GLFW.GLFW_KEY_PERIOD        -> { IronholdSlotDebug.resizePaperdoll(0,  step, 0); chat(IronholdSlotDebug.summary()); }

            case GLFW.GLFW_KEY_P -> {
                String dump = IronholdSlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\n")) chat(line);
            }
            case GLFW.GLFW_KEY_R -> {
                IronholdSlotDebug.resetAll();
                chat("§e[ironhold] reset to defaults");
            }
            default -> { return; } // don't cancel
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!IronholdSlotDebug.isEditMode()) return;
        // Only left-click drag
        if (event.getMouseButtonEvent().button() != 0) return;

        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if (IronholdSlotDebug.tryStartDrag(mx, my, screen.getGuiLeft(), screen.getGuiTop())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!IronholdSlotDebug.isDragging()) return;

        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        IronholdSlotDebug.updateDrag(mx, my, screen.getGuiLeft(), screen.getGuiTop());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        if (!IronholdSlotDebug.isDragging()) return;
        IronholdSlotDebug.endDrag();
        chat(IronholdSlotDebug.summary());
        event.setCanceled(true);
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
        }
    }
}
