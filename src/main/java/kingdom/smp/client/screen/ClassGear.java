package kingdom.smp.client.screen;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Display-only equipment loadouts used by the class selection screen's
 * 3D character previews. These ItemStacks are never given to the player.
 */
public final class ClassGear {

    private ClassGear() {}

    public record Loadout(ItemStack head, ItemStack chest, ItemStack legs,
                          ItemStack feet, ItemStack main, ItemStack off) {
        public static Loadout empty() {
            return new Loadout(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                               ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        }
    }

    public static Loadout forClass(PlayerClass pc) {
        return switch (pc) {
            // Tier 2 — full professional gear
            case KNIGHT -> new Loadout(
                new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.SHIELD));
            case RANGER -> new Loadout(
                dyedLeather(Items.LEATHER_HELMET, 0x3A5A2E),
                dyedLeather(Items.LEATHER_CHESTPLATE, 0x3A5A2E),
                dyedLeather(Items.LEATHER_LEGGINGS, 0x3A5A2E),
                dyedLeather(Items.LEATHER_BOOTS, 0x3A5A2E),
                new ItemStack(Ironhold.TEMPEST_BOW.get()),
                ItemStack.EMPTY);
            case WIZARD -> new Loadout(
                ItemStack.EMPTY,
                dyedLeather(Items.LEATHER_CHESTPLATE, 0x4A2A7A),
                dyedLeather(Items.LEATHER_LEGGINGS, 0x4A2A7A),
                dyedLeather(Items.LEATHER_BOOTS, 0x4A2A7A),
                new ItemStack(Ironhold.WIZARD_STAFF.get()),
                ItemStack.EMPTY);
            case CLERIC -> new Loadout(
                new ItemStack(Items.GOLDEN_HELMET),
                new ItemStack(Items.GOLDEN_CHESTPLATE),
                new ItemStack(Items.GOLDEN_LEGGINGS),
                new ItemStack(Items.GOLDEN_BOOTS),
                new ItemStack(Ironhold.SOLUNA_STAFF.get()),
                ItemStack.EMPTY);

            // Tier 1 — starter / apprentice gear (same family, simpler)
            case SQUIRE -> new Loadout(
                new ItemStack(Items.LEATHER_HELMET),
                new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.LEATHER_LEGGINGS),
                new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.STONE_SWORD),
                new ItemStack(Items.SHIELD));
            case ARCHER -> new Loadout(
                dyedLeather(Items.LEATHER_HELMET, 0x4A6E3E),
                dyedLeather(Items.LEATHER_CHESTPLATE, 0x4A6E3E),
                dyedLeather(Items.LEATHER_LEGGINGS, 0x4A6E3E),
                dyedLeather(Items.LEATHER_BOOTS, 0x4A6E3E),
                new ItemStack(Items.BOW),
                ItemStack.EMPTY);
            case MAGE_APPRENTICE -> new Loadout(
                ItemStack.EMPTY,
                dyedLeather(Items.LEATHER_CHESTPLATE, 0x6A4AA0),
                dyedLeather(Items.LEATHER_LEGGINGS, 0x6A4AA0),
                dyedLeather(Items.LEATHER_BOOTS, 0x6A4AA0),
                new ItemStack(Items.STICK),
                ItemStack.EMPTY);
            case MEDIC -> new Loadout(
                ItemStack.EMPTY,
                dyedLeather(Items.LEATHER_CHESTPLATE, 0xE8D8A0),
                dyedLeather(Items.LEATHER_LEGGINGS, 0xE8D8A0),
                dyedLeather(Items.LEATHER_BOOTS, 0xE8D8A0),
                new ItemStack(Items.STICK),
                ItemStack.EMPTY);

            default -> Loadout.empty();
        };
    }

    private static ItemStack dyedLeather(Item leather, int rgb) {
        ItemStack stack = new ItemStack(leather);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        return stack;
    }
}
