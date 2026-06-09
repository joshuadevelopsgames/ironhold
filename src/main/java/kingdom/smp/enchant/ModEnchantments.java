package kingdom.smp.enchant;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import kingdom.smp.Ironhold;

/**
 * Custom enchantment-system registrations.
 *
 * <p>The enchantments themselves are data-driven (JSON under {@code data/ironhold/enchantment/}); the
 * only thing that needs code is any custom <em>effect type</em> they reference. Right now that's the
 * {@link NoGravityProjectileEffect} used by Trueflight.
 */
public final class ModEnchantments {
    private ModEnchantments() {}

    /** Registry of {@code projectile_spawned} / {@code post_attack} / ... entity-effect codecs. */
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_EFFECTS =
        DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Ironhold.MODID);

    /** Effect id {@code ironhold:no_gravity_projectile} — referenced by Trueflight's JSON. */
    public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<NoGravityProjectileEffect>> NO_GRAVITY_PROJECTILE =
        ENTITY_EFFECTS.register("no_gravity_projectile", () -> NoGravityProjectileEffect.CODEC);

    public static void register(IEventBus modEventBus) {
        ENTITY_EFFECTS.register(modEventBus);
    }
}
