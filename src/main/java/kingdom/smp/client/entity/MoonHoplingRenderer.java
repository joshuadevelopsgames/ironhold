package kingdom.smp.client.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MoonHoplingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for the {@link MoonHoplingEntity}. Renders the HD
 * {@code moon_hopling.geo.json} directly (12-part chibi moon-bunny: big head with a
 * crescent brow + protruding snout, two long two-segment lunar ears, fluffy tail, four
 * stubby legs) so the Blockbench model is the single source of truth — no hand-ported
 * vanilla {@code CubeListBuilder} geometry to keep in sync.
 */
public class MoonHoplingRenderer extends GeoEntityRenderer<MoonHoplingEntity, LivingEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/hopling/moon_hopling.png");

    public MoonHoplingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.4F;
        // Additive lunar glow (crescent, ear tips, tail, eye glints, cuffs, body sparkles).
        withRenderLayer(new MoonHoplingGlowLayer(this));
    }

    /** Geo model wrapper that points the texture path at our hopling/ subfolder PNG. */
    private static class Model extends DefaultedEntityGeoModel<MoonHoplingEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "moon_hopling"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }
    }
}
