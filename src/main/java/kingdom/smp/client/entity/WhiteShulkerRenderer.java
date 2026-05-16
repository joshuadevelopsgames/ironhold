package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;

public class WhiteShulkerRenderer extends ShulkerRenderer {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/white_shulker.png");

    public WhiteShulkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(ShulkerRenderState state) {
        return TEXTURE;
    }
}
