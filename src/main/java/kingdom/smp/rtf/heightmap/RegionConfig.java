package kingdom.smp.rtf.heightmap;

import kingdom.smp.rtf.noise.module.Noise;

public record RegionConfig(int seed, int scale, Noise warpX, Noise warpZ, float warpStrength) {
}
