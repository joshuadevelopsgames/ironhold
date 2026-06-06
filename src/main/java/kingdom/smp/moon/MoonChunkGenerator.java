package kingdom.smp.moon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A chunk generator that produces a solid 256x256x256 cube of moon terrain,
 * centered at world origin (footprint x/z in [-128,127], y in [0,255]).
 * All six faces of the cube are sculpted with an independent noise heightmap,
 * each carrying a moon-dust surface shell over a moonstone core, so the moon
 * looks like a rocky cube from every direction. Outside the cube: void.
 */
public class MoonChunkGenerator extends ChunkGenerator {

    private static final int HALF_SIZE = 128;        // footprint half-width (x/z in [-128,127])
    // The cube floats inside the 256-tall dimension: void below (y<32) and above (y>223)
    // so all six faces — including the underside — have a walkable outer surface. Centre
    // stays at y=127.5 to match GravityHelper.CORE_Y.
    private static final int MIN_Y = 32;
    private static final int MAX_Y = 223;            // inclusive top of the cube
    private static final int DUST_LAYERS = 4;        // thickness of the dust shell on every face
    private static final int MAX_RELIEF = 1;         // minimal face relief — keeps a little texture but flattens bumps so the short wall-walking box doesn't slip under a lip and bury the first-person eye inside a block (heavier relief buried the camera under overhangs)

    // Block classification returned by classify()
    private static final int C_AIR = 0;
    private static final int C_DUST = 1;
    private static final int C_STONE = 2;

    // Permutation table for simplex noise (fixed seed for consistent terrain)
    private static final int[] PERM = new int[512];
    static {
        int[] p = {151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
            140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,
            247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,
            57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,
            74,165,71,134,139,48,27,166,77,146,158,231,83,111,229,122,
            60,211,133,230,220,105,92,41,55,46,245,40,244,102,143,54,
            65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,
            200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,64,
            52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,
            207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,
            119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
            129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,
            218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,
            81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,
            184,84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,
            222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
        for (int i = 0; i < 256; i++) { PERM[i] = p[i]; PERM[256 + i] = p[i]; }
    }

    public static final MapCodec<MoonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
        ).apply(instance, MoonChunkGenerator::new)
    );

    public MoonChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // --- Simplex-like 2D noise ---
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : z;
        double v = h < 4 ? z : (h == 12 || h == 14 ? x : 0);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private static double perlin2D(double x, double z) {
        int xi = (int) Math.floor(x) & 255;
        int zi = (int) Math.floor(z) & 255;
        double xf = x - Math.floor(x);
        double zf = z - Math.floor(z);
        double u = fade(xf);
        double v = fade(zf);
        int aa = PERM[PERM[xi] + zi];
        int ab = PERM[PERM[xi] + zi + 1];
        int ba = PERM[PERM[xi + 1] + zi];
        int bb = PERM[PERM[xi + 1] + zi + 1];
        return lerp(v,
            lerp(u, grad(aa, xf, zf), grad(ba, xf - 1, zf)),
            lerp(u, grad(ab, xf, zf - 1), grad(bb, xf - 1, zf - 1)));
    }

    /**
     * Per-face inward relief, in blocks, from a 2D noise map over the two
     * coordinates that span the face. {@code salt} shifts the sample window so
     * each of the six faces gets an independent-looking heightmap.
     * Returns a value in [0, MAX_RELIEF].
     */
    private static int faceRelief(double a, double b, int salt) {
        double sa = a + salt * 137.0;
        double sb = b + salt * 137.0;
        // Multi-octave: broad bumps + medium detail + fine grit.
        double n = perlin2D(sa * 0.03, sb * 0.03) * 1.0
                 + perlin2D(sa * 0.08, sb * 0.08) * 0.4
                 + perlin2D(sa * 0.18, sb * 0.18) * 0.15;
        double t = (n + 1.55) / 3.10; // normalize ~[-1.55,1.55] -> [0,1]
        if (t < 0.0) t = 0.0;
        if (t > 1.0) t = 1.0;
        return (int) Math.round(t * MAX_RELIEF);
    }

    /**
     * Classify a world block as core stone, dust shell, or air, based on how
     * far inside the cube it sits relative to all six noise-displaced faces.
     */
    private static int classify(int wx, int wy, int wz) {
        if (wx < -HALF_SIZE || wx >= HALF_SIZE || wz < -HALF_SIZE || wz >= HALF_SIZE
                || wy < MIN_Y || wy > MAX_Y) {
            return C_AIR;
        }
        // Distance (blocks) from each face's noise surface; positive = inside.
        int dXn = wx - (-HALF_SIZE + faceRelief(wy, wz, 1));
        int dXp = (HALF_SIZE - 1 - faceRelief(wy, wz, 2)) - wx;
        int dZn = wz - (-HALF_SIZE + faceRelief(wx, wy, 3));
        int dZp = (HALF_SIZE - 1 - faceRelief(wx, wy, 4)) - wz;
        int dYn = wy - (MIN_Y + faceRelief(wx, wz, 5));
        int dYp = (MAX_Y - faceRelief(wx, wz, 6)) - wy;

        int minD = Math.min(Math.min(Math.min(dXn, dXp), Math.min(dZn, dZp)), Math.min(dYn, dYp));
        if (minD < 0) return C_AIR;
        if (minD < DUST_LAYERS) return C_DUST;
        return C_STONE;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight(); // exclusive top

        BlockState moonStone = kingdom.smp.ModBlocks.MOON_STONE.get().defaultBlockState();
        BlockState moonDust = kingdom.smp.ModBlocks.MOON_DUST.get().defaultBlockState();

        // Heightmaps must be updated as we place blocks (mirrors FlatLevelSource).
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int loY = Math.max(minY, MIN_Y);
        int hiY = Math.min(maxY - 1, MAX_Y);

        for (int x = 0; x < 16; x++) {
            int worldX = chunkMinX + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = chunkMinZ + z;

                // Only generate within the 256x256 footprint (-128 to 127)
                if (worldX < -HALF_SIZE || worldX >= HALF_SIZE || worldZ < -HALF_SIZE || worldZ >= HALF_SIZE) {
                    continue;
                }

                for (int y = loY; y <= hiY; y++) {
                    int cls = classify(worldX, y, worldZ);
                    if (cls == C_AIR) continue;
                    BlockState state = (cls == C_STONE) ? moonStone : moonDust;
                    cursor.set(worldX, y, worldZ);
                    chunk.setBlockState(cursor, state, 0);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        if (x < -HALF_SIZE || x >= HALF_SIZE || z < -HALF_SIZE || z >= HALF_SIZE) {
            return level.getMinY();
        }
        // Highest solid block of the top face's noise surface, +1 for standing room.
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            if (classify(x, y, z) != C_AIR) return y + 1;
        }
        return level.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        if (x < -HALF_SIZE || x >= HALF_SIZE || z < -HALF_SIZE || z >= HALF_SIZE) {
            return new NoiseColumn(level.getMinY(), new BlockState[0]);
        }
        BlockState moonStone = kingdom.smp.ModBlocks.MOON_STONE.get().defaultBlockState();
        BlockState moonDust = kingdom.smp.ModBlocks.MOON_DUST.get().defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

        BlockState[] states = new BlockState[MAX_Y - MIN_Y + 1];
        for (int y = MIN_Y; y <= MAX_Y; y++) {
            int cls = classify(x, y, z);
            states[y - MIN_Y] = (cls == C_STONE) ? moonStone : (cls == C_DUST) ? moonDust : air;
        }
        return new NoiseColumn(level.getMinY(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Moon Chunk Generator (256-cube, all faces noised)");
    }
}
