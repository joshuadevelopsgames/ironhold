package milkucha.trmt;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge port of upstream {@code TRMTPotions}.
 *
 * <p>Upstream uses Fabric's {@code FabricPotionBrewingBuilder}; on
 * NeoForge brewing recipes are added in
 * {@link RegisterBrewingRecipesEvent} on the mod event bus.
 */
@EventBusSubscriber(modid = TRMT.MOD_ID)
public final class TRMTPotions {

    private static final DeferredRegister<Potion> REG =
            DeferredRegister.create(Registries.POTION, TRMT.MOD_ID);

    // Use TRMTEffects.lightnessHolder() (the DeferredHolder itself) — NOT
    // TRMTEffects.LIGHTNESS_ENTRY, which is null at registry-build time and
    // produces a Potion with a null-effect MobEffectInstance. That broken
    // potion then NPEs in PotionContents.getColorOptional the first time the
    // creative inventory tries to render its tint. (See crash 2026-05-19.)
    private static final DeferredHolder<Potion, Potion> H_LIGHTNESS = REG.register(
            "lightness",
            () -> new Potion("trmt.lightness",
                    new MobEffectInstance(TRMTEffects.lightnessHolder(), 3600)));

    private static final DeferredHolder<Potion, Potion> H_LONG_LIGHTNESS = REG.register(
            "long_lightness",
            () -> new Potion("trmt.lightness",
                    new MobEffectInstance(TRMTEffects.lightnessHolder(), 9600)));

    public static Potion LIGHTNESS;
    public static Potion LONG_LIGHTNESS;

    private TRMTPotions() {}

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }

    public static void resolve() {
        LIGHTNESS = H_LIGHTNESS.value();
        LONG_LIGHTNESS = H_LONG_LIGHTNESS.value();
    }

    @SubscribeEvent
    static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        Holder<Potion> lightness = H_LIGHTNESS;
        Holder<Potion> longLightness = H_LONG_LIGHTNESS;
        event.getBuilder().addMix(Potions.AWKWARD, Items.FEATHER, lightness);
        event.getBuilder().addMix(lightness, Items.REDSTONE, longLightness);
    }

    static void touch() {}
}
