package kingdom.smp.mixin;

import kingdom.smp.moon.ModMoonDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The moon has no skylight, and 1.26 ignores the old {@code ambient_light} dimension
 * key (the lightmap now uses an {@code ambientColor} from the new attribute system),
 * so the surface renders pitch dark. Force a bright ambient floor on the moon so you
 * can see the whole place, while the sky stays black + starry (handled separately).
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMoonBrightnessMixin {

    private static final Vector3f IRONHOLD_MOON_AMBIENT = new Vector3f(0.85f, 0.85f, 0.9f);

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("TAIL"))
    private void ironhold$brightenMoon(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().equals(ModMoonDimensions.MOON_LEVEL)) {
            state.ambientColor = IRONHOLD_MOON_AMBIENT;
        }
    }
}
