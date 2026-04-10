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
 * Renderer for the Purple Allay — uses the vanilla Allay model with a purple texture
 * and emissive yellow eyes that glow in the dark.
 */
public class PurpleAllayRenderer extends AllayRenderer {

    private static final Identifier PURPLE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/purple_allay.png");

    private static final Identifier EYES_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/purple_allay_eyes.png");

    public PurpleAllayRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        addLayer(new EyesLayer<AllayRenderState, AllayModel>(this) {
            @Override
            public RenderType renderType() {
                return RenderTypes.eyes(EYES_TEXTURE);
            }
        });
    }

    @Override
    public Identifier getTextureLocation(AllayRenderState state) {
        return PURPLE_TEXTURE;
    }
}
