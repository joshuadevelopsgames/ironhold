package kingdom.smp.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * The Tempest Bow. Mechanically a normal bow, but a {@link TempestArrowItem} detects when it was
 * fired from this bow and guarantees a lightning strike on impact (see
 * {@link TempestArrowItem#createArrow}). In creative, the bow auto-fires Tempest Arrows.
 */
public class TempestBowItem extends BowItem {
    public TempestBowItem(Properties props) {
        super(props);
    }

    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return kingdom.smp.ModItems.TEMPEST_ARROW.get().getDefaultInstance();
    }
}
