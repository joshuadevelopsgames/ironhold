package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.item.CloudInABottleItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Resets cloud double-jump charge on landing; applies boost in {@link kingdom.smp.net.ModNetworking}. */
public final class CloudDoubleJumpHandler {
    private CloudDoubleJumpHandler() {}

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        if (sp.onGround()) {
            sp.setData(ModAttachments.CLOUD_JUMP.get(), CloudJumpState.CHARGED);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.setData(ModAttachments.CLOUD_JUMP.get(), CloudJumpState.CHARGED);
        }
    }

    public static void tryApplyDoubleJump(ServerPlayer player) {
        if (!CloudInABottleItem.isEquipped(player)) return;
        // Server onGround can stay true for a tick after a jump; only block standing on ground.
        if (player.onGround() && player.getDeltaMovement().y <= 0.0) return;
        if (player.isInWater() || player.isInLava()) return;
        if (player.getAbilities().flying) return;
        if (player.isFallFlying()) return;
        if (player.getVehicle() != null) return;

        CloudJumpState state = player.getData(ModAttachments.CLOUD_JUMP.get());
        if (!state.midairChargeAvailable()) return;

        player.setData(ModAttachments.CLOUD_JUMP.get(), CloudJumpState.SPENT);

        Vec3 m = player.getDeltaMovement();
        double boostY = 0.52;
        player.setDeltaMovement(m.x, Math.max(m.y, 0.0) + boostY, m.z);
        player.resetFallDistance();

        if (!(player.level() instanceof ServerLevel sl)) return;

        Vec3 p = player.position();
        double feetY = p.y + 0.08;
        for (int i = 0; i < 28; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 1.35;
            double oz = (player.getRandom().nextDouble() - 0.5) * 1.35;
            sl.sendParticles(ParticleTypes.CLOUD,
                    p.x + ox, feetY, p.z + oz,
                    0, 0.0, 0.06, 0.0, 0.015);
        }
        for (int i = 0; i < 10; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 0.9;
            double oz = (player.getRandom().nextDouble() - 0.5) * 0.9;
            sl.sendParticles(ParticleTypes.POOF,
                    p.x + ox, feetY + 0.02, p.z + oz,
                    0, 0.0, 0.02, 0.0, 0.02);
        }

        // Breeze whoosh + softer wind layer (reads more “cloud” than jump alone)
        player.playSound(SoundEvents.WIND_CHARGE_THROW, 0.4F, 1.1F);
        player.playSound(SoundEvents.BREEZE_JUMP, 0.5F, 1.2F);
    }
}
