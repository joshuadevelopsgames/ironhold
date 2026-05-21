package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes compasses spin randomly in the Ebonwood Hollow, as if the
 * biome's magic interferes with navigation.
 */
@Mixin(CompassAngle.class)
public abstract class CompassEbonwoodMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void ironhold$spinInEbonwood(ItemStack stack, ClientLevel level, ItemOwner owner,
                                          int seed, CallbackInfoReturnable<Float> cir) {
        if (level == null || owner == null) return;
        if (level.getBiome(net.minecraft.core.BlockPos.containing(owner.position())).is(kingdom.smp.ModWorldgen.EBONWOOD_HOLLOW)) {
            // Spin erratically based on game time + seed
            long t = level.getGameTime();
            float spin = (float) ((Math.sin(t * 0.073 + seed * 31) * 0.5
                    + Math.sin(t * 0.131 + seed * 17) * 0.3
                    + Math.sin(t * 0.037 + seed * 7) * 0.2) * 0.5 + 0.5);
            cir.setReturnValue(spin);
        }
    }
}
