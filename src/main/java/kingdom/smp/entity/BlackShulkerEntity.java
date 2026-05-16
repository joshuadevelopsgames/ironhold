package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Black Shulker — glass-cannon void assassin. Shorter HP than vanilla, harder
 * teleport-strike. Telegraphs the strike with a 10-tick wind-up of portal
 * particles + a soft endermen-teleport stinger at the destination so alert
 * players can react. Bullets it fires apply Blindness instead of Levitation
 * (handled in {@link kingdom.smp.IronholdGameEvents}).
 */
public class BlackShulkerEntity extends Shulker {

    private static final int TELEPORT_COOLDOWN_TICKS = 80;
    private static final int WINDUP_TICKS = 10;
    private static final double TELEPORT_RANGE = 16.0;
    private static final double TELEPORT_MIN_RANGE_SQ = 9.0;
    private static final float TELEPORT_STRIKE_DAMAGE = 6.0F;
    private static final int BLIND_TICKS = 60;

    private int teleportCooldown;
    private int windupTicks;
    private Vec3 strikeDest;
    private LivingEntity strikeTarget;

    public BlackShulkerEntity(EntityType<? extends Shulker> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    /**
     * Copy the wall-attachment direction from the vanilla shulker we're
     * replacing, so the variant doesn't snap to a default face mid-spawn
     * and end up facing the wrong way (or floating away from the wall).
     */
    public void inheritAttachStateFrom(Shulker source) {
        this.entityData.set(DATA_ATTACH_FACE_ID, source.getAttachFace());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Shulker.createAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide() || !(level() instanceof ServerLevel server)) {
            return;
        }

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }

        if (windupTicks > 0) {
            tickWindup(server);
            return;
        }

        LivingEntity target = getTarget();
        if (target != null && teleportCooldown == 0
            && target.isAlive()
            && distanceToSqr(target) <= TELEPORT_RANGE * TELEPORT_RANGE
            && distanceToSqr(target) > TELEPORT_MIN_RANGE_SQ
            && hasLineOfSight(target)) {
            beginWindup(server, target);
        }
    }

    private void beginWindup(ServerLevel server, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(position());
        if (toTarget.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 dir = toTarget.normalize();
        strikeDest = target.position().subtract(dir.scale(1.5));
        strikeTarget = target;
        windupTicks = WINDUP_TICKS;

        server.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 0.4F, 0.6F);
    }

    private void tickWindup(ServerLevel server) {
        windupTicks--;
        if (strikeDest != null) {
            server.sendParticles(ParticleTypes.PORTAL,
                strikeDest.x, strikeDest.y + 0.6, strikeDest.z,
                4, 0.3, 0.4, 0.3, 0.4);
            server.sendParticles(ParticleTypes.PORTAL,
                getX(), getY() + 0.5, getZ(),
                3, 0.25, 0.25, 0.25, 0.3);
        }
        if (windupTicks <= 0) {
            executeStrike(server);
            strikeDest = null;
            strikeTarget = null;
        }
    }

    private void executeStrike(ServerLevel server) {
        Vec3 dest = strikeDest;
        LivingEntity target = strikeTarget;
        // Pay the cooldown for the windup unconditionally — otherwise a player
        // who kills the target mid-windup lets the shulker immediately re-windup
        // and burn the telegraph.
        teleportCooldown = TELEPORT_COOLDOWN_TICKS;

        if (dest == null || target == null || !target.isAlive()) {
            return;
        }
        // Re-check at strike time so dodging during the windup actually works.
        if (distanceToSqr(target) > TELEPORT_RANGE * TELEPORT_RANGE
            || !hasLineOfSight(target)) {
            return;
        }

        boolean teleported = randomTeleport(dest.x, dest.y, dest.z, true);
        if (!teleported) {
            return;
        }

        server.sendParticles(ParticleTypes.PORTAL,
            getX(), getY() + 0.5, getZ(),
            16, 0.4, 0.4, 0.4, 0.5);
        server.sendParticles(ParticleTypes.SMOKE,
            getX(), getY() + 0.5, getZ(),
            8, 0.3, 0.3, 0.3, 0.02);
        server.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 0.7F, 1.1F);

        target.hurtServer(server, damageSources().mobAttack(this), TELEPORT_STRIKE_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, true));
    }
}
