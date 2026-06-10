package kingdom.smp.game;

import java.util.Set;

import kingdom.smp.ModAttachments;
import kingdom.smp.block.BoundShrine;
import kingdom.smp.block.EnderShrineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Ender Shrine death-rescue. The vanilla Totem of Undying procs during the lethal hit (before death),
 * so reaching {@link LivingDeathEvent} already means no handheld totem saved the player — the shrine is
 * the deeper safety net. If the player has a bound shrine in the same dimension with a charge, the death
 * is cancelled and they're teleported home, healed, with a brief protective buff.
 *
 * <p>Reach: same dimension, any distance (cross-dimension = a later upgrade). Registered to the game bus
 * in {@code Ironhold}. Spec: {@code specs/fantasia-ports/03-ender-shrine.md}.
 */
public final class EnderShrineDeathHandler {
    private EnderShrineDeathHandler() {}

    // HIGH so the shrine resolves BEFORE Soulbound's stash runs — a shrine revive keeps the whole
    // inventory, so Soulbound (which skips canceled deaths) never needs to stash/restore.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BoundShrine bound = player.getData(ModAttachments.BOUND_SHRINE.get());
        GlobalPos gp = bound.pos();
        if (gp == null || !gp.dimension().equals(level.dimension())) {
            return; // unbound or in a different dimension
        }

        BlockPos pos = gp.pos();
        if (!(level.getBlockEntity(pos) instanceof EnderShrineBlockEntity shrine) || shrine.getCharges() <= 0) {
            return; // shrine gone or out of charges → normal death
        }

        event.setCanceled(true);
        shrine.consumeCharge();
        reviveAt(player, level, pos);
    }

    private static void reviveAt(ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Blastwave at the death spot before we whisk them away.
        BlockPos deathPos = player.blockPosition();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            deathPos.getX() + 0.5, deathPos.getY() + 1.0, deathPos.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.4);

        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));

        player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            Set.of(), player.getYRot(), player.getXRot(), false);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0;

        level.sendParticles(ParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 50, 0.4, 0.7, 0.4, 0.6);
        level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 1.0F);
    }
}
