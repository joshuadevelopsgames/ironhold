package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MimicEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the {@link MimicEntity} using the vanilla chest texture so it's
 * indistinguishable from a real chest until it wakes up.
 */
public class MimicRenderer extends MobRenderer<MimicEntity, MimicRenderState, MimicModel> {

    /** Custom mimic texture — looks like a vanilla chest but has white teeth + pink tongue. */
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/mimic.png");

    public MimicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MimicModel(ctx.bakeLayer(MimicModel.LAYER_LOCATION)), 0.0F);
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
    public void extractRenderState(MimicEntity entity, MimicRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.awakened = entity.isAwakened();
        state.awakeTicks = entity.getAwakeTicks();

        // When dormant, suppress hurt flash so it looks like a real chest
        if (!state.awakened) {
            state.hasRedOverlay = false;
        }
    }

    /**
     * LivingEntityRenderer.submit() applies scale(-1,-1,1) then translate(0,-1.501,0)
     * to flip entity models (which are authored upside-down). The chest model is
     * right-side-up, so we counter-flip here. This method is called right after
     * that scale(-1,-1,1).
     */
    @Override
    protected void scale(MimicRenderState state, PoseStack poseStack) {
        // Undo the -1,-1,1 scale that LivingEntityRenderer applies
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Undo the -1.501 translate and position the chest on the ground instead.
        poseStack.translate(0.0F, 1.501F, 0.0F);
        // Rotate 180° so the chest's front (lock side) faces the entity's walk direction.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Growth on awaken: scale up from 1.0 to 1.15 during the wake animation
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

        // Center the chest geometry on the entity position.
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    @Override
    public void submit(
        MimicRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState cameraState
    ) {
        // No extra transforms needed — scale() handles everything.
        super.submit(state, poseStack, collector, cameraState);
    }
}
