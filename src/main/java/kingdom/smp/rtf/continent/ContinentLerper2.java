package kingdom.smp.rtf.continent;

import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.cell.CellPopulator;
import kingdom.smp.rtf.noise.NoiseUtil;
import kingdom.smp.rtf.noise.function.Interpolation;
import kingdom.smp.rtf.noise.module.Noise;

public class ContinentLerper2 implements CellPopulator {
	private CellPopulator lower;
    private CellPopulator upper;
    private Interpolation interpolation;
    private float blendLower;
    private float blendUpper;
    private float blendRange;
    
    public ContinentLerper2(CellPopulator lower, CellPopulator upper, float min, float max) {
        this(lower, upper, min, max, Interpolation.LINEAR);
    }
    
    public ContinentLerper2(CellPopulator lower, CellPopulator upper, float min, float max, Interpolation interpolation) {
        this.lower = lower;
        this.upper = upper;
        this.interpolation = interpolation;
        this.blendLower = min;
        this.blendUpper = max;
        this.blendRange = this.blendUpper - this.blendLower;
    }
    
    @Override
    public void apply(Cell cell, float x, float y) {
        if (cell.continentEdge < this.blendLower) {
            this.lower.apply(cell, x, y);
            return;
        }
        if (cell.continentEdge > this.blendUpper) {
            this.upper.apply(cell, x, y);
            return;
        }
        float alpha = this.interpolation.apply((cell.continentEdge - this.blendLower) / this.blendRange);
        this.lower.apply(cell, x, y);
        float lowerHeight = cell.height;
        this.upper.apply(cell, x, y);
        float upperHeight = cell.height;
        cell.height = NoiseUtil.lerp(lowerHeight, upperHeight, alpha);
    }

    @Override
    public CellPopulator mapNoise(Noise.Visitor visitor) {
    	return new ContinentLerper2(this.lower.mapNoise(visitor), this.upper.mapNoise(visitor), this.blendLower, this.blendUpper);
    }
}
