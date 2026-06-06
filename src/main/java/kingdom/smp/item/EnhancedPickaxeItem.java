package kingdom.smp.item;

import net.minecraft.world.item.Item;

/**
 * Enhanced Pickaxe — automatically smelts ores as they are mined.
 *
 * When mining an ore block with this pickaxe, the smelted result is dropped
 * instead of the raw ore. For example:
 * - Iron Ore → Iron Ingot
 * - Gold Ore → Gold Ingot
 * - Diamond Ore → Diamond
 *
 * The smelting behavior is handled by EnhancedPickaxeSmeltHandler event listener.
 * Mining speed and damage attributes are set in ModItems registration.
 */
public class EnhancedPickaxeItem extends Item {
    public EnhancedPickaxeItem(Properties properties) {
        super(properties);
    }
}
