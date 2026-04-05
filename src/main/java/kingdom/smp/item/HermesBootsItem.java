package kingdom.smp.item;

import java.util.List;

import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Terraria-inspired accessory — movement speed via attribute modifier in {@link kingdom.smp.game.AccessoryTickHandler}
 * (~Speed I), removed as soon as the item leaves an accessory slot (no lingering {@code MobEffects.SPEED}).
 */
public class HermesBootsItem extends AccessoryItem {

    public HermesBootsItem(Properties props) {
        super(props);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        // Applied in AccessoryTickHandler so the modifier is cleared every tick when unequipped.
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
                Component.literal("  +Movement speed while equipped").withStyle(ChatFormatting.AQUA));
    }
}
