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
        // Ashwood Wastes — hot, arid volcanic wasteland in the Ironhold dimension
        context.register(Ironhold.ASHWOOD_WASTES, new Biome.BiomeBuilder()
            .downfall(0.0f)
            .temperature(1.0f)
            .hasPrecipitation(false)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x4A2810)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Verdant Glades — humid lush temperate woodland in the Ironhold dimension
        context.register(Ironhold.VERDANT_GLADES, new Biome.BiomeBuilder()
            .downfall(0.9f)
            .temperature(0.7f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x2B7A5E)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Frostspire Tundra — frigid frozen plains in the Ironhold dimension
        context.register(Ironhold.FROSTSPIRE_TUNDRA, new Biome.BiomeBuilder()
            .downfall(0.5f)
            .temperature(-0.5f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x2860A0)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Sunken Hollow — misty low-lying valley in the Ironhold dimension
        context.register(Ironhold.SUNKEN_HOLLOW, new Biome.BiomeBuilder()
            .downfall(0.9f)
            .temperature(0.5f)
            .hasPrecipitation(true)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x2A5040)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Crystal Depths — luminous underground cave biome
        context.register(Ironhold.CRYSTAL_DEPTHS, new Biome.BiomeBuilder()
            .downfall(0.0f)
            .temperature(0.0f)
            .hasPrecipitation(false)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x184870)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
        // Obsidian Depths — deep volcanic underground cave biome near bedrock
        context.register(Ironhold.OBSIDIAN_DEPTHS, new Biome.BiomeBuilder()
            .downfall(0.0f)
            .temperature(2.0f)
            .hasPrecipitation(false)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x3D0A00)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build());
    }
}
