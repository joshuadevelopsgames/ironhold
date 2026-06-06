package kingdom.smp.mirrors.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kingdom.smp.mirrors.Mirrors;
import kingdom.smp.mirrors.entity.MirrorEntity;
import kingdom.smp.mirrors.mixin.CameraInvoker;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Owns a small pool of off-screen reflection buffers — one per nearby mirror — so several mirrors can
 * each show their own view instead of sharing one. Each slot has its own {@link TextureTarget} and an
 * {@link AbstractTexture} wrapper registered under a per-slot id, which the entity render type
 * ({@code entitySolid(id)}) resolves to that slot's live color view. Each frame (in
 * {@code RenderFrameEvent.Post}) the nearest in-view mirrors are assigned slots and rendered as true
 * planar reflections via an off-axis projection from the player's reflected eye.
 */
public final class MirrorReflection {
    private MirrorReflection() {}

    /** Max simultaneous live mirrors (each costs one extra world render per frame). */
    public static final int MAX_SLOTS = 4;
    private static final int PX_PER_BLOCK = 256;
    private static final int MAX_DIM = 1024;

    private static final Slot[] SLOTS = new Slot[MAX_SLOTS];
    /** entityId -> slot index for the most recent capture; read by the renderer next frame. */
    private static Map<Integer, Integer> assignment = new HashMap<>();

    private static @Nullable RenderTarget activeTarget; // the slot currently being captured
    private static boolean capturing;
    private static int capturingMirrorId = -1; // entity id of the mirror whose reflection is being captured

    // ── Live debug nudge (set via /mirrorcam), applied to every capture ───────
    public static double dbgRight;
    public static double dbgUp;
    public static double dbgFwd;
    public static float dbgYaw;
    public static float dbgPitch;
    public static volatile String lastCamSummary = "(no capture yet)";

    // ── Per-capture diagnostics, folded into lastCamSummary for /mirrorcam print ──────────
    // Counts pertain to OTHER mirrors (not the one being captured) seen during the active pass.
    private static int diagSrTrue;        // shouldRender returned true (not frustum/distance culled)
    private static int diagSrFalse;       // shouldRender returned false (frustum/distance culled)
    private static int diagSubmitted;     // actually reached submit() (also passed isSectionCompiled)
    private static int diagSubmittedLive; // ...of those, how many had a live reflection texture (a slot)

    /** MirrorRenderer.shouldRender reports each NON-self mirror's visibility here during a capture. */
    public static void diagShouldRender(boolean visible) {
        if (visible) {
            diagSrTrue++;
        } else {
            diagSrFalse++;
        }
    }

    /** MirrorRenderer.submit reports each mirror that actually renders here during a capture. */
    public static void diagSubmit(boolean liveReflection) {
        diagSubmitted++;
        if (liveReflection) {
            diagSubmittedLive++;
        }
    }

    /** The texture id a given mirror should sample this frame, or null if it has no slot. */
    public static @Nullable Identifier textureIdFor(int entityId) {
        Integer slot = assignment.get(entityId);
        if (slot == null || SLOTS[slot] == null) {
            // No slot yet, or queried mid-capture before this slot was created this frame.
            return null;
        }
        return SLOTS[slot].textureId;
    }

    public static @Nullable RenderTarget activeCaptureTarget() {
        return capturing ? activeTarget : null;
    }

    public static boolean isCapturing() {
        return capturing;
    }

    /**
     * True only while capturing this exact mirror's own reflection. The capturing mirror must not draw
     * into its own pass — its pane sits on the near plane (it would occlude the view and self-recurse).
     * Every *other* mirror, however, does render here, sampling its own buffer; that yields recursive
     * mirror-in-mirror reflections that build up one bounce per frame and converge into an infinite
     * hallway, with no extra world-render passes.
     */
    public static boolean isCapturingMirror(int entityId) {
        return capturing && entityId == capturingMirrorId;
    }

