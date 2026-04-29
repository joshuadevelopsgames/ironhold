package kingdom.smp.gear;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Item tags used by the gear quality system.
 *
 * {@link #ORE_OR_INGOT} marks raw ores and smelted ingots that are eligible to carry a
 * {@link kingdom.smp.gear.GearComponents#QUALITY quality} component. Damageable items
 * (armor, tools, weapons, shields, elytra, etc.) are eligible by virtue of being damageable;
 * the tag is for non-damageable crafting inputs that need to *carry* quality through smelting
 * and crafting into the resulting gear.
 *
 * Tag JSON: {@code data/ironhold/tags/item/ore_or_ingot.json}
 */
public final class IronholdGearTags {
    private IronholdGearTags() {}

    public static final TagKey<Item> ORE_OR_INGOT = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "ore_or_ingot"));
}
