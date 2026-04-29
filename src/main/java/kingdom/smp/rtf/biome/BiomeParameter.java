package kingdom.smp.rtf.biome;

import kingdom.smp.rtf.noise.NoiseUtil;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;

public interface BiomeParameter {
	float min();
	
	float max();
	
	default float lerp(float alpha) {
		return NoiseUtil.lerp(this.min(), this.max(), alpha);
	}
	
	default float midpoint() {
		return (this.min() + this.max()) / 2.0F;
	}
	
	default Noise source() {
		return Noises.constant(this.midpoint());
	}
}
