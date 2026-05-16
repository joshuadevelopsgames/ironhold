package kingdom.smp.rtf.noise.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.StringRepresentable;
import kingdom.smp.rtf.noise.NoiseUtil;

public enum Interpolation implements CurveFunction, StringRepresentable {
    LINEAR("LINEAR") {
    	
        @Override
        public float apply(float f) {
            return f;
        }
    }, 
    CURVE3("CURVE3") {
    	
        @Override
        public float apply(float f) {
        	return NoiseUtil.interpHermite(f);
        }
    }, 
    CURVE4("CURVE4") {
    	
        @Override
        public float apply(float f) {
        	return NoiseUtil.interpQuintic(f);
        }
    };
	
	public static final Codec<Interpolation> ENUM_CODEC = StringRepresentable.fromEnum(() -> Interpolation.values());
	public static final Codec<Interpolation> CODEC = ENUM_CODEC;
	
	private String name;
	
	private Interpolation(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
	
	@Override
	public MapCodec<Interpolation> codec() {
		return CODEC.fieldOf("interpolation");
	}
}
