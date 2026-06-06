package kingdom.smp.mirrors.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import kingdom.smp.mirrors.entity.MirrorEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * Renders the mirror with the exact vanilla painting mesh — the same bordered frame (back + bevelled
 * edges drawn from the painting atlas {@code back} sprite) — but the front "art" face shows the
 * reflective surface instead of a painting variant. The surface is a placeholder texture for now;
 * Phase 2/3 swaps in the live reflection framebuffer.
 */
public class MirrorRenderer extends EntityRenderer<MirrorEntity, MirrorRenderState> {
    private static final Identifier BACK_SPRITE = Identifier.withDefaultNamespace("back");
    // Shown on mirrors that have no live reflection slot this frame (beyond the nearest few): a glassy
    // diagonal-shine surface so they read as a real mirror catching the light, not a see-through hole.
    private static final Identifier INACTIVE_SURFACE = Identifier.fromNamespaceAndPath(kingdom.smp.mirrors.Mirrors.MODID, "textures/block/mirror_surface.png");

    private static final float HALF_DEPTH = MirrorEntity.DEPTH / 2.0F; // ±0.03125, matches paintings
    private static final float REFLECTION_LIFT_Z_OFFSET = 0.0005F;
    private static final int REFLECTION_LIFT_COLOR = 0x30FFFFFF;

    private final TextureAtlas paintingsAtlas;

