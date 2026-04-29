package kingdom.smp.rtf;

import org.jetbrains.annotations.Nullable;

import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.heightmap.Heightmap;
import kingdom.smp.rtf.heightmap.Levels;
import kingdom.smp.rtf.heightmap.WorldLookup;
import kingdom.smp.rtf.tile.TileCache;
import kingdom.smp.rtf.tile.TileGenerator;
import kingdom.smp.rtf.tile.filter.WorldFilters;
import kingdom.smp.rtf.util.Seed;

public class GeneratorContext {
    public Seed seed;
    public Levels levels;
    public Preset preset;
    
    @Deprecated
    public ThreadLocal<Heightmap> localHeightmap;
    public TileGenerator generator;
    @Nullable
    public TileCache cache;
    public WorldLookup lookup;
    
    public GeneratorContext(Preset preset, int seed, int tileSize, int tileBorder, int batchCount, @Nullable TileCache cache) {
        this.preset = preset;
        this.seed = new Seed(seed);
        this.levels = new Levels(preset.world().properties.terrainScaler(), preset.world().properties.seaLevel);

        Heightmap globalHeightmap = Heightmap.make(this);
        this.localHeightmap = ThreadLocal.withInitial(globalHeightmap::cache);
        this.generator = new TileGenerator(this.localHeightmap, new WorldFilters(this, globalHeightmap), tileSize, tileBorder, batchCount);
        this.cache = cache;
        this.lookup = new WorldLookup(this);
    }

    public static GeneratorContext makeCached(Preset preset, int seed, int tileSize, int batchCount, boolean queue) {
    	GeneratorContext ctx = makeUncached(preset, seed, tileSize, Math.min(2, Math.max(1, preset.filters().erosion.dropletLifetime / 16)), batchCount);
    	ctx.cache = new TileCache(tileSize, queue, ctx.generator);
    	ctx.lookup = new WorldLookup(ctx);
    	return ctx;
    }
    
    public static GeneratorContext makeUncached(Preset preset, int seed, int tileSize, int tileBorder, int batchCount) {
    	return new GeneratorContext(preset, seed, tileSize, tileBorder, batchCount, null);
    }
}
