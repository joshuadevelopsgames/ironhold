package kingdom.smp.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kingdom.smp.entity.SpellBeamEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class SpellBeamRenderer extends EntityRenderer<SpellBeamEntity, SpellBeamRenderState> {

    private static final Identifier TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");

    private static final float INNER_HALF  = 0.04f;
    private static final float MID_HALF    = 0.08f;
    private static final float OUTER_HALF  = 0.14f;

    public SpellBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public SpellBeamRenderState createRenderState() {
        return new SpellBeamRenderState();
    }

    @Override
    public void extractRenderState(SpellBeamEntity entity, SpellBeamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // Origin offset: (originWorld - entityWorld)
        state.originDX = entity.getOriginX() - (float) entity.getX();
        state.originDY = entity.getOriginY() - (float) entity.getY();
        state.originDZ = entity.getOriginZ() - (float) entity.getZ();

        int color = entity.getBeamColor();
        state.beamR = ((color >> 16) & 0xFF) / 255.0f;
        state.beamG = ((color >> 8)  & 0xFF) / 255.0f;
        state.beamB = ( color        & 0xFF) / 255.0f;

        int maxLife = entity.getMaxLife();
        float life  = entity.tickCount + partialTick;
        // Fade in over first 2 ticks, fade out over last 6 ticks
        float t = life / maxLife;
        state.alpha = t < (2f / maxLife) ? (life / 2f)
                    : t > (1f - 6f / maxLife) ? ((maxLife - life) / 6f)
                    : 1.0f;
        state.alpha = Math.max(0f, Math.min(1f, state.alpha));
    }

    @Override
    public void submit(SpellBeamRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.alpha <= 0.005f) return;

        final float ox = state.originDX;
        final float oy = state.originDY;
        final float oz = state.originDZ;
        final float r  = state.beamR;
        final float g  = state.beamG;
        final float b  = state.beamB;
        final float a  = state.alpha;
        final int light  = state.lightCoords;
        final float uvShift = state.ageInTicks * 0.05f % 1.0f;
        final float camX = (float)(camera.pos.x - state.x);
        final float camY = (float)(camera.pos.y - state.y);
        final float camZ = (float)(camera.pos.z - state.z);

        var rt = RenderTypes.beaconBeam(TEXTURE, true);

        collector.submitCustomGeometry(pose, rt,
            (p, v) -> drawBeamBillboard(p, v, ox, oy, oz, camX, camY, camZ, r, g, b, a,        INNER_HALF, uvShift, light));
        collector.submitCustomGeometry(pose, rt,
            (p, v) -> drawBeamBillboard(p, v, ox, oy, oz, camX, camY, camZ, r, g, b, a * 0.5f,  MID_HALF,  uvShift, light));
        collector.submitCustomGeometry(pose, rt,
            (p, v) -> drawBeamBillboard(p, v, ox, oy, oz, camX, camY, camZ, r, g, b, a * 0.25f, OUTER_HALF, uvShift, light));
    }

    /**
     * Draws one billboard quad layer for the beam.
     * Origin is at (ox,oy,oz); end is at entity position (0,0,0) in pose-local space.
     * The quad is oriented perpendicular to both the beam axis and the camera view vector,
     * so it always faces the viewer.
     */
    private static void drawBeamBillboard(PoseStack.Pose pose, VertexConsumer v,
            float ox, float oy, float oz,
            float camX, float camY, float camZ,
            float r, float g, float b, float alpha,
            float halfWidth, float uvShift, int light) {

        // Beam axis: from end (0,0,0) toward origin
        float len = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (len < 0.05f) return;
        float adx = ox / len, ady = oy / len, adz = oz / len;

        // Midpoint of beam
        float midX = ox * 0.5f, midY = oy * 0.5f, midZ = oz * 0.5f;

        // Camera-to-midpoint direction
        float cmx = midX - camX, cmy = midY - camY, cmz = midZ - camZ;
        // Perpendicular = beamAxis × camToMid  (faces camera)
        float px = ady * cmz - adz * cmy;
        float py = adz * cmx - adx * cmz;
        float pz = adx * cmy - ady * cmx;
        float plen = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (plen < 0.001f) return;
        px = px / plen * halfWidth;
        py = py / plen * halfWidth;
        pz = pz / plen * halfWidth;

        // UV: u=0 left, u=1 right; v scrolls along beam length
        float v0 = uvShift;
        float v1 = uvShift + len * 0.15f;
        int ai = Math.round(alpha * 255);
        int ri = Math.round(r * 255), gi = Math.round(g * 255), bi = Math.round(b * 255);

        // Quad: origin+perp → end+perp → end-perp → origin-perp
        v.addVertex(pose, ox + px, oy + py, oz + pz).setColor(ri, gi, bi, ai).setUv(0f, v0).setLight(light).setNormal(0, 1, 0);
        v.addVertex(pose,      px,      py,      pz).setColor(ri, gi, bi, ai).setUv(0f, v1).setLight(light).setNormal(0, 1, 0);
        v.addVertex(pose,     -px,     -py,     -pz).setColor(ri, gi, bi, ai).setUv(1f, v1).setLight(light).setNormal(0, 1, 0);
        v.addVertex(pose, ox - px, oy - py, oz - pz).setColor(ri, gi, bi, ai).setUv(1f, v0).setLight(light).setNormal(0, 1, 0);
    }
}
