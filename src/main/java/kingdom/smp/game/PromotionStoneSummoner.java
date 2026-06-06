package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.ModBlocks;
import kingdom.smp.block.ClassStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

/**
 * Summons a class-stone pillar for a player who has hit a promotion gate.
 *
 * <p>The pillar is {@code [stone_brick, stone_brick, class_stone]} bottom-to-top,
 * placed on a solid surface somewhere within {@value #RADIUS} blocks of the
 * overworld spawn. The {@code class_stone} block is owner-bound to the summoning
 * player (see {@link ClassStoneBlockEntity#setOwner}) and recorded in their
 * {@link ActivePromotionStone} attachment so it can be removed on promotion.
 */
public final class PromotionStoneSummoner {
    private PromotionStoneSummoner() {}

    /** Maximum horizontal distance from spawn the stone may appear. */
    public static final int RADIUS = 2000;
    /** Keep it at least this far from spawn so finding it is a small journey. */
    private static final int MIN_DISTANCE = 200;
    /** How many candidate columns to try before giving up. */
    private static final int MAX_ATTEMPTS = 48;

    /**
     * Places a new owned promotion stone for {@code player} and records it on the
     * player. Returns the {@code class_stone} block position, or {@code null} if
     * no valid surface was found (caller should fall back to the screen).
     */
    public static BlockPos summonFor(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return null;
        }
        BlockPos spawn = overworld.getRespawnData().pos();
        RandomSource rand = overworld.getRandom();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int dx = rand.nextInt(RADIUS * 2 + 1) - RADIUS;
            int dz = rand.nextInt(RADIUS * 2 + 1) - RADIUS;
            if (Math.abs(dx) < MIN_DISTANCE && Math.abs(dz) < MIN_DISTANCE) {
                continue;
            }
            int x = spawn.getX() + dx;
            int z = spawn.getZ() + dz;

            // Force-generate the column so the heightmap is valid, then read the
            // first empty block above the surface.
            overworld.getChunk(x >> 4, z >> 4);
            int groundY = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos base = new BlockPos(x, groundY, z);

            if (!isPlaceableSurface(overworld, base)) {
                continue;
            }

            BlockPos midPos = base.above();
            BlockPos stonePos = base.above(2);

            overworld.setBlockAndUpdate(base, Blocks.STONE_BRICKS.defaultBlockState());
            overworld.setBlockAndUpdate(midPos, Blocks.STONE_BRICKS.defaultBlockState());
            overworld.setBlockAndUpdate(stonePos, ModBlocks.CLASS_STONE_BLOCK.get().defaultBlockState());

            BlockEntity be = overworld.getBlockEntity(stonePos);
            if (be instanceof ClassStoneBlockEntity stone) {
                stone.setOwner(player.getUUID());
            }

            player.setData(ModAttachments.ACTIVE_PROMOTION_STONE.get(),
                ActivePromotionStone.at(overworld.dimension().identifier().toString(), stonePos));
            return stonePos;
        }
        return null;
    }

    /** Removes a previously-summoned pillar (if the blocks are still ours) and clears the attachment. */
    public static void removeFor(ServerPlayer player) {
        ActivePromotionStone active = player.getData(ModAttachments.ACTIVE_PROMOTION_STONE.get());
        if (active.present()) {
            ServerLevel level = resolveLevel(player.level().getServer(), active.dimension());
            if (level != null) {
                BlockPos stonePos = active.pos();
                clearIfClassStone(level, stonePos);
                clearIfStoneBrick(level, stonePos.below());
                clearIfStoneBrick(level, stonePos.below(2));
            }
        }
        player.setData(ModAttachments.ACTIVE_PROMOTION_STONE.get(), ActivePromotionStone.NONE);
    }

    /** True if the player still has an outstanding summoned stone. */
    public static boolean hasActiveStone(ServerPlayer player) {
        return player.getData(ModAttachments.ACTIVE_PROMOTION_STONE.get()).present();
    }

    private static boolean isPlaceableSurface(ServerLevel level, BlockPos base) {
        if (base.getY() <= level.getMinY() + 2 || base.getY() >= level.getMaxY() - 4) {
            return false;
        }
        // The support block must be sturdy (so the pillar stands on ground, not water/foliage).
        BlockState support = level.getBlockState(base.below());
        if (!support.isFaceSturdy(level, base.below(), Direction.UP)) {
            return false;
        }
        // The three pillar cells must be free of liquid (no spawning in oceans/lava).
        for (int dy = 0; dy < 3; dy++) {
            FluidState fluid = level.getFluidState(base.above(dy));
            if (!fluid.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void clearIfClassStone(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.CLASS_STONE_BLOCK.get())) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void clearIfStoneBrick(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.STONE_BRICKS)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimensionId)) {
                return level;
            }
        }
        return server.getLevel(Level.OVERWORLD);
    }
}
