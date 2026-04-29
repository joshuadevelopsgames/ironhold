package kingdom.smp.rtf.data.preset;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.RTFCommon;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.registries.RTFRegistries;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;

public class PresetNoiseData {

	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		PresetTerrainNoise.bootstrap(preset, ctx);
		PresetClimateNoise.bootstrap(preset, ctx);
		PresetSurfaceNoise.bootstrap(preset, ctx);
		PresetStrataNoise.bootstrap(preset, ctx);
		PresetFeatureNoise.bootstrap(preset, ctx);
	}
	
	public static Noise getNoise(HolderGetter<Noise> noiseLookup, ResourceKey<Noise> key) {
		return new Noises.HolderHolder(noiseLookup.getOrThrow(key));
	}
	
	public static Noise registerAndWrap(BootstrapContext<Noise> ctx, ResourceKey<Noise> key, Noise noise) {
		return new Noises.HolderHolder(ctx.register(key, noise));
	}
	
	public static ResourceKey<Noise> createKey(String name) {
        return ResourceKey.create(RTFRegistries.NOISE, RTFCommon.location(name));
	}
}
