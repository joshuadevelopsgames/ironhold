package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Shipwreck Mimic — an underwater variant of the Mimic that spawns in
 * ocean structures. Tougher, slightly slower, and drops aquatic loot.
 * Reuses the chest model but with a barnacle/waterlogged texture.
 */
public class ShipwreckMimicEntity extends MimicEntity {

    public ShipwreckMimicEntity(EntityType<? extends ShipwreckMimicEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 55.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.7)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // Underwater bubble particles
        if (this.level().isClientSide() && this.isInWater() && this.tickCount % 10 == 0) {
            this.level().addParticle(ParticleTypes.BUBBLE,
                this.getRandomX(0.5), this.getY() + 0.5, this.getRandomZ(0.5),
                0, 0.05, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH_WATER;
    }
}
