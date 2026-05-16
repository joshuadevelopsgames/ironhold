package kingdom.smp.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes setters for {@link AbstractContainerScreen}'s otherwise-final
 * {@code imageWidth} / {@code imageHeight} fields, used by the Ironhold
 * inventory reskin to resize the panel after vanilla's constructor runs.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Mutable
    @Accessor("imageWidth")
    void ironhold$setImageWidth(int w);

    @Mutable
    @Accessor("imageHeight")
    void ironhold$setImageHeight(int h);
}
