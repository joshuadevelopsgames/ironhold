package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MiniDragonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
/**
 * Renders the Mini Dragon using the vanilla EnderDragonModel, scaled down
 * to fit under 1 block, with a custom blue texture.
 */
public class MiniDragonRenderer extends EntityRenderer<MiniDragonEntity, EnderDragonRenderState> {

    private static final Identifier BLUE_DRAGON_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/mini_dragon.png");
    private static final Identifier BLUE_DRAGON_EYES =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/mini_dragon_eyes.png");

    /** Scale factor — vanilla dragon is ~16 blocks wide; we want ~1 block. */
    private static final float SCALE = 0.0675F;

    private final EnderDragonModel model;

    public MiniDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.22F;
        this.model = new EnderDragonModel(ctx.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    @Override
    public EnderDragonRenderState createRenderState() {
        return new EnderDragonRenderState();
    }

    @Override
    public void extractRenderState(MiniDragonEntity entity, EnderDragonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        boolean perching = entity.isPerching();
        // Slower, gentler wing flaps when perching
        float flapSpeed = perching ? 40.0F : 20.0F;
        state.flapTime = (entity.tickCount + partialTick) / flapSpeed;
        state.deathTime = 0;
        state.hasRedOverlay = false;
        state.beamOffset = null;
        state.isLandingOrTakingOff = false;
        state.isSitting = perching;
        state.distanceToEgg = 0;
        state.partialTicks = partialTick;
        state.flightHistory.copyFrom(entity.getFlightHistory());
    }

    @Override
    public void submit(EnderDragonRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        // No shadow while perching on shoulder
        this.shadowRadius = state.isSitting ? 0.0F : 0.22F;

        // Scale down to pet size
        poseStack.scale(SCALE, SCALE, SCALE);

        // Same pose setup as vanilla EnderDragonRenderer
        float yr = state.getHistoricalPos(7).yRot();
        float rot2 = (float) (state.getHistoricalPos(5).y() - state.getHistoricalPos(10).y());
        // +180 because the dragon model's head faces -Z but MC yRot=0 means +Z
        poseStack.mulPose(Axis.YP.rotationDegrees(-yr + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(rot2 * 10.0F));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        // Render the dragon body with blue texture
        collector.submitModel(
            this.model, state, poseStack,
            BLUE_DRAGON_TEXTURE,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor, null);

        // Glowing eyes layer
        collector.submitModel(
            this.model, state, poseStack,
            RenderTypes.eyes(BLUE_DRAGON_EYES),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor, null);

        poseStack.popPose();
    }
}
