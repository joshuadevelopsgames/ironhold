package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kingdom.smp.block.wardheart.WardheartBlockEntity;
import kingdom.smp.block.wardheart.WardheartTier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
 * Renders the translucent purple force-field dome around an active Chorus Wardheart.
 * Uses two layers:
 *   1. A translucent fill of the dome via end-portal-textured triangles, additively blended
 *   2. A hex-grid wireframe of latitude/longitude line segments overlaid on the surface
 *
 * Impact ripples are visualized by brightening cells inside an expanding ring around the
 * impact point.
 */
public class WardheartBlockRenderer
        implements BlockEntityRenderer<WardheartBlockEntity, WardheartBlockRenderer.RenderState> {

    /** Latitude rings from south pole (-PI/2) to north pole (+PI/2) — full sphere
     *  so the shield protects underground areas as well as above-ground. */
    private static final int DOME_LAT_SEGMENTS = 24;
    private static final int DOME_LON_SEGMENTS = 32;

    /** Vanilla animated nether portal texture: 16x512 = 32 stacked 16x16 frames.
     *  We sample one frame at a time (V clamped to a 1/32 slice) and advance the
     *  frame index over time to animate. */
    private static final Identifier DOME_TEXTURE =
        Identifier.withDefaultNamespace("textures/block/nether_portal.png");
    private static final int NETHER_PORTAL_FRAMES = 32;
    private static final float FRAME_HEIGHT = 1f / NETHER_PORTAL_FRAMES;

    public WardheartBlockRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(WardheartBlockEntity be, RenderState state, float partialTicks,
                                    Vec3 cameraPosition,
                                    ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.tier = be.getTier();
        state.radius = state.tier.radius();
        state.colorR = ((state.tier.color() >> 16) & 0xFF) / 255f;
        state.colorG = ((state.tier.color() >> 8)  & 0xFF) / 255f;
        state.colorB = ( state.tier.color()         & 0xFF) / 255f;
        state.alpha = state.tier.alpha();
        state.phase = be.getClientPhase() + partialTicks * 0.001f;
        state.impactX = be.getImpactX();
        state.impactY = be.getImpactY();
        state.impactZ = be.getImpactZ();
        state.impactProgress = be.getImpactProgress();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(WardheartBlockEntity be) {
        var p = be.getBlockPos();
        int r = Math.max(2, be.getTier().radius());
        return new AABB(
            p.getX() + 0.5 - r, p.getY() + 0.5 - r, p.getZ() + 0.5 - r,
            p.getX() + 0.5 + r, p.getY() + 0.5 + r, p.getZ() + 0.5 + r);
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector,
                        CameraRenderState camera) {
        if (state.tier == WardheartTier.DORMANT || state.radius <= 0) return;

        // Translate from block position to block-center
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        final float r       = state.radius;
        final float cr      = state.colorR;
        final float cg      = state.colorG;
        final float cb      = state.colorB;
        final float baseA   = state.alpha;
        final float pulse   = 0.85f + 0.15f * (float) Math.sin(state.phase * Math.PI * 2);
        final float a       = baseA * pulse;
        final int   light   = state.lightCoords;

        // entityTranslucentEmissive on the animated nether_portal texture — emissive
        // means the dome stays bright at night; the texture's own .mcmeta cycles
        // frames; vertex color tints it toward the tier hue.
        final RenderType filledRT = RenderTypes.entityTranslucentEmissive(DOME_TEXTURE);

        final float ix = state.impactX;
        final float iy = state.impactY;
        final float iz = state.impactZ;
        final float impactT = state.impactProgress;
        final float phase   = state.phase;

        // Single layer: animated nether-portal swirl, tier-tinted, translucent.
        // Emit triangles in BOTH winding orders — entityTranslucent does cull
        // backfaces, so we need both windings to be visible from inside and outside.
        collector.submitCustomGeometry(pose, filledRT,
            (p, v) -> drawDomeFilled(p, v, r, cr, cg, cb, a, ix, iy, iz, impactT, phase));

        pose.popPose();
    }

    /**
     * Tessellated full sphere — protects the wardheart's volume both above and
     * below ground. Emits ONE QUAD (4 vertices) per cell, not two triangles —
     * the entityTranslucent pipeline uses VertexFormat.Mode.QUADS, so submitting
     * 6 vertices per cell would silently drop 2 of them as leftover, which was
     * the cause of the alternating-triangle visual bug.
     *
     * Inward winding (CCW when viewed from inside the sphere). entityTranslucent
     * backface-culls, so only inward triangles render — that's what we want for
     * a player standing inside the shield.
     *
     * Texture is the animated nether_portal swirl (32 frames stacked vertically).
     * We sample exactly ONE frame at a time, advancing through frames over time
     * via `frameV`. UV horizontal repetition tiles the swirl around the sphere.
     */
    private static void drawDomeFilled(PoseStack.Pose pose, VertexConsumer v, float r,
                                        float cr, float cg, float cb, float a,
                                        float ix, float iy, float iz, float impactT,
                                        float phase) {
        final int aBase = clamp255(a * 255);
        final int rI = clamp255(cr * 255);
        final int gI = clamp255(cg * 255);
        final int bI = clamp255(cb * 255);
        final int light = LightCoordsUtil.FULL_BRIGHT;

        // Animation: cycle through the 32 frames slowly. `phase` wraps 0..1 every
        // ~83 client ticks (~4 sec). Multiplier 16 → full 32-frame cycle takes
        // 32/16 = 2 phase units = ~166 ticks = ~8.3 seconds. Roughly half vanilla
        // nether-portal speed for a more "drifting energy" feel.
        int frameIndex = ((int) (phase * NETHER_PORTAL_FRAMES * 0.5f)) % NETHER_PORTAL_FRAMES;
        if (frameIndex < 0) frameIndex += NETHER_PORTAL_FRAMES;
        final float frameV0 = frameIndex * FRAME_HEIGHT;
        final float frameV1 = frameV0 + FRAME_HEIGHT;
        // Slow horizontal drift so the swirl appears to rotate around the sphere.
        final float uDrift = phase * 0.4f;

        for (int lat = 0; lat < DOME_LAT_SEGMENTS; lat++) {
            // Full sphere: phi from -PI/2 (south pole) to +PI/2 (north pole).
            float phi0 = (float) Math.PI * (lat       / (float) DOME_LAT_SEGMENTS - 0.5f);
            float phi1 = (float) Math.PI * ((lat + 1) / (float) DOME_LAT_SEGMENTS - 0.5f);
            for (int lon = 0; lon < DOME_LON_SEGMENTS; lon++) {
                float th0 = (float) (Math.PI * 2) * (lon       / (float) DOME_LON_SEGMENTS);
                float th1 = (float) (Math.PI * 2) * ((lon + 1) / (float) DOME_LON_SEGMENTS);

                float x00 = r * (float) (Math.cos(phi0) * Math.cos(th0));
                float y00 = r * (float) Math.sin(phi0);
                float z00 = r * (float) (Math.cos(phi0) * Math.sin(th0));
                float x01 = r * (float) (Math.cos(phi0) * Math.cos(th1));
                float y01 = r * (float) Math.sin(phi0);
                float z01 = r * (float) (Math.cos(phi0) * Math.sin(th1));
                float x10 = r * (float) (Math.cos(phi1) * Math.cos(th0));
                float y10 = r * (float) Math.sin(phi1);
                float z10 = r * (float) (Math.cos(phi1) * Math.sin(th0));
                float x11 = r * (float) (Math.cos(phi1) * Math.cos(th1));
                float y11 = r * (float) Math.sin(phi1);
                float z11 = r * (float) (Math.cos(phi1) * Math.sin(th1));

                int a00 = scaledAlpha(aBase, x00, y00, z00, ix, iy, iz, impactT);
                int a01 = scaledAlpha(aBase, x01, y01, z01, ix, iy, iz, impactT);
                int a10 = scaledAlpha(aBase, x10, y10, z10, ix, iy, iz, impactT);
                int a11 = scaledAlpha(aBase, x11, y11, z11, ix, iy, iz, impactT);

                // Each cell shows one full frame of the swirl horizontally tiled.
                float u0  = lon       + uDrift;
                float u1  = lon + 1   + uDrift;
                float v0t = frameV0;
                float v1t = frameV1;

                // 4 vertices per cell, CCW from inside (inward-facing).
                // QUADS mode: GPU rasterizes (V0, V1, V2) and (V0, V2, V3).
                emitTintVertex(pose, v, x11, y11, z11, u1, v1t, rI, gI, bI, a11, light);
                emitTintVertex(pose, v, x10, y10, z10, u0, v1t, rI, gI, bI, a10, light);
                emitTintVertex(pose, v, x00, y00, z00, u0, v0t, rI, gI, bI, a00, light);
                emitTintVertex(pose, v, x01, y01, z01, u1, v0t, rI, gI, bI, a01, light);
            }
        }
    }

    private static void emitTintVertex(PoseStack.Pose pose, VertexConsumer v,
                                        float x, float y, float z, float u, float vTex,
                                        int r, int g, int b, int a, int light) {
        v.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, vTex)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, x, y, z);
    }

    /**
     * Computes alpha boost for a vertex based on proximity to a moving impact-ripple ring.
     */
    private static int scaledAlpha(int baseAlpha, float vx, float vy, float vz,
                                    float ix, float iy, float iz, float impactT) {
        if (impactT < 0f || impactT > 1f) return baseAlpha;
        float dx = vx - ix, dy = vy - iy, dz = vz - iz;
        float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ringDist = impactT * 8f;
        float falloff = Math.max(0f, 1f - Math.abs(d - ringDist) / 1.6f);
        falloff *= (1f - impactT);
        int boost = Math.round(255 * falloff * 0.85f);
        return Math.min(255, baseAlpha + boost);
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }

    public static class RenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public WardheartTier tier = WardheartTier.DORMANT;
        public float radius;
        public float colorR, colorG, colorB, alpha;
        public float phase;
        public float impactX, impactY, impactZ, impactProgress;
    }
}
