package kingdom.smp.seasons;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Gates random-tick crop growth and bonemeal application against the current season.
 *
 * <p>Out-of-season behavior is configured via {@link SeasonConfig#OUT_OF_SEASON_BEHAVIOR}:
 * <ul>
 *   <li>0 — slow growth (5-in-6 chance to skip each tick — crops still grow but ~6× slower)</li>
 *   <li>1 — fully block growth</li>
 *   <li>2 — destroy the crop block</li>
 * </ul>
 *
 * <p>Bonemeal is always blocked when out of season — feels less arbitrary than letting players
 * paste through restrictions with stacks of bonemeal.
 */
public final class CropGrowthGate {
    private CropGrowthGate() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCropGrow(CropGrowEvent.Pre event) {
        LevelAccessor access = event.getLevel();
        if (!(access instanceof Level lvl)) return;
        if (!SeasonConfig.isDimensionEnabled(lvl.dimension())) return;

        CropFertility.Outcome outcome = CropFertility.check(lvl, access, event.getPos(), event.getState());
        if (outcome == CropFertility.Outcome.FERTILE) return;

        switch (SeasonConfig.OUT_OF_SEASON_BEHAVIOR) {
            case 1 -> event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            case 2 -> {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
                if (lvl instanceof ServerLevel server) {
                    server.setBlockAndUpdate(event.getPos(), Blocks.AIR.defaultBlockState());
                }
            }
            default -> {
                if (lvl.getRandom().nextInt(SeasonConfig.SLOW_GROWTH_SKIP_DIVISOR) != 0) {
                    event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBonemeal(BonemealEvent event) {
        Level lvl = event.getLevel();
        if (lvl == null) return;
        if (!SeasonConfig.isDimensionEnabled(lvl.dimension())) return;
        CropFertility.Outcome outcome = CropFertility.check(lvl, lvl, event.getPos(), event.getState());
        if (outcome == CropFertility.Outcome.OUT_OF_SEASON) {
            event.setCanceled(true);
        }
    }
}
