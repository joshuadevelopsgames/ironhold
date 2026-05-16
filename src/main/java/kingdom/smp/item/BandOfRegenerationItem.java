package kingdom.smp.item;

import java.util.List;

import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Terraria-inspired accessory — grants Regeneration I while equipped in an accessory slot.
 */
public class BandOfRegenerationItem extends AccessoryItem {

    public BandOfRegenerationItem(Properties props) {
        super(props);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        // Re-apply every 40 ticks; the 55-tick effect duration overlaps the next
        // refresh comfortably so regeneration stays continuous without spamming
        // addEffect every single tick.
        if (player.tickCount % 40 != 0) return;
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, 55, 0, true, false, true));
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
                Component.literal("  +Regeneration I while equipped").withStyle(ChatFormatting.GREEN));
    }
}
