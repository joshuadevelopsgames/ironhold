package kingdom.smp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import kingdom.smp.worldgen.BlueVinesFeature;

/** Worldgen feature registrations and biome resource keys, split out of {@link Ironhold}. */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, Ironhold.MODID);

    public static final DeferredHolder<Feature<?>, BlueVinesFeature> BLUE_VINES_FEATURE =
        FEATURES.register("blue_vines", () -> new BlueVinesFeature(NoneFeatureConfiguration.CODEC));

    /** Resource key for the Ebonwood Hollow biome (data-driven; defined in worldgen/biome/). */
    public static final ResourceKey<Biome> EBONWOOD_HOLLOW = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "ebonwood_hollow"));

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
