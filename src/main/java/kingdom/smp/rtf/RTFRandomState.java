package kingdom.smp.rtf;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.noise.module.Noise;

public interface RTFRandomState {
	void initialize(RegistryAccess registryAccess);

	void setPreset(@Nullable Preset preset);

	void setGeneratorContext(@Nullable GeneratorContext context);

	@Nullable
	RegistryAccess registryAccess();
	
	@Nullable
	Preset preset();

	@Nullable
	GeneratorContext generatorContext();
	
	DensityFunction wrap(DensityFunction function);

	Noise wrap(Noise noise);
}
