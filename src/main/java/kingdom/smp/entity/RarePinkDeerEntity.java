package kingdom.smp.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Rare Pink Deer — a 1-in-1000 variant of the Pink Deer with a simpler,
 * ethereal pale coat. Behaviourally identical to the normal Pink Deer;
 * the only difference is the texture used by its renderer.
 */
public class RarePinkDeerEntity extends PinkDeerEntity {

    public RarePinkDeerEntity(EntityType<? extends RarePinkDeerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        // Offspring always come out as normal Pink Deer
        return kingdom.smp.ModEntities.PINK_DEER.get().create(level,
            net.minecraft.world.entity.EntitySpawnReason.BREEDING);
    }
}
