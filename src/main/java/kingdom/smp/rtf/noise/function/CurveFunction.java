package kingdom.smp.rtf.noise.function;

import com.mojang.serialization.MapCodec;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import kingdom.smp.rtf.registries.RTFBuiltInRegistries;

public interface CurveFunction {
    public static final Codec<CurveFunction> CODEC = RTFBuiltInRegistries.CURVE_FUNCTION_TYPE.byNameCodec().dispatch(CurveFunction::codec, Function.identity());
	
	float apply(float f);
	
	MapCodec<? extends CurveFunction> codec();
}
