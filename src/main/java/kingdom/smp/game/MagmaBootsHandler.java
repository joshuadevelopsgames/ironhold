package kingdom.smp.game;

import kingdom.smp.ModBlocks;
import kingdom.smp.ModItems;
import kingdom.smp.block.MagmaCrustBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Per-tick gameplay behaviour for the Magma Boots, driven server-side from
 * {@link kingdom.smp.IronholdGameEvents#onPlayerTickPost}. While the wearer has
 * the boots in the FEET slot they:
 * <ul>
 *   <li>trail little puffs of smoke behind them while moving,</li>
 *   <li>melt ice and snow they walk over (into water / air, with a steam puff),</li>
 *   <li>walk on lava — laying a temporary, self-reverting magma crust over lava
 *       sources around their feet (Frost-Walker style).</li>
 * </ul>
 * All three are cheap no-ops when the boots aren't worn, and the lava/ice scans
 * only touch a tiny radius around the feet.
 */
public final class MagmaBootsHandler {
    private MagmaBootsHandler() {}

    /** 5x5 footprint of lava that gets crusted under/around the feet. */
    private static final int LAVA_RADIUS = 2;
    /** 3x3x3 box of ice/snow melted around the feet. */
    private static final int MELT_RADIUS = 1;
    /** Min horizontal movement per tick (squared) to count as "walking". */
    private static final double MOVE_THRESHOLD_SQR = 0.0025; // ~0.05 blocks/tick
    /** Emit a smoke puff every N ticks while moving (puffs, not a stream). */
    private static final int SMOKE_INTERVAL = 3;

    public static void tick(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.FEET).getItem() != ModItems.MAGMA_BOOTS.get()) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();

        smokeTrail(level, player);
        meltIce(level, feet);
        walkOnLava(level, player, feet);
    }

    /** Little puffs of smoke trailing behind the wearer as they move. */
    private static void smokeTrail(ServerLevel level, ServerPlayer player) {
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        double movedSqr = dx * dx + dz * dz;
        if (movedSqr < MOVE_THRESHOLD_SQR) return;
        if (player.tickCount % SMOKE_INTERVAL != 0) return;

        // Bias the puff slightly behind the direction of travel so it reads as a
        // trail rather than a cloud centred on the feet.
        double inv = 0.35 / Math.sqrt(movedSqr);
        double bx = player.getX() - dx * inv;
        double bz = player.getZ() - dz * inv;
        level.sendParticles(ParticleTypes.SMOKE, bx, player.getY() + 0.1, bz,
            2, 0.08, 0.02, 0.08, 0.01);
    }

    /** Melt nearby ice → water and clear snow, leaving a wisp of steam. */
    private static void meltIce(ServerLevel level, BlockPos feet) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ox = -MELT_RADIUS; ox <= MELT_RADIUS; ox++) {
            for (int oz = -MELT_RADIUS; oz <= MELT_RADIUS; oz++) {
                for (int oy = -1; oy <= 1; oy++) {
                    m.set(feet.getX() + ox, feet.getY() + oy, feet.getZ() + oz);
                    BlockState bs = level.getBlockState(m);
                    Block b = bs.getBlock();
                    if (b == Blocks.ICE || b == Blocks.FROSTED_ICE
                        || b == Blocks.PACKED_ICE || b == Blocks.BLUE_ICE) {
                        level.setBlockAndUpdate(m, Blocks.WATER.defaultBlockState());
                        steam(level, m);
                    } else if (b == Blocks.SNOW || b == Blocks.SNOW_BLOCK
                        || b == Blocks.POWDER_SNOW) {
                        level.setBlockAndUpdate(m, Blocks.AIR.defaultBlockState());
                        steam(level, m);
                    }
                }
            }
        }
    }

    private static void steam(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            4, 0.2, 0.1, 0.2, 0.02);
    }

    /** Frost-Walker for lava: crust lava sources under/around the feet. */
    private static void walkOnLava(ServerLevel level, ServerPlayer player, BlockPos feet) {
        Block crustBlock = ModBlocks.MAGMA_CRUST.get();
        BlockState crust = crustBlock.defaultBlockState();
        Vec3 center = player.position();
        for (BlockPos p : BlockPos.betweenClosed(
                feet.offset(-LAVA_RADIUS, -1, -LAVA_RADIUS),
                feet.offset(LAVA_RADIUS, -1, LAVA_RADIUS))) {
            if (!p.closerToCenterThan(center, LAVA_RADIUS + 0.5)) continue;
            // Need open space directly above the surface to stand in.
            if (!level.getBlockState(p.above()).isAir()) continue;

            BlockState below = level.getBlockState(p);
            if (below.is(Blocks.LAVA) && below.getFluidState().isSource()) {
                // onPlace schedules the first melt-back tick.
                level.setBlockAndUpdate(p.immutable(), crust);
            } else if (below.is(crustBlock) && below.getValue(MagmaCrustBlock.AGE) != 0) {
                // Refresh the platform under the wearer so it never melts out
                // from under their feet; a pending tick is already carried over.
                level.setBlock(p.immutable(), crust, 2);
            }
        }
    }
}
