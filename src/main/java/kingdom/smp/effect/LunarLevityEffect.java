package kingdom.smp.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Lunar Levity — the buff granted by eating Moonshroom Stew. While active, your gravity drops to
 * ~1/6 g (exactly the moon's top-face gravity; see {@link kingdom.smp.moon.MoonGravityHandler}),
 * so you jump high and drift back down slowly.
 *
 * <p>Implemented purely as a transient {@link Attributes#GRAVITY GRAVITY} attribute modifier owned
 * by the mob-effect framework: it is applied when the effect is added and stripped when it expires,
 * with no per-tick work. Fall damage is softened to match the gentle descent by
 * {@link LunarLevityHandler}.
 */
public class LunarLevityEffect extends MobEffect {

    /** Stable id of the gravity modifier this effect owns (so it applies/removes cleanly). */
    public static final Identifier GRAVITY_MODIFIER_ID =
        Identifier.fromNamespaceAndPath("ironhold", "lunar_levity_gravity");

    /** -5/6 via ADD_MULTIPLIED_TOTAL leaves 1/6 g — the moon's gravity (matches MoonGravityHandler). */
    public static final double GRAVITY_FACTOR = -5.0 / 6.0;

    public LunarLevityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9FCBE8); // soft lunar blue
        this.addAttributeModifier(Attributes.GRAVITY, GRAVITY_MODIFIER_ID,
            GRAVITY_FACTOR, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(3, 8 - Math.min(amplifier, 2) * 2);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!entity.isAlive()) return true;

        double width = Math.max(0.6, entity.getBbWidth());
        double height = Math.max(1.0, entity.getBbHeight());
        int count = 1 + Math.min(amplifier, 1);
        level.sendParticles(kingdom.smp.ModParticles.LUNAR_LEVITY_MOTE.get(),
            entity.getX(), entity.getY() + height * 0.42, entity.getZ(),
            count, width * 0.36, height * 0.28, width * 0.36, 0.004);
        return true;
    }
}
