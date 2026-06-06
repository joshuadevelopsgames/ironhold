package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;

public class VampireBatRenderer extends BatRenderer {
    private static final Identifier VAMPIRE_BAT_LOCATION = Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/vampire_bat.png");

    public VampireBatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(BatRenderState state) {
        return VAMPIRE_BAT_LOCATION;
    }
}
