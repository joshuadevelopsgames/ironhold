package kingdom.smp.mixin;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.DataFixer;

import kingdom.smp.rtf.RTFRandomState;
import kingdom.smp.rtf.WorldGenFlags;
import kingdom.smp.rtf.chunkgen.RTFChunkGenerator;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Once the server has built the per-level {@link RandomState}, hand it the registry access so it can resolve our
 * preset and build the {@code GeneratorContext} that backs all {@code CellSampler}/{@code NoiseSampler} markers
 * wired in {@link RandomStateRTFMixin}.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapRTFMixin {
    @Shadow
    private RandomState randomState;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void ironhold$initRandomState(ServerLevel level,
                                          LevelStorageSource.LevelStorageAccess levelStorage,
                                          DataFixer dataFixer,
                                          StructureTemplateManager structureManager,
                                          Executor executor,
                                          BlockableEventLoop<Runnable> mainThreadExecutor,
                                          LightChunkGetter chunkGetter,
                                          ChunkGenerator generator,
                                          ChunkStatusUpdateListener chunkStatusListener,
                                          Supplier<SavedDataStorage> overworldDataStorage,
                                          TicketStorage ticketStorage,
                                          int serverViewDistance,
                                          boolean syncWrites,
                                          CallbackInfo ci) {
        // Only initialize the RTF generator context on dimensions that actually
        // use the RTF chunk generator. Without this gate, the End and Nether
        // RandomStates also get a GeneratorContext, which makes NoiseChunkRTFMixin
        // start running RTF tile-cache lookups against End/Nether coordinates —
        // their density functions don't fit the RTF cell model and chunk-gen
        // hangs the first time a player teleports there.
        if (generator instanceof RTFChunkGenerator
            && (Object) this.randomState instanceof RTFRandomState rtfRandomState) {
            WorldGenFlags.setCullNoiseSections(true);
            rtfRandomState.initialize(level.registryAccess());
        }
    }
}
