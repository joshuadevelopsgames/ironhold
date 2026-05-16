package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.HoplingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class HoplingRenderer extends MobRenderer<HoplingEntity, HoplingRenderState, HoplingModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/hopling/hopling.png");

    public HoplingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HoplingModel(ctx.bakeLayer(HoplingModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public Identifier getTextureLocation(HoplingRenderState state) {
        return TEXTURE;
    }

    @Override
    public HoplingRenderState createRenderState() {
        return new HoplingRenderState();
    }

    @Override
    public void extractRenderState(HoplingEntity entity, HoplingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.teleportTicks = entity.getTeleportTicks();
        state.hopBob = entity.onGround() ? 0.0F : 0.15F;
    }
}
