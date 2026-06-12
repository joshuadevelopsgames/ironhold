package kingdom.smp.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kingdom.smp.block.ChaliceBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the liquid surface held inside a placed {@link ChaliceBlockEntity}: a small
 * tinted pool sitting just below the cup's rim, in the goblet's 2×2-pixel inner
 * well. The surface colour, opacity and emissivity come from whatever liquid was
 * poured in (water/honey/potion are translucent, lava glows full-bright).
 *
 * <p>A single near-white {@code chalice_liquid} sprite is multiplied by the liquid
 * tint, so one texture serves every liquid. The entity-translucent pipeline is
 * no-cull, so each face is emitted once and still reads from every angle.
 */
public class ChaliceRenderer
        implements BlockEntityRenderer<ChaliceBlockEntity, ChaliceRenderer.RenderState> {

    private static final Identifier LIQUID_TEX =
        Identifier.fromNamespaceAndPath("ironhold", "textures/block/chalice_liquid.png");

    // Inner well of the goblet, in block-local units (model px / 16). The cup's
    // rim ring leaves a 2×2 opening at x/z 7–9, solid below y6, open to y7; we
    // pool the liquid a touch inside the rim and just shy of the brim.
    // Clearances are a full 0.2px from every model face (bowl top y6, rim inner
    // walls x/z 7 & 9) — anything closer shimmers against the cup at distance.
    private static final float X0 = 7.2f  / 16f;
    private static final float X1 = 8.8f  / 16f;
    private static final float Z0 = 7.2f  / 16f;
    private static final float Z1 = 8.8f  / 16f;
    private static final float Y_FLOOR   = 6.20f / 16f;
    private static final float Y_SURFACE = 6.85f / 16f;

    public ChaliceRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(ChaliceBlockEntity be, RenderState state, float partialTicks,
                                   Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.empty = be.isEmpty();
        int rgb = be.rgb();
        state.r = (rgb >> 16) & 0xFF;
        state.g = (rgb >> 8) & 0xFF;
        state.b = rgb & 0xFF;
        state.a = Math.max(0, Math.min(255, Math.round(be.alpha() * 255f)));
        state.emissive = be.emissive();
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.empty) return;

        final int r = state.r, g = state.g, b = state.b, a = state.a;
        final int light = state.emissive ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
        final RenderType rt = state.emissive
            ? RenderTypes.entityTranslucentEmissive(LIQUID_TEX)
            : RenderTypes.entityTranslucent(LIQUID_TEX);

        collector.submitCustomGeometry(pose, rt, (p, v) -> drawPool(p, v, r, g, b, a, light));
    }

    private static void drawPool(PoseStack.Pose pose, VertexConsumer v,
                                 int r, int g, int b, int a, int light) {
        // Top surface (faces up).
        quad(pose, v, r, g, b, a, light, 0f, 1f, 0f,
            X0, Y_SURFACE, Z1,  X1, Y_SURFACE, Z1,  X1, Y_SURFACE, Z0,  X0, Y_SURFACE, Z0);
        // Four thin walls floor→surface so the pool has a little body from any angle.
        quad(pose, v, r, g, b, a, light, 0f, 0f, -1f,
            X1, Y_SURFACE, Z0,  X0, Y_SURFACE, Z0,  X0, Y_FLOOR, Z0,  X1, Y_FLOOR, Z0);   // north (-Z)
        quad(pose, v, r, g, b, a, light, 0f, 0f, 1f,
            X0, Y_SURFACE, Z1,  X1, Y_SURFACE, Z1,  X1, Y_FLOOR, Z1,  X0, Y_FLOOR, Z1);   // south (+Z)
        quad(pose, v, r, g, b, a, light, -1f, 0f, 0f,
            X0, Y_SURFACE, Z0,  X0, Y_SURFACE, Z1,  X0, Y_FLOOR, Z1,  X0, Y_FLOOR, Z0);   // west (-X)
        quad(pose, v, r, g, b, a, light, 1f, 0f, 0f,
            X1, Y_SURFACE, Z1,  X1, Y_SURFACE, Z0,  X1, Y_FLOOR, Z0,  X1, Y_FLOOR, Z1);   // east (+X)
    }

    /**
     * Emit a quad ONCE. The entity-translucent pipeline does NOT backface-cull
     * (26.1 made no-cull the entity default), so a single winding is already
     * visible from both sides — and the old double emission put two identical
     * coplanar quads in the buffer, which z-fought each other (visible flicker
     * on the liquid) and double-blended the alpha.
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer v,
                              int r, int g, int b, int a, int light,
                              float nx, float ny, float nz,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float x3, float y3, float z3, float x4, float y4, float z4) {
        vert(pose, v, x1, y1, z1, 0f, 0f, r, g, b, a, light, nx, ny, nz);
        vert(pose, v, x2, y2, z2, 1f, 0f, r, g, b, a, light, nx, ny, nz);
        vert(pose, v, x3, y3, z3, 1f, 1f, r, g, b, a, light, nx, ny, nz);
        vert(pose, v, x4, y4, z4, 0f, 1f, r, g, b, a, light, nx, ny, nz);
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer v,
                             float x, float y, float z, float u, float w,
                             int r, int g, int b, int a, int light,
                             float nx, float ny, float nz) {
        v.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, w)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }

    @Override
    public AABB getRenderBoundingBox(ChaliceBlockEntity be) {
        var p = be.getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1);
    }

    public static class RenderState extends BlockEntityRenderState {
        public boolean empty = true;
        public int r, g, b, a;
        public boolean emissive;
    }
}
