package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private static final float BASE_DAMAGE = 7.0F;
    private static final float MAX_BONUS_DAMAGE = 4.0F;
    private static final float MAGE_DAMAGE_MULTIPLIER = 1.5F;
    private static final int BASE_FIRE_SECONDS = 8;
    private static final float BASE_BLAST_RADIUS = 2.5F;
    private static final float MAX_BONUS_RADIUS = 1.5F;
    private static final DustParticleOptions GOLD_DUST = dust(255, 200, 50, 2.0F);
    private static final DustParticleOptions ORANGE_DUST = dust(255, 130, 20, 1.6F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        return new DustParticleOptions(0xFF000000 | (r << 16) | (g << 8) | b, scale);
    }

    private int lifeTicks;
    /** Charge power 0.0 (quick tap) to 1.0 (full charge). Scales size, damage, blast. */
    private float power;

    public SolarOrbEntity(EntityType<? extends SolarOrbEntity> type, Level level) {
        super(type, level);
    }

    public SolarOrbEntity(LivingEntity owner, Vec3 direction, Level level, float power) {
        super(Ironhold.SOLAR_ORB.get(), owner, direction, level);
        this.accelerationPower = 0.05;
        this.power = power;
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
            // Scale visual size with charge power (1x at min, ~2x at full)
            double s = 1.0 + this.power;
            double coreSpread = 0.05 * s;
            int coreCount = 2 + (int) (this.power * 3);

            // Golden core
            sl.sendParticles(GOLD_DUST, pos.x, pos.y, pos.z, coreCount, coreSpread, coreSpread, coreSpread, 0);
            sl.sendParticles(ORANGE_DUST, pos.x, pos.y, pos.z, 1 + (int) (this.power * 2), coreSpread * 0.8, coreSpread * 0.8, coreSpread * 0.8, 0);

            // Spiraling flame trail
            double a = this.lifeTicks * 0.8;
            double r = (0.3 + Math.sin(this.lifeTicks * 0.3) * 0.1) * s;
            sl.sendParticles(ParticleTypes.FLAME,
                pos.x + Math.cos(a) * r,
                pos.y + Math.sin(a * 0.6) * 0.12 * s,
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
        float mage = isOwnerMage() ? MAGE_DAMAGE_MULTIPLIER : 1.0F;
        float damage = (BASE_DAMAGE + this.power * MAX_BONUS_DAMAGE) * mage;
        int fireSecs = BASE_FIRE_SECONDS + (int) (this.power * 2);
        target.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), damage);
        target.igniteForSeconds(fireSecs);
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
        // Scale everything with charge power (1x at min, ~2x at full)
        double scale = 1.0 + this.power;

        // Build a plane perpendicular to the projectile's travel direction
        Vec3 forward = this.getDeltaMovement().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.9) worldUp = new Vec3(1, 0, 0);
        Vec3 right = forward.cross(worldUp).normalize();
        Vec3 planeUp = right.cross(forward).normalize();

        double sunRadius = 1.2 * scale;
        int circlePoints = 28 + (int) (this.power * 12);
        int rays = 8;
        int rayPoints = 4 + (int) (this.power * 2);
        double rayLength = 0.9 * scale;

        // ── Sun circle ──
        for (int i = 0; i < circlePoints; i++) {
            double angle = 2 * Math.PI * i / circlePoints;
            double lx = Math.cos(angle) * sunRadius;
            double ly = Math.sin(angle) * sunRadius;
            Vec3 p = pos.add(right.scale(lx)).add(planeUp.scale(ly));
            sl.sendParticles(GOLD_DUST, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }

        // ── Sun rays ──
        for (int i = 0; i < rays; i++) {
            double angle = 2 * Math.PI * i / rays;
            for (int j = 1; j <= rayPoints; j++) {
                double r = sunRadius + rayLength * j / rayPoints;
                double lx = Math.cos(angle) * r;
                double ly = Math.sin(angle) * r;
                Vec3 p = pos.add(right.scale(lx)).add(planeUp.scale(ly));
                sl.sendParticles(ORANGE_DUST, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
            }
        }

        // ── Center glow ──
        double glowSpread = 0.3 * scale;
        sl.sendParticles(GOLD_DUST, pos.x, pos.y, pos.z, 6 + (int) (this.power * 4), glowSpread, glowSpread, glowSpread, 0);
        sl.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 10 + (int) (this.power * 6), glowSpread * 1.2, glowSpread * 1.2, glowSpread * 1.2, 0.02);
        sl.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 4 + (int) (this.power * 3), glowSpread * 0.5, glowSpread * 0.5, glowSpread * 0.5, 0);

        this.playSound(SoundEvents.BLAZE_SHOOT, 0.6F + this.power * 0.3F, 1.2F - this.power * 0.3F);

        // Area fire damage — blast radius scales with charge
        float blastRadius = BASE_BLAST_RADIUS + this.power * MAX_BONUS_RADIUS;
        float blastRadiusSq = blastRadius * blastRadius;
        float mage = isOwnerMage() ? MAGE_DAMAGE_MULTIPLIER : 1.0F;
        float damage = (BASE_DAMAGE + this.power * MAX_BONUS_DAMAGE) * mage;
        for (var e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(blastRadius), e -> e != this.getOwner() && e.isAlive())) {
            double dist = e.distanceToSqr(pos.x, pos.y, pos.z);
            if (dist < blastRadiusSq) {
                float dmg = (float) (damage * 0.5 * (1.0 - Math.sqrt(dist) / blastRadius));
                e.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), dmg);
                e.igniteForSeconds(4 + (int) (this.power * 2));
            }
        }
    }

    /** Returns true if the owner is a player currently in a Mage-role class. */
    private boolean isOwnerMage() {
        return this.getOwner() instanceof ServerPlayer sp
            && "Mage".equals(sp.getData(ModAttachments.PLAYER_RPG.get()).playerClass().role());
    }
}
