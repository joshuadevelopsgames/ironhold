package kingdom.smp.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CaveMouthFeature — carves a shallow funnel-shaped pit from the surface downward,
 * creating natural cave entrances in the Ebonwood Hollow biome.
 *
 * Pit shape: 4–6 blocks deep, radius tapers from ~2 at the top to 1 at the bottom.
 * Walls and floor are randomly dressed with mossy cobblestone and mossy stone bricks,
 * giving them an ancient, overgrown appearance.
 *
 * These pits connect to natural underground caves when present, creating pocket
 * ambush spots ideal for filchers.
 */
public class CaveMouthFeature extends Feature<NoneFeatureConfiguration> {

    public CaveMouthFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin     = ctx.origin();
        RandomSource rng    = ctx.random();

        int depth = 4 + rng.nextInt(3); // 4, 5, or 6 blocks deep

        boolean placedAny = false;

        // ── Pass 1: carve the funnel ─────────────────────────────────────────
        for (int dy = 0; dy > -depth; dy--) {
            // Radius shrinks from ~2 at the rim to 1 near the bottom
            // At dy=0: radius = 2. At dy=-(depth-1): radius = 1.
            float progress = (float) (-dy) / (depth - 1); // 0 at top, 1 at bottom
            int radius = Math.max(1, Math.round(2 - progress));

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        // Only replace solid, non-fluid blocks (respect water, lava)
                        if (state.isSolid() && state.getFluidState().isEmpty()) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            placedAny = true;
                        }
                    }
                }
            }
        }

        if (!placedAny) return false;

        // ── Pass 2: dress the walls with mossy cobblestone & mossy stone bricks ──
        // For each block adjacent to the newly carved air, randomly replace with
        // mossy material to give the mouth an ancient, overgrown look.
        for (int dy = 1; dy > -(depth + 1); dy--) {
            float progress = (float) (-dy + 1) / (depth);
            int wallRadius = Math.max(1, Math.round(2 - progress)) + 1;

            for (int dx = -wallRadius; dx <= wallRadius; dx++) {
                for (int dz = -wallRadius; dz <= wallRadius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState here = level.getBlockState(pos);

                    // Only dress blocks that are solid stone/dirt variants adjacent to air
                    if (!isStoneLike(here)) continue;
                    if (!hasAdjacentAir(level, pos)) continue;

                    // 40% chance mossy cobblestone, 20% mossy stone bricks, rest untouched
                    float roll = rng.nextFloat();
                    if (roll < 0.40f) {
                        level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
                    } else if (roll < 0.60f) {
                        level.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
                    }
                    // else leave natural
                }
            }
        }

        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns true if the block is a natural stone or dirt type suitable for wall dressing. */
    private static boolean isStoneLike(BlockState state) {
        return state.is(Blocks.STONE)
            || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.DEEPSLATE)
            || state.is(Blocks.COBBLED_DEEPSLATE)
            || state.is(Blocks.DIRT)
            || state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.ROOTED_DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.TUFF)
            || state.is(Blocks.ANDESITE)
            || state.is(Blocks.DIORITE)
            || state.is(Blocks.GRANITE);
    }

    /** Returns true if any of the 6 face-adjacent blocks is air. */
    private static boolean hasAdjacentAir(WorldGenLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos.above())
            || level.isEmptyBlock(pos.below())
            || level.isEmptyBlock(pos.north())
            || level.isEmptyBlock(pos.south())
            || level.isEmptyBlock(pos.east())
            || level.isEmptyBlock(pos.west());
    }
}
