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
        // Refresh every tick so regeneration is continuous (tickCount % 80 delayed the first pulse for ~4s).
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, 55, 0, true, false, true));
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
                Component.literal("  +Regeneration I while equipped").withStyle(ChatFormatting.GREEN));
    }
}
