package kingdom.smp.rtf;

import kingdom.smp.rtf.biome.modifier.BiomeModifiers;
import kingdom.smp.rtf.densityfunction.RTFDensityFunctions;
import kingdom.smp.rtf.feature.RTFFeatures;
import kingdom.smp.rtf.feature.chance.RTFChanceModifiers;
import kingdom.smp.rtf.feature.placement.RTFPlacementModifiers;
import kingdom.smp.rtf.feature.template.decorator.TemplateDecorators;
import kingdom.smp.rtf.feature.template.placement.TemplatePlacements;
import kingdom.smp.rtf.floatproviders.RTFFloatProviderTypes;
import kingdom.smp.rtf.heightproviders.RTFHeightProviderTypes;
import kingdom.smp.rtf.chunkgen.RTFChunkGenerators;
import kingdom.smp.rtf.noise.domain.Domains;
import kingdom.smp.rtf.noise.function.CurveFunctions;
import kingdom.smp.rtf.noise.module.Noises;
import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.registries.RTFBuiltInRegistries;
import kingdom.smp.rtf.registries.RTFRegistries;
import kingdom.smp.rtf.structure.rule.StructureRules;
import kingdom.smp.rtf.surface.condition.RTFSurfaceConditions;
import kingdom.smp.rtf.surface.rule.RTFSurfaceRules;
import kingdom.smp.rtf.data.preset.settings.Preset;
import net.neoforged.bus.api.IEventBus;

public final class RTFBootstrap {
    private static boolean initialized = false;

    private RTFBootstrap() {
    }

    public static synchronized void init(IEventBus modBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        RegistryUtil.createDataRegistry(RTFRegistries.PRESET, Preset.CODEC);

        RTFBuiltInRegistries.bootstrap();
        RTFDensityFunctions.bootstrap();
        RTFFloatProviderTypes.bootstrap();
        RTFHeightProviderTypes.bootstrap();
        RTFSurfaceConditions.bootstrap();
        RTFSurfaceRules.bootstrap();
        Noises.bootstrap();
        Domains.bootstrap();
        CurveFunctions.bootstrap();
        RTFChanceModifiers.bootstrap();
        RTFFeatures.bootstrap();
        RTFPlacementModifiers.bootstrap();
        TemplatePlacements.bootstrap();
        TemplateDecorators.bootstrap();
        StructureRules.bootstrap();
        BiomeModifiers.bootstrap();
        RTFChunkGenerators.bootstrap();

        RegistryUtil.bindAll(modBus);
    }
}
