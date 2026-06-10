package kingdom.smp.item;

import kingdom.smp.entity.ButterflySpecies;
import net.minecraft.world.item.Item;

/**
 * A live butterfly captured by the butterfly net. It doubles as Terraria-style
 * fishing bait (see {@code ModItems#registerBaitProfiles}) and is what you
 * right-click into a Butterfly Jar block to display it.
 */
public class ButterflyItem extends Item {
    private final ButterflySpecies species;

    public ButterflyItem(Properties properties, ButterflySpecies species) {
        super(properties);
        this.species = species;
    }

    public ButterflySpecies species() {
        return species;
    }
}
