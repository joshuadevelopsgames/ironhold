package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.client.ClubTransformDebug;
import kingdom.smp.client.ClubTransformDebug.Pose;
import kingdom.smp.item.ClubItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renders a Club (plain / spiked / ribbed) using its GeckoLib 3D model, selected by the
 * {@code geoName} passed in. The full item model routes every display context to the
 * geckolib special model, so the 3D model is shown everywhere — including the inventory
 * slot (like a block), not a flat sprite.
 *
 * <p>Per-context poses come from {@link ClubTransformDebug} and are tunable live via
 * {@code /clubdebug} (the GUI/ground/fixed poses especially want an in-game eyeball). The
 * in-hand poses mirror the pitchfork's.</p>
 */
public class ClubRenderer extends GeoItemRenderer<ClubItem> {

    public ClubRenderer(String geoName) {
        super(new DefaultedItemGeoModel<ClubItem>(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, geoName)));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        Pose p = switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> ClubTransformDebug.FP;
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> ClubTransformDebug.TP;
            case GROUND -> ClubTransformDebug.GROUND;
            case FIXED -> ClubTransformDebug.FIXED;
            default -> ClubTransformDebug.GUI; // GUI + HEAD + NONE
        };
        apply(renderPassInfo.poseStack(), p);
    }

    /** translate, then scale, then Z·Y·X rotation (matches ClubTransformDebug's documented order). */
    private static void apply(PoseStack ps, Pose p) {
        ps.translate(p.tx, p.ty, p.tz);
        if (p.scale != 1.0F) ps.scale(p.scale, p.scale, p.scale);
        if (p.rz != 0F) ps.mulPose(Axis.ZP.rotationDegrees(p.rz));
        if (p.ry != 0F) ps.mulPose(Axis.YP.rotationDegrees(p.ry));
        if (p.rx != 0F) ps.mulPose(Axis.XP.rotationDegrees(p.rx));
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
