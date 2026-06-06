package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private static final float BASE_DAMAGE = 6.0F;
    private static final float MAX_BONUS_DAMAGE = 3.0F;
    private static final float MAGE_DAMAGE_MULTIPLIER = 1.5F;
    private static final float BASE_BLAST_RADIUS = 2.5F;
    private static final float MAX_BONUS_RADIUS = 1.5F;
    private static final DustParticleOptions SILVER_DUST = dust(200, 210, 240, 2.0F);
    private static final DustParticleOptions BLUE_DUST = dust(100, 140, 255, 1.6F);

    private static DustParticleOptions dust(int r, int g, int b, float scale) {
        return new DustParticleOptions(0xFF000000 | (r << 16) | (g << 8) | b, scale);
    }

    private int lifeTicks;
    /** Charge power 0.0 (quick tap) to 1.0 (full charge). Scales size, damage, blast. */
    private float power;

    public LunarOrbEntity(EntityType<? extends LunarOrbEntity> type, Level level) {
        super(type, level);
    }

    public LunarOrbEntity(LivingEntity owner, Vec3 direction, Level level, float power) {
        super(kingdom.smp.ModEntities.LUNAR_ORB.get(), owner, direction, level);
        this.power = power;
        double speed = 1.0 + power * 3.5;
        this.accelerationPower = speed * (1.0 - 0.97); // 0.97 is inertia
        this.setDeltaMovement(direction.normalize().scale(speed));
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
            // Scale visual size with charge power (1x at min, ~2x at full)
            double s = 1.0 + this.power;
            double coreSpread = 0.05 * s;
            int coreCount = 2 + (int) (this.power * 3);

            // Silver core
            sl.sendParticles(SILVER_DUST, pos.x, pos.y, pos.z, coreCount, coreSpread, coreSpread, coreSpread, 0);
            sl.sendParticles(BLUE_DUST, pos.x, pos.y, pos.z, 1 + (int) (this.power * 2), coreSpread * 0.8, coreSpread * 0.8, coreSpread * 0.8, 0);

            // Spiraling dust trail
            double a = this.lifeTicks * 0.7;
            double r = (0.3 + Math.sin(this.lifeTicks * 0.25) * 0.1) * s;
            sl.sendParticles(SILVER_DUST,
                pos.x + Math.cos(a) * r,
                pos.y + Math.sin(a * 0.6) * 0.12 * s,
                pos.z + Math.sin(a) * r,
                1, 0, 0, 0, 0);
            sl.sendParticles(BLUE_DUST,
                pos.x + Math.cos(a + Math.PI) * r * 0.8,
                pos.y + Math.sin((a + Math.PI) * 0.6) * 0.12 * s,
                pos.z + Math.sin(a + Math.PI) * r * 0.8,
                1, 0, 0, 0, 0);
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
        target.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), damage);
        // Moon debuffs: slowness + brief levitation — duration scales with charge
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60 + (int) (this.power * 40), 1));
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 15 + (int) (this.power * 10), 0));
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
        // Scale everything with charge power (1x at min, ~2x at full)
        double scale = 1.0 + this.power;

        // Build a plane perpendicular to the projectile's travel direction
        Vec3 forward = this.getDeltaMovement().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        if (Math.abs(forward.dot(worldUp)) > 0.9) worldUp = new Vec3(1, 0, 0);
        Vec3 right = forward.cross(worldUp).normalize();
        Vec3 planeUp = right.cross(forward).normalize();

        double moonRadius = 1.2 * scale;
        double cutoutRadius = 1.0 * scale;
        double cutoutOffset = 0.55 * scale;
        int outlinePoints = 48 + (int) (this.power * 16);

        // ── Crescent outline ──
        for (int i = 0; i < outlinePoints; i++) {
            double angle = 2 * Math.PI * i / outlinePoints;
            double px = Math.cos(angle) * moonRadius;
            double py = Math.sin(angle) * moonRadius;

            // Only keep points outside the cutout circle
            double dx = px - cutoutOffset;
            if (dx * dx + py * py > cutoutRadius * cutoutRadius) {
                Vec3 p = pos.add(right.scale(px)).add(planeUp.scale(py));
                sl.sendParticles(SILVER_DUST, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.005);
            }
        }

        // ── Fill the crescent interior with a soft glow ──
        int fillCount = 20 + (int) (this.power * 12);
        for (int i = 0; i < fillCount; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double r = Math.sqrt(Math.random()) * moonRadius;
            double px = Math.cos(angle) * r;
            double py = Math.sin(angle) * r;
            double dx = px - cutoutOffset;
            if (dx * dx + py * py > cutoutRadius * cutoutRadius) {
                Vec3 p = pos.add(right.scale(px)).add(planeUp.scale(py));
                sl.sendParticles(BLUE_DUST, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }

        // ── Snowflake scatter around the crescent ──
        double snowSpread = 0.6 * scale;
        sl.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z,
            12 + (int) (this.power * 8), snowSpread, snowSpread, snowSpread, 0.04);

        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F + this.power * 0.2F, 0.8F - this.power * 0.2F);

        // Area slow — blast radius scales with charge
        float blastRadius = BASE_BLAST_RADIUS + this.power * MAX_BONUS_RADIUS;
        float blastRadiusSq = blastRadius * blastRadius;
        for (var e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(blastRadius), e -> e != this.getOwner() && e.isAlive())) {
            double dist = e.distanceToSqr(pos.x, pos.y, pos.z);
            if (dist < blastRadiusSq) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 + (int) (this.power * 20), 0));
            }
        }

        StaffZoneEntity zone = new StaffZoneEntity(kingdom.smp.ModEntities.STAFF_ZONE.get(), sl);
        zone.setPos(pos);
        zone.setZoneType(1);
        zone.setRadius(blastRadius);
        sl.addFreshEntity(zone);
    }

    /** Returns true if the owner is a player currently in a Mage-role class. */
    private boolean isOwnerMage() {
        return this.getOwner() instanceof ServerPlayer sp
            && "Mage".equals(sp.getData(ModAttachments.PLAYER_RPG.get()).playerClass().role());
    }
}
