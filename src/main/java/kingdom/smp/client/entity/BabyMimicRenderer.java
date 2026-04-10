package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.BabyMimicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders the baby mimic at quarter scale using the mimic texture (has tongue UV).
 */
public class BabyMimicRenderer extends MobRenderer<BabyMimicEntity, BabyMimicRenderState, BabyMimicModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/mimic.png");

    public BabyMimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BabyMimicModel(ctx.bakeLayer(BabyMimicModel.LAYER_LOCATION)), 0.15F);
    }

    @Override
    public Identifier getTextureLocation(BabyMimicRenderState state) {
        return TEXTURE;
    }

    @Override
    public BabyMimicRenderState createRenderState() {
        return new BabyMimicRenderState();
    }

    @Override
    protected void scale(BabyMimicRenderState state, PoseStack poseStack) {
        float s = 0.25F;
        // Undo the -1,-1,1 flip from LivingEntityRenderer
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // After scale(), LivingEntityRenderer applies translate(0, -1.501, 0).
        // That translate happens in post-scale space (quarter scale), so it moves
        // -1.501 * 0.25 = -0.375 in world units. We need to add +0.375 here (pre-scale)
        // to cancel it out, so the model sits at Y=0 (ground level).
        poseStack.translate(0.0F, 1.501F * s, 0.0F);
        // Face the correct direction
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        // Quarter scale
        poseStack.scale(s, s, s);
        // Center the chest geometry
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }
}