    public MirrorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.paintingsAtlas = context.getAtlas(AtlasIds.PAINTINGS);
    }

    @Override
    public MirrorRenderState createRenderState() {
        return new MirrorRenderState();
    }

    @Override
    public boolean shouldRender(MirrorEntity entity, Frustum culler, double camX, double camY, double camZ) {
        // Don't draw a mirror into its OWN reflection pass — its pane sits on the near plane, so it
        // would occlude the view and self-recurse. Other mirrors DO render here (sampling their own
        // buffers), giving recursive mirror-in-mirror reflections that build up a bounce per frame.
        if (kingdom.smp.mirrors.client.MirrorReflection.isCapturingMirror(entity.getId())) {
            return false;
        }
        boolean visible = super.shouldRender(entity, culler, camX, camY, camZ);
        if (kingdom.smp.mirrors.client.MirrorReflection.isCapturing()) {
            kingdom.smp.mirrors.client.MirrorReflection.diagShouldRender(visible);
        }
        return visible;
    }

    @Override
    public void extractRenderState(MirrorEntity entity, MirrorRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.direction = entity.getDirection();
        state.lightCoords = LevelRenderer.getLightCoords(entity.level(), entity.blockPosition());
        state.widthBlocks = entity.getWidthBlocks();
        state.heightBlocks = entity.getHeightBlocks();
        state.surfaceTexture = kingdom.smp.mirrors.client.MirrorReflection.textureIdFor(entity.getId());
    }

    @Override
    public void submit(MirrorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // Same orientation convention as paintings: face outward from the wall.
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.direction.get2DDataValue() * 90));
        int light = state.lightCoords;
        int w = state.widthBlocks;
        int h = state.heightBlocks;

        // Frame (back + bevelled edges) from the vanilla painting atlas "back" sprite.
        TextureAtlasSprite back = this.paintingsAtlas.getSprite(BACK_SPRITE);
        RenderType frameType = RenderTypes.entitySolidZOffsetForward(back.atlasLocation());
        collector.submitCustomGeometry(poseStack, frameType, (pose, buffer) -> renderFrame(pose, buffer, back, light, w, h));

        // Front "art" face: this mirror's own reflection buffer (mapped 0..1), or a static blank
        // surface if it has no slot this frame so it never renders as a see-through hole.
        boolean liveReflection = state.surfaceTexture != null;
        if (kingdom.smp.mirrors.client.MirrorReflection.isCapturing()) {
            kingdom.smp.mirrors.client.MirrorReflection.diagSubmit(liveReflection);
        }
        Identifier surface = liveReflection ? state.surfaceTexture : INACTIVE_SURFACE;
        // The live framebuffer already contains world lighting; don't apply the mirror block's light again.
        int surfaceLight = liveReflection ? LightCoordsUtil.FULL_BRIGHT : light;
        RenderType surfaceType = RenderTypes.entitySolid(surface);
        collector.submitCustomGeometry(poseStack, surfaceType,
            (pose, buffer) -> renderSurface(pose, buffer, w, h, -HALF_DEPTH, surfaceLight, -1));
        // The additive "lift" glaze is applied once, in the main view only. Skipping it inside a
        // capture keeps it from compounding bounce-over-bounce and blooming the tunnel toward white.
        if (liveReflection && !kingdom.smp.mirrors.client.MirrorReflection.isCapturing()) {
            RenderType liftType = RenderTypes.eyes(surface);
            collector.submitCustomGeometry(poseStack, liftType,
                (pose, buffer) -> renderSurface(pose, buffer, w, h, -HALF_DEPTH - REFLECTION_LIFT_Z_OFFSET,
                    LightCoordsUtil.FULL_BRIGHT, REFLECTION_LIFT_COLOR));
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void renderSurface(PoseStack.Pose pose, VertexConsumer buffer, int w, int h, float z,
                                      int light, int color) {
        float x0 = w / 2.0F;
        float x1 = -w / 2.0F;
        float y0 = h / 2.0F;
        float y1 = -h / 2.0F;
        // V is flipped (bottom=0, top=1): framebuffer textures are bottom-left origin, so the
        // captured image would otherwise appear upside down.
        vertex(pose, buffer, x0, y1, z, 1.0F, 0.0F, 0, 0, -1, light, color);
        vertex(pose, buffer, x1, y1, z, 0.0F, 0.0F, 0, 0, -1, light, color);
        vertex(pose, buffer, x1, y0, z, 0.0F, 1.0F, 0, 0, -1, light, color);
        vertex(pose, buffer, x0, y0, z, 1.0F, 1.0F, 0, 0, -1, light, color);
    }

    /** Back face and the four bevelled border edges — vertex math copied from PaintingRenderer. */
    private static void renderFrame(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite back, int light, int w, int h) {
        float offsetX = -w / 2.0F;
        float offsetY = -h / 2.0F;
        float backU0 = back.getU0();
        float backU1 = back.getU1();
        float backV0 = back.getV0();
        float backV1 = back.getV1();
        float tbU0 = back.getU0();
        float tbU1 = back.getU1();
        float tbV0 = back.getV0();
        float tbV1 = back.getV(0.0625F);
        float lrU0 = back.getU0();
        float lrU1 = back.getU(0.0625F);
        float lrV0 = back.getV0();
        float lrV1 = back.getV1();

        for (int sx = 0; sx < w; sx++) {
            for (int sy = 0; sy < h; sy++) {
                float x0 = offsetX + (sx + 1);
                float x1 = offsetX + sx;
                float y0 = offsetY + (sy + 1);
                float y1 = offsetY + sy;
                // Back face (normal +Z).
                vertex(pose, buffer, x0, y0, HALF_DEPTH, backU1, backV0, 0, 0, 1, light);
                vertex(pose, buffer, x1, y0, HALF_DEPTH, backU0, backV0, 0, 0, 1, light);
                vertex(pose, buffer, x1, y1, HALF_DEPTH, backU0, backV1, 0, 0, 1, light);
                vertex(pose, buffer, x0, y1, HALF_DEPTH, backU1, backV1, 0, 0, 1, light);
                if (sy == h - 1) { // top edge
                    vertex(pose, buffer, x0, y0, -HALF_DEPTH, tbU0, tbV0, 0, 1, 0, light);
                    vertex(pose, buffer, x1, y0, -HALF_DEPTH, tbU1, tbV0, 0, 1, 0, light);
                    vertex(pose, buffer, x1, y0, HALF_DEPTH, tbU1, tbV1, 0, 1, 0, light);
                    vertex(pose, buffer, x0, y0, HALF_DEPTH, tbU0, tbV1, 0, 1, 0, light);
                }
                if (sy == 0) { // bottom edge
                    vertex(pose, buffer, x0, y1, HALF_DEPTH, tbU0, tbV0, 0, -1, 0, light);
                    vertex(pose, buffer, x1, y1, HALF_DEPTH, tbU1, tbV0, 0, -1, 0, light);
                    vertex(pose, buffer, x1, y1, -HALF_DEPTH, tbU1, tbV1, 0, -1, 0, light);
                    vertex(pose, buffer, x0, y1, -HALF_DEPTH, tbU0, tbV1, 0, -1, 0, light);
                }
                if (sx == w - 1) { // right edge
                    vertex(pose, buffer, x0, y0, HALF_DEPTH, lrU1, lrV0, -1, 0, 0, light);
                    vertex(pose, buffer, x0, y1, HALF_DEPTH, lrU1, lrV1, -1, 0, 0, light);
                    vertex(pose, buffer, x0, y1, -HALF_DEPTH, lrU0, lrV1, -1, 0, 0, light);
                    vertex(pose, buffer, x0, y0, -HALF_DEPTH, lrU0, lrV0, -1, 0, 0, light);
                }
                if (sx == 0) { // left edge
                    vertex(pose, buffer, x1, y0, -HALF_DEPTH, lrU1, lrV0, 1, 0, 0, light);
                    vertex(pose, buffer, x1, y1, -HALF_DEPTH, lrU1, lrV1, 1, 0, 0, light);
                    vertex(pose, buffer, x1, y1, HALF_DEPTH, lrU0, lrV1, 1, 0, 0, light);
                    vertex(pose, buffer, x1, y0, HALF_DEPTH, lrU0, lrV0, 1, 0, 0, light);
                }
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                               float u, float v, int nx, int ny, int nz, int light) {
        vertex(pose, buffer, x, y, z, u, v, nx, ny, nz, light, -1);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                               float u, float v, int nx, int ny, int nz, int light, int color) {
        buffer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
