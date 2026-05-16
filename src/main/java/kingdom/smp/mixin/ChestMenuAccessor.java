package kingdom.smp.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code container} field on {@link ChestMenu} so the
 * Ironhold reskin can distinguish ender chests ({@code PlayerEnderChestContainer})
 * from regular chests / barrels (other Container impls).
 */
@Mixin(ChestMenu.class)
public interface ChestMenuAccessor {
    @Accessor("container")
    Container ironhold$getContainer();
}
