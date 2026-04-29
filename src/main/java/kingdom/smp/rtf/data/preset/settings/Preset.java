package kingdom.smp.rtf.data.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.compat.terrablender.TBNoiseRouterData;
import kingdom.smp.rtf.data.preset.PresetBiomeData;
import kingdom.smp.rtf.data.preset.PresetBiomeModifierData;
import kingdom.smp.rtf.data.preset.PresetConfiguredCarvers;
import kingdom.smp.rtf.data.preset.PresetConfiguredFeatures;
import kingdom.smp.rtf.data.preset.PresetData;
import kingdom.smp.rtf.data.preset.PresetDimensionTypes;
import kingdom.smp.rtf.data.preset.PresetNoiseData;
import kingdom.smp.rtf.data.preset.PresetNoiseGeneratorSettings;
import kingdom.smp.rtf.data.preset.PresetNoiseParameters;
import kingdom.smp.rtf.data.preset.PresetNoiseRouterData;
import kingdom.smp.rtf.data.preset.PresetPlacedFeatures;
import kingdom.smp.rtf.data.preset.PresetStructureRuleData;
import kingdom.smp.rtf.data.preset.PresetStructureSets;
import kingdom.smp.rtf.data.preset.PresetSurfaceLayerData;
import kingdom.smp.rtf.registries.RTFRegistries;

//TODO make this actually immutable when we rework the gui
public record Preset(WorldSettings world, SurfaceSettings surface, CaveSettings caves, ClimateSettings climate, TerrainSettings terrain, RiverSettings rivers, FilterSettings filters, MiscellaneousSettings miscellaneous) {
	public static final Codec<Preset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		WorldSettings.CODEC.fieldOf("world").forGetter(Preset::world),
		SurfaceSettings.CODEC.optionalFieldOf("surface", new SurfaceSettings(new SurfaceSettings.Erosion(30, 140, 40, 95, 95, 0.65F, 0.475F, 0.4F, 0.45F, 6.0F, 3.0F))).forGetter(Preset::surface),
		CaveSettings.CODEC.optionalFieldOf("caves", new CaveSettings()).forGetter(Preset::caves),
		ClimateSettings.CODEC.fieldOf("climate").forGetter(Preset::climate),
		TerrainSettings.CODEC.fieldOf("terrain").forGetter(Preset::terrain),
		RiverSettings.CODEC.fieldOf("rivers").forGetter(Preset::rivers),
		FilterSettings.CODEC.fieldOf("filters").forGetter(Preset::filters),
		MiscellaneousSettings.CODEC.fieldOf("miscellaneous").forGetter(Preset::miscellaneous)
	).apply(instance, Preset::new));
	
	public Preset copy() {
		return new Preset(this.world.copy(), this.surface.copy(), this.caves.copy(), this.climate.copy(), this.terrain.copy(), this.rivers.copy(), this.filters.copy(), /* this.structures.copy(), */this.miscellaneous.copy());
	}

	public HolderLookup.Provider buildPatch(RegistryAccess registries) {
		RegistrySetBuilder builder = new RegistrySetBuilder();
//		this.addPatch(builder, RTFRegistries.PRESET, PresetData::bootstrap);
//		this.addPatch(builder, RTFRegistries.NOISE, PresetNoiseData::bootstrap);
//		this.addPatch(builder, RTFRegistries.BIOME_MODIFIER, PresetBiomeModifierData::bootstrap);
//		this.addPatch(builder, RTFRegistries.STRUCTURE_RULE, PresetStructureRuleData::bootstrap);
//		this.addPatch(builder, RTFRegistries.SURFACE_LAYERS, PresetSurfaceLayerData::bootstrap);
//		this.addPatch(builder, Registries.CONFIGURED_FEATURE, (preset, ctx) -> {
//			PresetConfiguredFeatures.bootstrap(preset, ctx);
//		});
//		this.addPatch(builder, Registries.CONFIGURED_CARVER, (preset, ctx) -> {
//			PresetConfiguredCarvers.bootstrap(preset, ctx);	
//		});
//		this.addPatch(builder, Registries.STRUCTURE_SET, PresetStructureSets::bootstrap);
//		this.addPatch(builder, Registries.PLACED_FEATURE, PresetPlacedFeatures::bootstrap);
//		this.addPatch(builder, Registries.BIOME, PresetBiomeData::bootstrap);
//		this.addPatch(builder, Registries.DIMENSION_TYPE, PresetDimensionTypes::bootstrap);
//		this.addPatch(builder, Registries.NOISE, PresetNoiseParameters::bootstrap);
//		this.addPatch(builder, Registries.DENSITY_FUNCTION, (preset, ctx) -> {
//			PresetNoiseRouterData.bootstrap(preset, ctx);
//			TBNoiseRouterData.bootstrap(ctx);
//		});
		this.addPatch(builder, Registries.NOISE_SETTINGS, PresetNoiseGeneratorSettings::bootstrap);
		return builder.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), registries);
	}
	
	private <T> void addPatch(RegistrySetBuilder builder, ResourceKey<? extends Registry<T>> key, Patch<T> patch) {
    	builder.add(key, (ctx) -> {
    		patch.apply(this, ctx);
    	});
    }
    
	private interface Patch<T> {
        void apply(Preset preset, BootstrapContext<T> ctx);
	}
}
