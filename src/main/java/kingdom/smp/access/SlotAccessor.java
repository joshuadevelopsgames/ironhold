package kingdom.smp.access;

/**
 * Plain interface — NOT a mixin. Implemented by {@link net.minecraft.world.inventory.Slot}
 * via {@link kingdom.smp.mixin.SlotMixin}, which strips {@code final} from {@code x}/{@code y}
 * and writes them.
 *
 * <p>Lives outside the {@code kingdom.smp.mixin} package on purpose: Sponge Mixin 0.17+
 * refuses {@code IllegalClassLoadError} on direct casts to interfaces declared as mixin
 * configs. Splitting the public-facing interface from the implementing mixin lets all
 * call sites cast to this regular interface safely.
 */
public interface SlotAccessor {
    void ironhold$setX(int x);
    void ironhold$setY(int y);
}
