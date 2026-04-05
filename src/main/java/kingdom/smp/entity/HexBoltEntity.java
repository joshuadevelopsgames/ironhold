package kingdom.smp.entity;

import kingdom.smp.Ironhold;
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
 * Blue-fire curse projectile for Arcane Wizard HEX casts.
 * Dodgable by movement because it has travel time.
 */
public class HexBoltEntity extends AbstractHurtingProjectile {
    private int lifeTicks;

    public HexBoltEntity(EntityType<? extends HexBoltEntity> type, Level level) {
        super(type, level);
    }

    public HexBoltEntity(LivingEntity owner, Vec3 direction, Level level) {
        super(Ironhold.HEX_BOLT.get(), owner, direction, level);
        this.accelerationPower = 0.058;
    }

    @Override
    protected float getInertia() {
        return 0.98F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;
        if (this.lifeTicks > 120) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel sl)) return;
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target instanceof ArcaneWizardEntity && owner instanceof ArcaneWizardEntity) {
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.2F);
            this.discard();
            return;
        }

        float hpRatio = 1.0F;
        if (owner instanceof LivingEntity livingOwner && livingOwner.getMaxHealth() > 0.0F) {
            hpRatio = livingOwner.getHealth() / livingOwner.getMaxHealth();
        }
        int amp = hpRatio < 0.34F ? 2 : hpRatio < 0.67F ? 1 : 0;
        float damage = 5.0F + (1.0F - hpRatio) * 3.0F;

        target.hurtServer(sl, sl.damageSources().indirectMagic(this, owner), damage);
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80 + this.random.nextInt(40), amp + 1));
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 60 + this.random.nextInt(40), amp));
        }

        Vec3 pos = result.getLocation();
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 18, 0.35, 0.35, 0.35, 0.03);
        this.playSound(SoundEvents.WITCH_THROW, 0.8F, 0.8F);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = result.getLocation();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.02);
        }
        this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.1F);
        this.discard();
    }
}
