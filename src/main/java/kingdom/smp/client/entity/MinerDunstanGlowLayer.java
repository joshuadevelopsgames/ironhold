package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Full-bright pass for the lamp mounted on Dunstan's mining helmet. */
public class MinerDunstanGlowLayer extends EyesLayer<MinerDunstanRenderState, MinerDunstanModel> {

    private static final RenderType GLOW = RenderTypes.eyes(
        Identifier.fromNamespaceAndPath(
            Ironhold.MODID, "textures/entity/miner_dunstan_glow.png"));

    public MinerDunstanGlowLayer(
        RenderLayerParent<MinerDunstanRenderState, MinerDunstanModel> renderer
    ) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
