package kingdom.smp.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.entity.MimicEntity;
import kingdom.smp.world.MimicChestData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Turns every {@link #INTERVAL}-th naturally-generated chest in the world into a
 * Mimic. A chest counts as "naturally generated" when its block entity still
 * carries an unrolled loot table — that only happens for structure/feature chests
 * the player hasn't opened yet; player-placed chests never have one.
 *
 * <p>Detection runs on chunk load (the loot table persists until the chest is
 * first opened, so the chest is still catchable whenever its chunk loads). Each
 * chest is tallied at most once via the serialized {@link ModAttachments#MIMIC_CHECKED}
 * flag, and the running count lives in {@link MimicChestData} on the overworld so
 * it is global across dimensions and survives restarts.
 *
 * <p>All world mutation is deferred to the next tick via {@link MinecraftServer#execute},
 * as {@link ChunkEvent.Load} warns against touching the level during the event.
 */
public final class MimicChestSpawner {
    private MimicChestSpawner() {}

    /** Every Nth natural chest becomes a Mimic. */
    private static final int INTERVAL = 20;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ChunkPos pos = event.getChunk().getPos();
        server.execute(() -> process(level, pos));
    }

    private static void process(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
        if (chunk == null) return;

        // Read-only pass: collect candidate positions, then mutate — never iterate
        // the block-entity map while removing blocks from it.
        List<BlockPos> candidates = new ArrayList<>();
        for (var entry : chunk.getBlockEntities().entrySet()) {
            if (isUncountedNaturalChest(entry.getValue())) {
                candidates.add(entry.getKey().immutable());
            }
        }
        if (candidates.isEmpty()) return;

        // Stable order so the "every 20th" cadence is deterministic for a given
        // set of newly-loaded chests rather than depending on map iteration order.
        candidates.sort(Comparator.comparingLong(BlockPos::asLong));

        MimicChestData data = level.getServer()
            .getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(MimicChestData.TYPE);

        for (BlockPos pos : candidates) {
            if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) continue;
            if (!isUncountedNaturalChest(chest)) continue;

            // Mark counted first so a chest is never re-tallied, whether or not it converts.
            chest.setData(ModAttachments.MIMIC_CHECKED.get(), Boolean.TRUE);
            chest.setChanged();

            if (data.recordAndShouldConvert(INTERVAL)) {
                convertToMimic(level, pos);
            }
        }
    }

    private static boolean isUncountedNaturalChest(BlockEntity be) {
        // Trapped chests extend ChestBlockEntity but are never part of natural loot gen.
        if (be instanceof TrappedChestBlockEntity) return false;
        if (!(be instanceof ChestBlockEntity chest)) return false;
        if (chest.getLootTable() == null) return false;
        return !Boolean.TRUE.equals(chest.getData(ModAttachments.MIMIC_CHECKED.get()));
    }

    private static void convertToMimic(ServerLevel level, BlockPos pos) {
        // Remove the chest (its loot is still unrolled, so nothing drops) and put a
        // dormant mimic in its place.
        level.removeBlock(pos, false);

        MimicEntity mimic = Ironhold.MIMIC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (mimic == null) return;
        mimic.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        mimic.setPersistenceRequired();
        mimic.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntity(mimic);
    }
}
