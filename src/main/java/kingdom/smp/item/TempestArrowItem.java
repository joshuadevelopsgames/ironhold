package kingdom.smp.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import kingdom.smp.entity.TempestArrowEntity;

public class TempestArrowItem extends ArrowItem {
    public TempestArrowItem(Properties props) {
        super(props);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
        // Lightning is guaranteed only when fired from the Tempest Bow; from any other bow this
        // behaves like a normal arrow (but keeps its electric look).
        boolean fromTempestBow = weapon.getItem() instanceof TempestBowItem;
        return new TempestArrowEntity(shooter, level, stack, weapon, fromTempestBow);
    }
}
