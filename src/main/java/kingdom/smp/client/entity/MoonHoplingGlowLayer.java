package kingdom.smp.client.entity;

import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MoonHoplingEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive lunar glow for the Moon Hopling — crescent brow, ear tips, tail, eye glints,
 * leg cuffs, and body star-speckles. Subclasses GeckoLib's {@link AutoGlowingGeoLayer} so
 * the glow re-renders aligned with the animated model, and swaps the render type to the
 * additive {@code eyes} blend so it reads as emitted moonlight in the dark.
 */
public class MoonHoplingGlowLayer
        extends AutoGlowingGeoLayer<MoonHoplingEntity, Void, LivingEntityRenderState> {

    private static final Identifier GLOW =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/hopling/moon_hopling_glow.png");

    public MoonHoplingGlowLayer(GeoRenderer<MoonHoplingEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    protected Identifier getTextureResource(LivingEntityRenderState state) {
        return GLOW;
    }

    @Override
    protected RenderType getRenderType(LivingEntityRenderState state) {
        return RenderTypes.eyes(GLOW);
    }
}
