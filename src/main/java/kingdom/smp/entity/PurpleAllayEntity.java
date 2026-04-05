package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;

/**
 * A dark purple variant of the Allay, native to the Ebonwood Hollow biome.
 * Glows in the dark and emits occasional purple particles.
 */
public class PurpleAllayEntity extends Allay {

    public PurpleAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    /** Glow effect — renders with full brightness like enderman eyes. */
    @Override
    public boolean isCurrentlyGlowing() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Spawn occasional purple particles on both client and server
        if (level().isClientSide() && tickCount % 8 == 0) {
            double x = getX() + (getRandom().nextDouble() - 0.5) * 0.6;
            double y = getY() + 0.3 + getRandom().nextDouble() * 0.4;
            double z = getZ() + (getRandom().nextDouble() - 0.5) * 0.6;
            level().addParticle(ParticleTypes.WITCH,
                x, y, z,
                (getRandom().nextDouble() - 0.5) * 0.02,
                getRandom().nextDouble() * 0.02,
                (getRandom().nextDouble() - 0.5) * 0.02);
        }

        // Less frequent portal particles for extra mystical feel
        if (level().isClientSide() && tickCount % 20 == 0) {
            double x = getX() + (getRandom().nextDouble() - 0.5) * 0.4;
            double y = getY() + 0.2 + getRandom().nextDouble() * 0.3;
            double z = getZ() + (getRandom().nextDouble() - 0.5) * 0.4;
            level().addParticle(ParticleTypes.PORTAL,
                x, y, z, 0, 0.05, 0);
        }
    }
}
