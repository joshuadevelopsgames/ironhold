package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.WillOWisp2Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class WillOWisp2Renderer extends MobRenderer<WillOWisp2Entity, WillOWisp2RenderState, WillOWisp2Model> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/will_o_wisp_2.png");

    private static final Identifier GLOW =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/will_o_wisp_2_glow.png");

    public WillOWisp2Renderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WillOWisp2Model(ctx.bakeLayer(WillOWisp2Model.LAYER_LOCATION)), 0.2F);
        addLayer(new EyesLayer<WillOWisp2RenderState, WillOWisp2Model>(this) {
            @Override
            public RenderType renderType() {
                return RenderTypes.eyes(GLOW);
            }
        });
    }

    @Override
    public Identifier getTextureLocation(WillOWisp2RenderState state) {
        return TEXTURE;
    }

    @Override
    public WillOWisp2RenderState createRenderState() {
        return new WillOWisp2RenderState();
    }
}
