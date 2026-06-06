package kingdom.smp.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8a0303); // Dark red
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Slow bleed-out: deal 1 HP (half-heart) per hit, spaced so a full-health
        // (20 HP) player dies in ~3 minutes at level I. 20 hits over 180s = 1 hit
        // every 9s (180 ticks). Higher amplifiers bleed proportionally faster.
        //   I -> 180 ticks (9.0s, ~3 min)   II -> 90 ticks (~1.5 min)   III -> 60 ticks (~1 min)
        int interval = Math.max(20, 180 / (amplifier + 1));
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Only deal damage and bleed if not stifled
        if (!entity.hasEffect(kingdom.smp.ModEffects.STIFLED_BLEEDING_EFFECT)) {
            // Damage ignoring armor
            entity.hurtServer(level, level.damageSources().magic(), 1.0F);
            
            // Drip particles
            double dx = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double dy = level.getRandom().nextDouble() * entity.getBbHeight();
            double dz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                1, 0, 0.02, 0, 0.0);
        }
        return true;
    }
}
