package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the Will-o'-the-Wisp — vanilla allay silhouette with a glowing
 * pale-blue texture and a fully-emissive overlay so it shines in the dark.
 */
public class WillOWispRenderer extends AllayRenderer {

    private static final Identifier WISP_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/will_o_wisp.png");

    private static final Identifier WISP_GLOW =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/will_o_wisp_glow.png");

    public WillOWispRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new EyesLayer<AllayRenderState, AllayModel>(this) {
            @Override
            public RenderType renderType() {
                return RenderTypes.eyes(WISP_GLOW);
            }
        });
    }

    @Override
    public Identifier getTextureLocation(AllayRenderState state) {
        return WISP_TEXTURE;
    }
}
