package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;

public class BlackShulkerRenderer extends ShulkerRenderer {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/black_shulker.png");

    public BlackShulkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(ShulkerRenderState state) {
        return TEXTURE;
    }
}
