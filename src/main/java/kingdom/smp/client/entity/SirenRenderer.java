package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.SirenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SirenRenderer extends MobRenderer<SirenEntity, SirenRenderState, SirenModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/siren.png");

    public SirenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SirenModel(ctx.bakeLayer(SirenModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(SirenRenderState state) {
        return TEXTURE;
    }

    @Override
    public SirenRenderState createRenderState() {
        return new SirenRenderState();
    }

    @Override
    public void extractRenderState(SirenEntity entity, SirenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isAggressive = entity.getTarget() != null || entity.isSinging();
    }
}
