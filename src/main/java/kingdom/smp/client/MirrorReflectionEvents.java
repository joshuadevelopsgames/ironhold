package kingdom.smp.client;

import java.util.Comparator;
import java.util.List;
import kingdom.smp.entity.MirrorEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Game-bus hook that captures the mirror reflections once per frame. Runs in {@link RenderFrameEvent.Post}
 * — after the main frame has fully rendered — so the second-camera passes never disturb the main view's
 * chunk culling. Captures the nearest in-front mirrors (up to {@link MirrorReflection#MAX_SLOTS}), each
 * into its own buffer; mirrors sample those buffers on the next frame (an imperceptible ~1-frame delay).
 */
public final class MirrorReflectionEvents {
    private MirrorReflectionEvents() {}

    private static final double CAPTURE_RANGE = 64.0;

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 eye = mc.player.getEyePosition(pt);

        // Build the main camera's view frustum. Its matrices still hold the just-finished main frame's
        // values here — the reflection captures (which overwrite them) run later, inside renderFrame().
        Camera cam = mc.gameRenderer.getMainCamera();
        var cameraState = mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Frustum frustum = new Frustum(cameraState.viewRotationMatrix, cameraState.projectionMatrix);
        Vec3 camPos = cam.position();
        frustum.prepare(camPos.x, camPos.y, camPos.z);

        // Mirrors within range whose pane is actually on screen — a reflection you can't see costs a full
        // world re-render for nothing. The AABB is inflated so a mirror scrolling into view starts
        // capturing a frame early, avoiding a blank-surface pop as it crosses the screen edge.
        List<MirrorEntity> mirrors = mc.level
            .getEntitiesOfClass(MirrorEntity.class, mc.player.getBoundingBox().inflate(CAPTURE_RANGE),
                m -> frustum.isVisible(m.getBoundingBox().inflate(2.0)));
        if (mirrors.isEmpty()) {
            return;
        }
        // Nearest first, capped to the slot count.
        mirrors.sort(Comparator.comparingDouble(m -> m.distanceToSqr(eye.x, eye.y, eye.z)));
        if (mirrors.size() > MirrorReflection.MAX_SLOTS) {
            mirrors = mirrors.subList(0, MirrorReflection.MAX_SLOTS);
        }
        MirrorReflection.renderFrame(event.getPartialTick(), mirrors);
    }
}
