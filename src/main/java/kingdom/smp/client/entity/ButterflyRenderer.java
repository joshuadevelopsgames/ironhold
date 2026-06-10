package kingdom.smp.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import kingdom.smp.Ironhold;
import kingdom.smp.client.ButterflyScaleDebug;
import kingdom.smp.entity.ButterflyEntity;
import kingdom.smp.entity.ButterflySpecies;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for {@link ButterflyEntity}. Species share one animation but use
 * dedicated encyclopedia-matched wing rigs and textures via a synced render-state ticket.
 */
public class ButterflyRenderer extends GeoEntityRenderer<ButterflyEntity, LivingEntityRenderState> {

    private static final DataTicket<String> SPECIES_TICKET =
        DataTicket.create("butterfly_species", String.class);

    public ButterflyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        withRenderLayer(new GlowLayer(this));
        this.shadowRadius = 0.12F;
    }

    @Override
    public void extractRenderState(ButterflyEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state instanceof GeoRenderState gs) {
            gs.addGeckolibData(SPECIES_TICKET, entity.getSpecies().id());
        }
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo,
                                    float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);
        float scale = ButterflyScaleDebug.scaleFor(speciesFrom(renderPassInfo.renderState()).modelShape());
        renderPassInfo.poseStack().scale(scale, scale, scale);
    }

    private static class Model extends DefaultedEntityGeoModel<ButterflyEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "butterfly"));
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            ButterflySpecies species = speciesFrom(state);
            return Identifier.fromNamespaceAndPath(Ironhold.MODID,
                "geckolib/models/entity/butterfly_" + species.id() + ".geo.json");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return texture(speciesFrom(state).id(), "");
        }
    }

    private static class GlowLayer
            extends AutoGlowingGeoLayer<ButterflyEntity, Void, LivingEntityRenderState> {

        GlowLayer(GeoRenderer<ButterflyEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        protected Identifier getTextureResource(LivingEntityRenderState state) {
            return texture(speciesFrom(state).id(), "_glowmask");
        }

        @Override
        protected RenderType getRenderType(LivingEntityRenderState state) {
            return speciesFrom(state).isGlowing() ? super.getRenderType(state) : null;
        }
    }

    private static ButterflySpecies speciesFrom(GeoRenderState state) {
        return ButterflySpecies.byId(state.getOrDefaultGeckolibData(SPECIES_TICKET, "monarch"));
    }

    private static Identifier texture(String species, String suffix) {
        return Identifier.fromNamespaceAndPath(Ironhold.MODID,
            "textures/entity/butterfly/" + species + suffix + ".png");
    }
}
