package kingdom.smp.rtf.surface.rule;

import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.SurfaceRules;
import kingdom.smp.rtf.compat.terrablender.TBSurfaceRules;
import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.surface.rule.StrataRule.Layer;

public class RTFSurfaceRules {

	public static void bootstrap() {
		register("layered", LayeredSurfaceRule.CODEC);
		register("strata", StrataRule.CODEC);
		register("noise", NoiseRule.CODEC);
	}
	
	public static LayeredSurfaceRule layered(TagKey<LayeredSurfaceRule.Layer> layers) {
		return new LayeredSurfaceRule(layers);
	}
	
	public static StrataRule strata(Identifier cacheId, int buffer, int iterations, Holder<Noise> selector, List<Layer> layers) {
		return new StrataRule(cacheId, buffer, iterations, selector, layers);
	}
	
	public static NoiseRule noise(Holder<Noise> noise, List<Pair<Float, SurfaceRules.RuleSource>> rules) {
		return new NoiseRule(noise, rules);
	}
	
	public static void register(String name, MapCodec<? extends SurfaceRules.RuleSource> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE, name, value);
	}
}
