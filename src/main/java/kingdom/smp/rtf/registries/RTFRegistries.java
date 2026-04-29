package kingdom.smp.rtf.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import kingdom.smp.rtf.RTFCommon;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.biome.modifier.BiomeModifier;
import kingdom.smp.rtf.feature.chance.ChanceModifier;
import kingdom.smp.rtf.feature.template.decorator.TemplateDecorator;
import kingdom.smp.rtf.feature.template.placement.TemplatePlacement;
import kingdom.smp.rtf.noise.domain.Domain;
import kingdom.smp.rtf.noise.function.CurveFunction;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.structure.rule.StructureRule;
import kingdom.smp.rtf.surface.rule.LayeredSurfaceRule;

public class RTFRegistries {
	public static final ResourceKey<Registry<MapCodec<? extends Noise>>> NOISE_TYPE = createKey("worldgen/noise_type");
	public static final ResourceKey<Registry<MapCodec<? extends Domain>>> DOMAIN_TYPE = createKey("worldgen/domain_type");
	public static final ResourceKey<Registry<MapCodec<? extends CurveFunction>>> CURVE_FUNCTION_TYPE = createKey("worldgen/curve_function_type");
	public static final ResourceKey<Registry<MapCodec<? extends ChanceModifier>>> CHANCE_MODIFIER_TYPE = createKey("worldgen/chance_modifier_type");
	public static final ResourceKey<Registry<MapCodec<? extends TemplatePlacement<?>>>> TEMPLATE_PLACEMENT_TYPE = createKey("worldgen/template_placement_type");
	public static final ResourceKey<Registry<MapCodec<? extends TemplateDecorator<?>>>> TEMPLATE_DECORATOR_TYPE = createKey("worldgen/template_decorator_type");
	public static final ResourceKey<Registry<MapCodec<? extends BiomeModifier>>> BIOME_MODIFIER_TYPE = createKey("worldgen/biome_modifier_type");
	public static final ResourceKey<Registry<MapCodec<? extends StructureRule>>> STRUCTURE_RULE_TYPE = createKey("worldgen/structure_rule_type");
	public static final ResourceKey<Registry<Noise>> NOISE = createKey("worldgen/noise");
	public static final ResourceKey<Registry<BiomeModifier>> BIOME_MODIFIER = createKey("worldgen/biome_modifier");
	public static final ResourceKey<Registry<StructureRule>> STRUCTURE_RULE = createKey("worldgen/structure_rule");
	public static final ResourceKey<Registry<LayeredSurfaceRule.Layer>> SURFACE_LAYERS = createKey("worldgen/surface_layers");

	public static final ResourceKey<Registry<Preset>> PRESET = createKey("worldgen/preset");
	
	public static <T> ResourceKey<T> createKey(ResourceKey<? extends Registry<T>> registryKey, String valueKey) {
		return ResourceKey.create(registryKey, RTFCommon.location(valueKey));
	}

	private static <T> ResourceKey<Registry<T>> createKey(String key) {
		return ResourceKey.createRegistryKey(RTFCommon.location(key));
	}
}
