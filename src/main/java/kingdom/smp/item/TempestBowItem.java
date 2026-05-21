package kingdom.smp.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.TempestArrowEntity;

public class TempestBowItem extends BowItem {
    public TempestBowItem(Properties props) {
        super(props);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        if (arrow instanceof TempestArrowEntity) {
            return arrow;
        }
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
            return arrow;
        }
        TempestArrowEntity replacement = new TempestArrowEntity(shooter, arrow.level(), projectileStack, weaponStack, false, true);
        replacement.setCritArrow(arrow.isCritArrow());
        return replacement;
    }

    @Override
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return kingdom.smp.ModItems.TEMPEST_ARROW.get().getDefaultInstance();
    }
}
