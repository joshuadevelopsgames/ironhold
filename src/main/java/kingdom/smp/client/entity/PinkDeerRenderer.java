package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.PinkDeerEntity;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Renders the Pink Deer with a custom deer model and pink texture.
 */
public class PinkDeerRenderer extends AgeableMobRenderer<PinkDeerEntity, DeerRenderState, PinkDeerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/pink_deer.png");

    public PinkDeerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new PinkDeerModel(ctx.bakeLayer(PinkDeerModel.LAYER_LOCATION)),
            new PinkDeerModel(ctx.bakeLayer(PinkDeerModel.BABY_LAYER)),
            0.4F);
    }

    @Override
    public Identifier getTextureLocation(DeerRenderState state) {
        return TEXTURE;
    }

    @Override
    public DeerRenderState createRenderState() {
        return new DeerRenderState();
    }

    @Override
    public void extractRenderState(PinkDeerEntity entity, DeerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isGrazing = entity.isGrazing();
    }
}
