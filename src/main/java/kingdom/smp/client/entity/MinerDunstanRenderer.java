package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.MinerDunstanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for Foreman Dunstan's dedicated lamp-helmet miner model.
 */
public class MinerDunstanRenderer
    extends MobRenderer<MinerDunstanEntity, MinerDunstanRenderState, MinerDunstanModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        Ironhold.MODID, "textures/entity/miner_dunstan.png");

    public MinerDunstanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MinerDunstanModel(ctx.bakeLayer(MinerDunstanModel.LAYER_LOCATION)), 0.55F);
        this.addLayer(new MinerDunstanGlowLayer(this));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(MinerDunstanRenderState state) {
        return TEXTURE;
    }

    @Override
    public MinerDunstanRenderState createRenderState() {
        return new MinerDunstanRenderState();
    }

    @Override
    public void extractRenderState(
        MinerDunstanEntity entity,
        MinerDunstanRenderState state,
        float partialTick
    ) {
        super.extractRenderState(entity, state, partialTick);
        ArmedEntityRenderState.extractArmedEntityRenderState(
            entity, state, this.itemModelResolver, partialTick);
    }
}
