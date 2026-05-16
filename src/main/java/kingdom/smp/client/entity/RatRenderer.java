package kingdom.smp.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.RatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib renderer for {@link RatEntity}. Uses the bbmodel-converted {@code rat.geo.json}.
 *
 * <p>Texture is selected per render based on the entity's Black Rat flag, plumbed through
 * a GeckoLib {@link DataTicket} so the model code can read it during {@code getTextureResource}.
 */
public class RatRenderer extends GeoEntityRenderer<RatEntity, LivingEntityRenderState> {

    private static final Identifier TEXTURE_NORMAL =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/rat.png");
    private static final Identifier TEXTURE_BLACK =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/rat_black.png");

    private static final DataTicket<Boolean> BLACK_RAT_TICKET =
        DataTicket.create("rat_is_black", Boolean.class);

    public RatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0.18F;
    }

    @Override
    public void extractRenderState(RatEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (state instanceof GeoRenderState gs) {
            gs.addGeckolibData(BLACK_RAT_TICKET, entity.isBlackRat());
        }
    }

    private static class Model extends DefaultedEntityGeoModel<RatEntity> {
        Model() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "rat"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            Boolean black = state.getOrDefaultGeckolibData(BLACK_RAT_TICKET, false);
            return black ? TEXTURE_BLACK : TEXTURE_NORMAL;
        }
    }
}
