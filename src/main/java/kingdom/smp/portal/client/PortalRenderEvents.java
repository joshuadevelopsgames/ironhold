package kingdom.smp.portal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import kingdom.smp.portal.PortalLink;
import kingdom.smp.portal.PortalSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Drives the portal see-through each frame: captures every visible portal's destination view in
 * {@code RenderFrameEvent.Post} (after the main frame), then draws each portal's quad — sampling that
 * capture — into the real world during {@code RenderLevelStageEvent}. Recursion is capped at depth 1:
 * portal quads are not drawn while a portal view is itself being captured.
 */
public final class PortalRenderEvents {
    private PortalRenderEvents() {}

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        List<PortalLink> links = ClientPortalRegistry.get();
        if (!links.isEmpty()) {
            PortalRenderer.renderFrame(event.getPartialTick(), links);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (PortalRenderer.isCapturing()) {
            return; // depth-1 cap: no portals inside a portal view
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        List<PortalLink> links = ClientPortalRegistry.get();
        if (links.isEmpty()) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (PortalLink link : links) {
            Identifier tex = PortalRenderer.textureIdFor(link.destDim().identifier());
            if (tex == null) {
                continue;
            }
            RenderType rt = RenderTypes.entitySolid(tex);
            VertexConsumer vc = buffers.getBuffer(rt);
            drawPortalQuad(pose.last(), vc, link.surface(), cam);
            buffers.endBatch(rt);
        }
    }

    /** How far to lift the see-through quad toward the viewer so it covers the portal block's own
     *  (blue/purple) surface instead of z-fighting or sitting behind it. */
    private static final double SURFACE_LIFT = 0.08;

    /** Draw the portal opening as a double-sided textured quad sampling the captured view (V-flipped
     *  for the bottom-left-origin framebuffer). The quad is lifted toward whichever side the camera is
     *  on so the vanilla portal surface is hidden behind it. */
    private static void drawPortalQuad(PoseStack.Pose pose, VertexConsumer vc, PortalSurface s, Vec3 cam) {
        Vec3 n = s.normal();
        double side = Math.signum(cam.subtract(s.center()).dot(n));
        if (side == 0) {
            side = 1;
        }
        Vec3 lift = n.scale(SURFACE_LIFT * side);
        Vec3[] c = s.corners(); // BL, BR, TL, TR
        Vec3 bl = c[0].add(lift), br = c[1].add(lift), tl = c[2].add(lift), tr = c[3].add(lift);
        int nx = (int) Math.round(n.x), ny = (int) Math.round(n.y), nz = (int) Math.round(n.z);
        // Front face (+normal): BL,BR,TR,TL
        vertex(pose, vc, bl, cam, 0f, 0f, nx, ny, nz);
        vertex(pose, vc, br, cam, 1f, 0f, nx, ny, nz);
        vertex(pose, vc, tr, cam, 1f, 1f, nx, ny, nz);
        vertex(pose, vc, tl, cam, 0f, 1f, nx, ny, nz);
        // Back face (-normal): BL,TL,TR,BR
        vertex(pose, vc, bl, cam, 0f, 0f, -nx, -ny, -nz);
        vertex(pose, vc, tl, cam, 0f, 1f, -nx, -ny, -nz);
        vertex(pose, vc, tr, cam, 1f, 1f, -nx, -ny, -nz);
        vertex(pose, vc, br, cam, 1f, 0f, -nx, -ny, -nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, Vec3 cam,
                               float u, float v, int nx, int ny, int nz) {
        vc.addVertex(pose, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal(pose, nx, ny, nz);
    }
}
