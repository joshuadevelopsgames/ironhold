package kingdom.smp.command;

import java.util.Collection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.entity.VillagerProfession;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.RpgProgressionActions;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.net.OpenClassSelectionPayload;
import kingdom.smp.net.OpenKingdomSelectionPayload;
import kingdom.smp.net.OpenMenuPayload;
import kingdom.smp.net.OpenProfilePayload;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IronholdCommands {
    private IronholdCommands() {}

    private static final SuggestionProvider<CommandSourceStack> PROFESSION_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (VillagerProfession p : VillagerProfession.values()) {
            if (rem.isEmpty() || p.id().startsWith(rem)) {
                builder.suggest(p.id());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> CLASS_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (PlayerClass c : PlayerClass.values()) {
            String id = c.id().toLowerCase();
            if (rem.isEmpty() || id.startsWith(rem)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    };

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher()
            .register(
                Commands.literal("k2")
                    .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("whoami").executes(ctx -> whoami(ctx.getSource())))
                    .then(
                        Commands.literal("class")
                            .then(
                                Commands.literal("set")
                                    .then(
                                        Commands.argument("id", StringArgumentType.word())
                                            .suggests(CLASS_SUGGESTIONS)
                                            .executes(
                                                ctx -> setClass(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "id"))))))
                    .then(
                        Commands.literal("kingdom")
                            .then(
                                Commands.literal("set")
                                    .then(
                                        Commands.argument("index", IntegerArgumentType.integer(0, 3))
                                            .executes(
                                                ctx -> setKingdom(
                                                    ctx.getSource(),
                                                    IntegerArgumentType.getInteger(ctx, "index"))))))
                    .then(
                        Commands.literal("classxp")
                            .then(
                                Commands.literal("add")
                                    .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(
                                                ctx -> addClassXp(
                                                    ctx.getSource(),
                                                    IntegerArgumentType.getInteger(ctx, "amount")))
                                            .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                    .executes(
                                                        ctx -> addClassXpTargets(
                                                            ctx.getSource(),
                                                            IntegerArgumentType.getInteger(ctx, "amount"),
                                                            EntityArgument.getPlayers(ctx, "targets"))))))
                            .then(
                                Commands.literal("remove")
                                    .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(
                                                ctx -> removeClassXp(
                                                    ctx.getSource(),
                                                    IntegerArgumentType.getInteger(ctx, "amount"))))))
                    .then(
                        Commands.literal("kingdomxp")
                            .then(
                                Commands.literal("add")
                                    .then(
                                        Commands.argument("kingdom", IntegerArgumentType.integer(0, 3))
                                            .then(
                                                Commands.argument("amount", LongArgumentType.longArg(1L))
                                                    .executes(
                                                        ctx -> addKingdomXp(
                                                            ctx.getSource(),
                                                            IntegerArgumentType.getInteger(ctx, "kingdom"),
                                                            LongArgumentType.getLong(ctx, "amount")))))))
                    .then(Commands.literal("gates").executes(ctx -> gates(ctx.getSource())))
                    .then(Commands.literal("weight").executes(ctx -> weight(ctx.getSource())))
                    .then(Commands.literal("classgui").executes(ctx -> openClassGui(ctx.getSource())))
                    .then(Commands.literal("kingdomgui").executes(ctx -> openKingdomGui(ctx.getSource())))
                    .then(Commands.literal("profile").executes(ctx -> openProfile(ctx.getSource())))
                    .then(
                        Commands.literal("villager")
                            .then(
                                Commands.argument("profession", StringArgumentType.word())
                                    .suggests(PROFESSION_SUGGESTIONS)
                                    .executes(
                                        ctx -> spawnVillager(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "profession"))))));

        // /menu — all players (opens MainMenuScreen on client)
        event.getDispatcher()
            .register(Commands.literal("menu").executes(ctx -> openMenu(ctx.getSource())));

        // Self gamemode shortcuts (PermissionSet: moderator+, like main SMP /c and /s)
        event.getDispatcher()
            .register(Commands.literal("c")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.CREATIVE)));
        event.getDispatcher()
            .register(Commands.literal("s")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.SURVIVAL)));
    }

    private static int spawnVillager(CommandSourceStack src, String professionId) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        VillagerProfession prof = VillagerProfession.fromId(professionId.toLowerCase());
        ServerLevel level = (ServerLevel) player.level();

        KingdomVillagerEntity villager = new KingdomVillagerEntity(Ironhold.KINGDOM_VILLAGER.get(), level);
        villager.setProfession(prof);
        villager.setPos(player.getX() + 2, player.getY(), player.getZ());
        level.addFreshEntity(villager);

        String tag = prof.canTalk() ? "\u00A7b[Talker] " : "\u00A7e[Silent] ";
        src.sendSuccess(
            () -> Component.literal(tag + "Spawned " + prof.displayName()
                + ": " + villager.getPersonality().name()
                + " (" + villager.getPersonality().temperament().id() + ")"),
            true);
        return 1;
    }

    private static int setSelfGameMode(CommandSourceStack src, GameType mode) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        if (player.setGameMode(mode)) {
            src.sendSuccess(
                    () -> Component.literal("Set game mode to ").append(mode.getLongDisplayName()),
                    true);
            return 1;
        }
        src.sendFailure(Component.literal("Unable to change game mode."));
        return 0;
    }

    private static int whoami(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        PlayerKingdomRpgData d = player.getData(ModAttachments.PLAYER_RPG.get());
        int need = RpgProgression.xpToReachNextLevel(d.classLevel());
        src.sendSuccess(
            () -> Component.literal(
                "Kingdom "
                    + d.kingdomIndexClamped()
                    + " | "
                    + d.playerClass().id()
                    + " L"
                    + d.classLevel()
                    + " ("
                    + d.xpIntoLevel()
                    + "/"
                    + need
                    + " XP)"),
            false);
        return 1;
    }

    private static int setClass(CommandSourceStack src, String raw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        PlayerClass c = PlayerClass.parse(raw);
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData next =
            new PlayerKingdomRpgData(cur.kingdomIndex(), c.ordinal(), cur.classLevel(), cur.xpIntoLevel());
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        src.sendSuccess(() -> Component.literal("Class set to " + c.id()), false);
        return 1;
    }

    private static int setKingdom(CommandSourceStack src, int index) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData next =
            new PlayerKingdomRpgData(index, cur.classIndex(), cur.classLevel(), cur.xpIntoLevel());
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        src.sendSuccess(() -> Component.literal("Kingdom index set to " + index), false);
        return 1;
    }

    private static int addClassXp(CommandSourceStack src, int amount) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        applyClassXp(src, player, amount);
        return 1;
    }

    private static int addClassXpTargets(CommandSourceStack src, int amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            applyClassXp(src, p, amount);
        }
        return targets.size();
    }

    private static void applyClassXp(CommandSourceStack src, ServerPlayer player, int amount) {
        PlayerKingdomRpgData leveled = RpgProgressionActions.grantClassXp(player, amount);
        src.sendSuccess(
            () -> Component.literal(
                "Added "
                    + amount
                    + " class XP to "
                    + player.getName().getString()
                    + " (now L"
                    + leveled.classLevel()
                    + ")"),
            true);
    }

    private static int removeClassXp(CommandSourceStack src, int amount) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData reduced = RpgProgression.removeClassXp(cur, amount);
        player.setData(ModAttachments.PLAYER_RPG.get(), reduced);
        src.sendSuccess(
            () -> Component.literal(
                "Removed "
                    + amount
                    + " class XP from "
                    + player.getName().getString()
                    + " (now L"
                    + reduced.classLevel()
                    + ", "
                    + reduced.xpIntoLevel()
                    + " XP)"),
            true);
        return 1;
    }

    private static int addKingdomXp(CommandSourceStack src, int kingdom, long amount) {
        ServerLevel level = src.getLevel();
        KingdomWorldData data = overworldData(level);
        data.addKingdomXp(kingdom, amount);
        src.sendSuccess(
            () -> Component.literal("Kingdom " + kingdom + " +" + amount + " pooled XP (gates rechecked)."),
            true);
        return 1;
    }

    private static int gates(CommandSourceStack src) {
        KingdomWorldData data = overworldData(src.getLevel());
        src.sendSuccess(
            () -> Component.literal(
                "Nether: "
                    + (data.isNetherUnlocked() ? "open" : "locked")
                    + " (need any kingdom ≥ "
                    + KingdomWorldData.NETHER_UNLOCK_XP
                    + "). End: "
                    + (data.isEndUnlocked() ? "open" : "locked")
                    + " (≥ "
                    + KingdomWorldData.END_UNLOCK_XP
                    + "). Pools k0–k3: "
                    + data.getKingdomXp(0)
                    + ", "
                    + data.getKingdomXp(1)
                    + ", "
                    + data.getKingdomXp(2)
                    + ", "
                    + data.getKingdomXp(3)),
            false);
        return 1;
    }

    private static KingdomWorldData overworldData(ServerLevel any) {
        ServerLevel ow = any.getServer().getLevel(Level.OVERWORLD);
        return ow.getDataStorage().computeIfAbsent(KingdomWorldData.TYPE);
    }

    private static int weight(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        int w = EncumbranceHandler.weightFor(player);
        int max = rpg.playerClass().maxCarryWeight();
        src.sendSuccess(
            () -> Component.literal(
                "Carry weight: "
                    + w
                    + " / "
                    + max
                    + " (class "
                    + rpg.playerClass().id()
                    + "). Over cap if first number > second."),
            false);
        return 1;
    }

    private static int openClassGui(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        // Sync latest data then open the screen
        ModNetworking.syncToClient(player);
        PacketDistributor.sendToPlayer(player, new OpenClassSelectionPayload());
        return 1;
    }

    private static int openKingdomGui(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ModNetworking.syncToClient(player);
        PacketDistributor.sendToPlayer(player, new OpenKingdomSelectionPayload());
        return 1;
    }

    private static int openProfile(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ModNetworking.syncToClient(player);
        PacketDistributor.sendToPlayer(player, new OpenProfilePayload());
        return 1;
    }

    private static int openMenu(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ModNetworking.syncToClient(player);
        PacketDistributor.sendToPlayer(player, new OpenMenuPayload());
        return 1;
    }
}
