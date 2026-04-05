package kingdom.smp.item.gear;

import java.util.function.Consumer;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Armor item tagged with a specific class and tier for restriction and full-set bonus logic. */
public class ClassArmorItem extends Item {

    private final PlayerClass armorClass;
    private final GearTier tier;

    public ClassArmorItem(PlayerClass armorClass, GearTier tier, Properties properties) {
        super(properties);
        this.armorClass = armorClass;
        this.tier = tier;
    }

    public PlayerClass armorClass() { return armorClass; }
    public GearTier tier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.literal("Class: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(capitalize(armorClass.name()))
                .withStyle(classColor(armorClass))));
        tooltip.accept(Component.literal("Requires " + capitalize(armorClass.name()) + " level " + tier.classLevelRequired)
            .withStyle(ChatFormatting.DARK_GRAY));
    }

    static ChatFormatting classColor(PlayerClass c) {
        return switch (c) {
            case KNIGHT  -> ChatFormatting.BLUE;
            case RANGER  -> ChatFormatting.GREEN;
            case ROGUE   -> ChatFormatting.DARK_GRAY;
            case CLERIC  -> ChatFormatting.GOLD;
            case WIZARD  -> ChatFormatting.LIGHT_PURPLE;
            case PEASANT -> ChatFormatting.WHITE;
        };
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
