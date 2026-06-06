package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.Ironhold;
import kingdom.smp.item.BattleHammerItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;

public class BattleHammerRenderer extends GeoItemRenderer<BattleHammerItem> {

    public BattleHammerRenderer() {
        super(new DefaultedItemGeoModel<BattleHammerItem>(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer"))
            .withAltTexture(Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer_geo")));
        // Forge glow: battle_hammer_geo_glowmask.png lights the inner-ring cubes. Charge-gated
        // subclass of AutoGlowing — invisible at rest, fades in as the hammer charges.
        withRenderLayer(new BattleHammerForgeGlowLayer(this));
    }

    // Charge fraction (0..1 while charging) and slam progress (0..1 during the post-release
    // cooldown window), computed from the holder and applied as a grip-pivot swing in the pose.
    private static final DataTicket<Float> CHARGE = DataTicket.create("battle_hammer_pose_charge", Float.class);
    private static final DataTicket<Float> SLAM = DataTicket.create("battle_hammer_pose_slam", Float.class);

    // Handle grip pivot: geo [-5, 1.2, 6] / 16 = blocks.
    private static final float GX = -5f / 16f, GY = 1.2f / 16f, GZ = 6f / 16f;
    private static final float WINDUP_DEG = 22f;     // subtle cock-back at full charge
    private static final float SLAM_PEAK_DEG = -132f; // forward/down peak of the slam (slight overshoot)
    private static final float SLAM_RISE = 0.18f;    // fraction of the slam spent swinging down (snappier strike)

    @Override
    public void captureDefaultRenderState(BattleHammerItem animatable, RenderData data,
                                          GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, data, renderState, partialTick);
        ItemOwner owner = data.itemOwner();
        LivingEntity living = owner != null ? owner.asLivingEntity() : null;

        float charge = 0f;
        if (living != null && living.isUsingItem()
                && living.getUseItem().getItem() instanceof BattleHammerItem) {
            charge = Math.min(living.getTicksUsingItem() / (float) BattleHammerItem.FULL_CHARGE_TICKS, 1f);
        }

        float slam = 1f; // 1 == finished / no slam
        if (living instanceof Player p) {
            float cd = p.getCooldowns().getCooldownPercent(data.itemStack(), partialTick); // 1 just released -> 0 done
            if (cd > 0.001f) slam = Math.min((1f - cd) / 0.34f, 1f); // slam over the first ~third of cooldown
        }

        renderState.addGeckolibData(CHARGE, charge);
        renderState.addGeckolibData(SLAM, slam);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        ItemDisplayContext ctx = renderPassInfo.renderState()
            .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.FIXED);

        var ps = renderPassInfo.poseStack();

        switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND -> apply(ps,
                kingdom.smp.client.BattleHammerTransformDebug.fpTransX,
                kingdom.smp.client.BattleHammerTransformDebug.fpTransY,
                kingdom.smp.client.BattleHammerTransformDebug.fpTransZ,
                kingdom.smp.client.BattleHammerTransformDebug.fpRotX,
                kingdom.smp.client.BattleHammerTransformDebug.fpRotY,
                kingdom.smp.client.BattleHammerTransformDebug.fpRotZ,
                kingdom.smp.client.BattleHammerTransformDebug.fpScale);
            case FIRST_PERSON_LEFT_HAND -> apply(ps,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpTransX,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpTransY,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpTransZ,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpRotX,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpRotY,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpRotZ,
                kingdom.smp.client.BattleHammerTransformDebug.ohFpScale);
            case THIRD_PERSON_RIGHT_HAND -> apply(ps,
                kingdom.smp.client.BattleHammerTransformDebug.tpTransX,
                kingdom.smp.client.BattleHammerTransformDebug.tpTransY,
                kingdom.smp.client.BattleHammerTransformDebug.tpTransZ,
                kingdom.smp.client.BattleHammerTransformDebug.tpRotX,
                kingdom.smp.client.BattleHammerTransformDebug.tpRotY,
                kingdom.smp.client.BattleHammerTransformDebug.tpRotZ,
                kingdom.smp.client.BattleHammerTransformDebug.tpScale);
            case THIRD_PERSON_LEFT_HAND -> apply(ps,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpTransX,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpTransY,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpTransZ,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpRotX,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpRotY,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpRotZ,
                kingdom.smp.client.BattleHammerTransformDebug.ohTpScale);
            default -> ps.translate(0.5F, 0.51F, 0.5F);
        }

        // Wind-up (while charging) / slam (post-release) swing, pivoting at the grip.
        float charge = renderPassInfo.renderState().getOrDefaultGeckolibData(CHARGE, 0f);
        float slam = renderPassInfo.renderState().getOrDefaultGeckolibData(SLAM, 1f);
        float angle = 0f;
        if (charge > 0.01f) {
            angle = WINDUP_DEG * charge;            // tilt back as it charges
        } else if (slam < 0.999f) {
            angle = slamAngle(slam);                // swing forward/down then settle
        }
        if (angle != 0f) {
            ps.translate(GX, GY, GZ);
            ps.mulPose(Axis.XP.rotationDegrees(angle));
            ps.translate(-GX, -GY, -GZ);
        }
    }

    /** Slam arc: continue from the wound-up angle, swing past to the forward peak, then settle. */
    private static float slamAngle(float p) {
        if (p < SLAM_RISE) {
            return WINDUP_DEG + (SLAM_PEAK_DEG - WINDUP_DEG) * (p / SLAM_RISE);
        }
        return SLAM_PEAK_DEG * (1f - (p - SLAM_RISE) / (1f - SLAM_RISE));
    }

    /** trans, then scale, then Z*Y*X rotations (tunable live via /hammerdebug). */
    private static void apply(com.mojang.blaze3d.vertex.PoseStack ps,
                              float tx, float ty, float tz, float rx, float ry, float rz, float scale) {
        ps.translate(tx, ty, tz);
        if (scale != 1.0F) ps.scale(scale, scale, scale);
        ps.mulPose(Axis.ZP.rotationDegrees(rz));
        ps.mulPose(Axis.YP.rotationDegrees(ry));
        ps.mulPose(Axis.XP.rotationDegrees(rx));
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
