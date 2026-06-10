package kingdom.smp.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kingdom.smp.admin.AdminModeData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /admin} — a one-command toggle between an op's normal play state and a clean creative
 * "build mode".
 *
 * <p><b>Entering</b> (first {@code /admin}): snapshots the player's exact position, facing,
 * dimension, game mode, and entire inventory to {@link AdminModeData}; empties their inventory;
 * and switches them to creative.
 *
 * <p><b>Leaving</b> (a second {@code /admin}): restores their previous game mode, wipes whatever
 * they were holding (so creative-grabbed items don't leak into survival), gives back every saved
 * item in its original slot, and teleports them to the saved spot.
 *
 * <p>Gated on op permission level 2 — a non-opped player cannot see or run it.
 */
public final class AdminModeCommand {
    private AdminModeCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("admin")
                // Op level 2 (GAMEMASTERS) — same gate vanilla puts on /gamemode.
                // A non-opped player can neither see nor run it.
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> toggle(ctx.getSource()))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource()))));
    }

    private static int toggle(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        MinecraftServer server = src.getServer();
        AdminModeData data = AdminModeData.get(server);

        if (data.isInAdminMode(player.getUUID())) {
            return exitAdminMode(src, player, server, data);
        }
        return enterAdminMode(src, player, server, data);
    }

    private static int enterAdminMode(CommandSourceStack src, ServerPlayer player,
                                      MinecraftServer server, AdminModeData data) {
        Inventory inv = player.getInventory();

        // Capture every occupied slot (the flat Container view spans main + armor + offhand).
        List<AdminModeData.SlotStack> items = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                items.add(new AdminModeData.SlotStack(i, stack.copy()));
            }
        }

        AdminModeData.Snapshot snapshot = new AdminModeData.Snapshot(
            items,
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot(),
            player.level().dimension().identifier().toString(),
            player.gameMode.getGameModeForPlayer().getName());

        data.put(player.getUUID(), snapshot);

        inv.clearContent();
        player.inventoryMenu.broadcastChanges();
        player.setGameMode(GameType.CREATIVE);

        src.sendSuccess(() -> Component.literal(
            "§6[Admin] §aEntered build mode — position & inventory saved, switched to creative. "
                + "Run §e/admin§a again to return everything.").withStyle(ChatFormatting.RESET), false);
        return 1;
    }

    private static int exitAdminMode(CommandSourceStack src, ServerPlayer player,
                                     MinecraftServer server, AdminModeData data) {
        AdminModeData.Snapshot snap = data.remove(player.getUUID());
        if (snap == null) {
            src.sendFailure(Component.literal("§6[Admin] §cNo saved admin state found."));
            return 0;
        }

        // Restore the saved game mode first.
        GameType previous = GameType.byName(snap.gameType(), GameType.SURVIVAL);
        player.setGameMode(previous);

        // Wipe current contents (creative grabs included), then put the saved items back.
        Inventory inv = player.getInventory();
        inv.clearContent();
        for (AdminModeData.SlotStack ss : snap.items()) {
            if (ss.slot() >= 0 && ss.slot() < inv.getContainerSize()) {
                inv.setItem(ss.slot(), ss.stack().copy());
            }
        }
        player.inventoryMenu.broadcastChanges();

        // Teleport back to the saved spot (falling back to the overworld if that
        // dimension no longer exists).
        ServerLevel target = server.getLevel(
            ResourceKey.create(Registries.DIMENSION, Identifier.parse(snap.dimension())));
        if (target == null) {
            target = server.getLevel(Level.OVERWORLD);
        }
        player.teleportTo(target, snap.x(), snap.y(), snap.z(), Set.of(), snap.yaw(), snap.pitch(), false);

        src.sendSuccess(() -> Component.literal(
            "§6[Admin] §aReturned — items restored and teleported to your saved position.")
            .withStyle(ChatFormatting.RESET), false);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        boolean active = AdminModeData.get(src.getServer()).isInAdminMode(player.getUUID());
        src.sendSuccess(() -> Component.literal(
            "§6[Admin] §7Build mode: " + (active ? "§aACTIVE §7(run /admin to return)" : "§cinactive")),
            false);
        return 1;
    }
}
