package kingdom.smp.item;

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
 * Zephyr Charm — an accessory that grants a gale-step dash ability.
 * When the player is sprinting and sneaks, a sudden gust hurls them
 * forward in a burst of wind. 3-second cooldown.
 *
 * <p>(The class name, registry id {@code wraiths_sigil}, and texture are
 * kept for save/loot/tag compatibility; the player-facing name is the
 * "Zephyr Charm".)
 */
public class WraithsSigilItem extends AccessoryItem {

    private static final int COOLDOWN_TICKS = 60; // 3 seconds
    private static final double DASH_POWER = 2.0;
    private static final double DASH_Y = 0.25;

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static final Map<UUID, Boolean> wasSneaking = new HashMap<>();

    public WraithsSigilItem(Properties props) {
        super(props);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID id = player.getUUID();
        int cd = cooldowns.getOrDefault(id, 0);
        if (cd > 0) {
            cooldowns.put(id, cd - 1);
        }

        boolean sneakNow = player.isShiftKeyDown();
        boolean sneakPrev = wasSneaking.getOrDefault(id, false);
        wasSneaking.put(id, sneakNow);

        // Trigger: start sneaking while sprinting
        if (sneakNow && !sneakPrev && player.isSprinting() && cd <= 0) {
            performDash(sp);
            cooldowns.put(id, COOLDOWN_TICKS);
        }
    }

    private void performDash(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * DASH_POWER, DASH_Y, look.z * DASH_POWER);
        player.hurtMarked = true;

        player.level().playSound(null, player.blockPosition(),
            SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.8F, 1.1F);
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.5F, 1.3F);

        if (player.level() instanceof ServerLevel sl) {
            // A single gust poof at the launch point...
            sl.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                player.getX(), player.getY() + 0.4, player.getZ(),
                1, 0, 0, 0, 0);
            // ...wreathed in swirling wind and a wispy slipstream.
            sl.sendParticles(ParticleTypes.GUST,
                player.getX(), player.getY() + 0.5, player.getZ(),
                10, 0.3, 0.2, 0.3, 0.0);
            sl.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.2, player.getZ(),
                12, 0.4, 0.1, 0.4, 0.02);
        }
    }

    @Override
    public void onUnequipped(Player player, ItemStack stack) {
        cooldowns.remove(player.getUUID());
        wasSneaking.remove(player.getUUID());
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("Gale Step").withStyle(ChatFormatting.AQUA),
            Component.literal("Sprint + Sneak to ride the gale forward").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            Component.literal("3s cooldown").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }
}
