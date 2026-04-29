package kingdom.smp.rtf.surface.condition;

import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.noise.module.Noise;

abstract class ThresholdCondition extends CellCondition {
	private Noise threshold;
	private Noise variance;
	
	public ThresholdCondition(Context context, Noise threshold, Noise variance) {
		super(context);
		
		this.threshold = threshold;
		this.variance = variance;
	}

	@Override
	public boolean test(Cell cell, int x, int z) {
		return this.sample(cell) + this.variance.compute(x, z, 0) > this.threshold.compute(x, z, 0);
	}
	
	protected abstract float sample(Cell cell);
}
