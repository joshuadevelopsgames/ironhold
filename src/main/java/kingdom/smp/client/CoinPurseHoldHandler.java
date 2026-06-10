package kingdom.smp.client;

import kingdom.smp.item.CoinPurseItem;
import kingdom.smp.net.CoinPurseWithdrawPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Press-and-hold withdraw for the {@link CoinPurseItem}: holding right-click on the purse in any
 * container screen pulls coins onto the cursor, accelerating the longer you hold (1 → 5 → 20 → 50 per
 * pulse). The right-click is cancelled so vanilla doesn't pick the purse up; left-click still moves it.
 * Each pulse sends a {@link CoinPurseWithdrawPayload}; the server does the actual withdraw.
 *
 * <p>Registered on the game bus in {@code IronholdClient}.
 */
public final class CoinPurseHoldHandler {
    private CoinPurseHoldHandler() {}

    private static final long PULSE_INTERVAL_MS = 120L;

    private static boolean holding = false;
    private static int heldSlot = -1;
    private static long holdStartMs = 0L;
    private static long lastPulseMs = 0L;

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getMouseButtonEvent().button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> acs)) {
            return;
        }
        int idx = slotIndexAt(acs, event.getMouseX(), event.getMouseY());
        if (idx < 0) {
            return;
        }
        ItemStack stack = acs.getMenu().getSlot(idx).getItem();
        if (!(stack.getItem() instanceof CoinPurseItem) || CoinPurseItem.getBalance(stack) <= 0) {
            return; // not a purse (or empty) → let vanilla handle it
        }
        holding = true;
        heldSlot = idx;
        holdStartMs = System.currentTimeMillis();
        pulse(1); // first coin immediately
        event.setCanceled(true); // suppress the vanilla pickup; we withdraw instead
    }

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getMouseButtonEvent().button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            stop();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!holding) {
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
            stop();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPulseMs < PULSE_INTERVAL_MS) {
            return;
        }
        long held = now - holdStartMs;
        int amount = held < 600 ? 1 : held < 1500 ? 5 : held < 3000 ? 20 : 50;
        pulse(amount);
    }

    private static void pulse(int amount) {
        if (Minecraft.getInstance().player != null && heldSlot >= 0) {
            ClientPayloads.sendToServer(new CoinPurseWithdrawPayload(heldSlot, amount));
            lastPulseMs = System.currentTimeMillis();
        }
    }

    private static void stop() {
        holding = false;
        heldSlot = -1;
    }

    /** Index of the menu slot under (mx, my), or -1. */
    private static int slotIndexAt(AbstractContainerScreen<?> acs, double mx, double my) {
        int guiLeft = acs.getGuiLeft();
        int guiTop = acs.getGuiTop();
        var slots = acs.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            int sx = guiLeft + s.x;
            int sy = guiTop + s.y;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                return i;
            }
        }
        return -1;
    }
}
