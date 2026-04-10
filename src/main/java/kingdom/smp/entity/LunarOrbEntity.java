package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lunar orb projectile — fired by the Soluna Staff during nighttime.
 * Silver/blue particles, applies slowness and levitation on impact.
 */
public class LunarOrbEntity extends AbstractHurtingProjectile {

    private static final float DAMAGE = 6.0F;
    private static final DustParticleOptions SILVER_DUST = dust(200, 210, 240, 2.0F);
    private static final DustParticleOptions BLUE_DUST = dust(100, 140, 255, 1.6F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        return new DustParticleOptions(0xFF000000 | (r << 16) | (g << 8) | b, scale);
    }

    private int lifeTicks;

    public LunarOrbEntity(EntityType<? extends LunarOrbEntity> type, Level level) {
        super(type, level);
    }

    public LunarOrbEntity(LivingEntity owner, Vec3 direction, Level level) {
        super(Ironhold.LUNAR_ORB.get(), owner, direction, level);
        this.accelerationPower = 0.04;
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
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = this.position();
            // Silver core
            sl.sendParticles(SILVER_DUST, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            sl.sendParticles(BLUE_DUST, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0);

            // Spiraling end rod trail (moonlight wisps)
            double a = this.lifeTicks * 0.7;
            double r = 0.3 + Math.sin(this.lifeTicks * 0.25) * 0.1;
            sl.sendParticles(ParticleTypes.END_ROD,
                pos.x + Math.cos(a) * r,
                pos.y + Math.sin(a * 0.6) * 0.12,
                pos.z + Math.sin(a) * r,
                1, 0, 0, 0, 0);
            // Frost sparkle
            if (this.lifeTicks % 3 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.x + (Math.random() - 0.5) * 0.3,
                    pos.y + (Math.random() - 0.5) * 0.3,
                    pos.z + (Math.random() - 0.5) * 0.3,
                    1, 0, 0, 0, 0.01);
            }
        }

        if (this.lifeTicks > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;
        Entity target = result.getEntity();
        target.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), DAMAGE);
        // Moon debuffs: slowness + brief levitation (gravity pull)
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 15, 0));
        }
        this.impactBurst(sl, result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            this.impactBurst(sl, result.getLocation());
        }
        this.playSound(SoundEvents.GLASS_BREAK, 0.5F, 1.8F);
        this.discard();
    }

    private void impactBurst(ServerLevel sl, Vec3 pos) {
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sphereOut(sl, pos, ParticleTypes.END_ROD, 18, 0.7, 0.15);
        sphereOut(sl, pos, ParticleTypes.SNOWFLAKE, 14, 0.9, 0.2);
        burst(sl, pos, SILVER_DUST, 12, 0.3, 0.3, 0.3, 0.04);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.8F);

        // Area slow on nearby entities
        for (var e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(2.5), e -> e != this.getOwner() && e.isAlive())) {
            double dist = e.distanceToSqr(pos.x, pos.y, pos.z);
            if (dist < 6.25) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
            }
        }
    }

    private static void sphereOut(ServerLevel sl, Vec3 c, ParticleOptions p, int count, double radius, double speed) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2.0 * Math.random() - 1.0);
            double theta = Math.random() * Math.PI * 2;
            sl.sendParticles(p, c.x, c.y, c.z, 0,
                Math.sin(phi) * Math.cos(theta),
                Math.cos(phi),
                Math.sin(phi) * Math.sin(theta), speed);
        }
    }

    private static void burst(ServerLevel sl, Vec3 pos, ParticleOptions p, int n, double dx, double dy, double dz, double speed) {
        sl.sendParticles(p, pos.x, pos.y, pos.z, n, dx, dy, dz, speed);
    }
}
