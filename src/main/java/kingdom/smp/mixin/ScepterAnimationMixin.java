package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adjusts the vanilla spear charge animation for the Arcane Scepter:
 * adds a gentle tilt-back on top of vanilla's positioning.
 */
@Mixin(SpearAnimations.class)
public abstract class ScepterAnimationMixin {

    @Inject(method = "firstPersonUse", at = @At("TAIL"))
    private static void ironhold$wizardChargeTilt(float hitFeedback, PoseStack poseStack,
                                                   float chargeProgress, HumanoidArm arm,
                                                   ItemStack stack, CallbackInfo ci) {
        if (!stack.is(Ironhold.ARCANE_SCEPTER.get()) && !stack.is(Ironhold.SOLUNA_STAFF.get())) return;

        float ticksCharged = 72000.0F - chargeProgress;
        float charge = Math.min(ticksCharged / 15.0F, 1.0F);

        // Gentle tilt back on top of vanilla's spear pose
        poseStack.mulPose(Axis.XP.rotationDegrees(charge * 12.0F));
    }
}
