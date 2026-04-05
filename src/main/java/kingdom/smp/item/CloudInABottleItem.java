package kingdom.smp.item;

import java.util.List;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Mid-air jump accessory (Breeze in a Bottle; see {@link kingdom.smp.game.CloudDoubleJumpHandler}).
 */
public class CloudInABottleItem extends AccessoryItem {

    public CloudInABottleItem(Properties props) {
        super(props);
    }

    public static boolean isEquipped(Player player) {
        // Use getData: on clients getExistingDataOrNull can be null before attachment is materialized,
        // and synced accessory contents still live on the player's attachment instance.
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        var cloud = Ironhold.CLOUD_IN_A_BOTTLE.get();
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).is(cloud)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
                Component.literal("  +Mid-air breeze jump while equipped").withStyle(ChatFormatting.AQUA));
    }
}
