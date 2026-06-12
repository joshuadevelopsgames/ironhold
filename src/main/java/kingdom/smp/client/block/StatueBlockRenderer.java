package kingdom.smp.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import kingdom.smp.Ironhold;
import kingdom.smp.ModBlocks;
import kingdom.smp.block.StatueBlock;
import kingdom.smp.block.StatueBlockEntity;
import kingdom.smp.client.entity.StatueBaseLayer;
import kingdom.smp.client.entity.StatueRenderState;
import kingdom.smp.client.entity.StoneStatueRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders a {@link StatueBlockEntity} exactly like the old statue entity: the
 * vanilla player-shaped {@link HumanoidModel} frozen in its rest pose, standing
 * on the two-tier stone plinth from {@link StatueBaseLayer}, with the variant's
 * stonified skin. The pose-stack chain mirrors what LivingEntityRenderer +
 * StoneStatueRenderer applied (pedestal lift, 180-yaw, the (-1,-1,1) model-space
 * flip, and the 1.501 dance) so the block version is pixel-identical to the
 * entity it replaces.
 */
public class StatueBlockRenderer
    implements BlockEntityRenderer<StatueBlockEntity, StatueBlockRenderer.RenderState> {

    private static final RenderType BASE_RENDER_TYPE = RenderTypes.entityCutout(
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/statue/base.png"));

    /** Block → stonified skin. Built lazily: blocks aren't registered when the
     *  class loads, but are by the time the first statue is rendered. */
    private static Map<Block, Identifier> textures;

    private final HumanoidModel<StatueRenderState> figure;
    private final ModelPart base;

    public StatueBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        this.figure = new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER));
        // Pose the rig once with a zeroed state — the same frozen pose the
        // statue entity rendered with (StoneStatueRenderer zeroed its state).
        this.figure.setupAnim(new StatueRenderState());
        this.base = ctx.bakeLayer(StatueBaseLayer.LAYER_LOCATION).getChild("base");
    }

    private static Identifier textureFor(Block block) {
        if (textures == null) {
            textures = Map.of(
                ModBlocks.KANGARUDE_STATUE_BLOCK.get(),  StoneStatueRenderer.KANGARUDE_TEXTURE,
                ModBlocks.HAALINA_STATUE_BLOCK.get(),    StoneStatueRenderer.HAALINA_TEXTURE,
                ModBlocks.FACELACES_STATUE_BLOCK.get(),  StoneStatueRenderer.FACELACES_TEXTURE,
                ModBlocks.RED_RAICHU_STATUE_BLOCK.get(), StoneStatueRenderer.RED_RAICHU_TEXTURE,
                ModBlocks.TWOHRD_STATUE_BLOCK.get(),     StoneStatueRenderer.TWOHRD_TEXTURE,
                ModBlocks.ARCATHEONE_STATUE_BLOCK.get(), StoneStatueRenderer.ARCATHEONE_TEXTURE,
                ModBlocks.CHEAKIE_STATUE_BLOCK.get(),    StoneStatueRenderer.CHEAKIE_TEXTURE);
        }
        return textures.getOrDefault(block, StoneStatueRenderer.KANGARUDE_TEXTURE);
    }

    public static final class RenderState extends BlockEntityRenderState {
        Identifier texture = StoneStatueRenderer.KANGARUDE_TEXTURE;
        float yawDegrees;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        StatueBlockEntity be, RenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        var blockState = be.getBlockState();
        state.texture = textureFor(blockState.getBlock());
        state.yawDegrees = blockState.getValue(StatueBlock.FACING).toYRot();
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        // Block centre + the 8px pedestal lift StoneStatueRenderer applied.
        pose.translate(0.5F, StoneStatueRenderer.PEDESTAL_HEIGHT / 16.0F, 0.5F);
        // The vanilla living-entity model-space transform (yaw, flip, 1.501).
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - state.yawDegrees));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);

        RenderType figureType = RenderTypes.entityCutout(state.texture);
        for (ModelPart part : new ModelPart[] {
            figure.head, figure.hat, figure.body,
            figure.rightArm, figure.leftArm, figure.rightLeg, figure.leftLeg
        }) {
            collector.submitModelPart(part, pose, figureType, state.lightCoords,
                OverlayTexture.NO_OVERLAY, null, -1, null);
        }
        collector.submitModelPart(base, pose, BASE_RENDER_TYPE, state.lightCoords,
            OverlayTexture.NO_OVERLAY, null, -1, null);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StatueBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                        pos.getX() + 1.5, pos.getY() + 3.0, pos.getZ() + 1.5);
    }
}
