package kingdom.smp.gear;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;

/**
 * Defines which items participate in the quality system — both for tooltip display and stat scaling.
 *
 * Eligible:
 * <ul>
 *   <li>Armor (damageable items in armor slots)</li>
 *   <li>Weapons — swords, axes (as weapons), {@link BowItem}, {@link CrossbowItem},
 *       {@link TridentItem}, {@link MaceItem}</li>
 *   <li>Tools — pickaxes, axes, shovels, hoes</li>
 *   <li>Ores and ingots in the {@link IronholdGearTags#ORE_OR_INGOT} tag — carrier metadata
 *       for the crafting pipeline.</li>
 * </ul>
 *
 * Explicitly excluded (utility gear — durability stays vanilla, no quality tag carried):
 * <ul>
 *   <li>Shield, fishing rod, shears, flint and steel, brush (instanceof checks)</li>
 *   <li>Elytra, carrot on a stick, warped fungus on a stick, wolf armor (item identity)</li>
 * </ul>
 *
 * Not eligible at all: blocks, food, sticks, dyes, banners, anything else not damageable
 * and not in the ore tag. Plain Minecraft.
 */
public final class QualityScope {
    private QualityScope() {}

    public static boolean isEligible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Ores and ingots carry quality even though they're not damageable.
        if (stack.is(IronholdGearTags.ORE_OR_INGOT)) return true;
        // Damageable items: gear/weapons/tools, with utility blacklist.
        if (!stack.isDamageableItem()) return false;
        return !isUtility(stack.getItem());
    }

    /**
     * Utility items — durability stays vanilla, no quality scaling applied.
     * Hardcoded blacklist; kept small and stable. If a modpack needs to extend, refactor
     * this to a {@code ironhold:utility_gear_excluded} tag.
     */
    private static boolean isUtility(Item item) {
        if (item instanceof ShieldItem) return true;
        if (item instanceof FishingRodItem) return true;
        if (item instanceof ShearsItem) return true;
        if (item instanceof FlintAndSteelItem) return true;
        if (item instanceof BrushItem) return true;
        if (item == Items.ELYTRA) return true;
        if (item == Items.CARROT_ON_A_STICK) return true;
        if (item == Items.WARPED_FUNGUS_ON_A_STICK) return true;
        if (item == Items.WOLF_ARMOR) return true;
        return false;
    }
}
