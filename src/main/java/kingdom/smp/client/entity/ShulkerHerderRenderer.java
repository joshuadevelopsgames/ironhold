package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ShulkerHerderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ShulkerHerderRenderer
    extends MobRenderer<ShulkerHerderEntity, ShulkerHerderRenderState, ShulkerHerderModel> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/shulker_herder.png");

    public ShulkerHerderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShulkerHerderModel(ctx.bakeLayer(ShulkerHerderModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(ShulkerHerderRenderState state) {
        return TEXTURE;
    }

    @Override
    public ShulkerHerderRenderState createRenderState() {
        return new ShulkerHerderRenderState();
    }

    @Override
    public void extractRenderState(ShulkerHerderEntity entity, ShulkerHerderRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isAggressive = entity.getTarget() != null;
    }
}
