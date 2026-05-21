package kingdom.smp.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Slimed — the goop a Slime Pet leaves on whatever it bites.
 *
 * <p>For its (short) duration it pins the victim down: strong, hidden Slowness plus a
 * deeply-negative Jump Boost so the jump impulse goes negative and they can't leave the
 * ground. The visible cue is a cloud of pink ooze dust. Applied for 60 ticks (3s).
 *
 * <p>The vanilla Slowness/Jump re-applications are kept hidden (no icon, no swirl) so the
 * only thing the player sees in their effect list is "Slimed".
 */
public class SlimedEffect extends MobEffect {

    /** Hot-pink ooze. */
    private static final int PINK = 0xFF6EC7;
    private static final DustParticleOptions OOZE = new DustParticleOptions(0xFF000000 | PINK, 1.2F);

    /** Negative jump boost — 0.1 * (amp + 1) = -12.7, well below the 0.42 base jump impulse. */
    private static final int JUMP_LOCK_AMPLIFIER = -128;

    public SlimedEffect() {
        super(MobEffectCategory.HARMFUL, PINK);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Keep the mechanics fresh each tick; they expire a few ticks after Slimed ends.
        entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 3, true, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 5, JUMP_LOCK_AMPLIFIER, true, false, false));

        if (level.getGameTime() % 4 == 0) {
            double dx = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            double dy = level.getRandom().nextDouble() * entity.getBbHeight();
            double dz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
            level.sendParticles(OOZE,
                entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                2, 0.0, 0.02, 0.0, 0.0);
        }
        return true;
    }
}
