package kingdom.smp.item.gear;

import java.util.function.Consumer;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Ranger shortbow — extends BowItem so it inherits full charge-and-fire mechanics. */
public class ClassBowItem extends BowItem {

    private final PlayerClass weaponClass;
    private final GearTier tier;

    public ClassBowItem(PlayerClass weaponClass, GearTier tier, Properties props) {
        super(props);
        this.weaponClass = weaponClass;
        this.tier = tier;
    }

    public PlayerClass weaponClass() { return weaponClass; }
    public GearTier tier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Class: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(ClassArmorItem.capitalize(weaponClass.name()))
                .withStyle(ClassArmorItem.classColor(weaponClass))));
        tooltip.accept(Component.literal("Requires " + ClassArmorItem.capitalize(weaponClass.name()) + " level " + tier.classLevelRequired)
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
