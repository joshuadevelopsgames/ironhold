package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ender Eye projectile thrown by King Enderman. On impact:
 * <ul>
 *   <li>Direct hit damage to the struck entity</li>
 *   <li>Small explosion (no block damage) for splash</li>
 *   <li>Lingering slowness cloud at impact for ~5s</li>
 * </ul>
 * Visual is intentionally portal-particle heavy to read as "eye of ender".
 */
public class KingEnderEyeEntity extends AbstractHurtingProjectile implements ItemSupplier {

    private static final ItemStack DISPLAY_ITEM = new ItemStack(Items.ENDER_EYE);

    @Override
    public ItemStack getItem() {
        return DISPLAY_ITEM;
    }


    private static final float DIRECT_DAMAGE = 7.0F;
    private static final float SPLASH_DAMAGE = 4.0F;
    private static final float SPLASH_RADIUS = 2.5F;
    private static final int   CLOUD_DURATION_TICKS = 100;   // 5s
    private static final float CLOUD_RADIUS = 3.0F;
    private static final int   LIFE_TICKS = 100;

    private int lifeTicks;

    public KingEnderEyeEntity(EntityType<? extends KingEnderEyeEntity> type, Level level) {
        super(type, level);
    }

    public KingEnderEyeEntity(LivingEntity owner, Vec3 direction, Level level) {
        super(kingdom.smp.ModEntities.KING_ENDER_EYE.get(), owner, direction, level);
        this.accelerationPower = 0.045;
    }

    @Override
    protected float getInertia() {
        return 0.965F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.PORTAL;
    }

    @Override
    public void tick() {
        super.tick();
        // Extra "eye" trail flair: scattered reverse-portal particles + occasional enchant glyphs.
        if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
        this.lifeTicks++;
        if (this.lifeTicks > LIFE_TICKS) this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;
        Entity hit = result.getEntity();
        if (hit == this.getOwner()) return;
        // Direct hit damage.
        hit.hurtServer(sl, sl.damageSources().indirectMagic(this, this.getOwner()), DIRECT_DAMAGE);
        explodeAndLinger(sl, result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            explodeAndLinger(sl, result.getLocation());
        }
    }

    private void explodeAndLinger(ServerLevel sl, Vec3 pos) {
        // Splash damage — manual radius hurt instead of vanilla explode (no block damage).
        Entity owner = this.getOwner();
        for (LivingEntity living : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SPLASH_RADIUS))) {
            if (living == owner) continue;
            double dSq = living.distanceToSqr(pos);
            if (dSq > SPLASH_RADIUS * SPLASH_RADIUS) continue;
            float falloff = 1.0F - (float) (Math.sqrt(dSq) / SPLASH_RADIUS);
            living.hurtServer(sl, sl.damageSources().indirectMagic(this, owner), SPLASH_DAMAGE * falloff);
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
        }

        // Lingering slowness cloud — anyone walking through gets slowed.
        AreaEffectCloud cloud = new AreaEffectCloud(sl, pos.x, pos.y, pos.z);
        if (owner instanceof LivingEntity ownerLiving) cloud.setOwner(ownerLiving);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setRadiusOnUse(-0.05F);
        cloud.setRadiusPerTick(-CLOUD_RADIUS / CLOUD_DURATION_TICKS);
        cloud.setDuration(CLOUD_DURATION_TICKS);
        cloud.setCustomParticle(ParticleTypes.PORTAL);
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        sl.addFreshEntity(cloud);

        // Visuals + sounds for the burst.
        sl.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 60, 0.6, 0.6, 0.6, 0.4);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 24, 0.4, 0.4, 0.4, 0.1);
        sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        this.playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 0.9F, 1.4F);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5F, 0.8F);
        this.discard();
    }
}
