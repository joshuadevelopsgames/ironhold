package kingdom.smp.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kingdom.smp.block.TripwireRackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders the item a tripwire hook is holding (see {@link TripwireRackBlockEntity}).
 *
 * <p>Mirrors the look of the original "Item Racks" datapack, which hung the item
 * in an invisible armor stand's raised hand: the item is drawn in the held
 * ({@code THIRD_PERSON_RIGHT_HAND}) pose so it juts out from the wall in 3D, like
 * a tool resting on a peg. The block entity's {@code orientation} (0-3) spins it
 * in the wall plane so it hangs one of four ways: down, left, up, right.
 *
 * <p>The geometry dials live in {@link TripwireRackTuning} so they can be nudged live in-game with
 * {@code /rackdebug} — tweak them if the item floats too far off the wall, sits too high/low, or
 * doesn't start "hanging down" at orientation 0. The item hangs on the hook's <em>ring</em> (the
 * metal hole the tripwire string threads through); from the vanilla model that ring sits at roughly
 * block-fraction (0.5, 0.49, 0.60) — just below the block's vertical centre and tucked toward the
 * wall — so {@code hookY} + {@code hang} place the item's hang point there.
 */
public class TripwireRackRenderer
    implements BlockEntityRenderer<TripwireRackBlockEntity, TripwireRackRenderer.RenderState> {

    public TripwireRackRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static final class RenderState extends BlockEntityRenderState {
        public ItemStack item = ItemStack.EMPTY;
        public Direction facing = Direction.NORTH;
        public int orientation = 0;
        public int seed = 0;
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(
        TripwireRackBlockEntity be, RenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.item = be.getItem();
        state.orientation = be.getOrientation();
        BlockState bs = be.getBlockState();
        state.facing = bs.hasProperty(TripWireHookBlock.FACING)
            ? bs.getValue(TripWireHookBlock.FACING)
            : Direction.NORTH;
        // Stable per-position seed so randomised item models don't flicker.
        state.seed = (int) be.getBlockPos().asLong();
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        Direction facing = state.facing;
        if (facing.getAxis().isVertical()) return; // hooks are always wall-mounted

        // Live dials (see TripwireRackTuning / the /rackdebug command). Read once per frame.
        final float hookY = TripwireRackTuning.hookY;
        final float hang = TripwireRackTuning.hang;
        final float outLift = TripwireRackTuning.outLift;
        final float jutPitch = TripwireRackTuning.jutPitch;
        final float hangRollBase = TripwireRackTuning.hangRollBase;
        final float scale = TripwireRackTuning.scale;

        poseStack.pushPose();

        // Pivot point: cell centre horizontally, at hook height.
        poseStack.translate(0.5f, hookY, 0.5f);

        // Face out of the wall — after this, local +Z points away from the wall
        // (item-frame math; horizontal facings only need the yaw term).
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - facing.toYRot()));

        // Hang direction: spin in the wall plane (down → left → up → right).
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.orientation * 90.0f + hangRollBase));

        // Tilt out of the wall plane so the item juts off the hook in 3D.
        poseStack.mulPose(Axis.XP.rotationDegrees(jutPitch));

        // Dangle below the (now-rotated) pivot and push out off the wall.
        poseStack.translate(0.0f, -hang, outLift);
        poseStack.scale(scale, scale, scale);

        Minecraft mc = Minecraft.getInstance();
        ItemStackRenderState itemState = new ItemStackRenderState();
        // THIRD_PERSON_RIGHT_HAND = the "held tool" pose the datapack's armor
        // stand used, giving the item real 3D depth as it hangs on the hook.
        mc.getItemModelResolver().updateForTopItem(
            itemState, state.item, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, mc.level, null, state.seed);
        itemState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TripwireRackBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.5);
    }
}
