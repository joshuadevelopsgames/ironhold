package kingdom.smp.client.entity;

import com.mojang.math.Axis;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.ThrownPitchforkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * Renders the thrown pitchfork using the same GeckoLib 3D model as the held item.
 * Rotates the model tip-first in the direction of travel.
 */
public class ThrownPitchforkRenderer extends GeoEntityRenderer<ThrownPitchforkEntity, ThrownPitchforkRenderState> {

    private static final Identifier PITCHFORK_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/item/pitchfork.png");

    public ThrownPitchforkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<ThrownPitchforkRenderState> renderPassInfo) {
        ThrownPitchforkRenderState state = renderPassInfo.renderState();
        float yaw   = state.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW,   0f);
        float pitch = state.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH,  0f);

        var ps = renderPassInfo.poseStack();

        // Match vanilla trident orientation
        ps.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(pitch + 90.0F));
    }

    /** GeoModel that uses the pitchfork item's geo file but points texture to the item texture. */
    private static class Model extends DefaultedEntityGeoModel<ThrownPitchforkEntity> {
        Model() {
            super(Ironhold.THROWN_PITCHFORK.get());
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return PITCHFORK_TEXTURE;
        }
    }
}
