package kingdom.smp.client.entity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

import kingdom.smp.entity.TempestArrowEntity;

public class TempestArrowRenderer extends ArrowRenderer<TempestArrowEntity, ArrowRenderState> {
    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");

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
