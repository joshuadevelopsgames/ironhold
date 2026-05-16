package kingdom.smp.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.noise.module.Noise;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

@Mixin(RandomState.class)
public abstract class RandomStateMixin implements RTFRandomState {
    @Unique
    @Nullable
    private RegistryAccess ironhold$registryAccess;

    @Unique
    @Nullable
    private Preset ironhold$preset;

    @Unique
    @Nullable
    private GeneratorContext ironhold$generatorContext;

    @Override
    public void initialize(RegistryAccess registryAccess) {
        this.ironhold$registryAccess = registryAccess;
    }

    @Override
    @Nullable
    public RegistryAccess registryAccess() {
        return this.ironhold$registryAccess;
    }

    @Override
    @Nullable
    public Preset preset() {
        return this.ironhold$preset;
    }

    @Override
    @Nullable
    public GeneratorContext generatorContext() {
        return this.ironhold$generatorContext;
    }

    @Override
    public void setPreset(@Nullable Preset preset) {
        this.ironhold$preset = preset;
    }

    @Override
    public void setGeneratorContext(@Nullable GeneratorContext generatorContext) {
        this.ironhold$generatorContext = generatorContext;
    }

    @Override
    public DensityFunction wrap(DensityFunction function) {
        return function;
    }

    @Override
    public Noise wrap(Noise noise) {
        return noise;
    }
}
