package kingdom.smp.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds Quality / Condition / Fatigue lines to gear tooltips. Only shows on damageable items
 * (armor, tools, weapons) so plain blocks and consumables stay clean.
 *
 * Registered to {@code NeoForge.EVENT_BUS} in {@link kingdom.smp.Ironhold}.
 */
public final class GearTooltipHandler {
    private GearTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!QualityScope.isEligible(stack)) return;

        boolean damageable = stack.isDamageableItem();

        ItemQuality quality = GearComponents.getQuality(stack);
        Style qualityLabel = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
        Style qualityValue = Style.EMPTY.withColor(quality.tooltipColor()).withBold(quality == ItemQuality.MINT);

        event.getToolTip().add(Component.literal("Quality: ").withStyle(qualityLabel)
                .append(Component.literal(quality.displayName()).withStyle(qualityValue)));

        // Condition + fatigue only apply to damageable gear; ores have no condition or fatigue.
        if (!damageable) return;

        ItemCondition condition = ItemCondition.fromStack(stack);
        Style condLabel = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
        Style condValue = Style.EMPTY.withColor(condition.tooltipColor());
        event.getToolTip().add(Component.literal("Condition: ").withStyle(condLabel)
                .append(Component.literal(condition.displayName()).withStyle(condValue)));

        RepairFatigue fatigue = GearComponents.getFatigue(stack);
        if (fatigue.level() > 0) {
            String fatigueText = "Fatigue: " + fatigue.level() + " / " + RepairFatigue.MAX_LEVEL;
            ChatFormatting fatigueColor = fatigue.level() >= 4 ? ChatFormatting.RED
                    : fatigue.level() >= 2 ? ChatFormatting.YELLOW
                    : ChatFormatting.GRAY;
            event.getToolTip().add(Component.literal(fatigueText)
                    .withStyle(Style.EMPTY.withColor(fatigueColor)));
        }
    }
}
