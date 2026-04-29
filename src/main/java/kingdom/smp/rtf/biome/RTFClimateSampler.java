package kingdom.smp.rtf.biome;

import net.minecraft.core.BlockPos;

public interface RTFClimateSampler {
	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
