package kingdom.smp.game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Rest/campfire healing (Phase 5 ④): when out of combat near a lit campfire, the player slowly regains
 * health. Purely additive — stacks on vanilla regen, no gating. Registered to the game bus in {@code Ironhold}.
 */
public final class CampfireRestHandler {
    private CampfireRestHandler() {}

    private static final int OUT_OF_COMBAT_TICKS = 160; // 8s since last damage
    private static final int HEAL_INTERVAL = 40;        // heal tick every 2s
    private static final float HEAL_AMOUNT = 1.0F;

    private static final Map<UUID, Long> lastDamage = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDamaged(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            lastDamage.put(p.getUUID(), p.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % HEAL_INTERVAL != 0 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now - lastDamage.getOrDefault(player.getUUID(), -1000L) < OUT_OF_COMBAT_TICKS) {
            return; // still in combat
        }
        if (!(player.level() instanceof ServerLevel level) || !nearLitCampfire(level, player.blockPosition())) {
            return;
        }
        player.heal(HEAL_AMOUNT);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.6, player.getZ(),
            1, 0.2, 0.2, 0.2, 0.0);
    }

    private static boolean nearLitCampfire(ServerLevel level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
            BlockState st = level.getBlockState(p);
            if (st.is(BlockTags.CAMPFIRES)
                    && st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT)) {
                return true;
            }
        }
        return false;
    }
}
