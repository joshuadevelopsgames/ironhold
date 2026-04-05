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
        return new TempestArrowEntity(shooter, level, stack, weapon, true, false);
    }
}
