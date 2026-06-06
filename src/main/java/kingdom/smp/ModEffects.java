package kingdom.smp;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status-effect registrations, split out of {@link Ironhold}. */
public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, Ironhold.MODID);

    public static final DeferredHolder<MobEffect, kingdom.smp.effect.PlagueEffect> PLAGUE_EFFECT =
        MOB_EFFECTS.register("plague", kingdom.smp.effect.PlagueEffect::new);

    public static final DeferredHolder<MobEffect, kingdom.smp.effect.SlimedEffect> SLIMED_EFFECT =
        MOB_EFFECTS.register("slimed", kingdom.smp.effect.SlimedEffect::new);

    public static final DeferredHolder<MobEffect, kingdom.smp.effect.BleedingEffect> BLEEDING_EFFECT =
        MOB_EFFECTS.register("bleeding", kingdom.smp.effect.BleedingEffect::new);

    public static final DeferredHolder<MobEffect, kingdom.smp.effect.StifledBleedingEffect> STIFLED_BLEEDING_EFFECT =
        MOB_EFFECTS.register("stifled_bleeding", kingdom.smp.effect.StifledBleedingEffect::new);

    /** Low-gravity (~1/6 g) buff from eating Moonshroom Stew. */
    public static final DeferredHolder<MobEffect, kingdom.smp.effect.LunarLevityEffect> LUNAR_LEVITY_EFFECT =
        MOB_EFFECTS.register("lunar_levity", kingdom.smp.effect.LunarLevityEffect::new);

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
