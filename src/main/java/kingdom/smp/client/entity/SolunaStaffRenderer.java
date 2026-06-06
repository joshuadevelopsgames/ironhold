package kingdom.smp.client.entity;

import com.mojang.math.Axis;
import kingdom.smp.item.SolunaStaffItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renders the Soluna Staff using the animated GeckoLib model.
 * Fades the model alpha during the sun/moon texture transition.
 */
public class SolunaStaffRenderer extends GeoItemRenderer<SolunaStaffItem> {

    public SolunaStaffRenderer() {
        super(new SolunaStaffModel());
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public int getRenderColor(SolunaStaffItem item, GeoItemRenderer.RenderData renderData, float partialTick) {
        float alpha = SolunaStaffModel.getFadeAlpha();
        int a = (int) (alpha * 255);
        return ARGB.color(a, 255, 255, 255);
    }

    @Override
    public RenderType getRenderType(GeoRenderState state, Identifier texture) {
        float alpha = SolunaStaffModel.getFadeAlpha();
        if (alpha < 1f) {
            return RenderTypes.entityTranslucent(texture);
        }
        return super.getRenderType(state, texture);
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
                ps.translate(0.00F, 0.20F, 0.50F);
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
