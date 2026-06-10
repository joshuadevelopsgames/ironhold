package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a two-tier stone-brick plinth under a {@link StoneStatueEntity}. The
 * plinth is modelled in the humanoid frame where the feet sit at model-y 24px
 * and grows down to 32px (half a block); {@link StoneStatueRenderer} lifts the
 * figure 8px so the plinth rests on the ground and the statue stands on top.
 */
public class StatueBaseLayer extends RenderLayer<StatueRenderState, HumanoidModel<StatueRenderState>> {

    public static final ModelLayerLocation LAYER_LOCATION =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ironhold.MODID, "statue_base"), "main");

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/base.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    private final ModelPart base;

    public StatueBaseLayer(RenderLayerParent<StatueRenderState, HumanoidModel<StatueRenderState>> parent, ModelPart root) {
        super(parent);
        this.base = root.getChild("base");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, 28.0F, -8.0F, 16.0F, 4.0F, 16.0F)   // wide bottom step (on ground)
                .texOffs(0, 0).addBox(-6.0F, 24.0F, -6.0F, 12.0F, 4.0F, 12.0F),  // narrow top step (statue stands here)
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       StatueRenderState state, float yRot, float xRot) {
        collector.submitModelPart(this.base, pose, RENDER_TYPE, packedLight,
            OverlayTexture.NO_OVERLAY, null, -1, null);
    }
}
