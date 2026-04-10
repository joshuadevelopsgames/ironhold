package kingdom.smp.entity;

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
 * Solar orb projectile — fired by the Soluna Staff during daytime.
 * Golden/fiery particles, sets targets on fire on impact, area burn.
 */
public class SolarOrbEntity extends AbstractHurtingProjectile {

    private static final float DAMAGE = 7.0F;
    private static final int FIRE_SECONDS = 4;
    private static final DustParticleOptions GOLD_DUST = dust(255, 200, 50, 2.0F);
    private static final DustParticleOptions ORANGE_DUST = dust(255, 130, 20, 1.6F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        return new DustParticleOptions(0xFF000000 | (r << 16) | (g << 8) | b, scale);
    }

    private int lifeTicks;

    public SolarOrbEntity(EntityType<? extends SolarOrbEntity> type, Level level) {
        super(type, level);
    }

    public SolarOrbEntity(LivingEntity owner, Vec3 direction, Level level) {
        super(Ironhold.SOLAR_ORB.get(), owner, direction, level);
        this.accelerationPower = 0.05;
    }

    @Override
    protected float getInertia() {
        return 0.96F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = this.position();
            // Golden core
            sl.sendParticles(GOLD_DUST, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            sl.sendParticles(ORANGE_DUST, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0);

            // Spiraling flame trail
            double a = this.lifeTicks * 0.8;
            double r = 0.3 + Math.sin(this.lifeTicks * 0.3) * 0.1;
            sl.sendParticles(ParticleTypes.FLAME,
                pos.x + Math.cos(a) * r,
                pos.y + Math.sin(a * 0.6) * 0.12,
                pos.z + Math.sin(a) * r,
                1, 0, 0, 0, 0);
            // Warm glow
            if (this.lifeTicks % 3 == 0) {
                sl.sendParticles(ParticleTypes.LAVA,
                    pos.x, pos.y, pos.z, 0, 0, 0, 0, 0);
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
        target.igniteForSeconds(FIRE_SECONDS);
        this.impactBurst(sl, result.getLocation());
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            this.impactBurst(sl, result.getLocation());
        }
        this.playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
        this.discard();
    }

    private void impactBurst(ServerLevel sl, Vec3 pos) {
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        sphereOut(sl, pos, ParticleTypes.FLAME, 20, 0.8, 0.18);
        sphereOut(sl, pos, ParticleTypes.LAVA, 8, 0.5, 0.1);
        burst(sl, pos, GOLD_DUST, 12, 0.3, 0.3, 0.3, 0.04);
        this.playSound(SoundEvents.BLAZE_SHOOT, 0.6F, 1.2F);

        // Area fire damage
        for (var e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(2.5), e -> e != this.getOwner() && e.isAlive())) {
            double dist = e.distanceToSqr(pos.x, pos.y, pos.z);
            if (dist < 6.25) {
                float dmg = (float) (DAMAGE * 0.5 * (1.0 - Math.sqrt(dist) / 2.5));
                e.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), dmg);
                e.igniteForSeconds(2);
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
