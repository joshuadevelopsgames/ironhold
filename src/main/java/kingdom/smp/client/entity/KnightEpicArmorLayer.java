package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Renders Epic Knights armor textures (64x32 armor UV format) on top of
 * the knight's PlayerModel body. Uses PLAYER_ARMOR model set so the armor
 * geometry exactly matches the entity body proportions.
 *
 * layer1Texture covers: head (y=0-15) + body/arms/boots (y=16-31)
 * layer2Texture covers: legs (y=16-31)
 */
public class KnightEpicArmorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final ArmorModelSet<PlayerModel> armorModels;
    private final Identifier layer1Texture;
    private final Identifier layer2Texture;

    public KnightEpicArmorLayer(
        RenderLayerParent<AvatarRenderState, PlayerModel> parent,
        net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx,
        Identifier layer1Texture,
        Identifier layer2Texture
    ) {
        super(parent);
        this.armorModels = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(), p -> new PlayerModel(p, false));
        this.layer1Texture = layer1Texture;
        this.layer2Texture = layer2Texture;
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int packedLight,
        AvatarRenderState state,
        float yRot,
        float xRot
    ) {
        if (state.isInvisible) {
            return;
        }

        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

        // Sync all armor models to match the entity model's current animation pose.
        // storePose() captures xRot/yRot/zRot/x/y/z; loadPose() applies them.
        PlayerModel entityModel = this.getParentModel();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            PlayerModel armorPiece = armorModels.get(slot);
            armorPiece.head.loadPose(entityModel.head.storePose());
            armorPiece.hat.loadPose(entityModel.hat.storePose());
            armorPiece.body.loadPose(entityModel.body.storePose());
            armorPiece.rightArm.loadPose(entityModel.rightArm.storePose());
            armorPiece.leftArm.loadPose(entityModel.leftArm.storePose());
            armorPiece.rightLeg.loadPose(entityModel.rightLeg.storePose());
            armorPiece.leftLeg.loadPose(entityModel.leftLeg.storePose());
        }

        renderPiece(armorModels.get(EquipmentSlot.HEAD),  EquipmentSlot.HEAD,  layer1Texture, poseStack, submitNodeCollector, packedLight, state, overlay);
        renderPiece(armorModels.get(EquipmentSlot.CHEST), EquipmentSlot.CHEST, layer1Texture, poseStack, submitNodeCollector, packedLight, state, overlay);
        renderPiece(armorModels.get(EquipmentSlot.LEGS),  EquipmentSlot.LEGS,  layer2Texture, poseStack, submitNodeCollector, packedLight, state, overlay);
        renderPiece(armorModels.get(EquipmentSlot.FEET),  EquipmentSlot.FEET,  layer1Texture, poseStack, submitNodeCollector, packedLight, state, overlay);
    }

    private void renderPiece(
        PlayerModel model,
        EquipmentSlot slot,
        Identifier texture,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int packedLight,
        AvatarRenderState state,
        int overlay
    ) {
        setPartVisibility(model, slot);
        // -1 = 0xFFFFFFFF = fully opaque white tint (no color modification)
        renderColoredCutoutModel(model, texture, poseStack, submitNodeCollector, packedLight, state, overlay, -1);
    }

    private static void setPartVisibility(PlayerModel model, EquipmentSlot slot) {
        // Hide everything first
        model.head.visible      = false;
        model.hat.visible       = false;
        model.body.visible      = false;
        model.rightArm.visible  = false;
        model.leftArm.visible   = false;
        model.rightLeg.visible  = false;
        model.leftLeg.visible   = false;
        model.jacket.visible    = false;
        model.leftSleeve.visible  = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible   = false;
        model.rightPants.visible  = false;

        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                // hat is the outer player skin layer — armor textures don't use it
            }
            case CHEST -> {
                model.body.visible     = true;
                model.rightArm.visible = true;
                model.leftArm.visible  = true;
            }
            case LEGS -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible  = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible  = true;
            }
            default -> { /* no-op */ }
        }
    }
}
