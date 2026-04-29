package kingdom.smp.rtf.biome.modifier;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import kingdom.smp.rtf.registries.RTFBuiltInRegistries;

public interface BiomeModifier {
    public static final Codec<BiomeModifier> CODEC = RTFBuiltInRegistries.BIOME_MODIFIER_TYPE.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());
    
	MapCodec<? extends BiomeModifier> codec();
}
