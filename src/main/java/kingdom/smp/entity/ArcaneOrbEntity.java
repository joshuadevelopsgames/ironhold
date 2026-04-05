package kingdom.smp.entity;

import java.util.UUID;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
/**
 * Homing arcane orb for {@link ArcaneInvokerEntity} barrage: purple/blue dust core, spiral trail,
 * explosion-style burst on impact.
 */
public class ArcaneOrbEntity extends AbstractHurtingProjectile {

    private static final float DAMAGE = 5.5F;
    private static final DustParticleOptions CORE_DUST = dust(148, 96, 232, 2.0F);
    private static final DustParticleOptions CORE_DUST_ALT = dust(72, 124, 255, 1.6F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        int rgb = 0xFF000000 | (r << 16) | (g << 8) | b;
        return new DustParticleOptions(rgb, scale);
    }

    private int lifeTicks;
    private @org.jspecify.annotations.Nullable UUID targetUuid;

    public ArcaneOrbEntity(EntityType<? extends ArcaneOrbEntity> type, Level level) {
        super(type, level);
    }

    public ArcaneOrbEntity(LivingEntity owner, LivingEntity homingTarget, Vec3 initialDirection, Level level) {
        super(Ironhold.ARCANE_ORB.get(), owner, initialDirection, level);
        this.accelerationPower = 0.04;
        if (homingTarget != null) {
            this.targetUuid = homingTarget.getUUID();
        }
    }

    @Override
    protected float getInertia() {
        return 0.97F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.REVERSE_PORTAL;
    }

    private @org.jspecify.annotations.Nullable LivingEntity resolveTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(this.targetUuid);
        return e instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        LivingEntity target = this.resolveTarget();
        Vec3 motion = this.getDeltaMovement();
        if (target != null) {
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.45, 0).subtract(this.position());
            if (aim.lengthSqr() > 1.0e-4) {
                Vec3 desired = aim.normalize().scale(0.42);
                motion = motion.scale(0.22).add(desired.scale(0.78));
                if (motion.lengthSqr() > 0.18) {
                    motion = motion.normalize().scale(0.42);
                }
                this.setDeltaMovement(motion);
            }
        }

        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = this.position();
            sl.sendParticles(CORE_DUST, pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0);
            sl.sendParticles(CORE_DUST_ALT, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0);

            double a = this.lifeTicks * 0.9;
            double r = 0.35 + Math.sin(this.lifeTicks * 0.35) * 0.12;
            sl.sendParticles(ParticleTypes.END_ROD,
                pos.x + Math.cos(a) * r,
                pos.y + Math.sin(a * 0.7) * 0.15,
                pos.z + Math.sin(a) * r,
                1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                pos.x - motion.x * 0.15,
                pos.y - motion.y * 0.15,
                pos.z - motion.z * 0.15,
                0, -motion.x * 0.08, -motion.y * 0.08, -motion.z * 0.08, 0.02);
        }

        if (this.lifeTicks > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (result.getEntity() instanceof ArcaneWizardEntity && this.getOwner() instanceof ArcaneWizardEntity) {
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.2F);
            this.discard();
            return;
        }

        result.getEntity().hurtServer(
            sl, sl.damageSources().indirectMagic(this, this.getOwner()), DAMAGE);
        this.impactBurst(sl, result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            this.impactBurst(sl, result.getLocation());
        }
        this.playSound(SoundEvents.ILLUSIONER_HURT, 0.5F, 1.5F);
        this.discard();
    }

    private void impactBurst(ServerLevel sl, Vec3 pos) {
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sphereOut(sl, pos, ParticleTypes.END_ROD, 28, 0.85, 0.22);
        sphereOut(sl, pos, ParticleTypes.REVERSE_PORTAL, 22, 1.0, 0.28);
        burst(sl, pos, dust(128, 52, 240, 1.8F), 14, 0.35, 0.35, 0.35, 0.04);
        this.playSound(SoundEvents.WITHER_SHOOT, 0.35F, 1.5F);
    }

    private static void sphereOut(ServerLevel sl, Vec3 c, ParticleOptions p, int count, double radius, double speed) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            double theta = Math.random() * Math.PI * 2;
            double sx = Math.sin(phi) * Math.cos(theta);
            double sy = Math.cos(phi);
            double sz = Math.sin(phi) * Math.sin(theta);
            sl.sendParticles(p, c.x, c.y, c.z, 0, sx, sy, sz, speed);
        }
    }

    private static void burst(ServerLevel sl, Vec3 pos, ParticleOptions p, int n, double dx, double dy, double dz, double speed) {
        sl.sendParticles(p, pos.x, pos.y, pos.z, n, dx, dy, dz, speed);
    }
}
