package kingdom.smp.rtf.feature.placement;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.terrain.Terrain;

class TerrainFilter extends CellFilter {
	public static final Codec<TerrainFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Terrain.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("terrain").forGetter((filter) -> filter.terrain),
		Codec.BOOL.fieldOf("exclude").forGetter((filter) -> filter.exclude)
	).apply(instance, TerrainFilter::new));
	
	private Set<Terrain> terrain;
	private boolean exclude;
	
	public TerrainFilter(Set<Terrain> terrain, boolean exclude) {
		this.terrain = terrain;
		this.exclude = exclude;
	}

	@Override
	protected boolean shouldPlace(Cell cell, PlacementContext ctx, RandomSource rand, BlockPos pos) {
		boolean match = this.terrain.contains(cell.terrain);
		return this.exclude ? !match : match;
	}
	
	@Override
	public PlacementModifierType<TerrainFilter> type() {
		return RTFPlacementModifiers.TERRAIN_FILTER;
	}
}
