package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ShroomlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ShroomlingRenderer extends MobRenderer<ShroomlingEntity, ShroomlingRenderState, ShroomlingModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shroomling/shroomling.png");
    private static final Identifier TEXTURE_ORANGE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shroomling/shroomling_orange.png");

    public ShroomlingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShroomlingModel(ctx.bakeLayer(ShroomlingModel.LAYER_LOCATION)), 0.35F);
        this.addLayer(new ShroomlingGlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(ShroomlingRenderState state) {
        return state.orange ? TEXTURE_ORANGE : TEXTURE;
    }

    @Override
    public ShroomlingRenderState createRenderState() {
        return new ShroomlingRenderState();
    }

    @Override
    public void extractRenderState(ShroomlingEntity entity, ShroomlingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.hopBob = entity.onGround() ? 0.0F : 0.12F;
        state.orange = entity.isOrange();
        state.capless = entity.isCapless();
    }
}
