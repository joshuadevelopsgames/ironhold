package kingdom.smp.gear;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Adds affix lines to gear tooltips. Registered to the game bus in {@code Ironhold}. */
public final class AffixTooltipHandler {
    private AffixTooltipHandler() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<AffixInstance> affixes = AffixData.get(event.getItemStack());
        if (affixes.isEmpty()) {
            return;
        }
        event.getToolTip().add(Component.literal("Affixes:").withStyle(ChatFormatting.DARK_GRAY));
        for (AffixInstance ai : affixes) {
            Affix a = Affix.byId(ai.id());
            if (a == null) {
                continue;
            }
            String value = a.percent()
                ? "+" + Math.round(ai.roll() * 100) + "%"
                : "+" + (Math.round(ai.roll() * 10) / 10.0);
            event.getToolTip().add(Component.literal("  " + a.displayName() + " " + value).withStyle(a.color()));
        }
    }
}
