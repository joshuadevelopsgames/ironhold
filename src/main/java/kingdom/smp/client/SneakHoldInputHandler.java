package kingdom.smp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Keeps the player sneaking while a GUI screen is open AND across the close
 * transition, until the player physically releases the sneak key.
 *
 * <p>Vanilla behavior: opening any screen un-sneaks you, because the
 * {@link net.minecraft.client.KeyMapping#isDown()} check for {@code keyShift}
 * gates on {@code KeyConflictContext.IN_GAME}, so {@link
 * net.minecraft.client.player.KeyboardInput#tick()} writes {@code shift=false}
 * into {@link ClientInput#keyPresses} as soon as any screen is on top. The
 * close transition can also produce a 1-tick blip where the server briefly
 * sees you stand up before re-engaging sneak.
 *
 * <p>This handler:
 * <ol>
 *   <li>Captures a "force sneak" flag when a screen opens while the player
 *       is crouching.</li>
 *   <li>Forces {@code shift=true} every tick the flag is on, regardless of
 *       whether a screen is open.</li>
 *   <li>Post-close, releases the flag the moment the raw sneak key shows
 *       "not held" — i.e. once the player has removed their hand. If they
 *       were holding shift throughout, the flag releases instantly the
 *       moment they let go and there's no blip.</li>
 * </ol>
 */
public final class SneakHoldInputHandler {
    private SneakHoldInputHandler() {}

    private static boolean forceSneak = false;
    private static boolean prevScreenOpen = false;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        boolean screenOpen = mc.screen != null;
        boolean justOpened = screenOpen && !prevScreenOpen;
        prevScreenOpen = screenOpen;

        // Engage force-sneak when a screen opens with the player crouching,
        // or refresh it any tick a screen is open and they're still crouched
        // (covers chained screen-to-screen flows).
        if (justOpened && (player.isCrouching() || mc.options.keyShift.isDown())) {
            forceSneak = true;
        } else if (screenOpen && player.isCrouching()) {
            forceSneak = true;
        }

        if (!forceSneak) return;

        // Post-close release: hand is off the sneak key → uncrouch.
        if (!screenOpen && !mc.options.keyShift.isDown()) {
            forceSneak = false;
            return;
        }

        // Override the input's shift flag to true.
        ClientInput ci = event.getInput();
        Input cur = ci.keyPresses;
        if (cur.shift()) return;
        ci.keyPresses = new Input(
            cur.forward(), cur.backward(), cur.left(), cur.right(),
            cur.jump(), true, cur.sprint()
        );
    }
}
