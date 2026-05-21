package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.SlimePetEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders a {@link SlimePetEntity} as a small player head wearing a baked-in skin texture
 * (Je11ie or Cheakie, picked by variant). Scaling math mirrors {@link BabyMimicRenderer}:
 * shrink, sit on the ground, and centre the cube over the entity origin.
 */
public class SlimePetRenderer extends MobRenderer<SlimePetEntity, SlimePetRenderState, SlimePetModel> {

    private static final float SCALE = 0.54F;

    private static final Identifier[] TEXTURES = {
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/slime_pet_je11ie.png"),
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/slime_pet_cheakie.png"),
    };

    public SlimePetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SlimePetModel(ctx.bakeLayer(SlimePetModel.LAYER_LOCATION)), 0.1F);
    }

    @Override
    public SlimePetRenderState createRenderState() {
        return new SlimePetRenderState();
    }

    @Override
    public void extractRenderState(SlimePetEntity entity, SlimePetRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinTexture = TEXTURES[entity.variant() % TEXTURES.length];
    }

    @Override
    public Identifier getTextureLocation(SlimePetRenderState state) {
        return state.skinTexture;
    }

    @Override
    protected void scale(SlimePetRenderState state, PoseStack poseStack) {
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, 1.501F * SCALE, 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }
}
