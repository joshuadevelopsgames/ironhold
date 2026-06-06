package kingdom.smp.client.entity;

import kingdom.smp.ModItems;
import kingdom.smp.item.BattleHammerItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Custom third-person arm pose for the Battle Hammer's charge wind-up. While the player
 * holds right-click to charge, the using arm cocks up/back (scaling with charge) so the
 * raised arm matches the hammer body's wind-up tilt in {@link BattleHammerRenderer}. On
 * release the item is no longer "in use" so this pose drops out and the vanilla attack
 * swing (triggered by {@code player.swing(...)} in {@link BattleHammerItem#releaseUsing})
 * carries the arm back down for the slam.
 *
 * <p>The pose is a NeoForge enum extension of {@link HumanoidModel.ArmPose}: the
 * {@link #CHARGE_POSE} EnumProxy is referenced from {@code META-INF/enumextensions.json},
 * which supplies the private {@code ArmPose(boolean twoHanded, boolean affectsOffhandPose,
 * IArmPoseTransformer)} constructor args. We return its value from
 * {@link IClientItemExtensions#getArmPose} while the hammer is being used.</p>
 */
public final class BattleHammerArmPose {

    private BattleHammerArmPose() {}

    /** Referenced by META-INF/enumextensions.json (field "CHARGE_POSE"). */
    public static final EnumProxy<HumanoidModel.ArmPose> CHARGE_POSE = new EnumProxy<>(
        HumanoidModel.ArmPose.class,
        false,                                                  // twoHanded
        false,                                                  // affectsOffhandPose
        (IArmPoseTransformer) BattleHammerArmPose::applyTransform);

    private static void applyTransform(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float frac = Math.min(state.ticksUsingItem / (float) BattleHammerItem.FULL_CHARGE_TICKS, 1f);
        ModelPart limb = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        // Raise the arm overhead as charge builds (negative xRot swings the arm up/back),
        // with a slight inward yaw so the hammer reads as cocked behind the shoulder.
        limb.xRot = -0.35f - 1.45f * frac;
        limb.yRot = (arm == HumanoidArm.RIGHT ? -0.1f : 0.1f) * frac;
        limb.zRot = 0f;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(BattleHammerArmPose::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (entity.isUsingItem()
                        && entity.getUseItem() == stack
                        && stack.getItem() instanceof BattleHammerItem) {
                    return CHARGE_POSE.getValue();
                }
                return null;
            }
        }, ModItems.BATTLE_HAMMER.get());
    }
}
