package kingdom.smp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Draws a brief translucent orange box over a block the moment it's honeycomb-waxed (driven by
 * {@link kingdom.smp.net.WaxOverlayPayload}), fading out over a few seconds. Purely cosmetic
 * feedback — the wax protection itself is server-side state. Registered on the client game bus.
 */
public final class WaxOverlayRenderer {
    private WaxOverlayRenderer() {}

    private static final float DURATION_TICKS = 50.0f; // ~2.5 s
    private static final float MAX_ALPHA = 0.55f;
    private static final int R = 255, G = 140, B = 0; // orange
    private static final double INFLATE = 0.012;       // sit just outside the block faces (no z-fight)

    /** pos -> client game-time tick when the wax was applied. Touched only on the client main thread. */
    private static final Map<BlockPos, Long> ACTIVE = new HashMap<>();

    /** Called from the network handler (client main thread) when a wax overlay packet arrives. */
    public static void add(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ACTIVE.put(pos.immutable(), mc.level.getGameTime());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;

        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType rt = RenderTypes.debugQuads(); // POSITION_COLOR, translucent, depth-tested, no cull
        VertexConsumer vc = buffers.getBuffer(rt);

        Iterator<Map.Entry<BlockPos, Long>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            float elapsed = (float) (now - entry.getValue());
            if (elapsed < 0 || elapsed >= DURATION_TICKS) {
                it.remove();
                continue;
            }
            int alpha = (int) ((1.0f - elapsed / DURATION_TICKS) * MAX_ALPHA * 255.0f);
            if (alpha > 0) {
                drawBox(pose.last(), vc, entry.getKey(), cam, alpha);
            }
        }
        buffers.endBatch(rt);
    }

    private static void drawBox(PoseStack.Pose pose, VertexConsumer vc, BlockPos pos, Vec3 cam, int a) {
        double x0 = pos.getX() - cam.x - INFLATE;
        double y0 = pos.getY() - cam.y - INFLATE;
        double z0 = pos.getZ() - cam.z - INFLATE;
        double x1 = pos.getX() + 1 - cam.x + INFLATE;
        double y1 = pos.getY() + 1 - cam.y + INFLATE;
        double z1 = pos.getZ() + 1 - cam.z + INFLATE;
        // Six faces; culling is off for debugQuads so winding doesn't matter.
        q(pose, vc, a, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // down
        q(pose, vc, a, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // up
        q(pose, vc, a, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        q(pose, vc, a, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        q(pose, vc, a, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        q(pose, vc, a, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }

    private static void q(PoseStack.Pose pose, VertexConsumer vc, int a,
                          double ax, double ay, double az, double bx, double by, double bz,
                          double cx, double cy, double cz, double dx, double dy, double dz) {
        v(pose, vc, ax, ay, az, a);
        v(pose, vc, bx, by, bz, a);
        v(pose, vc, cx, cy, cz, a);
        v(pose, vc, dx, dy, dz, a);
    }

    private static void v(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z, int a) {
        vc.addVertex(pose, (float) x, (float) y, (float) z).setColor(R, G, B, a);
    }
}
