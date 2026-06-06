package kingdom.smp.effect;

import kingdom.smp.ModEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Softens fall damage while {@link LunarLevityEffect Lunar Levity} is active. At ~1/6 g an entity
 * strikes the ground with roughly 1/6 the impact energy, so we scale the fall distance to match —
 * big moon-jumps land gently, but deliberate cliff-dives still sting. Mirrors the moon's own soft
 * landings ({@link kingdom.smp.moon.MoonGravityHandler#onLivingFall}) without the full immunity that
 * the moon's gravity-flipping faces require.
 */
public final class LunarLevityHandler {
    private LunarLevityHandler() {}

    /** Impact energy scales with gravity: 1/6 g -> 1/6 the effective fall distance. */
    private static final float FALL_DISTANCE_FACTOR = 1.0F / 6.0F;

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().hasEffect(ModEffects.LUNAR_LEVITY_EFFECT)) {
            event.setDistance(event.getDistance() * FALL_DISTANCE_FACTOR);
        }
    }
}
