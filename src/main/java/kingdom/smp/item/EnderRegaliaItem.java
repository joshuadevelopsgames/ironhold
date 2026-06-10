package kingdom.smp.item;

import java.util.List;

import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Ender Regalia — King Enderman's signature accessory. Passive: endermen ignore you and cannot harm
 * you. Active (flagship): a short forward blink on the accessory key. Behaviour lives in
 * {@link kingdom.smp.game.BossArtifactHandler}. Spec: {@code specs/fantasia-ports/02-boss-accessories.md}.
 */
public class EnderRegaliaItem extends AccessoryItem {

    public EnderRegaliaItem(Properties props) {
        super(props);
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("  Endermen ignore you and cannot harm you").withStyle(ChatFormatting.DARK_PURPLE),
            Component.literal("  Active: blink forward (accessory key)").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
