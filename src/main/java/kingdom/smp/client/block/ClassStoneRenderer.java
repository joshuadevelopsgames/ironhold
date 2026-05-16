package kingdom.smp.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.block.ClassStoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders a hovering, slowly-rotating, gently-bobbing item above a Class
 * Stone — same trick vanilla uses for the enchanting table's book, but with
 * an item carousel instead of one model.
 *
 * <p>Cycles through {@link ClassStoneBlockEntity#carousel()} every
 * {@link ClassStoneBlockEntity#CYCLE_TICKS} ticks, with a quick scale-fade
 * during {@link ClassStoneBlockEntity#FADE_TICKS} between slots so the swap
 * doesn't pop.
 */
public class ClassStoneRenderer
    implements BlockEntityRenderer<ClassStoneBlockEntity, ClassStoneRenderer.RenderState> {

    public ClassStoneRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static final class RenderState extends BlockEntityRenderState {
        public float time;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        ClassStoneBlockEntity be, RenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.time = be.time + partialTicks;
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ItemStack[] carousel = ClassStoneBlockEntity.carousel();
        int holdTicks = ClassStoneBlockEntity.TICKS_PER_SLOT;
        int fadeTicks = ClassStoneBlockEntity.FADE_TICKS;

        // Which slot is "active" right now and how far through its window we are.
        float t = state.time;
        int slot = ((int) t / holdTicks) % carousel.length;
        float intoSlot = t - (((int) t / holdTicks) * holdTicks);

        // Scale-fade between adjacent items during the last `fadeTicks` of each slot.
        ItemStack outgoing = carousel[slot];
        ItemStack incoming = carousel[(slot + 1) % carousel.length];
        float scaleOut = 1.0F;
        float scaleIn  = 0.0F;
        if (intoSlot >= holdTicks - fadeTicks) {
            float p = (intoSlot - (holdTicks - fadeTicks)) / fadeTicks; // 0 → 1
            scaleOut = 1.0F - p;
            scaleIn  = p;
        }

        // Bob and slow yaw spin shared between active items.
        float bob = Mth.sin(state.time * 0.10F) * 0.06F;
        float yaw = state.time * 4.0F; // degrees per tick

        // Peak scale (1.0 = full vanilla item size). Items are visible at this
        // size during the hold and shrink-fade in/out during transitions.
        final float PEAK_SCALE = 1.1F;

        // Outgoing item
        if (scaleOut > 0.001F) {
            renderHovering(outgoing, poseStack, collector, state, bob, yaw, scaleOut * PEAK_SCALE);
        }
        // Incoming item
        if (scaleIn > 0.001F) {
            renderHovering(incoming, poseStack, collector, state, bob, yaw, scaleIn * PEAK_SCALE);
        }
    }

    private void renderHovering(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                                RenderState state, float bob, float yawDeg, float scale) {
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        // Slab top is at 0.75; sits a small gap above so items hover but stay
        // visually anchored to the stone.
        poseStack.translate(0.5F, 1.10F + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.scale(scale, scale, scale);

        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState itemState = new ItemStackRenderState();
        // GROUND context renders the item upright like a dropped item — looks
        // right floating in midair. Seed includes time so animated items
        // (e.g. potions, enchanted glint) keep frame-consistent randomness.
        mc.getItemModelResolver().updateForTopItem(
            itemState, stack, ItemDisplayContext.GROUND,
            mc.level, null, (int) state.time
        );
        itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ClassStoneBlockEntity be) {
        net.minecraft.core.BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
