package kingdom.smp.rtf.data;

import java.util.Set;

import kingdom.smp.Ironhold;
import kingdom.smp.rtf.data.preset.PresetNoiseGeneratorSettings;
import kingdom.smp.rtf.data.preset.PresetNoiseRouterData;
import kingdom.smp.rtf.data.preset.settings.BuiltinPresets;
import kingdom.smp.rtf.data.preset.settings.Preset;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Bake the active RTF preset into datapack JSONs that override
 * {@code data/minecraft/worldgen/density_function/...} and
 * {@code data/minecraft/worldgen/noise_settings/overworld.json}.
 *
 * <p>This makes vanilla's {@link net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator} read RTF cell data
 * (via the {@link kingdom.smp.rtf.densityfunction.CellSampler.Marker} / {@link kingdom.smp.rtf.densityfunction.NoiseSampler.Marker}
 * markers wired in {@link kingdom.smp.mixin.RandomStateRTFMixin}) instead of vanilla noise samplers.</p>
 */
@EventBusSubscriber(modid = Ironhold.MODID)
public final class RTFData {
    private RTFData() {}

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent.Client event) {
        Preset preset = BuiltinPresets.makeDefault();

        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.DENSITY_FUNCTION, ctx -> PresetNoiseRouterData.bootstrap(preset, ctx));
        builder.add(Registries.NOISE_SETTINGS, ctx -> PresetNoiseGeneratorSettings.bootstrap(preset, ctx));

        event.createDatapackRegistryObjects(builder, Set.of("minecraft"));
    }
}
