package kingdom.smp.client.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.GargoyleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for the {@link GargoyleEntity}. Renders the {@code gargoyle.geo.json} rig
 * directly (winged stone creature: horned head, two wings, stubby arms/legs, tail) so the
 * Blockbench model is the single source of truth.
 */
public class GargoyleRenderer extends GeoEntityRenderer<GargoyleEntity, LivingEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/gargoyle.png");

    public GargoyleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.5F;
    }

    private static class Model extends DefaultedEntityGeoModel<GargoyleEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "gargoyle"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }
    }
}
