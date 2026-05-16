package kingdom.smp.client;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ChestMenu;

/**
 * Client-side chest type detection. Vanilla's {@code PlayerEnderChestContainer}
 * only exists server-side — on the client every {@link ChestMenu} backs to a
 * generic {@code SimpleContainer} regardless of source. To distinguish ender
 * chests from regular chests on the client, we check the screen's title for
 * the {@code "container.enderchest"} translation key (set by
 * {@code EnderChestBlock.useWithoutItem}).
 *
 * <p>Custom-named ender chests (renamed via anvil) won't match — they fall
 * back to the regular chest reskin. That's acceptable.
 */
public final class ChestKind {
    private ChestKind() {}

    private static final String ENDER_CHEST_KEY = "container.enderchest";

    public static boolean isEnderChest(ContainerScreen screen) {
        if (screen.getMenu().getRowCount() != 3) return false;
        Component title = screen.getTitle();
        return title.getContents() instanceof TranslatableContents tc
            && ENDER_CHEST_KEY.equals(tc.getKey());
    }

    public static boolean isRegularThreeRow(ContainerScreen screen) {
        return screen.getMenu().getRowCount() == 3 && !isEnderChest(screen);
    }

    public static boolean isSixRow(ContainerScreen screen) {
        return screen.getMenu().getRowCount() == 6;
    }
}
