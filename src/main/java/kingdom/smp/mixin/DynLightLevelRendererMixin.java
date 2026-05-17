package kingdom.smp.mixin;

import kingdom.smp.dynlight.DynamicLights;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches the per-block lightmap value used by terrain rendering so it reflects nearby
 * dynamic light sources (held torches, registered wands, glowing mobs).
 *
 * <p>Solid blocks short-circuit — their lightmap is sampled from the air block above them and
 * patching them would produce visible seams.
 */
@Mixin(LevelRenderer.class)
public abstract class DynLightLevelRendererMixin {

    @Inject(
        method = "getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void ironhold$injectDynamicLight(
            LevelRenderer.BrightnessGetter getter,
            BlockAndLightGetter level,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        if (state.isSolidRender()) return;
        int original = cir.getReturnValueI();
        int patched = DynamicLights.patchLightmap(level, pos, original);
        if (patched != original) cir.setReturnValue(patched);
    }
}
