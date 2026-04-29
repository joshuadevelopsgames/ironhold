package kingdom.smp.rtf.registries;

import com.mojang.serialization.MapCodec;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.biome.modifier.BiomeModifier;
import kingdom.smp.rtf.feature.chance.ChanceModifier;
import kingdom.smp.rtf.feature.template.decorator.TemplateDecorator;
import kingdom.smp.rtf.feature.template.placement.TemplatePlacement;
import kingdom.smp.rtf.noise.domain.Domain;
import kingdom.smp.rtf.noise.function.CurveFunction;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.structure.rule.StructureRule;

public class RTFBuiltInRegistries {
	public static final Registry<MapCodec<? extends Noise>> NOISE_TYPE = RegistryUtil.createRegistry(RTFRegistries.NOISE_TYPE);
	public static final Registry<MapCodec<? extends Domain>> DOMAIN_TYPE = RegistryUtil.createRegistry(RTFRegistries.DOMAIN_TYPE);
	public static final Registry<MapCodec<? extends CurveFunction>> CURVE_FUNCTION_TYPE = RegistryUtil.createRegistry(RTFRegistries.CURVE_FUNCTION_TYPE);
	public static final Registry<MapCodec<? extends ChanceModifier>> CHANCE_MODIFIER_TYPE = RegistryUtil.createRegistry(RTFRegistries.CHANCE_MODIFIER_TYPE);
	public static final Registry<MapCodec<? extends TemplatePlacement<?>>> TEMPLATE_PLACEMENT_TYPE = RegistryUtil.createRegistry(RTFRegistries.TEMPLATE_PLACEMENT_TYPE);
	public static final Registry<MapCodec<? extends TemplateDecorator<?>>> TEMPLATE_DECORATOR_TYPE = RegistryUtil.createRegistry(RTFRegistries.TEMPLATE_DECORATOR_TYPE);
	public static final Registry<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_TYPE = RegistryUtil.createRegistry(RTFRegistries.BIOME_MODIFIER_TYPE);
	public static final Registry<MapCodec<? extends StructureRule>> STRUCTURE_RULE_TYPE = RegistryUtil.createRegistry(RTFRegistries.STRUCTURE_RULE_TYPE);

	public static void bootstrap() {
	}
}
