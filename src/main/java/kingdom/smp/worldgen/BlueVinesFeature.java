package kingdom.smp.worldgen;

import com.mojang.serialization.Codec;
import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * BlueVinesFeature — places clusters of blue vines (tip + body) growing upward
 * from the floor of the Ebonwood Hollow biome.
 *
 * For each chunk invocation, 64 random candidates are sampled within ±8 blocks
 * (XZ) and ±4 blocks (Y) of the origin. Each valid floor spawns a vine column
 * 1–6 blocks tall: BlueVinesPlantBlock body segments topped by a BlueVinesBlock
 * tip with a random age (0–25), matching vanilla Twisting Vines behaviour.
 */
public class BlueVinesFeature extends Feature<NoneFeatureConfiguration> {

    public BlueVinesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        BlockState tipState = kingdom.smp.ModBlocks.BLUE_VINES.get().defaultBlockState();
        BlockState bodyState = kingdom.smp.ModBlocks.BLUE_VINES_PLANT.get().defaultBlockState();

        boolean placedAny = false;

        for (int i = 0; i < 12; i++) {
            int dx = random.nextInt(8) - random.nextInt(8);
            int dy = random.nextInt(4) - random.nextInt(4);
            int dz = random.nextInt(8) - random.nextInt(8);

            BlockPos candidate = origin.offset(dx, dy, dz);

            // Need air at candidate and a solid block below it
            if (!level.isEmptyBlock(candidate)) continue;
            if (!level.getBlockState(candidate.below()).isSolid()) continue;

            int height = 1 + random.nextInt(6); // 1–6 blocks tall

            boolean columnOk = true;
            for (int h = 0; h < height; h++) {
                if (!level.isEmptyBlock(candidate.above(h))) {
                    columnOk = false;
                    break;
                }
            }
            if (!columnOk) continue;

            // Place body segments below the tip
            for (int h = 0; h < height - 1; h++) {
                level.setBlock(candidate.above(h), bodyState, 2);
            }

            // Place the tip at the top with a random age
            BlockState tip = tipState.setValue(BlockStateProperties.AGE_25, random.nextInt(26));
            level.setBlock(candidate.above(height - 1), tip, 2);

            placedAny = true;
        }

        return placedAny;
    }
}
