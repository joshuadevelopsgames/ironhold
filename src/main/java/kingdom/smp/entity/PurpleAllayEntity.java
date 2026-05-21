package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A dark purple variant of the Allay, native to the Ebonwood Hollow biome.
 * Glows in the dark and emits occasional purple particles.
 */
public class PurpleAllayEntity extends Allay {

    public PurpleAllayEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Biome boundary check — nudge back toward ebonwood every 40 ticks
        if (!level().isClientSide() && tickCount % 40 == 0) {
            if (!level().getBiome(blockPosition()).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
                // Find nearest ebonwood direction and fly back
                BlockPos pos = blockPosition();
                for (int dist = 8; dist <= 16; dist += 8) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            BlockPos check = pos.offset(dx * dist, 0, dz * dist);
                            if (level().getBiome(check).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
                                Vec3 target = Vec3.atCenterOf(check);
                                getNavigation().moveTo(target.x, target.y, target.z, 1.0);
                                return;
                            }
                        }
                    }
                }
            }
        }

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
