package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Slow-moving arcane bolt fired by the {@link ArcaneWizardEntity}.
 * Uses ENCHANT trail particles and deals magic damage on impact.
 * Travels at roughly half the speed of a blaze fireball so players can sidestep it.
 */
public class ArcaneBoltEntity extends AbstractHurtingProjectile {

    private static final float DAMAGE = 4.0F;
    private int lifeTicks;

    public ArcaneBoltEntity(EntityType<? extends ArcaneBoltEntity> type, Level level) {
        super(type, level);
    }

    public ArcaneBoltEntity(LivingEntity owner, Vec3 direction, Level level) {
        super(kingdom.smp.ModEntities.ARCANE_BOLT.get(), owner, direction, level);
        this.accelerationPower = 0.065;
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
        return ParticleTypes.ENCHANT;
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = this.position();
            double angle = this.lifeTicks * 0.6;
            double r = 0.25;
            sl.sendParticles(ParticleTypes.ENCHANT,
                pos.x + Math.cos(angle) * r,
                pos.y + Math.sin(angle) * r,
                pos.z + Math.sin(angle + 1.0) * r,
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
        if (result.getEntity() instanceof ArcaneWizardEntity && this.getOwner() instanceof ArcaneWizardEntity) {
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.4F, 1.2F);
            this.discard();
            return;
        }

        result.getEntity().hurtServer(
            sl, sl.damageSources().indirectMagic(this, this.getOwner()), DAMAGE);

        sl.sendParticles(ParticleTypes.ENCHANT,
            result.getLocation().x, result.getLocation().y, result.getLocation().z,
            12, 0.3, 0.3, 0.3, 0.05);
        this.playSound(SoundEvents.ILLUSIONER_HURT, 0.6F, 1.6F);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel sl) {
            Vec3 pos = result.getLocation();
            sl.sendParticles(ParticleTypes.ENCHANT,
                pos.x, pos.y, pos.z, 8, 0.2, 0.2, 0.2, 0.02);
        }
        this.playSound(SoundEvents.ILLUSIONER_HURT, 0.4F, 1.8F);
        this.discard();
    }
}
