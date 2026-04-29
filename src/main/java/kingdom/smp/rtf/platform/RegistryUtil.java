package kingdom.smp.rtf.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;

import kingdom.smp.rtf.RTFCommon;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RegistryUtil {
	private static final Map<ResourceKey<?>, DeferredRegister<?>> DEFERRED = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<?>, Codec<?>> DATA_REGISTRIES = new ConcurrentHashMap<>();

	private RegistryUtil() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> void register(Registry<T> registry, String name, T value) {
		DeferredRegister<T> dr = (DeferredRegister<T>) DEFERRED.computeIfAbsent(registry.key(),
			k -> DeferredRegister.create((ResourceKey) k, RTFCommon.MOD_ID));
		dr.register(name, () -> value);
	}

	public static <T> WritableRegistry<T> getWritable(Registry<T> registry) {
		throw new UnsupportedOperationException(
			"WritableRegistry access is not supported on NeoForge; use register(Registry, name, value).");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Registry<T> createRegistry(ResourceKey<? extends Registry<T>> key) {
		DeferredRegister<T> dr = (DeferredRegister<T>) DEFERRED.computeIfAbsent(key,
			k -> DeferredRegister.create(key, RTFCommon.MOD_ID));
		return ((DeferredRegister<T>) DEFERRED.get(key)).makeRegistry(b -> {});
	}

	public static <T> void createDataRegistry(ResourceKey<? extends Registry<T>> key, Codec<T> codec) {
		DATA_REGISTRIES.put(key, codec);
	}

	public static Registry<BiomeModifier> getBiomeModifierRegistry() {
		throw new UnsupportedOperationException(
			"NeoForge BiomeModifiers are managed via NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS; access serializers there directly.");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void bindAll(IEventBus modBus) {
		for (DeferredRegister<?> dr : DEFERRED.values()) {
			dr.register(modBus);
		}
		modBus.addListener((DataPackRegistryEvent.NewRegistry event) -> {
			for (Map.Entry<ResourceKey<?>, Codec<?>> entry : DATA_REGISTRIES.entrySet()) {
				event.dataPackRegistry((ResourceKey) entry.getKey(), (Codec) entry.getValue());
			}
		});
	}

	public static Registry<net.neoforged.neoforge.common.world.BiomeModifier> neoForgeBiomeModifierSerializers() {
		return (Registry) NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS;
	}
}
