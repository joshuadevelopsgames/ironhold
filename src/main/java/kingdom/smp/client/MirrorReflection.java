package kingdom.smp.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.MirrorEntity;
import kingdom.smp.mixin.CameraInvoker;
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

    /**
     * Terrain cap for the reflection pass: while capturing, {@code MirrorTerrainCullMixin} draws every
     * loaded section the mirror's view cone touches within this many chunks (sections) of the player —
     * sourced straight from {@code ViewArea}, with no occlusion BFS, so no holes and no stale-graph snap.
     * Bounds the cost and matches "show the N chunks behind you". 3–5 is the useful range. REVERTABLE: a
     * large value widens the box back toward full render distance.
     */
    private static final int CAP_CHUNKS = 5;
    /** The player's real eye for this capture; the cull box is centred on this section. */
    private static Vec3 captureCenter = Vec3.ZERO;

    /**
     * Temporal staggering: when several mirrors are live, refresh just one per frame (round-robin)
     * instead of re-rendering all of them every frame — a wall mirror updating at a fraction of the
     * frame rate is imperceptible. REVERTABLE: set to {@code false} to restore capturing every assigned
     * mirror every frame (the only behavioural change; the first-capture / slot-change guards below are
     * no-ops in that mode). A lone mirror is never staggered, so the common case stays full-rate.
     */
    private static final boolean STAGGER = true;
    private static long frameCounter;

    private static final Slot[] SLOTS = new Slot[MAX_SLOTS];
    /** entityId -> slot index for the most recent capture; read by the renderer next frame. */
    private static Map<Integer, Integer> assignment = new HashMap<>();

    private static @Nullable RenderTarget activeTarget; // the slot currently being captured
    private static boolean capturing;

    // ── Live debug nudge (set via /mirrorcam), applied to every capture ───────
    public static double dbgRight;
    public static double dbgUp;
    public static double dbgFwd;
    public static float dbgYaw;
    public static float dbgPitch;
    public static volatile String lastCamSummary = "(no capture yet)";

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

    /** Center of the reflection terrain cap (the player's real eye for the active capture). */
    public static Vec3 captureCenter() {
        return captureCenter;
    }

    /** Half-extent of the reflection cull box, in chunks/sections. */
    public static int capChunks() {
        return CAP_CHUNKS;
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
        Map<Integer, Integer> prevAssignment = assignment;
        assignment = next;

        frameCounter++;
        int n = mirrors.size();
        for (int i = 0; i < n; i++) {
            MirrorEntity m = mirrors.get(i);
            Integer slotIndex = assignment.get(m.getId());
            if (slotIndex == null) {
                continue;
            }
            Slot slot = SLOTS[slotIndex] != null ? SLOTS[slotIndex] : (SLOTS[slotIndex] = new Slot(slotIndex));
            // Stagger refreshes across frames, but always capture a mirror that has no buffer yet or that
            // just took over this slot — otherwise it would sample a stale/foreign reflection until its turn.
            boolean firstCapture = slot.target == null;
            boolean slotChanged = !Objects.equals(prevAssignment.get(m.getId()), slotIndex);
            boolean myTurn = !STAGGER || n <= 1 || (frameCounter % n) == i;
            if (firstCapture || slotChanged || myTurn) {
                captureInto(slot, delta, m);
            }
        }
    }

    /** Render one mirror as a planar reflection into its slot's buffer. */
    private static void captureInto(Slot slot, DeltaTracker delta, MirrorEntity mirror) {
        int mw = mirror.getWidthBlocks();
        int mh = mirror.getHeightBlocks();

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

        // Size the buffer to this pane's shape (so the off-axis frustum maps 1:1) at a resolution that
        // falls off with viewing distance — a far mirror doesn't need a 1024² capture.
        slot.ensureSize(mw, mh, pixelsPerBlockFor(savedPos.distanceTo(surface)));

        // Reflect the actual (smoothly interpolated) camera eye, not getEyePosition() — the latter
        // snaps its height the instant the crouch pose flips, while the real camera eases the crouch.
        Vec3 eyePos = savedPos;
        double signedDist = eyePos.subtract(surface).dot(n);
        Vec3 reflectedEye = eyePos.subtract(n.scale(2.0 * signedDist))
            .add(right.scale(dbgRight)).add(worldUp.scale(dbgUp)).add(n.scale(dbgFwd));

        camMover.ironhold$setPosition(reflectedEye);
        camMover.ironhold$setRotation(dir.toYRot() + dbgYaw, dbgPitch);
        camMover.ironhold$setDetached(true);

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

        lastCamSummary = String.format(
            "slot=%d near=%.2f far=%.1f l=%.2f r=%.2f b=%.2f t=%.2f eye=(%.1f,%.1f,%.1f)",
            slot.index, near, far, l, r, b, t, reflectedEye.x, reflectedEye.y, reflectedEye.z);

        activeTarget = slot.target;
        captureCenter = savedPos; // measure the terrain cap from the real eye, not the reflected one
        capturing = true;
        try {
            mc.gameRenderer.renderLevel(delta);
        } finally {
            capturing = false;
            activeTarget = null;
        }

        camMover.ironhold$setPosition(savedPos);
        camMover.ironhold$setRotation(savedYRot, savedXRot);
        camMover.ironhold$setDetached(savedDetached);
    }

    /**
     * Reflection resolution per pane-block, stepped down with distance. Coarse buckets (not a smooth
     * curve) so the GPU buffer is only reallocated when the player crosses a threshold, never every
     * frame as they walk toward or away from a mirror.
     */
    private static int pixelsPerBlockFor(double dist) {
        if (dist < 12.0) {
            return PX_PER_BLOCK; // 256
        }
        if (dist < 24.0) {
            return PX_PER_BLOCK * 3 / 4; // 192
        }
        if (dist < 40.0) {
            return PX_PER_BLOCK / 2; // 128
        }
        return PX_PER_BLOCK / 4; // 64
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
            this.textureId = Identifier.fromNamespaceAndPath(Ironhold.MODID, "mirror_reflection_" + index);
        }

        void ensureSize(int blocksW, int blocksH, int pixelsPerBlock) {
            int w = Math.min(blocksW * pixelsPerBlock, MAX_DIM);
            int h = Math.min(blocksH * pixelsPerBlock, MAX_DIM);
            if (target == null) {
                target = new TextureTarget("ironhold_mirror_" + index, w, h, true);
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
