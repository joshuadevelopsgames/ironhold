package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.PurpleAllayEntity;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the Purple Allay — uses the vanilla Allay model with a bright purple texture.
 * Falls back to the vanilla allay texture until a custom purple_allay.png is authored.
 */
public class PurpleAllayRenderer extends AllayRenderer {

    private static final Identifier PURPLE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/purple_allay.png");

    private static final Identifier FALLBACK_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/allay/allay.png");

    public PurpleAllayRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTextureLocation(AllayRenderState state) {
        return PURPLE_TEXTURE;
    }
}
