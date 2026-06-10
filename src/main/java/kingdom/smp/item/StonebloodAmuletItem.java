package kingdom.smp.item;

import java.util.List;

import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Stoneblood Amulet — the Stone Golem's signature accessory. Passive tank suite: flat damage
 * reduction, knockback immunity, slowness immunity. Behaviour lives in
 * {@link kingdom.smp.game.BossArtifactHandler}. Spec: {@code specs/fantasia-ports/02-boss-accessories.md}.
 */
public class StonebloodAmuletItem extends AccessoryItem {

    public StonebloodAmuletItem(Properties props) {
        super(props);
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("  -15% damage taken").withStyle(ChatFormatting.GRAY),
            Component.literal("  Immune to knockback and slowness").withStyle(ChatFormatting.GRAY));
    }
}
