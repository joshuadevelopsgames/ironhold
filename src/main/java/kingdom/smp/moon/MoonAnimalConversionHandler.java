package kingdom.smp.moon;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModEntities;
import kingdom.smp.entity.MoonshroomEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Lunar fungal infection: cows and mooshrooms that linger in the moon dimension slowly take on
 * the moon's bloom and turn into {@link MoonshroomEntity Moonshrooms}.
 *
 * <p>Each animal accumulates {@link ModAttachments#MOON_EXPOSURE_TICKS exposure ticks} while it is
 * on the moon; once it has spent {@link #CONVERSION_DELAY_TICKS} there it converts. The counter is
 * persisted on the entity, so the countdown is uninterrupted by chunk unloads or restarts. Leaving
 * the moon simply pauses the countdown (the accumulated time is kept), and the bookkeeping is
 * throttled to once per second to keep the per-tick cost negligible.
 */
public final class MoonAnimalConversionHandler {
    private MoonAnimalConversionHandler() {}

    /** Real-world time an animal must spend on the moon before it becomes a moonshroom (~1 minute). */
    private static final int CONVERSION_DELAY_TICKS = 20 * 60;

    /** How often (in ticks) we update an animal's exposure counter. Also the increment per update. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onAnimalTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        // Cows and mooshrooms (both extend AbstractCow) convert — but a moonshroom is the *result*,
        // so it must never re-trigger.
        if (!(entity instanceof AbstractCow animal) || entity instanceof MoonshroomEntity) {
            return;
        }
        if (!(animal.level() instanceof ServerLevel level)
            || level.dimension() != ModMoonDimensions.MOON_LEVEL) {
            return;
        }
        // Throttle to once per second; the tickCount phase naturally spreads the work across animals.
        if (animal.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        int exposure = animal.getData(ModAttachments.MOON_EXPOSURE_TICKS.get()) + CHECK_INTERVAL_TICKS;
        if (exposure < CONVERSION_DELAY_TICKS) {
            animal.setData(ModAttachments.MOON_EXPOSURE_TICKS.get(), exposure);
            return;
        }
        convertToMoonshroom(animal, level);
    }

    private static void convertToMoonshroom(AbstractCow animal, ServerLevel level) {
        // Honour other mods' veto of the conversion (e.g. protection systems).
        if (!net.neoforged.neoforge.event.EventHooks.canLivingConvert(animal, ModEntities.MOONSHROOM.get(), timer -> {})) {
            return;
        }
        boolean baby = animal.isBaby();
        animal.convertTo(ModEntities.MOONSHROOM.get(), ConversionParams.single(animal, false, false), moonshroom -> {
            net.neoforged.neoforge.event.EventHooks.onLivingConvert(animal, moonshroom);
            // SINGLE conversion doesn't carry age across, so preserve calf/adult status explicitly.
            if (baby) {
                moonshroom.setBaby(true);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, animal.getX(), animal.getY(0.5), animal.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, animal.blockPosition(), SoundEvents.MOOSHROOM_CONVERT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        });
    }
}
