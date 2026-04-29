package kingdom.smp.rtf.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;

public class PresetFeatureNoise {
	public static final ResourceKey<Noise> MEADOW_TREES = createKey("meadow_trees");
	
	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		ctx.register(MEADOW_TREES, createMeadowTrees());
	}
	
	private static Noise createMeadowTrees() {
		return Noises.simplex(0, 75, 2);
	}
	
	public static ResourceKey<Noise> createKey(String name) {
        return PresetNoiseData.createKey("features/" + name);
	}
}
