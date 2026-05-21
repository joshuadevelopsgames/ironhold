package kingdom.smp.seasons;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Per-block seasonal allow-list resolver. Combines hard-coded fallbacks for vanilla crops with
 * datapack-driven block tags ({@link SeasonTags#SPRING_CROPS} etc.), so server admins can
 * adjust without recompiling.
 */
public final class CropFertility {
    private CropFertility() {}

    public enum Outcome {
        FERTILE,
        OUT_OF_SEASON,
    }

    public static Outcome check(Level level, LevelReader access, BlockPos pos, BlockState state) {
        if (pos.getY() < SeasonConfig.UNDERGROUND_FERTILITY_Y) return Outcome.FERTILE;
        if (hasGreenhouseGlassAbove(access, pos)) return Outcome.FERTILE;

        Block block = state.getBlock();
        if (matches(block, SeasonTags.YEAR_ROUND_CROPS)) return Outcome.FERTILE;

        Season.SubSeason sub = Seasons.current(level).subSeason();
        Season season = sub.parent();

        int mask = seasonMask(block);
        if (mask == 0) {
            // Unlisted crops default to fertile outside winter.
            return season == Season.WINTER ? Outcome.OUT_OF_SEASON : Outcome.FERTILE;
        }
        int seasonBit = 1 << season.ordinal();
        return (mask & seasonBit) != 0 ? Outcome.FERTILE : Outcome.OUT_OF_SEASON;
    }

    private static int seasonMask(Block block) {
        BlockState s = block.defaultBlockState();
        int mask = 0;
        if (s.is(SeasonTags.SPRING_CROPS)) mask |= 1 << Season.SPRING.ordinal();
        if (s.is(SeasonTags.SUMMER_CROPS)) mask |= 1 << Season.SUMMER.ordinal();
        if (s.is(SeasonTags.AUTUMN_CROPS)) mask |= 1 << Season.AUTUMN.ordinal();
        if (s.is(SeasonTags.WINTER_CROPS)) mask |= 1 << Season.WINTER.ordinal();
        return mask;
    }

    private static boolean matches(Block block, TagKey<Block> tag) {
        return block.defaultBlockState().is(tag);
    }

    private static boolean hasGreenhouseGlassAbove(LevelReader level, BlockPos pos) {
        for (int i = 1; i <= 4; i++) {
            BlockPos above = pos.above(i);
            BlockState s = level.getBlockState(above);
            if (s.isAir()) continue;
            return s.is(SeasonTags.GREENHOUSE_GLASS);
        }
        return false;
    }
}
