package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.ArcaneMageEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllusionerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Same illager model/UVs and texture as {@link ArcaneInvokerRenderer} ({@code evil_evoker.png}). */
public class ArcaneMageRenderer extends IllagerRenderer<ArcaneMageEntity, IllusionerRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/evil_evoker.png");

    public ArcaneMageRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new IllagerModel<>(ctx.bakeLayer(ModelLayers.ILLUSIONER)), 0.5F);
        this.addLayer(
            new ItemInHandLayer<IllusionerRenderState, IllagerModel<IllusionerRenderState>>(this) {
                @Override
                public void submit(
                    PoseStack poseStack,
                    SubmitNodeCollector submitNodeCollector,
                    int packedLight,
                    IllusionerRenderState state,
                    float yRot,
                    float xRot
                ) {
                    if (state.isCastingSpell || state.isAggressive) {
                        super.submit(poseStack, submitNodeCollector, packedLight, state, yRot, xRot);
                    }
                }
            });
        this.model.getHat().visible = true;
    }

    @Override
    public Identifier getTextureLocation(IllusionerRenderState state) {
        return TEXTURE;
    }

    @Override
    public IllusionerRenderState createRenderState() {
        return new IllusionerRenderState();
    }

    @Override
    public void extractRenderState(ArcaneMageEntity entity, IllusionerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Vec3[] offsets = entity.getIllusionOffsets(partialTick);
        state.illusionOffsets = Arrays.copyOf(offsets, offsets.length);
        state.isCastingSpell = entity.isCasting();
    }

    @Override
    public void submit(
        IllusionerRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState
    ) {
        if (state.isInvisible) {
            Vec3[] offsets = state.illusionOffsets;
            for (int i = 0; i < offsets.length; i++) {
                poseStack.pushPose();
                poseStack.translate(
                    offsets[i].x + Mth.cos(i + state.ageInTicks * 0.5F) * 0.025F,
                    offsets[i].y + Mth.cos(i + state.ageInTicks * 0.75F) * 0.0125F,
                    offsets[i].z + Mth.cos(i + state.ageInTicks * 0.7F) * 0.025F);
                super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
                poseStack.popPose();
            }
        } else {
            super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
        }
    }

    @Override
    protected boolean isBodyVisible(IllusionerRenderState state) {
        return true;
    }

    @Override
    protected AABB getBoundingBoxForCulling(ArcaneMageEntity entity) {
        return super.getBoundingBoxForCulling(entity).inflate(3.0, 0.0, 3.0);
    }
}
