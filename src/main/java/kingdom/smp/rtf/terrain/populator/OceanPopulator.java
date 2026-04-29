package kingdom.smp.rtf.terrain.populator;

import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.cell.CellPopulator;
import kingdom.smp.rtf.noise.module.Noise;
import kingdom.smp.rtf.terrain.Terrain;

public record OceanPopulator(Terrain terrainType, Noise height) implements CellPopulator {

	@Override
	public void apply(Cell cell, float x, float z) {
		cell.terrain = this.terrainType;
		cell.height = Math.max(this.height.compute(x, z, 0), 0.0F);
		
		//TODO dont do this
		cell.erosion = -1.1F;
		cell.weirdness = -1.1F;
	}
}
