package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.Ironhold;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive glow for the rare orange Shroomling — lights up the cap's pale spots at
 * full brightness via {@link RenderTypes#eyes}, exactly like vanilla's spider/enderman
 * eye layers. Gated on the variant flag so only the orange Shroomling glows; the blue
 * one renders without it.
 */
public class ShroomlingGlowLayer extends EyesLayer<ShroomlingRenderState, ShroomlingModel> {

    private static final RenderType GLOW =
        RenderTypes.eyes(Identifier.fromNamespaceAndPath(Ironhold.MODID,
            "textures/entity/shroomling/shroomling_orange_glow.png"));

    public ShroomlingGlowLayer(RenderLayerParent<ShroomlingRenderState, ShroomlingModel> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       ShroomlingRenderState state, float yRot, float xRot) {
        // No cap → no glow spots to light up.
        if (!state.orange || state.capless) return;
        super.submit(pose, collector, packedLight, state, yRot, xRot);
    }
}
