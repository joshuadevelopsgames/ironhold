package kingdom.smp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;

/**
 * Will-o'-the-Wisp — a tiny glowing companion inspired by Terraria's Pixie
 * and Minecraft's Allay/Vex. Drifts beside its bonded player, lights the path
 * with end-rod sparks, and suppresses hostile mob spawns within {@link #LIGHT_RADIUS}
 * blocks (handled by {@link kingdom.smp.IronholdGameEvents#onMobFinalizeSpawn}).
 */
public class WillOWispEntity extends Allay {

    /** Suppress hostile spawns within this many blocks (treated as a light source). */
    public static final int LIGHT_RADIUS = 8;

    public WillOWispEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide()) return;

        // Bright core sparkle every tick — sells the "lantern" feel
        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.END_ROD,
                getX(), getY() + 0.4, getZ(),
                (getRandom().nextDouble() - 0.5) * 0.01,
                (getRandom().nextDouble() - 0.5) * 0.01,
                (getRandom().nextDouble() - 0.5) * 0.01);
        }

        // Soft halo of small flames drifting outward
        if (tickCount % 4 == 0) {
            double angle = getRandom().nextDouble() * Math.PI * 2.0;
            double r = 0.35;
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                getX() + Math.cos(angle) * r,
                getY() + 0.3 + getRandom().nextDouble() * 0.3,
                getZ() + Math.sin(angle) * r,
                0, 0.02, 0);
        }

        // Occasional pixie-dust trail
        if (tickCount % 12 == 0) {
            level().addParticle(ParticleTypes.GLOW,
                getX(), getY() + 0.3, getZ(),
                (getRandom().nextDouble() - 0.5) * 0.05,
                getRandom().nextDouble() * 0.04,
                (getRandom().nextDouble() - 0.5) * 0.05);
        }
    }

    /** Wisps are pure light — they don't burn, drown, or take fall damage well anyway. */
    @Override
    public boolean fireImmune() {
        return true;
    }
}
