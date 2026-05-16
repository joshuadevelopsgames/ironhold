package kingdom.smp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Keeps the player sneaking while a GUI screen is open.
 *
 * <p>Vanilla behavior: opening any screen un-sneaks you, because the
 * {@link net.minecraft.client.KeyMapping#isDown()} check for {@code keyShift}
 * gates on {@code KeyConflictContext.IN_GAME}, so {@link
 * net.minecraft.client.player.KeyboardInput#tick()} writes {@code shift=false}
 * into {@link ClientInput#keyPresses} as soon as any screen is on top.
 *
 * <p>Fix: when a screen is open AND the player was crouching last tick,
 * override the input's shift to true after {@code KeyboardInput.tick()} has
 * built its record but before the rest of {@code LocalPlayer.aiStep()}
 * consumes it. The override is conditional on the existing crouching state,
 * so it never starts sneaking a non-sneaking player.
 */
public final class SneakHoldInputHandler {
    private SneakHoldInputHandler() {}

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        LocalPlayer player = mc.player;
        if (player == null) return;
        if (!player.isCrouching()) return;

        ClientInput ci = event.getInput();
        Input cur = ci.keyPresses;
        if (cur.shift()) return;
        ci.keyPresses = new Input(
            cur.forward(), cur.backward(), cur.left(), cur.right(),
            cur.jump(), true, cur.sprint()
        );
    }
}
