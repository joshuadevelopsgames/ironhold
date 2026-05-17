package kingdom.smp.mixin;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.Command;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async-ifies {@code /locate biome} and {@code /locate structure}.
 *
 * <p>Vanilla runs both searches synchronously on the server thread, blocking
 * the tick loop. A taiga-village search has been measured at ~7 s on this
 * server ("Can't keep up! Running 7044ms or 140 ticks behind"). A search for
 * a rare biome with a constrained climate niche (like
 * {@code ironhold:ebonwood_hollow}) is even slower and can trip the watchdog.
 *
 * <p>The fix dispatches the actual search to {@link Util#backgroundExecutor()},
 * sends the player a "Searching..." message immediately, then formats and
 * delivers the result via {@link MinecraftServer#execute} once the worker
 * finishes. The vanilla {@code showLocateResult} helpers are public, so we
 * reuse the same display + click-to-teleport formatting.
 *
 * <p>{@code locatePoi} is left alone — its 256-block search is bounded and
 * fast enough that async dispatch isn't worth the overhead.
 */
@Mixin(LocateCommand.class)
public class LocateCommandAsyncMixin {

    @Inject(method = "locateBiome", at = @At("HEAD"), cancellable = true)
    private static void ironhold$asyncLocateBiome(
        CommandSourceStack source,
        ResourceOrTagArgument.Result<Biome> elementOrTag,
        CallbackInfoReturnable<Integer> cir
    ) {
        MinecraftServer server = source.getServer();
        if (server == null) return;

        BlockPos sourcePos = BlockPos.containing(source.getPosition());
        ServerLevel level = source.getLevel();

        source.sendSuccess(() -> Component.literal("Searching for biome " + elementOrTag.asPrintable() + "...")
            .withStyle(ChatFormatting.GRAY), false);

        CompletableFuture.supplyAsync(() -> {
            Stopwatch sw = Stopwatch.createStarted(Util.TICKER);
            Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(elementOrTag, sourcePos, 6400, 32, 64);
            sw.stop();
            return new SearchResult<>(result, sw.elapsed().toMillis());
        }, Util.backgroundExecutor()).whenComplete((res, err) -> server.execute(() -> {
            if (err != null) {
                source.sendFailure(Component.literal("Biome search failed: " + err.getMessage()));
                return;
            }
            if (res.result == null) {
                source.sendFailure(Component.translatable("commands.locate.biome.not_found",
                    elementOrTag.asPrintable()));
                return;
            }
            LocateCommand.showLocateResult(source, elementOrTag, sourcePos, res.result,
                "commands.locate.biome.success", true, java.time.Duration.ofMillis(res.elapsedMs));
        }));

        cir.setReturnValue(Command.SINGLE_SUCCESS);
    }

    @Inject(method = "locateStructure", at = @At("HEAD"), cancellable = true)
    private static void ironhold$asyncLocateStructure(
        CommandSourceStack source,
        ResourceOrTagKeyArgument.Result<Structure> resourceOrTag,
        CallbackInfoReturnable<Integer> cir
    ) {
        MinecraftServer server = source.getServer();
        if (server == null) return;

        ServerLevel level = source.getLevel();
        Optional<? extends HolderSet.ListBacked<Structure>> holdersOpt = resourceOrTag.unwrap().map(
            id -> level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(id).map(HolderSet::direct),
            level.registryAccess().lookupOrThrow(Registries.STRUCTURE)::get
        );
        if (holdersOpt.isEmpty()) return; // fall through to vanilla error path
        HolderSet<Structure> target = (HolderSet<Structure>) holdersOpt.get();
        BlockPos sourcePos = BlockPos.containing(source.getPosition());

        source.sendSuccess(() -> Component.literal("Searching for structure " + resourceOrTag.asPrintable() + "...")
            .withStyle(ChatFormatting.GRAY), false);

        CompletableFuture.supplyAsync(() -> {
            Stopwatch sw = Stopwatch.createStarted(Util.TICKER);
            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, target, sourcePos, 100, false);
            sw.stop();
            return new SearchResult<>(result, sw.elapsed().toMillis());
        }, Util.backgroundExecutor()).whenComplete((res, err) -> server.execute(() -> {
            if (err != null) {
                source.sendFailure(Component.literal("Structure search failed: " + err.getMessage()));
                return;
            }
            if (res.result == null) {
                source.sendFailure(Component.translatable("commands.locate.structure.not_found",
                    resourceOrTag.asPrintable()));
                return;
            }
            LocateCommand.showLocateResult(source, resourceOrTag, sourcePos, res.result,
                "commands.locate.structure.success", false, java.time.Duration.ofMillis(res.elapsedMs));
        }));

        cir.setReturnValue(Command.SINGLE_SUCCESS);
    }

    private record SearchResult<T>(Pair<BlockPos, Holder<T>> result, long elapsedMs) {}
}