    /**
     * Assign slots to the given mirrors (already chosen: nearest, in-view, ≤ MAX_SLOTS) and render
     * each into its own buffer. Keeps each mirror in its previous slot when possible to avoid flicker.
     */
    public static void renderFrame(DeltaTracker delta, List<MirrorEntity> mirrors) {
        RenderSystem.assertOnRenderThread();
        Map<Integer, Integer> next = new HashMap<>();
        boolean[] used = new boolean[MAX_SLOTS];
        // 1) keep mirrors in the slot they used last frame, if still free
        for (MirrorEntity m : mirrors) {
            Integer prev = assignment.get(m.getId());
            if (prev != null && !used[prev]) {
                next.put(m.getId(), prev);
                used[prev] = true;
            }
        }
        // 2) give the rest the first free slot
        int free = 0;
        for (MirrorEntity m : mirrors) {
            if (next.containsKey(m.getId())) {
                continue;
            }
            while (free < MAX_SLOTS && used[free]) {
                free++;
            }
            if (free >= MAX_SLOTS) {
                break;
            }
            next.put(m.getId(), free);
            used[free] = true;
        }
        assignment = next;

        // The reflected camera sits behind the mirror, embedded in the wall. Minecraft decides chunk
        // visibility with a BFS through the section-occlusion graph starting from the camera's own
        // section; from inside solid blocks that BFS starves, so reflections rendered only a few
        // sections — a tiny render distance, and a mirror reflected inside another mirror came back
        // nearly empty (reading as "invisible"), killing the recursion. Disable occlusion culling for
        // the capture passes — exactly what vanilla does for a spectator stuck inside a block — so the
        // full off-axis frustum renders. That frustum is narrow (bounded to the pane), so the extra
        // sections are few. Restored immediately; the main view recomputes its own culling next frame.
        Minecraft mc = Minecraft.getInstance();
        boolean savedSmartCull = mc.smartCull;
        mc.smartCull = false;
        StringBuilder diag = new StringBuilder("inRange=").append(mirrors.size());
        try {
            for (MirrorEntity m : mirrors) {
                Integer slotIndex = assignment.get(m.getId());
                if (slotIndex == null) {
                    continue;
                }
                Slot slot = SLOTS[slotIndex] != null ? SLOTS[slotIndex] : (SLOTS[slotIndex] = new Slot(slotIndex));
                diag.append(captureInto(slot, delta, m));
            }
        } finally {
            mc.smartCull = savedSmartCull;
        }
        lastCamSummary = diag.toString();
    }

    /** Render one mirror as a planar reflection into its slot's buffer; returns a /mirrorcam diag line. */
    private static String captureInto(Slot slot, DeltaTracker delta, MirrorEntity mirror) {
        int mw = mirror.getWidthBlocks();
        int mh = mirror.getHeightBlocks();
        slot.ensureSize(mw, mh); // buffer matches this pane's shape so the off-axis frustum maps 1:1

        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        CameraInvoker camMover = (CameraInvoker) cam;

        Vec3 savedPos = cam.position();
        float savedYRot = cam.yRot();
        float savedXRot = cam.xRot();
        boolean savedDetached = cam.isDetached();

        Direction dir = mirror.getDirection();
        Vec3 n = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()); // unit outward normal
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = worldUp.cross(n);
        right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 surface = mirror.position().add(n.scale(MirrorEntity.DEPTH / 2.0));

        // Reflect the actual (smoothly interpolated) camera eye, not getEyePosition() — the latter
        // snaps its height the instant the crouch pose flips, while the real camera eases the crouch.
        Vec3 eyePos = savedPos;
        double signedDist = eyePos.subtract(surface).dot(n);
        Vec3 reflectedEye = eyePos.subtract(n.scale(2.0 * signedDist))
            .add(right.scale(dbgRight)).add(worldUp.scale(dbgUp)).add(n.scale(dbgFwd));

        camMover.mirrors$setPosition(reflectedEye);
        camMover.mirrors$setRotation(dir.toYRot() + dbgYaw, dbgPitch);
        camMover.mirrors$setDetached(true);

