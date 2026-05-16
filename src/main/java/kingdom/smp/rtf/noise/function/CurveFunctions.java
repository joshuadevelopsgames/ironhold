package kingdom.smp.rtf.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.registries.RTFBuiltInRegistries;

public class CurveFunctions {

	public static void bootstrap() {
		register("interpolation", Interpolation.LINEAR.codec());
		register("scurve", SCurveFunction.CODEC);
		register("terrace", TerraceFunction.CODEC);
	}

	public static SCurveFunction scurve(float lower, float upper) {
		return new SCurveFunction(lower, upper);
	}
	
	public static TerraceFunction terrace(float inputRange, float ramp, float cliff, float rampHeight, float blendRange, int steps) {
		return new TerraceFunction(inputRange, ramp, cliff, rampHeight, blendRange, steps);
	}
	
	private static void register(String name, MapCodec<? extends CurveFunction> value) {
		RegistryUtil.register(RTFBuiltInRegistries.CURVE_FUNCTION_TYPE, name, value);
	}
}
