package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects the Ebonwood Hollow biome into the vanilla biome bootstrap.
 * This registers the biome key during VanillaRegistries validation,
 * satisfying the "Unreferenced key" cross-reference check.
 * The actual biome data from the JSON datapack overrides this stub at world load.
 */
@Mixin(BiomeData.class)
public class VanillaRegistriesMixin {

    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void ironhold$registerBiomes(BootstrapContext<Biome> context, CallbackInfo ci) {
        // Ebonwood Hollow — dark coniferous forest in the vanilla overworld
        context.register(Ironhold.EBONWOOD_HOLLOW, new Biome.BiomeBuilder()
            .downfall(0.4f)
            .temperature(0.4f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x0C1028)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Heartland — temperate open farmland in the Ironhold dimension
        context.register(Ironhold.HEARTLAND, new Biome.BiomeBuilder()
            .downfall(0.4f)
            .temperature(0.5f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x3B6FBE)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Moors — cold flat heathland in the Ironhold dimension
        context.register(Ironhold.MOORS, new Biome.BiomeBuilder()
            .downfall(0.8f)
            .temperature(0.2f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x3C5A6B)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Craglands — rocky highland mountains in the Ironhold dimension
        context.register(Ironhold.CRAGLANDS, new Biome.BiomeBuilder()
            .downfall(0.3f)
            .temperature(0.0f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x2B4E80)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
    }
}
