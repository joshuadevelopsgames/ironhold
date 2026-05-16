package kingdom.smp.mine;

import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.gear.QualityScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/**
 * Stamps {@link ItemQuality} onto raw-ore drops based on the {@link MineGeography}
 * of the break position. Runs at {@link EventPriority#LOW} so that profession
 * bonus drops (added at NORMAL priority by skill handlers) are also stamped
 * uniformly with the rolled tier.
 *
 * One roll per break — every eligible drop from a single break receives the
 * same quality. Models "this vein had quality X" rather than per-item RNG, and
 * keeps stack merging in the player's inventory clean.
 *
 * Silk-touched ore blocks fall through unstamped (the ore-block items are not
 * in the {@code ironhold:ore_or_ingot} tag), so quality is determined at smelt
 * time via the §6.1 propagation hook. Documented loophole pending a smelting
 * source-tracking pass.
 */
public final class MineDropQualityHandler {
    private MineDropQualityHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!event.getState().is(Tags.Blocks.ORES)) return;
        List<ItemEntity> drops = event.getDrops();
        if (drops.isEmpty()) return;

        ServerLevel level = event.getLevel();
        MineGeography zone = MineLookup.classify(level, event.getPos());
        ItemQuality quality = zone.rollQuality(level.getRandom());
        if (quality == ItemQuality.defaultQuality()) return;

        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (QualityScope.isEligible(stack)) {
                GearComponents.setQuality(stack, quality);
            }
        }
    }
}
