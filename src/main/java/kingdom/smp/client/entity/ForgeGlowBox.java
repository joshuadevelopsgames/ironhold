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
 * Draws axis-aligned, full-bright (emissive) translucent boxes — used for the
 * Battle Hammer's orange forge-power glow over its inner-ring cubes. Each face is
 * emitted in both windings so the box is visible from any angle regardless of
 * cull direction (alpha is halved to compensate for the double blend).
 */
public final class ForgeGlowBox {
    private ForgeGlowBox() {}

    public static final Identifier GLOW_TEXTURE =
        Identifier.fromNamespaceAndPath(kingdom.smp.Ironhold.MODID, "textures/misc/forge_glow.png");

    public static void draw(PoseStack pose, SubmitNodeCollector collector, Identifier texture,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float cr, float cg, float cb, float alpha) {
        RenderType rt = RenderTypes.entityTranslucentEmissive(texture);
        collector.submitCustomGeometry(pose, rt,
            (p, v) -> box(p, v, x0, y0, z0, x1, y1, z1, cr, cg, cb, alpha));
    }

    private static void box(PoseStack.Pose pose, VertexConsumer v,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float cr, float cg, float cb, float alpha) {
        int r = c(cr), g = c(cg), b = c(cb), a = c(alpha * 0.5f); // halved: double-winding
        // Each face: 4 CCW verts, then 4 reversed — visible from both sides.
        // north (-Z)
        quad2(pose, v, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, r,g,b,a);
        // south (+Z)
        quad2(pose, v, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, r,g,b,a);
        // west (-X)
        quad2(pose, v, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, r,g,b,a);
        // east (+X)
        quad2(pose, v, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, r,g,b,a);
        // down (-Y)
        quad2(pose, v, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, r,g,b,a);
        // up (+Y)
        quad2(pose, v, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0, r,g,b,a);
    }

    private static void quad2(PoseStack.Pose pose, VertexConsumer v,
                              float ax,float ay,float az, float bx,float by,float bz,
                              float cx,float cy,float cz, float dx,float dy,float dz,
                              int r,int g,int b,int a) {
        vert(pose,v,ax,ay,az,0,0,r,g,b,a); vert(pose,v,bx,by,bz,1,0,r,g,b,a);
        vert(pose,v,cx,cy,cz,1,1,r,g,b,a); vert(pose,v,dx,dy,dz,0,1,r,g,b,a);
        // reversed winding
        vert(pose,v,dx,dy,dz,0,1,r,g,b,a); vert(pose,v,cx,cy,cz,1,1,r,g,b,a);
        vert(pose,v,bx,by,bz,1,0,r,g,b,a); vert(pose,v,ax,ay,az,0,0,r,g,b,a);
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z,
                             float u, float w, int r, int g, int b, int a) {
        v.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, w)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal(pose, 0, 1, 0);
    }

    private static int c(float f) { return Math.max(0, Math.min(255, Math.round(f * 255))); }
}
