package kingdom.smp.lobby;

import com.mojang.brigadier.context.CommandContext;

import kingdom.smp.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Admin command tree for setting up and using the spawn lobby. Op (gamemaster) only. */
public final class LobbyCommand {
    private LobbyCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lobby")
            .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(ctx -> teleportSelf(ctx.getSource()))
            .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
            .then(Commands.literal("setspawn").executes(ctx -> setSpawn(ctx.getSource())))
            .then(Commands.literal("setportal")
                .then(Commands.literal("pos1").executes(ctx -> setPortal(ctx, 1)))
                .then(Commands.literal("pos2").executes(ctx -> setPortal(ctx, 2))))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static ServerPlayer playerOrNull(CommandSourceStack src) {
        return src.getEntity() instanceof ServerPlayer p ? p : null;
    }

    private static int teleportSelf(CommandSourceStack src) {
        ServerPlayer player = playerOrNull(src);
        if (player == null) { src.sendFailure(Component.literal("Players only.")); return 0; }
        if (!Lobby.sendToLobby(player)) {
            src.sendFailure(Component.literal("Lobby dimension isn't loaded — check the datapack/restart."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Teleported to the lobby."), false);
        return 1;
    }

    private static int leave(CommandSourceStack src) {
        ServerPlayer player = playerOrNull(src);
        if (player == null) { src.sendFailure(Component.literal("Players only.")); return 0; }
        Lobby.sendToWorld(player);
        src.sendSuccess(() -> Component.literal("Sent to the world spawn."), false);
        return 1;
    }

    private static int setSpawn(CommandSourceStack src) {
        ServerPlayer player = playerOrNull(src);
        if (player == null) { src.sendFailure(Component.literal("Players only.")); return 0; }
        if (!Lobby.isLobby(player)) {
            src.sendFailure(Component.literal("Stand in the lobby dimension first (/lobby)."));
            return 0;
        }
        LobbySavedData.get(src.getServer()).setSpawn(
            player.getX(), player.getY(), player.getZ(), player.getYRot());
        src.sendSuccess(() -> Component.literal("Lobby spawn set to your current position."), true);
        return 1;
    }

    private static int setPortal(CommandContext<CommandSourceStack> ctx, int corner) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = playerOrNull(src);
        if (player == null) { src.sendFailure(Component.literal("Players only.")); return 0; }
        if (!Lobby.isLobby(player)) {
            src.sendFailure(Component.literal("Stand in the lobby dimension first (/lobby)."));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        LobbySavedData data = LobbySavedData.get(src.getServer());
        if (corner == 1) {
            data.setPortalCorner1(pos.getX(), pos.getY(), pos.getZ());
            src.sendSuccess(() -> Component.literal(
                "Portal corner 1 set to " + pos.toShortString() + ". Now stand at the other corner and run /lobby setportal pos2."), true);
        } else {
            data.setPortalCorner2(pos.getX(), pos.getY(), pos.getZ());
            src.sendSuccess(() -> Component.literal(
                "Portal corner 2 set to " + pos.toShortString() + ". Exit portal is now active: " + data.describePortal()), true);
        }
        return 1;
    }

    private static int status(CommandSourceStack src) {
        LobbySavedData data = LobbySavedData.get(src.getServer());
        boolean enabled = Config.LOBBY_ENABLED.get();
        boolean everyJoin = Config.LOBBY_EVERY_JOIN.get();
        String spawn = data.hasSpawn()
            ? String.format("(%.1f, %.1f, %.1f)", data.spawnX(), data.spawnY(), data.spawnZ())
            : "default (8.5, 1.0, 8.5)";
        src.sendSuccess(() -> Component.literal(
            "Lobby — enabled: " + enabled
            + " | route every join: " + everyJoin
            + " | spawn: " + spawn
            + " | exit portal: " + data.describePortal()), false);
        return 1;
    }
}
