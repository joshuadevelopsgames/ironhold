package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.PossessedArmorEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Renders the Possessed Armor mob. The entity is always invisible, so the
 * body model is skipped, but the armor layer and item-in-hand layer still
 * render — creating a floating suit of netherite armor holding a sword.
 */
public class PossessedArmorRenderer extends HumanoidMobRenderer<PossessedArmorEntity, ZombieRenderState, ZombieModel<ZombieRenderState>> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/possessed_armor.png");

    public PossessedArmorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new ZombieModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)),
            0.5F);
        // Add the armor layer — HumanoidMobRenderer does NOT add it by default.
        // ZombieRenderer/AbstractZombieRenderer normally handles this.
        ArmorModelSet<ZombieModel<ZombieRenderState>> armorModels =
            ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, ctx.getModelSet(), ZombieModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armorModels, ctx.getEquipmentRenderer()));
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(PossessedArmorEntity entity, HumanoidArm arm) {
        return HumanoidModel.ArmPose.ITEM;
    }

    private float smoothedSpeed;

    @Override
    public void extractRenderState(PossessedArmorEntity entity, ZombieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Only count horizontal movement (ignore vertical jitter from collisions)
        float raw = state.walkAnimationSpeed;
        // Smooth toward target to avoid sudden spikes
        smoothedSpeed += (Math.min(raw, 0.8F) - smoothedSpeed) * 0.1F;
        state.walkAnimationPos = 0;
        state.walkAnimationSpeed = 0;
    }

    @Override
    protected void scale(ZombieRenderState state, PoseStack poseStack) {
        float moveBoost = Math.min(smoothedSpeed * 0.55F, 0.25F);
        float amplitude = 0.12F + moveBoost;
        float bob = (float) Math.sin(state.ageInTicks * 0.07) * amplitude;
        poseStack.translate(0.0F, bob, 0.0F);
    }
}
