package kingdom.smp.portal.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kingdom.smp.Ironhold;
import kingdom.smp.mixin.CameraInvoker;
import kingdom.smp.portal.PortalLink;
import kingdom.smp.portal.PortalSurface;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Captures the live see-through view for each visible cross-dimensional portal. Mechanically a sibling
 * of {@link kingdom.smp.client.MirrorReflection}: each frame (in {@code RenderFrameEvent.Post}, after the
 * main frame is done) it moves the main camera to the portal's virtual eye in the destination dimension,
 * sets an off-axis frustum bound exactly to the portal opening, and re-renders — but into the
 * <em>secondary</em> {@code ClientLevel} via that level's dedicated {@code LevelRenderer}.
 *
 * <p>The redirect happens through two hooks consulted while {@link #isCapturing()}:
 * {@code MinecraftMainTargetMixin} sends the render output to {@link #activeCaptureTarget()}, and
 * {@code GameRendererPortalRendererMixin} swaps {@code minecraft.levelRenderer} for {@link #activeRenderer()}.
 * {@code mc.level} is swapped directly (it is a public field). The captured colour buffer is exposed as a
 * texture id that {@link PortalRenderEvents} samples onto the in-world portal quad.
 */
public final class PortalRenderer {
    private PortalRenderer() {}

    private static final int PX_PER_BLOCK = 128;
    private static final int MAX_DIM = 1024;

    /** One capture buffer per destination dimension (one portal per dim assumed for the first cut). */
    private static final Map<String, Slot> SLOTS = new HashMap<>();

    private static boolean capturing;
    private static @Nullable RenderTarget activeTarget;
    private static @Nullable LevelRenderer activeRenderer;

    // ── Hooks read by the mixins during capture ──────────────────────────────
    public static boolean isCapturing() {
        return capturing;
    }

    public static @Nullable RenderTarget activeCaptureTarget() {
        return capturing ? activeTarget : null;
    }

    public static @Nullable LevelRenderer activeRenderer() {
        return capturing ? activeRenderer : null;
    }

    /** The texture id holding the most recent capture for a destination dim, or null if none. */
    public static @Nullable Identifier textureIdFor(Identifier destDim) {
        Slot slot = SLOTS.get(destDim.toString());
        return slot == null ? null : slot.textureId;
    }

    /** Capture every visible portal whose destination level is loaded on the client. */
    public static void renderFrame(DeltaTracker delta, List<PortalLink> links) {
        RenderSystem.assertOnRenderThread();
        for (PortalLink link : links) {
            ClientDimensionStack.Secondary sec = ClientDimensionStack.get(link.destDim());
            if (sec == null) {
                continue;
            }
            try {
                captureInto(link, sec, delta);
            } catch (Exception e) {
                Ironhold.LOGGER.error("[portal] capture failed for {}", link.destDim().identifier(), e);
            }
        }
    }

    private static void captureInto(PortalLink link, ClientDimensionStack.Secondary sec, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        PortalSurface surface = link.surface();
        Slot slot = SLOTS.computeIfAbsent(link.destDim().identifier().toString(),
            k -> new Slot(link.destDim().identifier()));
        slot.ensureSize((int) Math.ceil(surface.halfWidth() * 2), (int) Math.ceil(surface.halfHeight() * 2));

        Camera cam = mc.gameRenderer.getMainCamera();
        CameraInvoker camMover = (CameraInvoker) cam;
        Vec3 savedPos = cam.position();
        float savedYRot = cam.yRot();
        float savedXRot = cam.xRot();
        boolean savedDetached = cam.isDetached();
        ClientLevel savedLevel = mc.level;

        Vec3 center = surface.center();
        Vec3 anchor = link.destAnchor();
        // Map the real eye through the portal: virtual eye sits the same offset behind the destination
        // anchor as the real eye sits from the portal centre. (Rotation between dims is deferred.)
        Vec3 virtualEye = savedPos.subtract(center).add(anchor);

        camMover.ironhold$setPosition(virtualEye);
        camMover.ironhold$setRotation(savedYRot, savedXRot);
        camMover.ironhold$setDetached(true);

        capturing = true;
        activeRenderer = sec.renderer();
        activeTarget = slot.target;
        mc.level = sec.level();
        try {
            // Build the dedicated renderer's visible-section set for the virtual camera, then extract
            // (a fresh dedicated renderer has no visibility graph yet; the main renderer reuses the
            // frame's own). update + extract is the normal per-frame order.
            sec.renderer().update(cam);
            mc.gameRenderer.extract(delta, true);

            // Off-axis frustum bound to the portal opening, mapped into destination space.
            var cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
            org.joml.Matrix4f view = cameraState.viewRotationMatrix;
            float l = Float.MAX_VALUE, r = -Float.MAX_VALUE, b = Float.MAX_VALUE, t = -Float.MAX_VALUE, near = 0;
            for (Vec3 sc : surface.corners()) {
                Vec3 dc = sc.subtract(center).add(anchor);
                org.joml.Vector3f v = view.transformPosition(new org.joml.Vector3f(
                    (float) (dc.x - virtualEye.x), (float) (dc.y - virtualEye.y), (float) (dc.z - virtualEye.z)));
                l = Math.min(l, v.x);
                r = Math.max(r, v.x);
                b = Math.min(b, v.y);
                t = Math.max(t, v.y);
                near += -v.z;
            }
            near /= 4.0f;
            if (near < 0.05f) {
                near = 0.05f;
            }
            float far = cameraState.depthFar + 2.0f * near;
            cameraState.projectionMatrix.setFrustum(l, r, b, t, near, far, RenderSystem.getDevice().isZZeroToOne());

            mc.gameRenderer.renderLevel(delta);
        } finally {
            capturing = false;
            activeTarget = null;
            activeRenderer = null;
            mc.level = savedLevel;
            camMover.ironhold$setPosition(savedPos);
            camMover.ironhold$setRotation(savedYRot, savedXRot);
            camMover.ironhold$setDetached(savedDetached);
        }

        slot.wrapper.pointAt(slot.target.getColorTexture(), slot.target.getColorTextureView());
    }

    public static void clear() {
        for (Slot slot : SLOTS.values()) {
            if (slot.target != null) {
                slot.target.destroyBuffers();
            }
        }
        SLOTS.clear();
    }

    /** One capture buffer + its registered sampling texture. */
    private static final class Slot {
        final Identifier textureId;
        @Nullable TextureTarget target;
        WrappedTexture wrapper;
        int curW;
        int curH;

        Slot(Identifier destDim) {
            this.textureId = Identifier.fromNamespaceAndPath(Ironhold.MODID, "portal_view_" + destDim.getPath());
        }

        void ensureSize(int blocksW, int blocksH) {
            int w = Math.min(Math.max(blocksW, 1) * PX_PER_BLOCK, MAX_DIM);
            int h = Math.min(Math.max(blocksH, 1) * PX_PER_BLOCK, MAX_DIM);
            if (target == null) {
                target = new TextureTarget("ironhold_portal_" + textureId.getPath(), w, h, true);
                wrapper = new WrappedTexture();
                Minecraft.getInstance().getTextureManager().register(textureId, wrapper);
                curW = w;
                curH = h;
            } else if (w != curW || h != curH) {
                target.resize(w, h);
                curW = w;
                curH = h;
            }
        }
    }

    private static final class WrappedTexture extends AbstractTexture {
        void pointAt(GpuTexture tex, GpuTextureView view) {
            this.texture = tex;
            this.textureView = view;
        }

        @Override
        public void close() {
            // The TextureTarget owns these GPU resources; nothing to free here.
        }
    }
}
