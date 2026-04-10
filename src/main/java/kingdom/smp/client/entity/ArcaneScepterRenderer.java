package kingdom.smp.client.entity;

import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.item.ArcaneScepterItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

public class ArcaneScepterRenderer extends GeoItemRenderer<ArcaneScepterItem> {

    public ArcaneScepterRenderer() {
        super(new DefaultedItemGeoModel<ArcaneScepterItem>(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "arcane_scepter"))
            .withAltTexture(Identifier.fromNamespaceAndPath(Ironhold.MODID, "arcane_scepter_geo")));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        // Don't call super — the default (0.5, 0.51, 0.5) centers in a block which is wrong for items
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        var ps = renderPassInfo.poseStack();

        switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // Original working position — don't touch
                ps.translate(0.5F, 0.51F, 0.5F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                ps.translate(-0.70F, 0.20F, 0.50F);
                ps.scale(1.5F, 1.5F, 1.5F);
                ps.mulPose(Axis.YP.rotationDegrees(1.0F));
                ps.mulPose(Axis.XP.rotationDegrees(0.0F));
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
