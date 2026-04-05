package kingdom.smp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Utility for vanity-related client checks. Equipment visuals are overridden in
 * {@link kingdom.smp.mixin.LivingEntityVanityMixin} using {@link VanityCache} plus the
 * synced {@code AccessoryInventory} attachment; inventory UIs still read real armor from menus.
 */
public final class VanityRenderHook {
    private VanityRenderHook() {}

    /** Returns {@code true} when the given player is <strong>not</strong> the local client player. */
    public static boolean isRemotePlayer(Player player) {
        Player local = Minecraft.getInstance().player;
        return local != null && !player.getUUID().equals(local.getUUID());
    }
}
