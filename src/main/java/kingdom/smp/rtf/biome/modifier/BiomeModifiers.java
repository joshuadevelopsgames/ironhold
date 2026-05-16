package kingdom.smp.rtf.biome.modifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.registries.RTFBuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

public class BiomeModifiers {

	public static void bootstrap() {
	}

	@SafeVarargs
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Holder<PlacedFeature>... features) {
		return add(order, step, HolderSet.direct(features));
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.empty(), features);
	}

	@SafeVarargs
	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, Holder<PlacedFeature>... features) {
		return add(order, step, filterBehavior, biomes, HolderSet.direct(features));
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Filter.Behavior filterBehavior, HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
		return add(order, step, Optional.of(Pair.of(filterBehavior, biomes)), features);
	}

	public static BiomeModifier add(Order order, GenerationStep.Decoration step, Optional<Pair<Filter.Behavior, HolderSet<Biome>>> biomes, HolderSet<PlacedFeature> features) {
		if (biomes.isEmpty()) {
			return new BridgedBiomeModifier(new AddFeaturesNativeModifier(step, Optional.empty(), false, features));
		}
		Filter.Behavior behavior = biomes.get().getFirst();
		HolderSet<Biome> filterSet = biomes.get().getSecond();
		if (behavior == Filter.Behavior.WHITELIST) {
			var native_ = new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(filterSet, features, step);
			return new BridgedBiomeModifier(native_);
		}
		return new BridgedBiomeModifier(new AddFeaturesNativeModifier(step, Optional.of(filterSet), true, features));
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return replace(step, Optional.empty(), replacements);
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, HolderSet<Biome> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return replace(step, Optional.of(biomes), replacements);
	}

	public static BiomeModifier replace(GenerationStep.Decoration step, Optional<HolderSet<Biome>> biomes, Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements) {
		return new BridgedBiomeModifier(new ReplaceFeaturesNativeModifier(step, biomes, replacements));
	}

	public static void register(String name, MapCodec<? extends BiomeModifier> value) {
		RegistryUtil.register(RTFBuiltInRegistries.BIOME_MODIFIER_TYPE, name, value);
	}

	private record BridgedBiomeModifier(net.neoforged.neoforge.common.world.BiomeModifier delegate) implements BiomeModifier {
		@Override
		public void modify(Holder<net.minecraft.world.level.biome.Biome> biome, net.neoforged.neoforge.common.world.BiomeModifier.Phase phase, net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder builder) {
			delegate.modify(biome, phase, builder);
		}

		@Override
		public MapCodec<? extends BiomeModifier> codec() {
			throw new UnsupportedOperationException(
				"BridgedBiomeModifier is a runtime-only adapter; not codec-serializable. Wrap data-driven instances directly through NeoForge's biome_modifier registry instead.");
		}
	}

	private record AddFeaturesNativeModifier(
			GenerationStep.Decoration step,
			Optional<HolderSet<Biome>> biomes,
			boolean blacklist,
			HolderSet<PlacedFeature> features
	) implements net.neoforged.neoforge.common.world.BiomeModifier {
		@Override
		public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
			if (phase != Phase.ADD) {
				return;
			}
			if (biomes.isPresent()) {
				boolean contains = biomes.get().contains(biome);
				if (blacklist == contains) {
					return;
				}
			}
			List<Holder<PlacedFeature>> target = builder.getGenerationSettings().getFeatures(step);
			for (Holder<PlacedFeature> feature : features) {
				target.add(feature);
			}
		}

		@Override
		public MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier> codec() {
			throw new UnsupportedOperationException(
				"AddFeaturesNativeModifier is a runtime-only adapter; not codec-serializable.");
		}
	}

	private record ReplaceFeaturesNativeModifier(
			GenerationStep.Decoration step,
			Optional<HolderSet<Biome>> biomes,
			Map<ResourceKey<PlacedFeature>, Holder<PlacedFeature>> replacements
	) implements net.neoforged.neoforge.common.world.BiomeModifier {
		@Override
		public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
			if (phase != Phase.MODIFY) {
				return;
			}
			if (biomes.isPresent() && !biomes.get().contains(biome)) {
				return;
			}
			List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(step);
			for (int i = 0; i < features.size(); i++) {
				Holder<PlacedFeature> existing = features.get(i);
				Optional<ResourceKey<PlacedFeature>> keyOpt = existing.unwrapKey();
				if (keyOpt.isEmpty()) {
					continue;
				}
				Holder<PlacedFeature> replacement = replacements.get(keyOpt.get());
				if (replacement != null) {
					features.set(i, replacement);
				}
			}
		}

		@Override
		public MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier> codec() {
			throw new UnsupportedOperationException(
				"ReplaceFeaturesNativeModifier is a runtime-only adapter; not codec-serializable.");
		}
	}
}
