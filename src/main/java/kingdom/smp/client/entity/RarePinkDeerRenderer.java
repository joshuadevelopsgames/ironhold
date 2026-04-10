package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.RarePinkDeerEntity;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Renders the Rare Pink Deer using the same model as the normal Pink Deer
 * but with the simpler, ethereal pale-pink texture.
 */
public class RarePinkDeerRenderer extends AgeableMobRenderer<RarePinkDeerEntity, DeerRenderState, PinkDeerModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/pink_deer_rare.png");

    public RarePinkDeerRenderer(EntityRendererProvider.Context ctx) {
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
    public void extractRenderState(RarePinkDeerEntity entity, DeerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isGrazing = entity.isGrazing();
    }
}
