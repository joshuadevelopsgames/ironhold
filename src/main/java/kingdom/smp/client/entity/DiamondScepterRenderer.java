package kingdom.smp.client.entity;

import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.item.DiamondScepterItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public class DiamondScepterRenderer extends GeoItemRenderer<DiamondScepterItem> {

    public DiamondScepterRenderer() {
        super(new DefaultedItemGeoModel<DiamondScepterItem>(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "diamond_scepter"))
            .withAltTexture(Identifier.fromNamespaceAndPath(Ironhold.MODID, "diamond_scepter_geo")));
        // Emissive glow: diamond_scepter_geo_glowmask.png makes the bright cyan
        // crystal pixels render full-bright. Glowmask derives from the geo (alt) texture name.
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        var ps = renderPassInfo.poseStack();

        switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                ps.translate(0.5F, 0.51F, 0.5F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                ps.translate(-0.70F, 0.20F, 0.50F);
                ps.scale(1.5F, 1.5F, 1.5F);
                ps.mulPose(Axis.YP.rotationDegrees(1.0F));
                ps.mulPose(Axis.ZP.rotationDegrees(270.0F));
            }
            default -> {
                ps.translate(0.5F, 0.51F, 0.5F);
            }
        }
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                     float widthScale, float heightScale) {
        super.scaleModelForRender(renderPassInfo, widthScale, heightScale);

        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            renderPassInfo.poseStack().translate(0.0F, -0.8F, 0.0F);
        }
    }
}
