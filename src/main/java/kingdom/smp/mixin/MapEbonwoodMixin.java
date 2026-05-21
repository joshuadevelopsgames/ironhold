package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Maps stop updating in the Ebonwood Hollow — the biome's magic
 * blanks them out. Existing map data is preserved but no new
 * terrain is revealed while inside the biome.
 */
@Mixin(MapItem.class)
public abstract class MapEbonwoodMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void ironhold$blankMapInEbonwood(Level level, Entity entity,
                                              MapItemSavedData data, CallbackInfo ci) {
        if (entity.level().getBiome(entity.blockPosition()).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
            ci.cancel();
        }
    }
}
