package kingdom.smp.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import kingdom.smp.block.ButterflyJarBlockEntity;
import kingdom.smp.entity.ButterflyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the butterflies held inside a placed Butterfly Jar as their real GeckoLib mob
 * model — one persistent, world-less dummy {@link ButterflyEntity} per slot (owned by the
 * block entity), drawn via the entity render dispatcher. Each butterfly drifts gently inside
 * the glass. Using a distinct instance per slot keeps the GeckoLib idle animation running
 * even when several share the same species (a single shared instance freezes).
 */
public class ButterflyJarRenderer
    implements BlockEntityRenderer<ButterflyJarBlockEntity, ButterflyJarRenderer.RenderState> {

    /** Up to three resting spots inside the jar interior (block-local coords). */
    private static final float[][] SLOTS = {
        {0.50F, 0.28F, 0.50F},
        {0.43F, 0.17F, 0.55F},
        {0.57F, 0.40F, 0.45F},
    };

    public ButterflyJarRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static final class RenderState extends BlockEntityRenderState {
        public final List<ButterflyEntity> mobs = new ArrayList<>(ButterflyJarBlockEntity.MAX_BUTTERFLIES);
        public float time;
        public float partialTick;
        public double worldX, worldY, worldZ;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        ButterflyJarBlockEntity be, RenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.mobs.clear();
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        state.time = gameTime + partialTicks;
        state.partialTick = partialTicks;
        net.minecraft.core.BlockPos pos = be.getBlockPos();
        state.worldX = pos.getX() + 0.5;
        state.worldY = pos.getY() + 0.4;
        state.worldZ = pos.getZ() + 0.5;

        int count = be.getContents().size();
        for (int i = 0; i < count && i < SLOTS.length; i++) {
            ButterflyEntity mob = be.displayMob(i);
            if (mob == null) continue;
            // Advance the idle flutter (1 per tick), phase-shifted per slot so they don't sync.
            mob.tickCount = (int) gameTime + i * 13;
            mob.setPos(state.worldX, state.worldY, state.worldZ);  // light sampled at the jar
            state.mobs.add(mob);
        }
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int count = state.mobs.size();
        if (count == 0) return;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        for (int i = 0; i < count; i++) {
            ButterflyEntity mob = state.mobs.get(i);
            float[] slot = SLOTS[i];
            // Slight wandering flight inside the jar (small — at full mob scale the butterfly
            // nearly fills the glass).
            float t = state.time;
            float dx = Mth.sin(t * 0.045F + i * 2.0F) * 0.018F;
            float dy = Mth.sin(t * 0.070F + i * 1.1F) * 0.022F;
            float dz = Mth.cos(t * 0.055F + i * 0.6F) * 0.018F;
            float yaw = t * 1.6F + i * 120F;

            // Scale + offset are tunable live via /jardebug.
            float s = kingdom.smp.client.ButterflyJarScaleDebug.scale();
            float ox = kingdom.smp.client.ButterflyJarScaleDebug.offsetX();
            float oy = kingdom.smp.client.ButterflyJarScaleDebug.offsetY();
            float oz = kingdom.smp.client.ButterflyJarScaleDebug.offsetZ();

            var renderState = dispatcher.extractEntity(mob, state.partialTick);
            poseStack.pushPose();
            poseStack.translate(slot[0] + dx + ox, slot[1] + dy + oy, slot[2] + dz + oz);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            // The entity render path already applies the mob's own scaleModelForRender; this
            // factor (default 0.6, tune with /jardebug scale) trims it to match the in-world mob.
            poseStack.scale(s, s, s);
            dispatcher.submit(renderState, camera, 0.0, 0.0, 0.0, poseStack, collector);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(ButterflyJarBlockEntity be) {
        net.minecraft.core.BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
