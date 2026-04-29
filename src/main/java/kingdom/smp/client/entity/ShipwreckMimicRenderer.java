package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ShipwreckMimicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Renders the Shipwreck Mimic using the same chest model as the regular Mimic
 * but with a waterlogged/barnacle texture.
 */
public class ShipwreckMimicRenderer extends MobRenderer<ShipwreckMimicEntity, MimicRenderState, ShipwreckMimicModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shipwreck_mimic.png");

    public ShipwreckMimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShipwreckMimicModel(ctx.bakeLayer(ShipwreckMimicModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public Identifier getTextureLocation(MimicRenderState state) {
        return TEXTURE;
    }

    @Override
    public MimicRenderState createRenderState() {
        return new MimicRenderState();
    }

    @Override
    public void extractRenderState(ShipwreckMimicEntity entity, MimicRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.awakened = entity.isAwakened();
        state.awakeTicks = entity.getAwakeTicks();
        if (!state.awakened) {
            state.hasRedOverlay = false;
        }
    }

    @Override
    protected void scale(MimicRenderState state, PoseStack poseStack) {
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        if (state.awakened) {
            float growth;
            if (state.awakeTicks <= 15) {
                float progress = (float) state.awakeTicks / 15.0F;
                growth = 1.0F + 0.15F * progress;
            } else {
                growth = 1.15F;
            }
            poseStack.scale(growth, growth, growth);
        }

        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }
}
