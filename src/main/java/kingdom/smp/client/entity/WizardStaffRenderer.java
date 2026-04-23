package kingdom.smp.client.entity;

import kingdom.smp.item.WizardStaffItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import com.mojang.math.Axis;

public class WizardStaffRenderer extends GeoItemRenderer<WizardStaffItem> {

    public WizardStaffRenderer() {
        super(new WizardStaffModel());
    }

    /**
     * Reads the loaded gem index from the ItemStack's CustomModelData and adds it to the
     * render state so WizardStaffModel.getModelResource() can pick the correct geo.json.
     */
    @Override
    public void captureDefaultRenderState(WizardStaffItem item, GeoItemRenderer.RenderData renderData,
                                          GeoRenderState state, float partialTick) {
        super.captureDefaultRenderState(item, renderData, state, partialTick);
        ItemStack stack = renderData.itemStack();
        CustomModelData cmd = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        Float gemFloat = cmd.getFloat(0);
        int gemIdx = (gemFloat != null) ? Math.round(gemFloat) : 0;
        state.addGeckolibData(WizardStaffModel.GEM_INDEX, gemIdx);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);
        var ps = renderPassInfo.poseStack();
        switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
                ps.translate(0.5F, 0.51F, 0.5F);
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                ps.translate(0.00F, 0.20F, 0.50F);
                ps.scale(1.5F, 1.5F, 1.5F);
                ps.mulPose(Axis.ZP.rotationDegrees(270.0F));
            }
            default -> ps.translate(0.5F, 0.51F, 0.5F);
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
