package kingdom.smp.rtf.structure.rule;

import com.mojang.serialization.MapCodec;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;
import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.heightmap.WorldLookup;
import kingdom.smp.rtf.terrain.Terrain;
import kingdom.smp.rtf.terrain.TerrainType;

record CellTest(float cutoff, Set<Terrain> terrainTypeBlacklist) implements StructureRule {
	public static final MapCodec<CellTest> CODEC = RecordCodecBuilder.<CellTest>mapCodec(instance -> instance.group(
		Codec.FLOAT.fieldOf("cutoff").forGetter(CellTest::cutoff),
		Codec.STRING.xmap(TerrainType::get, Terrain::getName).listOf().fieldOf("terrain_type_blacklist").forGetter((set) -> set.terrainTypeBlacklist().stream().toList())
	).apply(instance, CellTest::new));

	public CellTest(float cutoff, List<Terrain> terrainTypeBlacklist) {
		this(cutoff, ImmutableSet.copyOf(terrainTypeBlacklist));
	}
	
	@Override
	public boolean test(RandomState randomState, BlockPos pos) {
		if((Object) randomState instanceof RTFRandomState rtfRandomState) {
			@Nullable
			GeneratorContext generatorContext = rtfRandomState.generatorContext();
			if(generatorContext != null) {
				WorldLookup worldLookup = generatorContext.lookup;
				Cell cell = new Cell();
				worldLookup.apply(cell.reset(), pos.getX(), pos.getZ());
				if(cell.riverDistance < this.cutoff) {//FIXME this breaks ancient city generation || this.terrainTypeBlacklist.contains(cell.terrain)) {
					return false;
				}
			}
			return true;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public MapCodec<CellTest> codec() {
		return CODEC;
	}
}
