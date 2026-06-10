package kingdom.smp.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import kingdom.smp.client.MirrorReflection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the world from going hole-y — or laggy/snappy — inside a mirror reflection.
 *
 * <p>Vanilla {@code cullTerrain} runs a directional occlusion BFS (smart cull) seeded from the camera's
 * section, and stores the result in a single, shared, asynchronously-rebuilt {@code SectionOcclusionGraph}.
 * Neither half suits a second camera: from the reflected eye (embedded behind the mirror's wall) the BFS
 * over-prunes, dropping the chunks behind the player out of the reflection; and reusing the main camera's
 * graph makes the reflection snap (it only refreshes in async steps) and draws its whole pruned set.
 *
 * <p>For the capture pass only, this skips the graph entirely. It pulls the loaded sections in a small box
 * around the player straight from {@link ViewArea} (always current → no snap) and keeps the ones the
 * mirror's actual view cone touches — the narrow off-axis frustum set up just before this render, not the
 * wide main-camera frustum (→ few sections, no lag). No occlusion test, so nothing in that box is dropped
 * (→ no holes). Cancelling at HEAD leaves the persistent occlusion graph and camera deltas untouched, so
 * the next main-view frame is undisturbed.
 */
@Mixin(LevelRenderer.class)
public abstract class MirrorTerrainCullMixin {

    @Shadow
    @Nullable
    private ViewArea viewArea;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections;

    @Inject(method = "cullTerrain", at = @At("HEAD"), cancellable = true)
    private void ironhold$mirrorBoxedCull(Camera camera, Frustum frustum, boolean spectator, CallbackInfo ci) {
        ViewArea area = this.viewArea;
        if (!MirrorReflection.isCapturing() || area == null) {
            return;
        }

        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();

        // The mirror's true view cone: the off-axis projection set on the camera state just before this
        // renderLevel (see MirrorReflection.captureInto), prepared at the reflected eye. Far narrower than
        // the wide main-camera cull frustum, so the boxed set stays small.
        CameraRenderState cam = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        Frustum cone = new Frustum(cam.viewRotationMatrix, cam.projectionMatrix);
        Vec3 eye = camera.position();
        cone.prepare(eye.x, eye.y, eye.z);

        Vec3 c = MirrorReflection.captureCenter();
        int r = MirrorReflection.capChunks();
        int cx = SectionPos.posToSectionCoord(c.x);
        int cy = SectionPos.posToSectionCoord(c.y);
        int cz = SectionPos.posToSectionCoord(c.z);
        ViewAreaInvoker access = (ViewAreaInvoker) area;

        for (int sx = cx - r; sx <= cx + r; sx++) {
            for (int sy = cy - r; sy <= cy + r; sy++) {
                for (int sz = cz - r; sz <= cz + r; sz++) {
                    SectionRenderDispatcher.RenderSection rs =
                        access.ironhold$getRenderSection(SectionPos.asLong(sx, sy, sz));
                    if (rs == null || !cone.isVisible(rs.getBoundingBox())) {
                        continue;
                    }
                    this.visibleSections.add(rs);
                    if (Math.abs(sx - cx) <= 1 && Math.abs(sy - cy) <= 1 && Math.abs(sz - cz) <= 1) {
                        this.nearbyVisibleSections.add(rs);
                    }
                }
            }
        }
        ci.cancel();
    }
}
