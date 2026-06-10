package kingdom.smp.client.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MagicMinecartEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for the {@link MagicMinecartEntity}. Renders the custom
 * {@code magic_minecart.geo.json} (dark-indigo trapezoidal tub, silver rim with
 * raised corner posts, glowing cyan runes, interior energy pool, wheels with
 * emissive purple hubs) instead of the vanilla minecart model, plus an additive
 * {@link MagicMinecartGlowLayer} for the glow-in-the-dark detailing.
 */
public class MagicMinecartRenderer
        extends GeoEntityRenderer<MagicMinecartEntity, EntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/magic_minecart.png");

    public MagicMinecartRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.55F;
        withRenderLayer(new MagicMinecartGlowLayer(this));
    }

    /** Geo model wrapper pointing at the entity's base texture. */
    private static class Model extends DefaultedEntityGeoModel<MagicMinecartEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "magic_minecart"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }
    }
}
