package kingdom.smp.seasons.client;

import kingdom.smp.seasons.CropFertility;
import kingdom.smp.seasons.Season;
import kingdom.smp.seasons.SeasonConfig;
import kingdom.smp.seasons.Seasons;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.EnumSet;

/**
 * Stardew-style seasonal hint on plantable crop items: lists the season(s) the crop is fertile in
 * and — when the player is in a season-enabled dimension — whether it is in season right now.
 *
 * <p>Client-only (registered on the game bus from {@code IronholdClient}); {@link ItemTooltipEvent}
 * never fires server-side. Crop classification reuses {@link CropFertility} so the tooltip can never
 * disagree with the actual growth gate.
 */
public final class CropSeasonTooltipHandler {
    private CropSeasonTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Block block = resolveCropBlock(stack);
        if (block == null || !CropFertility.isSeasonalCrop(block)) return;

        var tooltip = event.getToolTip();

        if (CropFertility.isYearRound(block)) {
            tooltip.add(Component.literal("Grows year-round").withStyle(ChatFormatting.GREEN));
            return;
        }

        EnumSet<Season> seasons = CropFertility.fertileSeasons(block);
        if (seasons.isEmpty()) return;

        MutableComponent line = Component.literal("Seasons: ").withStyle(ChatFormatting.GRAY);
        boolean first = true;
        for (Season s : Season.values()) {
            if (!seasons.contains(s)) continue;
            if (!first) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            line.append(Component.literal(displayName(s)).withStyle(color(s)));
            first = false;
        }
        tooltip.add(line);

        // Live "in season now" indicator — only meaningful in a season-enabled dimension.
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level != null && Seasons.isEnabled(level)) {
            Season now = Seasons.current(level).subSeason().parent();
            if (seasons.contains(now)) {
                tooltip.add(Component.literal("✔ In season").withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.literal("✖ Out of season (now " + displayName(now) + ")")
                    .withStyle(ChatFormatting.RED));
            }
        }

        tooltip.add(Component.literal("Any season under glass or below Y" + SeasonConfig.UNDERGROUND_FERTILITY_Y)
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Resolve the crop block a plantable item places, or null if the item isn't a crop. */
    private static Block resolveCropBlock(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock();
        }
        if (stack.is(Items.COCOA_BEANS)) {
            return Blocks.COCOA; // cocoa beans aren't a BlockItem but plant the cocoa crop
        }
        return null;
    }

    private static String displayName(Season s) {
        return switch (s) {
            case SPRING -> "Spring";
            case SUMMER -> "Summer";
            case AUTUMN -> "Autumn";
            case WINTER -> "Winter";
        };
    }

    private static ChatFormatting color(Season s) {
        return switch (s) {
            case SPRING -> ChatFormatting.GREEN;
            case SUMMER -> ChatFormatting.YELLOW;
            case AUTUMN -> ChatFormatting.GOLD;
            case WINTER -> ChatFormatting.AQUA;
        };
    }
}
