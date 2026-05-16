package kingdom.smp.rtf.biome.modifier;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import kingdom.smp.rtf.registries.RTFBuiltInRegistries;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.common.world.BiomeModifier.Phase;

public interface BiomeModifier extends net.neoforged.neoforge.common.world.BiomeModifier {
    public static final Codec<BiomeModifier> CODEC = RTFBuiltInRegistries.BIOME_MODIFIER_TYPE.byNameCodec().dispatch(BiomeModifier::codec, Function.identity());

    @Override
    MapCodec<? extends BiomeModifier> codec();

    @Override
    default void modify(Holder<net.minecraft.world.level.biome.Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
    }
}
