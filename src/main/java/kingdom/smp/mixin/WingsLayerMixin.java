package kingdom.smp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.ModItems;
import kingdom.smp.client.VanityAccessoryRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the elytra / wings equipment visual while a player is wearing the angel-wings
 * vanity accessory, so the two don't overlap on the back. Only the render is suppressed;
 * gliding is server-side physics and is unaffected. Non-player entities never carry
 * {@link VanityAccessoryRenderState}, so their wings render normally.
 */
@Mixin(WingsLayer.class)
public class WingsLayerMixin {

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
        at = @At("HEAD"),
        cancellable = true)
    private void ironhold$hideWingsForAngelWings(PoseStack pose, SubmitNodeCollector collector, int light,
                                                 HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (state instanceof VanityAccessoryRenderState acc
                && acc.ironhold$chestAccessory().is(ModItems.ANGEL_WINGS.get())) {
            ci.cancel();
        }
    }
}
