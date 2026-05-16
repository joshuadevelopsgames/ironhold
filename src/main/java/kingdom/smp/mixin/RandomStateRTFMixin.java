package kingdom.smp.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.data.preset.PresetData;
import kingdom.smp.rtf.data.preset.settings.BuiltinPresets;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.densityfunction.CellSampler;
import kingdom.smp.rtf.densityfunction.NoiseSampler;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;
import kingdom.smp.rtf.registries.RTFRegistries;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(RandomState.class)
public abstract class RandomStateRTFMixin implements RTFRandomState {
    @Unique
    @Nullable
    private RegistryAccess ironhold$registries;

    @Unique
    @Nullable
    private Preset ironhold$preset;

    @Unique
    @Nullable
    private GeneratorContext ironhold$context;

    @Unique
    private long ironhold$seed;

    @Unique
    @Nullable
    private DensityFunction.Visitor ironhold$visitor;

    /**
     * Capture the RandomState seed and wrap the visitor that vanilla applies to the {@link NoiseRouter} so that
     * any RTF marker density functions ({@link CellSampler.Marker}, {@link NoiseSampler.Marker}) become live
     * cell- and noise-aware samplers reading from the runtime {@link GeneratorContext}.
     */
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
        ),
        require = 1
    )
    private NoiseRouter ironhold$wrapNoiseRouter(NoiseRouter router, DensityFunction.Visitor vanillaVisitor,
                                                 NoiseGeneratorSettings settings,
                                                 HolderGetter<NormalNoise.NoiseParameters> params,
                                                 long seed) {
        this.ironhold$seed = seed;
        DensityFunction.Visitor wrapped = new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (function instanceof NoiseSampler.Marker marker) {
                    return new NoiseSampler(marker.noise(), (int) seed);
                }
                if (function instanceof CellSampler.Marker marker) {
                    return new CellSampler(() -> RandomStateRTFMixin.this.ironhold$context, marker.field());
                }
                return vanillaVisitor.apply(function);
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
                return vanillaVisitor.visitNoise(holder);
            }
        };
        this.ironhold$visitor = wrapped;
        return router.mapAll(wrapped);
    }

    @Override
    public void initialize(RegistryAccess registryAccess) {
        this.ironhold$registries = registryAccess;
        if (this.ironhold$context != null) {
            return;
        }

        Preset preset = null;
        try {
            RegistryLookup<Preset> presets = registryAccess.lookupOrThrow(RTFRegistries.PRESET);
            preset = presets.get(PresetData.PRESET).map(holder -> holder.value()).orElse(null);
        } catch (IllegalStateException missingRegistry) {
            // RTFRegistries.PRESET not yet bound on this side (e.g., client lookup); fall through to default
        }
        if (preset == null) {
            preset = BuiltinPresets.makeDefault();
        }

        this.ironhold$preset = preset;
        this.ironhold$context = GeneratorContext.makeCached(preset, (int) this.ironhold$seed, 16, 2, true);
    }

    @Override
    public void setPreset(@Nullable Preset preset) {
        this.ironhold$preset = preset;
    }

    @Override
    public void setGeneratorContext(@Nullable GeneratorContext context) {
        this.ironhold$context = context;
    }

    @Override
    @Nullable
    public RegistryAccess registryAccess() {
        return this.ironhold$registries;
    }

    @Override
    @Nullable
    public Preset preset() {
        return this.ironhold$preset;
    }

    @Override
    @Nullable
    public GeneratorContext generatorContext() {
        return this.ironhold$context;
    }

    @Override
    public DensityFunction wrap(DensityFunction function) {
        DensityFunction.Visitor visitor = this.ironhold$visitor;
        return visitor == null ? function : function.mapAll(visitor);
    }

    @Override
    public Noise wrap(Noise noise) {
        return Noises.shiftSeed(noise, (int) this.ironhold$seed);
    }
}
