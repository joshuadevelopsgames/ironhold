package kingdom.smp.rtf.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.data.preset.settings.ClimateSettings;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.data.preset.settings.WorldSettings;
import kingdom.smp.rtf.noise.module.Noise;

public class PresetClimateNoise {
	public static final ResourceKey<Noise> BIOME_EDGE_SHAPE = createKey("biome_edge_shape");
	
	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		
		ClimateSettings climateSettings = preset.climate();
		ClimateSettings.BiomeNoise biomeEdgeShape = climateSettings.biomeEdgeShape;
		
		ctx.register(BIOME_EDGE_SHAPE, biomeEdgeShape.build(0));
	}

	private static ResourceKey<Noise> createKey(String name) {
		return PresetNoiseData.createKey("climate/" + name);
	}
}
