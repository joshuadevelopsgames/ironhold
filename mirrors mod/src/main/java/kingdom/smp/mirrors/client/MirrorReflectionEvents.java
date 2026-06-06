package kingdom.smp.mirrors.client;

import java.util.Comparator;
import java.util.List;
import kingdom.smp.mirrors.entity.MirrorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Game-bus hook that captures the mirror reflections once per frame. Runs in {@link RenderFrameEvent.Post}
 * — after the main frame has fully rendered — so the second-camera passes never disturb the main view's
 * chunk culling. Captures the nearest mirrors in range (up to {@link MirrorReflection#MAX_SLOTS}),
 * regardless of facing, each into its own buffer; mirrors sample those buffers on the next frame (an
 * imperceptible ~1-frame delay). Capturing mirrors behind the player too is what lets a mirror seen
 * inside another mirror show its own live reflection — the recursive infinite-hallway effect.
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

        // All mirrors within range — NOT just the ones in front of us. A mirror behind the player is
        // invisible directly, but it's visible *inside* the mirror they're facing, and for the
        // infinite-hallway recursion that reflected mirror needs its own live buffer too. Nearest-first
        // + the MAX_SLOTS cap below keep the cost identical to before (at most MAX_SLOTS captures).
        List<MirrorEntity> mirrors = mc.level
            .getEntitiesOfClass(MirrorEntity.class, mc.player.getBoundingBox().inflate(CAPTURE_RANGE));
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
