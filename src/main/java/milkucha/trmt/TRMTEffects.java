package milkucha.trmt;

import milkucha.trmt.effect.LightnessEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge port of upstream {@code TRMTEffects}.
 *
 * <p>{@code LIGHTNESS_ENTRY} keeps the upstream Holder shape — every
 * mixin and potion reference works unchanged. {@code DeferredHolder} is
 * already a {@code Holder<T>}, so the assignment is a straight upcast.
 */
public final class TRMTEffects {

    private static final DeferredRegister<MobEffect> REG =
            DeferredRegister.create(Registries.MOB_EFFECT, TRMT.MOD_ID);

    private static final DeferredHolder<MobEffect, LightnessEffect> H_LIGHTNESS =
            REG.register("lightness", LightnessEffect::new);

    /**
     * Holder accessor used by potion suppliers that run at registry-event time
     * (before {@link #resolve()} fires). {@link DeferredHolder} is itself a
     * {@code Holder<MobEffect>}, so passing it is safe even before the
     * underlying effect is registered — the holder resolves lazily.
     */
    static Holder<MobEffect> lightnessHolder() {
        return H_LIGHTNESS;
    }

    public static Holder<MobEffect> LIGHTNESS_ENTRY;

    private TRMTEffects() {}

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }

    public static void resolve() {
        LIGHTNESS_ENTRY = H_LIGHTNESS;
    }

    static void touch() {}
}
