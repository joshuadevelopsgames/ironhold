package kingdom.smp.rtf.chunkgen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import kingdom.smp.rtf.GeneratorContext;
import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.cell.Cell;
import kingdom.smp.rtf.data.preset.settings.BuiltinPresets;
import kingdom.smp.rtf.data.preset.settings.Preset;
import kingdom.smp.rtf.heightmap.Levels;
import kingdom.smp.rtf.heightmap.WorldLookup;
import kingdom.smp.rtf.terrain.Terrain;
import kingdom.smp.rtf.terrain.TerrainType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class RTFChunkGenerator extends ChunkGenerator {
    private static volatile Long globalSeedHint;

    public static final MapCodec<RTFChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings),
        Preset.CODEC.optionalFieldOf("preset", BuiltinPresets.makeDefault()).forGetter(g -> g.preset)
    ).apply(instance, RTFChunkGenerator::new));

    private final Holder<NoiseGeneratorSettings> settings;
    private final Preset preset;
    private final NoiseBasedChunkGenerator delegate;
    private Long contextSeed;
    private GeneratorContext generatorContext;
    private WorldLookup worldLookup;

    public RTFChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, Preset preset) {
        super(biomeSource);
        this.settings = settings;
        this.preset = preset;
        this.delegate = new NoiseBasedChunkGenerator(biomeSource, settings);
    }

    public Preset preset() {
        return preset;
    }

    public Holder<NoiseGeneratorSettings> settings() {
        return settings;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
        return delegate.createBiomes(randomState, blender, structureManager, chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        ensureContext(region.getSeed());
        bindRandomState(randomState);
        delegate.applyCarvers(region, seed, randomState, biomeManager, structureManager, chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        ensureContext(region.getSeed());
        bindRandomState(randomState);
        delegate.buildSurface(region, structureManager, randomState, chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ensureContext(region.getSeed());
        delegate.spawnOriginalMobs(region);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        if (worldLookup == null && globalSeedHint != null) {
            ensureContext(globalSeedHint.longValue());
        }
        if (worldLookup == null || generatorContext == null) {
            return delegate.fillFromNoise(blender, randomState, structureManager, chunk);
        }

        Levels levels = generatorContext.levels;
        int seaLevel = settings.value().seaLevel();
        int dimensionMinY = chunk.getMinY();
        int dimensionMaxY = dimensionMinY + chunk.getHeight();

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = settings.value().defaultBlock();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState granite = Blocks.GRANITE.defaultBlockState();
        BlockState diorite = Blocks.DIORITE.defaultBlockState();
        BlockState andesite = Blocks.ANDESITE.defaultBlockState();
        BlockState tuff = Blocks.TUFF.defaultBlockState();
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState water = settings.value().defaultFluid();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        ChunkPos pos = chunk.getPos();
        Cell cell = new Cell();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos biomeCursor = new BlockPos.MutableBlockPos();
        long seed = generatorContext.seed.root();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = pos.getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = pos.getMinBlockZ() + localZ;

                cell.reset();
                worldLookup.apply(cell, worldX, worldZ);
                int top = Mth.clamp(levels.scale(cell.height), dimensionMinY + 1, dimensionMaxY - 1);
                Terrain terrain = cell.terrain != null ? cell.terrain : TerrainType.NONE;

                top = applyRiverCarving(terrain, top, seaLevel, cell.riverDistance);

                biomeCursor.set(worldX, top, worldZ);
                Holder<Biome> biomeHolder = chunk.getNoiseBiome(worldX >> 2, top >> 2, worldZ >> 2);

                BlockState topBlock = pickTopBlock(terrain, biomeHolder, top, seaLevel);
                BlockState fillerBlock = pickFillerBlock(terrain, biomeHolder, top, seaLevel);
                int fillerDepth = pickFillerDepth(terrain);

                int deepslateStart = -8 + ((hash(worldX, 0, worldZ, seed) & 0x7) - 4);

                for (int y = dimensionMinY; y < dimensionMaxY; y++) {
                    cursor.set(worldX, y, worldZ);
                    BlockState state;
                    int bedrockNoise = (int) ((cell.continentNoise + 1.0F) * 2.0F);
                    if (y < dimensionMinY + 5 && y < dimensionMinY + 1 + bedrockNoise) {
                        state = bedrock;
                    } else if (y < top - fillerDepth) {
                        state = pickStoneStrata(worldX, y, worldZ, seed, top, seaLevel, stone, deepslate, granite, diorite, andesite, tuff, calcite, sandstone, deepslateStart);
                    } else if (y < top - 1) {
                        state = fillerBlock;
                    } else if (y < top) {
                        state = applySnowCap(topBlock, biomeHolder, y, seaLevel);
                    } else if (y < seaLevel) {
                        state = water;
                    } else {
                        state = air;
                    }
                    chunk.setBlockState(cursor, state);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, Set.of(
            Heightmap.Types.OCEAN_FLOOR_WG,
            Heightmap.Types.WORLD_SURFACE_WG
        ));
        return CompletableFuture.completedFuture(chunk);
    }

    private static int applyRiverCarving(Terrain terrain, int top, int seaLevel, float riverDistance) {
        if (terrain == TerrainType.RIVER) {
            int target = seaLevel - 3;
            return Math.min(top, target);
        }
        if (terrain == TerrainType.LAKE) {
            int target = seaLevel - 5;
            return Math.min(top, target);
        }
        return top;
    }

    private static final long STRATA_SALT = 0x5C1A7AL;

    private static BlockState pickStoneStrata(int x, int y, int z, long seed, int top, int seaLevel, BlockState stone, BlockState deepslate, BlockState granite, BlockState diorite, BlockState andesite, BlockState tuff, BlockState calcite, BlockState sandstone, int deepslateStart) {
        if (y < deepslateStart) {
            int deepBlob = blobHash(x, y, z, seed ^ STRATA_SALT);
            if ((deepBlob & 0x3) == 0) return tuff;
            return deepslate;
        }
        int bandIdx = ((y + (hash(x >> 4, 0, z >> 4, seed) & 0xF)) >> 3);
        int bandKind = (bandIdx ^ (int)(seed & 0xFFFFFF)) & 0x7;
        if (y < seaLevel - 16) {
            switch (bandKind) {
                case 0: return tuff;
                case 1: return diorite;
                case 2: return andesite;
                default: break;
            }
        }
        if (y > seaLevel + 30 && y < seaLevel + 70 && (bandKind == 3 || bandKind == 5)) {
            return sandstone;
        }
        if (y > seaLevel + 60 && bandKind == 4) {
            return calcite;
        }
        int blob = blobHash(x, y, z, seed);
        switch (blob) {
            case 1: return granite;
            case 2: return diorite;
            case 3: return andesite;
            default: return stone;
        }
    }

    private static int blobHash(int x, int y, int z, long seed) {
        int bx = Math.floorDiv(x, 7);
        int by = Math.floorDiv(y, 6);
        int bz = Math.floorDiv(z, 7);
        int h = hash(bx, by, bz, seed);
        int kind = (h & 0xFF);
        if (kind < 8) return 1;
        if (kind < 16) return 2;
        if (kind < 24) return 3;
        return 0;
    }

    private static int hash(int x, int y, int z, long seed) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= y * 0xBF58476D1CE4E5B9L;
        h ^= z * 0x94D049BB133111EBL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (int) (h & 0x7FFFFFFF);
    }

    private static BlockState applySnowCap(BlockState topBlock, Holder<Biome> biome, int y, int seaLevel) {
        if (topBlock != Blocks.GRASS_BLOCK.defaultBlockState() && topBlock != Blocks.DIRT.defaultBlockState() && topBlock != Blocks.PODZOL.defaultBlockState()) {
            return topBlock;
        }
        if (biome != null && biome.is(BiomeTags.IS_NETHER)) {
            return topBlock;
        }
        boolean cold = biome != null && (biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.HAS_VILLAGE_SNOWY));
        int snowLine = cold ? seaLevel + 30 : seaLevel + 90;
        if (y > snowLine) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        return topBlock;
    }

    private static BlockState pickTopBlock(Terrain terrain, Holder<Biome> biome, int top, int seaLevel) {
        if (terrain == TerrainType.BEACH) {
            return biome != null && biome.is(BiomeTags.HAS_DESERT_PYRAMID) ? Blocks.SAND.defaultBlockState() : Blocks.SAND.defaultBlockState();
        }
        if (terrain == TerrainType.RIVER || terrain == TerrainType.LAKE) return Blocks.GRAVEL.defaultBlockState();
        if (terrain == TerrainType.DEEP_OCEAN || terrain == TerrainType.SHALLOW_OCEAN) return Blocks.GRAVEL.defaultBlockState();
        if (terrain == TerrainType.BADLANDS) return Blocks.RED_SAND.defaultBlockState();
        if (terrain == TerrainType.MOUNTAINS_1 || terrain == TerrainType.MOUNTAINS_2 || terrain == TerrainType.MOUNTAINS_3 || terrain == TerrainType.MOUNTAIN_CHAIN || terrain == TerrainType.MOUNTAIN_CLIFFS) {
            if (top > seaLevel + 80) return Blocks.STONE.defaultBlockState();
            if (top > seaLevel + 50) return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (biome != null) {
            if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) return Blocks.SAND.defaultBlockState();
            if (biome.is(BiomeTags.IS_BADLANDS)) return Blocks.RED_SAND.defaultBlockState();
            if (biome.is(BiomeTags.IS_TAIGA)) return Blocks.PODZOL.defaultBlockState();
            if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY)) return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        if (top < seaLevel) return Blocks.DIRT.defaultBlockState();
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static BlockState pickFillerBlock(Terrain terrain, Holder<Biome> biome, int top, int seaLevel) {
        if (terrain == TerrainType.BEACH) return Blocks.SANDSTONE.defaultBlockState();
        if (terrain == TerrainType.RIVER || terrain == TerrainType.LAKE) return Blocks.GRAVEL.defaultBlockState();
        if (terrain == TerrainType.DEEP_OCEAN || terrain == TerrainType.SHALLOW_OCEAN) return Blocks.GRAVEL.defaultBlockState();
        if (terrain == TerrainType.BADLANDS) return Blocks.RED_SANDSTONE.defaultBlockState();
        if (terrain == TerrainType.MOUNTAINS_1 || terrain == TerrainType.MOUNTAINS_2 || terrain == TerrainType.MOUNTAINS_3 || terrain == TerrainType.MOUNTAIN_CHAIN || terrain == TerrainType.MOUNTAIN_CLIFFS) {
            if (top > seaLevel + 80) return Blocks.STONE.defaultBlockState();
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (biome != null) {
            if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) return Blocks.SANDSTONE.defaultBlockState();
            if (biome.is(BiomeTags.IS_BADLANDS)) return Blocks.RED_SANDSTONE.defaultBlockState();
        }
        return Blocks.DIRT.defaultBlockState();
    }

    private static int pickFillerDepth(Terrain terrain) {
        if (terrain == TerrainType.BEACH) return 4;
        if (terrain == TerrainType.RIVER || terrain == TerrainType.LAKE) return 3;
        if (terrain == TerrainType.DEEP_OCEAN) return 5;
        if (terrain == TerrainType.SHALLOW_OCEAN) return 4;
        if (terrain == TerrainType.MOUNTAINS_1 || terrain == TerrainType.MOUNTAINS_2 || terrain == TerrainType.MOUNTAINS_3 || terrain == TerrainType.MOUNTAIN_CHAIN || terrain == TerrainType.MOUNTAIN_CLIFFS) {
            return 2;
        }
        return 4;
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    @Override
    public int getGenDepth() {
        return delegate.getGenDepth();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        if (worldLookup == null && globalSeedHint != null) {
            ensureContext(globalSeedHint.longValue());
        }
        if (worldLookup == null || generatorContext == null) {
            return delegate.getBaseHeight(x, z, types, levelHeightAccessor, randomState);
        }
        Cell cell = new Cell();
        worldLookup.apply(cell, x, z);
        int minY = levelHeightAccessor.getMinY();
        int maxY = minY + levelHeightAccessor.getHeight();
        int top = Mth.clamp(generatorContext.levels.scale(cell.height), minY + 1, maxY - 1);
        if (types == Heightmap.Types.WORLD_SURFACE || types == Heightmap.Types.WORLD_SURFACE_WG) {
            int sea = settings.value().seaLevel();
            return Math.max(top, sea);
        }
        return top;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        if (worldLookup == null && globalSeedHint != null) {
            ensureContext(globalSeedHint.longValue());
        }
        if (worldLookup == null) {
            return delegate.getBaseColumn(x, z, levelHeightAccessor, randomState);
        }
        int minY = levelHeightAccessor.getMinY();
        int height = levelHeightAccessor.getHeight();
        int seaLevel = settings.value().seaLevel();
        int top = getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, levelHeightAccessor, randomState);
        BlockState[] states = new BlockState[height];
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState block = settings.value().defaultBlock();
        BlockState fluid = settings.value().defaultFluid();
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            if (y < top) {
                states[i] = block;
            } else if (y < seaLevel) {
                states[i] = fluid;
            } else {
                states[i] = air;
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        delegate.addDebugScreenInfo(lines, randomState, pos);
    }

    private synchronized void ensureContext(long seed) {
        if (worldLookup != null && contextSeed != null && contextSeed.longValue() == seed) {
            return;
        }
        contextSeed = seed;
        org.slf4j.LoggerFactory.getLogger("ReTerraForged").info("[RTF] Initializing GeneratorContext for seed={}", seed);
        generatorContext = GeneratorContext.makeCached(preset, (int) seed, 16, 2, false);
        worldLookup = generatorContext.lookup;
    }

    private void bindRandomState(RandomState randomState) {
        if (generatorContext == null) {
            return;
        }
        Object asObject = randomState;
        if (asObject instanceof RTFRandomState rtfState && rtfState.generatorContext() != generatorContext) {
            rtfState.setPreset(preset);
            rtfState.setGeneratorContext(generatorContext);
        }
    }

    public static void setGlobalSeedHint(long seed) {
        globalSeedHint = seed;
    }

    public static Long getGlobalSeedHint() {
        return globalSeedHint;
    }
}
