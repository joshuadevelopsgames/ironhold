package milkucha.trmt.handler;

import milkucha.trmt.TRMT;
import milkucha.trmt.TRMTConfig;
import milkucha.trmt.erosion.ErosionMapManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The {@code /trmt} command tree. Ported verbatim from upstream
 * {@code TRMT.onInitialize()} — {@code Permissions.COMMANDS_GAMEMASTER}
 * is the vanilla MC 26.1 enum entry and resolves identically on NeoForge.
 */
@EventBusSubscriber(modid = TRMT.MOD_ID)
public final class TRMTCommands {
    private TRMTCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("trmt")
                .then(Commands.literal("reloadconfig")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> {
                            TRMTConfig.load();
                            ctx.getSource().sendSuccess(() -> Component.literal("[TRMT] Config reloaded."), true);
                            return 1;
                        }))
                .then(Commands.literal("convert-to-vanilla")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[TRMT] WARNING: This will convert all existing eroded blocks in all currently loaded chunks to their vanilla counterparts. This cannot be undone. ")
                                    .append(Component.literal("[Click to confirm]")
                                            .withStyle(s -> s
                                                    .withClickEvent(new ClickEvent.RunCommand(
                                                            "/trmt convert-to-vanilla confirm"))
                                                    .withColor(ChatFormatting.YELLOW)
                                                    .withUnderlined(true))), false);
                            return 1;
                        })
                        .then(Commands.literal("confirm")
                                .executes(ctx -> {
                                    ErosionMapManager.getInstance().convertAllErodedToVanilla(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[TRMT] All eroded blocks in loaded chunks converted to their vanilla counterparts."), true);
                                    return 1;
                                })))
                .then(Commands.literal("eroded-chunks")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(ctx -> {
                            ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
                            Set<ChunkPos> allChunks = ErosionMapManager.getInstance().getErodedChunkPositions();

                            List<ChunkPos> unloaded = new ArrayList<>();
                            int loadedCount = 0;
                            for (ChunkPos cp : allChunks) {
                                if (overworld != null && overworld.getChunk(cp.x(), cp.z(), ChunkStatus.FULL, false) != null) {
                                    loadedCount++;
                                } else {
                                    unloaded.add(cp);
                                }
                            }

                            int total = allChunks.size();
                            int unloadedCount = unloaded.size();
                            int loadedFinal = loadedCount;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[TRMT] Eroded chunks: " + total + " chunk(s) total — " + loadedFinal + " loaded, " + unloadedCount + " unloaded."), false);

                            if (unloaded.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                        "[TRMT] All eroded chunks are currently loaded."), false);
                            } else {
                                TRMT.LOGGER.info("[TRMT] Unloaded chunks with erosion data ({}):", unloadedCount);
                                for (ChunkPos cp : unloaded) {
                                    TRMT.LOGGER.info("[TRMT]   {} {}", cp.getMinBlockX(), cp.getMinBlockZ());
                                }
                                if (unloadedCount <= 20) {
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[TRMT] Unloaded chunk coordinates:"), false);
                                    for (ChunkPos cp : unloaded) {
                                        int bx = cp.getMinBlockX(), bz = cp.getMinBlockZ();
                                        ctx.getSource().sendSuccess(() -> Component.literal("  " + bx + " " + bz), false);
                                    }
                                } else {
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "[TRMT] " + unloadedCount + " unloaded chunks — full list printed to server console."), false);
                                }
                            }
                            return 1;
                        })));
    }
}
