package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implements {@link SlotAccessor} on {@link Slot}. Strips {@code final} from
 * {@code x}/{@code y} and writes them through the public-facing accessor interface.
 *
 * <p>Pattern note: the interface lives in {@code kingdom.smp.access} (not under the
 * mixin package) so that direct casts to it don't trigger Sponge Mixin 0.17+'s
 * {@code IllegalClassLoadError} on dedicated servers. The interface is "regular"
 * Java; only this mixin class is.
 */
@Mixin(Slot.class)
public abstract class SlotMixin implements SlotAccessor {

    @Mutable @Final @Shadow public int x;
    @Mutable @Final @Shadow public int y;

    @Override
    public void ironhold$setX(int x) { this.x = x; }

    @Override
    public void ironhold$setY(int y) { this.y = y; }
}
