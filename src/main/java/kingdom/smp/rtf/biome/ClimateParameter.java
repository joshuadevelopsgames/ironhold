package kingdom.smp.rtf.biome;

import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;

public interface ClimateParameter {
	float min();
	
	float max();
	
	default float mid() {
		return (this.min() + this.max()) / 2.0F;
	}
	
	default Noise source() {
		return Noises.constant(this.mid());
	}
}