        mc.gameRenderer.extract(delta, true);

        // Off-axis projection: bound the four pane corners (in view space) exactly, with the mirror
        // plane as the near plane (clipping the wall in front of the reflected camera).
        var cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        float hw = mw / 2.0F;
        float hh = mh / 2.0F;
        Vec3[] corners = {
            surface.add(right.scale(-hw)).add(worldUp.scale(-hh)),
            surface.add(right.scale(hw)).add(worldUp.scale(-hh)),
            surface.add(right.scale(-hw)).add(worldUp.scale(hh)),
            surface.add(right.scale(hw)).add(worldUp.scale(hh)),
        };
        float l = Float.MAX_VALUE, r = -Float.MAX_VALUE, b = Float.MAX_VALUE, t = -Float.MAX_VALUE, near = 0;
        org.joml.Matrix4f view = cameraState.viewRotationMatrix;
        for (Vec3 c : corners) {
            org.joml.Vector3f v = view.transformPosition(new org.joml.Vector3f(
                (float) (c.x - reflectedEye.x), (float) (c.y - reflectedEye.y), (float) (c.z - reflectedEye.z)));
            l = Math.min(l, v.x);
            r = Math.max(r, v.x);
            b = Math.min(b, v.y);
            t = Math.max(t, v.y);
            near += -v.z;
        }
        near /= corners.length;
        float far = cameraState.depthFar + 2.0f * near;
        boolean zZeroToOne = RenderSystem.getDevice().isZZeroToOne();
        cameraState.projectionMatrix.setFrustum(l, r, b, t, near, far, zZeroToOne);

        activeTarget = slot.target;
        capturing = true;
        capturingMirrorId = mirror.getId();
        diagSrTrue = 0;
        diagSrFalse = 0;
        diagSubmitted = 0;
        diagSubmittedLive = 0;
        try {
            mc.gameRenderer.renderLevel(delta);
        } finally {
            capturing = false;
            capturingMirrorId = -1;
            activeTarget = null;
        }

        camMover.mirrors$setPosition(savedPos);
        camMover.mirrors$setRotation(savedYRot, savedXRot);
        camMover.mirrors$setDetached(savedDetached);

        // Diagnostic line per capture: what happened to the OTHER mirrors during this pass.
        //   srT/srF = shouldRender true/false (frustum/distance cull)
        //   sub     = reached submit() (also cleared the isSectionCompiled gate)
        //   live    = of those, how many drew their live reflection texture (had a slot)
        return String.format(
            " | id=%d slot=%d near=%.1f far=%.0f LRBT=%.1f/%.1f/%.1f/%.1f others[srT=%d srF=%d sub=%d live=%d]",
            mirror.getId(), slot.index, near, far, l, r, b, t,
            diagSrTrue, diagSrFalse, diagSubmitted, diagSubmittedLive);
    }

    /** One reflection buffer + its registered sampling texture. */
    private static final class Slot {
        final int index;
        final Identifier textureId;
        TextureTarget target;
        WrappedTexture wrapper;
        int curW;
        int curH;

        Slot(int index) {
            this.index = index;
            this.textureId = Identifier.fromNamespaceAndPath(Mirrors.MODID, "mirror_reflection_" + index);
        }

        void ensureSize(int blocksW, int blocksH) {
            int w = Math.min(blocksW * PX_PER_BLOCK, MAX_DIM);
            int h = Math.min(blocksH * PX_PER_BLOCK, MAX_DIM);
            if (target == null) {
                target = new TextureTarget("mirrors_mirror_" + index, w, h, true);
                wrapper = new WrappedTexture();
                Minecraft.getInstance().getTextureManager().register(textureId, wrapper);
                curW = w;
                curH = h;
            } else if (w != curW || h != curH) {
                target.resize(w, h);
                curW = w;
                curH = h;
            }
            wrapper.pointAt(target.getColorTexture(), target.getColorTextureView());
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
