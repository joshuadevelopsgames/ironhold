package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws a translucent, emissive force-field sphere using the animated vanilla
 * nether-portal swirl, tinted to an arbitrary colour. Shared sphere-fill logic
 * adapted from {@link WardheartBlockRenderer} (minus the impact-ripple), so it
 * can be reused anywhere a {@link SubmitNodeCollector} is available — e.g. a
 * player render layer for the Diamond Scepter charge field.
 */
public final class ForcefieldDome {
    private ForcefieldDome() {}

    private static final int LAT_SEGMENTS = 24;
    private static final int LON_SEGMENTS = 32;

    public static final Identifier FIELD_TEXTURE =
        Identifier.fromNamespaceAndPath(kingdom.smp.Ironhold.MODID, "textures/misc/diamond_scepter_field.png");

    /** Bright white-cyan tint (emissive, so it reads as a glowing light blue). */
    public static final float R = 0.66f, G = 0.92f, B = 1.0f;

    /** Outer halo: a larger, fainter shell for a glow rim. */
    private static final float HALO_SCALE = 1.12f;
    private static final float HALO_ALPHA = 0.4f;

    public static RenderType renderType(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    /** Deferred path (renderers/layers with a SubmitNodeCollector): glowing inner shell + halo. */
    public static void drawGlowing(PoseStack pose, SubmitNodeCollector collector, Identifier texture,
                                   float radius, float alpha, float phase) {
        collector.submitCustomGeometry(pose, renderType(texture), (p, v) -> {
            fillSphere(p, v, radius, R, G, B, alpha, phase);
            fillSphere(p, v, radius * HALO_SCALE, R, G, B, alpha * HALO_ALPHA, phase);
        });
    }

    /** Immediate path (e.g. RenderLevelStageEvent): glowing inner shell + halo. */
    public static void fillGlowing(PoseStack.Pose pose, VertexConsumer v,
                                   float radius, float alpha, float phase) {
        fillSphere(pose, v, radius, R, G, B, alpha, phase);
        fillSphere(pose, v, radius * HALO_SCALE, R, G, B, alpha * HALO_ALPHA, phase);
    }

    /** Immediate path (e.g. RenderLevelStageEvent world rendering). */
    public static void fillSphere(PoseStack.Pose pose, VertexConsumer v, float r,
                                   float cr, float cg, float cb, float alpha, float phase) {
        final int rI = clamp255(cr * 255);
        final int gI = clamp255(cg * 255);
        final int bI = clamp255(cb * 255);
        final int aI = clamp255(alpha * 255);
        final int light = LightCoordsUtil.FULL_BRIGHT;

        // Slow vertical drift of the streak texture + horizontal swirl.
        final float vScroll = phase * 0.5f;
        final float uDrift = phase * 0.4f;

        for (int lat = 0; lat < LAT_SEGMENTS; lat++) {
            float phi0 = (float) Math.PI * (lat       / (float) LAT_SEGMENTS - 0.5f);
            float phi1 = (float) Math.PI * ((lat + 1) / (float) LAT_SEGMENTS - 0.5f);
            for (int lon = 0; lon < LON_SEGMENTS; lon++) {
                float th0 = (float) (Math.PI * 2) * (lon       / (float) LON_SEGMENTS);
                float th1 = (float) (Math.PI * 2) * ((lon + 1) / (float) LON_SEGMENTS);

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

                float u0  = lon       + uDrift;
                float u1  = lon + 1   + uDrift;
                float v0t = lat       + vScroll;
                float v1t = lat + 1   + vScroll;

                emit(pose, v, x11, y11, z11, u1, v1t, rI, gI, bI, aI, light);
                emit(pose, v, x10, y10, z10, u0, v1t, rI, gI, bI, aI, light);
                emit(pose, v, x00, y00, z00, u0, v0t, rI, gI, bI, aI, light);
                emit(pose, v, x01, y01, z01, u1, v0t, rI, gI, bI, aI, light);
            }
        }
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer v,
                             float x, float y, float z, float u, float vTex,
                             int r, int g, int b, int a, int light) {
        v.addVertex(pose, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, vTex)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, x, y, z);
    }

    private static int clamp255(float f) {
        return Math.max(0, Math.min(255, Math.round(f)));
    }
}
