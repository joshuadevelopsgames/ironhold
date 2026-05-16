package kingdom.smp.rtf.heightproviders;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import kingdom.smp.rtf.platform.RegistryUtil;

public class RTFHeightProviderTypes {
	public static final HeightProviderType<LegacyCarverHeight> LEGACY_CARVER = register("legacy_carver", LegacyCarverHeight.CODEC);
	
	public static void bootstrap() {
	}
	
	private static <T extends HeightProvider> HeightProviderType<T> register(String name, MapCodec<T> codec) {
		HeightProviderType<T> type = () -> codec;
		RegistryUtil.register(BuiltInRegistries.HEIGHT_PROVIDER_TYPE, name, type);
		return type;
	}
}
