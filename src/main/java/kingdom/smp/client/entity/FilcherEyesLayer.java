package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive glowing-yellow eyes for the Filcher. Rendered additively at full brightness via
 * {@link RenderTypes#eyes}, exactly like vanilla's spider/enderman eye layers, so the eyes
 * glow in the dark over the filcher's dark face texture.
 */
public class FilcherEyesLayer extends EyesLayer<FilcherRenderState, ZombieModel<FilcherRenderState>> {

    private static final RenderType EYES =
        RenderTypes.eyes(Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/filcher_eyes.png"));

    public FilcherEyesLayer(RenderLayerParent<FilcherRenderState, ZombieModel<FilcherRenderState>> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
