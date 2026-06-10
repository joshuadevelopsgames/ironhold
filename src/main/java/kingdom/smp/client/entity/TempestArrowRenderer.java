package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.TempestArrowEntity;

public class TempestArrowRenderer extends ArrowRenderer<TempestArrowEntity, ArrowRenderState> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/projectiles/tempest_arrow.png");

    public TempestArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
