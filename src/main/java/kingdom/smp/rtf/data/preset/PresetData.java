package kingdom.smp.rtf.data.preset;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.registries.RTFRegistries;

//TODO support different presets per dimension
public class PresetData {
	public static final ResourceKey<Preset> PRESET = RTFRegistries.createKey(RTFRegistries.PRESET, "preset");
	
	public static void bootstrap(Preset preset, BootstrapContext<Preset> ctx) {
		ctx.register(PRESET, preset);
	}
}
