package kingdom.smp.rtf.data.preset;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import kingdom.smp.rtf.data.preset.settings.CaveSettings;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.data.preset.settings.WorldSettings;
import kingdom.smp.rtf.registries.RTFRegistries;
import kingdom.smp.rtf.noise.module.Noise;

public class PresetNoiseGeneratorSettings {
	
	public static void bootstrap(Preset preset, BootstrapContext<NoiseGeneratorSettings> ctx) {
		HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = ctx.lookup(Registries.NOISE);
		HolderGetter<Noise> noises = ctx.lookup(RTFRegistries.NOISE);
		
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;
//    	Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
    	
		CaveSettings caveSettings = preset.caves();

		ctx.register(NoiseGeneratorSettings.OVERWORLD, new NoiseGeneratorSettings(
			NoiseSettings.create(-worldDepth, worldDepth + worldHeight, 1, 2), 
			Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), 
			PresetNoiseRouterData.overworld(preset, densityFunctions, noiseParams, noises),
			// RTFSurfaceRules and RTFSurfaceConditions still need a SurfaceRegion bridge mixin before
			// PresetSurfaceRuleData can be wired in safely. For now, keep vanilla overworld surface rules
			// so the JSON is generatable without our RTF noise registry being populated at datagen time.
			SurfaceRuleData.overworld(),
			properties.spawnType.getParameterPoints(), 
			properties.seaLevel, 
			false, 
			true, 
			caveSettings.largeOreVeins, 
			false
		));
    }
}
