package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kingdom.smp.item.DiamondScepterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the Diamond Scepter charge force-field in world space around the LOCAL
 * player while charging, but ONLY in first person — where the player-body
 * {@link DiamondScepterFieldLayer} cannot render (the body model isn't drawn in
 * first person). In third person the body layer handles it, so this is gated to
 * first person to avoid double-rendering. Uses immediate-mode buffers since the
 * deferred submit collector isn't exposed to RenderLevelStageEvent.
 */
public final class DiamondScepterFieldWorldRenderer {
    private DiamondScepterFieldWorldRenderer() {}

    private static final float FULL_CHARGE = 40f;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() != null && !mc.options.getCameraType().isFirstPerson()) return;

        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!p.isUsingItem() || !(p.getUseItem().getItem() instanceof DiamondScepterItem)) return;

        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float frac = Math.min(p.getTicksUsingItem() / FULL_CHARGE, 1f);
        float radius = 1.3f + 0.6f * frac;
        float age = p.tickCount + pt;
        float phase = age * 0.012f;
        float pulse = 0.85f + 0.15f * (float) Math.sin(age * 0.3f);
        // First person: noticeably more translucent than the third-person body shell.
        float alpha = (0.12f + 0.09f * frac) * pulse;

        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        Vec3 mid = p.getPosition(pt).add(0.0, 0.95, 0.0); // midsection

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(mid.x - cam.x, mid.y - cam.y, mid.z - cam.z);

        RenderType rt = ForcefieldDome.renderType(ForcefieldDome.FIELD_TEXTURE);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(rt);
        ForcefieldDome.fillGlowing(pose.last(), vc, radius, alpha, phase);
        buffers.endBatch(rt);

        pose.popPose();
    }
}
