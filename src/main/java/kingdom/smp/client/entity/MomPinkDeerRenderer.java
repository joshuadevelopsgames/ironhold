package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.MomPinkDeerEntity;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Renders the Mom Pink Deer using a dedicated model (from mom_pink_deer.bbmodel)
 * with its own texture mapping.
 */
public class MomPinkDeerRenderer extends AgeableMobRenderer<MomPinkDeerEntity, DeerRenderState, MomPinkDeerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/pink_deer_mom.png");

    public MomPinkDeerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new MomPinkDeerModel(ctx.bakeLayer(MomPinkDeerModel.LAYER_LOCATION)),
            new MomPinkDeerModel(ctx.bakeLayer(MomPinkDeerModel.BABY_LAYER)),
            0.65F); // larger shadow
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
    public void extractRenderState(MomPinkDeerEntity entity, DeerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isGrazing = entity.isGrazing();
    }
}
