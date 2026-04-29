package kingdom.smp.rtf.cell;

import kingdom.smp.rtf.noise.module.Noise;

public interface CellPopulator {
    void apply(Cell cell, float x, float z);
    
    default CellPopulator mapNoise(Noise.Visitor visitor) {
    	return this;
    }
}
