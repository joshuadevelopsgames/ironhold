package kingdom.smp.client.entity;

import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MagicMinecartEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive glow for the Magic Minecart — the cyan diamond runes, the interior
 * energy pool, the magenta body sparks, and the purple wheel hubs. Subclasses
 * GeckoLib's {@link AutoGlowingGeoLayer} so the glow re-renders aligned with the
 * model, swapping to the additive {@code eyes} blend so it reads as emitted light
 * in the dark.
 */
public class MagicMinecartGlowLayer
        extends AutoGlowingGeoLayer<MagicMinecartEntity, Void, EntityRenderState> {

    private static final Identifier GLOW =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/magic_minecart_glow.png");

    public MagicMinecartGlowLayer(GeoRenderer<MagicMinecartEntity, Void, EntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    protected Identifier getTextureResource(EntityRenderState state) {
        return GLOW;
    }

    @Override
    protected RenderType getRenderType(EntityRenderState state) {
        return RenderTypes.eyes(GLOW);
    }
}
