package kingdom.smp.item;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seashell — accessory that grants an underwater dash.
 *
 * <p>Modeled on {@link WraithsSigilItem}'s dash, but:
 * <ul>
 *   <li>Only triggers when the player is in water (otherwise key press is no-op).</li>
 *   <li>Triggered by a configurable keybind (default Left Shift) — discrete
 *       presses only via the client's {@code consumeClick()} loop, so holding
 *       the key won't auto-fire.</li>
 *   <li>No long cooldown — just a 3-tick anti-spam floor (~6 dashes/sec) so
 *       you can chain dashes "non-stop" by mashing the key.</li>
 * </ul>
 *
 * <p>The accessory item itself does no per-tick work. The keybind in
 * {@link kingdom.smp.client.IronholdKeys#SEASHELL_DASH} is detected client-side
 * in {@link kingdom.smp.client.ClientNeoForgeEvents}, which sends a
 * {@link kingdom.smp.net.SeashellDashPayload} to the server; the server-side
 * handler in {@link kingdom.smp.net.ModNetworking} calls {@link #tryDash}.
 */
public class SeashellItem extends AccessoryItem {

    /** Anti-spam floor — caps usable dash rate around 6/sec. */
    private static final int MIN_COOLDOWN_TICKS = 3;

    /** Forward push along look vector. Lower than the sigil's 2.0 — water has drag. */
    private static final double DASH_POWER = 1.5;

    /** Vertical kick. Lower than the sigil's 0.25 so you don't pop out of the water. */
    private static final double DASH_Y = 0.15;

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();

    public SeashellItem(Properties props) {
        super(props);
    }

    /** Equipped check that mirrors the existing {@link CloudInABottleItem#isEquipped}. */
    public static boolean isEquipped(Player player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        var seashell = Ironhold.SEASHELL.get();
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).is(seashell)) return true;
        }
        return false;
    }

    /**
     * Server-side dash entry point, called from the network handler when the
     * client reports a key press. Validates equip + in-water + cooldown, then
     * applies the velocity and FX.
     */
    public static void tryDash(ServerPlayer player) {
        if (!isEquipped(player)) return;
        if (!player.isInWater()) return;

        UUID id = player.getUUID();
        int cd = cooldowns.getOrDefault(id, 0);
        long now = player.level().getGameTime() & 0x7FFFFFFF;
        // Use cooldowns map as a "next allowed game-tick" timestamp.
        if (cd > now) return;
        cooldowns.put(id, (int) (now + MIN_COOLDOWN_TICKS));

        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * DASH_POWER, look.y * DASH_POWER + DASH_Y, look.z * DASH_POWER);
        player.hurtMarked = true;

        player.level().playSound(null, player.blockPosition(),
            SoundEvents.DOLPHIN_AMBIENT_WATER, SoundSource.PLAYERS, 0.7F, 1.4F);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.BUBBLE,
                player.getX(), player.getY() + 0.5, player.getZ(),
                20, 0.4, 0.3, 0.4, 0.05);
            sl.sendParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 0.2, player.getZ(),
                10, 0.5, 0.1, 0.5, 0.0);
        }
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        // Nothing per-tick — the dash is keypress-driven via the network packet.
    }

    @Override
    public void onUnequipped(Player player, ItemStack stack) {
        cooldowns.remove(player.getUUID());
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("Tide Surge").withStyle(ChatFormatting.AQUA),
            Component.literal("In water: tap dash key to lunge in look direction")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            Component.literal("Default key: Left Shift (rebindable)")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }
}
