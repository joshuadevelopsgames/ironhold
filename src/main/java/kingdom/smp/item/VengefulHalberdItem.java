package kingdom.smp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

/**
 * Vengeful Halberd — a legendary long-reach weapon dropped by Possessed Armor.
 * Deals more damage the lower the wielder's health is.
 * Base stats (damage, speed, range) are set via Item.Properties at registration.
 */
public class VengefulHalberdItem extends Item {

    public VengefulHalberdItem(Properties props) {
        super(props);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel sl) {
            // Bonus damage scales with missing health: 0% HP = +8 bonus, 100% HP = +0
            float healthRatio = attacker.getHealth() / attacker.getMaxHealth();
            float bonusDamage = (1.0F - healthRatio) * 8.0F;
            if (bonusDamage > 0.5F) {
                target.hurtServer(sl, sl.damageSources().mobAttack(attacker), bonusDamage);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Vengeance").withStyle(ChatFormatting.DARK_RED));
        tooltip.accept(Component.literal("Deals more damage the lower your health")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
