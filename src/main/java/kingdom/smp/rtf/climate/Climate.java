package kingdom.smp.rtf.climate;

import kingdom.smp.rtf.data.preset.settings.ClimateSettings;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.data.preset.settings.WorldSettings;
import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.cell.CellPopulator;
import kingdom.smp.rtf.continent.Continent;
import kingdom.smp.rtf.heightmap.Levels;
import kingdom.smp.rtf.noise.NoiseUtil;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.noise.module.Noises;
import kingdom.smp.rtf.terrain.TerrainType;

public record Climate(int randomSeed, Noise offsetX, Noise offsetZ, int offsetDistance, Levels levels, BiomeNoise biomeNoise) implements CellPopulator {

	@Override
	public void apply(Cell cell, float x, float z) {
		this.biomeNoise.apply(cell, x, z, x, z, true);
		float edgeBlend = 0.4F;
		if (cell.height <= this.levels.water) {
			if (cell.terrain == TerrainType.COAST) {
				cell.terrain = TerrainType.SHALLOW_OCEAN;
			}
		} else if (cell.biomeRegionEdge < edgeBlend || cell.terrain == TerrainType.MOUNTAIN_CHAIN) {
			float modifier = 1.0F - NoiseUtil.map(cell.biomeRegionEdge, 0.0F, edgeBlend, edgeBlend);
			float distance = this.offsetDistance * modifier;
			float dx = this.offsetX.compute(x, z, 0) * distance;
			float dz = this.offsetZ.compute(x, z, 0) * distance;
			float ox = x;
			float oz = z;
			x += dx;
			z += dz;
			this.biomeNoise.apply(cell, x, z, ox, oz, false);
		}
	}
	
	public static Climate make(Continent continent, GeneratorContext context) {
		Preset preset = context.preset;
		
		WorldSettings worldSettings = preset.world();
		ClimateSettings climateSettings = preset.climate();
		
		BiomeNoise biomeNoise = new BiomeNoise(context.seed, continent, worldSettings.controlPoints, climateSettings, context.levels);
		Levels levels = context.levels;
		int randSeed = context.seed.next();
		
		Noise biomeEdgeShape = climateSettings.biomeEdgeShape.build(0);
		Noise offsetX = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
		Noise offsetZ = Noises.shiftSeed(biomeEdgeShape, context.seed.next());
		int offsetDistance = climateSettings.biomeEdgeShape.strength;
		return new Climate(randSeed, offsetX, offsetZ, offsetDistance, levels, biomeNoise);
	}
}
