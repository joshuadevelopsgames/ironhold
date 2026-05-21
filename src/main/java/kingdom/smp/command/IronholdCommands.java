package kingdom.smp.command;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.entity.KangarudeEntity;
import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.entity.VillagerProfession;
import kingdom.smp.food.CookingRecipe;
import kingdom.smp.food.CookingRecipes;
import kingdom.smp.food.CookingService;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemCondition;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.skill.PlayerSkillState;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.SkillSavedData;
import kingdom.smp.skill.useskill.PlayerUseSkills;
import kingdom.smp.skill.useskill.UseSkill;
import kingdom.smp.skill.useskill.UseSkillCurve;
import kingdom.smp.structure.IscScanner;
import kingdom.smp.structure.IscStorage;
import kingdom.smp.structure.IscStructure;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import kingdom.smp.game.RpgProgressionActions;
import kingdom.smp.net.ModNetworking;
import kingdom.smp.net.OpenClassSelectionPayload;
import kingdom.smp.net.OpenConsolePayload;
import kingdom.smp.net.OpenKingdomSelectionPayload;
import kingdom.smp.net.OpenMenuPayload;
import kingdom.smp.net.OpenProfilePayload;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import kingdom.smp.world.KingdomWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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

    private static final SuggestionProvider<CommandSourceStack> QUALITY_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (ItemQuality q : ItemQuality.values()) {
            String id = q.getSerializedName();
            if (rem.isEmpty() || id.startsWith(rem)) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> BOOL_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (String b : new String[]{"true", "false"}) {
            if (rem.isEmpty() || b.startsWith(rem)) builder.suggest(b);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_NAME_SUGGESTIONS = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        try {
            String rem = builder.getRemaining().toLowerCase();
            for (String name : IscStorage.list(server)) {
                if (rem.isEmpty() || name.startsWith(rem)) builder.suggest(name);
            }
        } catch (java.io.IOException ignored) {
            // Storage dir not yet created — no suggestions to offer.
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PROFESSION_NAME_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (Profession p : Profession.values()) {
            String id = p.getSerializedName();
            if (rem.isEmpty() || id.startsWith(rem)) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    /** Profession ranks plus the literal "none" for clearing. */
    private static final SuggestionProvider<CommandSourceStack> PROFESSION_RANK_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        if ("none".startsWith(rem)) builder.suggest("none");
        for (ProfessionRank r : ProfessionRank.values()) {
            String id = r.getSerializedName();
            if (rem.isEmpty() || id.startsWith(rem)) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> USESKILL_NAME_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (UseSkill s : UseSkill.values()) {
            String id = s.getSerializedName();
            if (rem.isEmpty() || id.startsWith(rem)) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> RECIPE_ID_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (CookingRecipe r : CookingRecipes.all()) {
            String id = r.id().toString();
            if (rem.isEmpty() || id.startsWith(rem)) builder.suggest(id);
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
                                                    StringArgumentType.getString(ctx, "id")))))
                            .then(
                                Commands.literal("clear")
                                    .executes(ctx -> clearClassSelf(ctx.getSource()))
                                    .then(
                                        Commands.argument("targets", EntityArgument.players())
                                            .executes(
                                                ctx -> clearClassTargets(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "targets"))))))
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
                        Commands.literal("classlevel")
                            .then(
                                Commands.literal("set")
                                    .then(
                                        Commands.argument("level", IntegerArgumentType.integer(1, 999))
                                            .executes(
                                                ctx -> setClassLevel(
                                                    ctx.getSource(),
                                                    IntegerArgumentType.getInteger(ctx, "level")))
                                            .then(
                                                Commands.argument("targets", EntityArgument.players())
                                                    .executes(
                                                        ctx -> setClassLevelTargets(
                                                            ctx.getSource(),
                                                            IntegerArgumentType.getInteger(ctx, "level"),
                                                            EntityArgument.getPlayers(ctx, "targets")))))))
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
                    .then(Commands.literal("questboard").executes(ctx -> openQuestBoard(ctx.getSource())))
                    .then(
                        Commands.literal("villager")
                            .then(
                                Commands.argument("profession", StringArgumentType.word())
                                    .suggests(PROFESSION_SUGGESTIONS)
                                    .executes(
                                        ctx -> spawnVillager(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "profession")))))
                    .then(
                        Commands.literal("kanga")
                            .then(Commands.literal("join").executes(ctx -> kangaJoin(ctx.getSource())))
                            .then(Commands.literal("leave").executes(ctx -> kangaLeave(ctx.getSource()))))
                    .then(
                        Commands.literal("gear")
                            .then(Commands.literal("info").executes(ctx -> gearInfo(ctx.getSource())))
                            .then(Commands.literal("setquality")
                                .then(Commands.argument("quality", StringArgumentType.word())
                                    .suggests(QUALITY_SUGGESTIONS)
                                    .executes(ctx -> setGearQuality(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "quality")))))
                            .then(Commands.literal("setpristine")
                                .then(Commands.argument("flag", StringArgumentType.word())
                                    .suggests(BOOL_SUGGESTIONS)
                                    .executes(ctx -> setGearPristine(
                                        ctx.getSource(),
                                        Boolean.parseBoolean(StringArgumentType.getString(ctx, "flag")))))))
                    .then(
                        Commands.literal("skill")
                            .then(Commands.literal("info").executes(ctx -> skillInfo(ctx.getSource())))
                            .then(Commands.literal("addpoints")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                    .executes(ctx -> skillAddPoints(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                            .then(Commands.literal("spend")
                                .then(Commands.argument("profession", StringArgumentType.word())
                                    .suggests(PROFESSION_NAME_SUGGESTIONS)
                                    .executes(ctx -> skillSpend(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "profession")))))
                            .then(Commands.literal("respec")
                                .then(Commands.argument("profession", StringArgumentType.word())
                                    .suggests(PROFESSION_NAME_SUGGESTIONS)
                                    .executes(ctx -> skillRespec(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "profession")))))
                            .then(Commands.literal("milestone")
                                .then(Commands.argument("id", StringArgumentType.word())
                                    .then(Commands.argument("points", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> skillAwardMilestone(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "id"),
                                            IntegerArgumentType.getInteger(ctx, "points"))))))
                            .then(Commands.literal("reset").executes(ctx -> skillReset(ctx.getSource())))
                            .then(Commands.literal("setrank")
                                .then(Commands.argument("profession", StringArgumentType.word())
                                    .suggests(PROFESSION_NAME_SUGGESTIONS)
                                    .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests(PROFESSION_RANK_SUGGESTIONS)
                                        .executes(ctx -> skillSetRank(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "profession"),
                                            StringArgumentType.getString(ctx, "rank")))))))
                    .then(
                        Commands.literal("useskill")
                            .then(Commands.literal("info").executes(ctx -> useSkillInfo(ctx.getSource())))
                            .then(Commands.literal("setlevel")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                    .suggests(USESKILL_NAME_SUGGESTIONS)
                                    .then(Commands.argument("level", IntegerArgumentType.integer(0, UseSkill.MAX_LEVEL))
                                        .executes(ctx -> useSkillSetLevel(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "skill"),
                                            IntegerArgumentType.getInteger(ctx, "level")))))))
                    .then(
                        Commands.literal("cook")
                            .then(Commands.argument("recipe", IdentifierArgument.id())
                                .suggests(RECIPE_ID_SUGGESTIONS)
                                .executes(ctx -> cookRecipe(ctx.getSource(),
                                    IdentifierArgument.getId(ctx, "recipe")))))
                    .then(
                        Commands.literal("cookbook")
                            .then(Commands.literal("learn")
                                .then(Commands.argument("recipe", IdentifierArgument.id())
                                    .suggests(RECIPE_ID_SUGGESTIONS)
                                    .executes(ctx -> cookbookLearn(ctx.getSource(),
                                        IdentifierArgument.getId(ctx, "recipe")))))
                            .then(Commands.literal("forget")
                                .then(Commands.argument("recipe", IdentifierArgument.id())
                                    .suggests(RECIPE_ID_SUGGESTIONS)
                                    .executes(ctx -> cookbookForget(ctx.getSource(),
                                        IdentifierArgument.getId(ctx, "recipe"))))))
                    .then(
                        Commands.literal("struct")
                            .then(Commands.literal("scan")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .then(Commands.argument("name", StringArgumentType.word())
                                            .executes(ctx -> structScan(
                                                ctx.getSource(),
                                                BlockPosArgument.getBlockPos(ctx, "from"),
                                                BlockPosArgument.getBlockPos(ctx, "to"),
                                                StringArgumentType.getString(ctx, "name")))))))
                            .then(Commands.literal("build")
                                .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(STRUCTURE_NAME_SUGGESTIONS)
                                    .executes(ctx -> structBuild(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"),
                                        null))
                                    .then(Commands.argument("at", BlockPosArgument.blockPos())
                                        .executes(ctx -> structBuild(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "name"),
                                            BlockPosArgument.getBlockPos(ctx, "at"))))))
                            .then(Commands.literal("list").executes(ctx -> structList(ctx.getSource())))
                            .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(STRUCTURE_NAME_SUGGESTIONS)
                                    .executes(ctx -> structDelete(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))));

        // /menu — all players (opens MainMenuScreen on client)
        event.getDispatcher()
            .register(Commands.literal("menu").executes(ctx -> openMenu(ctx.getSource())));

        // /console — opens the King's Console screen
        event.getDispatcher()
            .register(Commands.literal("console").executes(ctx -> openConsole(ctx.getSource())));

        // Self gamemode shortcuts (PermissionSet: moderator+, like main SMP /c and /s)
        event.getDispatcher()
            .register(Commands.literal("c")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.CREATIVE)));
        event.getDispatcher()
            .register(Commands.literal("s")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> setSelfGameMode(ctx.getSource(), GameType.SURVIVAL)));

        // /dimension <overworld|nether|end> — teleport between dimensions
        SuggestionProvider<CommandSourceStack> dimSuggestions = (ctx, builder) -> {
            for (String d : new String[]{"overworld", "nether", "end"}) {
                if (d.startsWith(builder.getRemaining().toLowerCase())) builder.suggest(d);
            }
            return builder.buildFuture();
        };
        event.getDispatcher()
            .register(Commands.literal("dimension")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .then(Commands.argument("dim", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(ctx -> switchDimension(ctx.getSource(),
                        StringArgumentType.getString(ctx, "dim")))));
    }

    private static int switchDimension(CommandSourceStack src, String dim) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        var targetKey = switch (dim.toLowerCase()) {
            case "overworld" -> Level.OVERWORLD;
            case "nether", "the_nether" -> Level.NETHER;
            case "end", "the_end" -> Level.END;
            default -> null;
        };
        if (targetKey == null) {
            src.sendFailure(Component.literal("Unknown dimension: " + dim + ". Use overworld, nether, or end."));
            return 0;
        }
        ServerLevel targetLevel = player.level().getServer().getLevel(targetKey);
        if (targetLevel == null) {
            src.sendFailure(Component.literal("Dimension not loaded: " + dim));
            return 0;
        }
        if (player.level().dimension() == targetKey) {
            src.sendFailure(Component.literal("Already in " + dim + "."));
            return 0;
        }
        player.teleportTo(targetLevel, player.getX(), player.getY(), player.getZ(),
            java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        src.sendSuccess(() -> Component.literal("Teleported to " + dim + "."), true);
        return 1;
    }

    private static int kangaJoin(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        KangarudeEntity npc = new KangarudeEntity(kingdom.smp.ModEntities.KANGARUDE.get(), level);
        npc.setPos(player.getX() + 1.5, player.getY(), player.getZ());
        // Face the player.
        npc.setYRot(player.getYRot() + 180.0F);
        npc.setYHeadRot(player.getYRot() + 180.0F);
        level.addFreshEntity(npc);

        kingdom.smp.entity.KangarudeTabList.add(src.getServer());

        Component joinMsg = Component.translatable(
                "multiplayer.player.joined",
                Component.literal("Kangarude"))
            .withStyle(ChatFormatting.YELLOW);
        src.getServer().getPlayerList().broadcastSystemMessage(joinMsg, false);
        return 1;
    }

    private static int kangaLeave(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ServerLevel level = (ServerLevel) player.level();
        KangarudeEntity nearest = level.getEntitiesOfClass(
                KangarudeEntity.class,
                player.getBoundingBox().inflate(64.0))
            .stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
            .orElse(null);
        if (nearest == null) {
            src.sendFailure(Component.literal("No Kangarude within 64 blocks."));
            return 0;
        }
        nearest.discard();

        kingdom.smp.entity.KangarudeTabList.remove(src.getServer());

        Component leaveMsg = Component.translatable(
                "multiplayer.player.left",
                Component.literal("Kangarude"))
            .withStyle(ChatFormatting.YELLOW);
        src.getServer().getPlayerList().broadcastSystemMessage(leaveMsg, false);
        return 1;
    }

    private static int spawnVillager(CommandSourceStack src, String professionId) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        VillagerProfession prof = VillagerProfession.fromId(professionId.toLowerCase());
        ServerLevel level = (ServerLevel) player.level();

        KingdomVillagerEntity villager = new KingdomVillagerEntity(kingdom.smp.ModEntities.KINGDOM_VILLAGER.get(), level);
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

    private static int clearClassSelf(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only — pass a target selector to clear someone else."));
            return 0;
        }
        applyClassClear(src, player);
        return 1;
    }

    private static int clearClassTargets(CommandSourceStack src, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            applyClassClear(src, p);
        }
        return targets.size();
    }

    /**
     * Resets a player to PEASANT L1 0xp, wipes all profession skill ranks and milestones,
     * preserves their kingdom assignment, re-applies class stats and resyncs to client.
     */
    private static void applyClassClear(CommandSourceStack src, ServerPlayer player) {
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData next = new PlayerKingdomRpgData(
            cur.kingdomIndex(), PlayerClass.PEASANT.ordinal(), 1, 0);
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        kingdom.smp.game.ClassStatHandler.apply(player, next);
        kingdom.smp.game.RpgXpBarSync.sync(player, next);
        kingdom.smp.net.ModNetworking.syncToClient(player);

        SkillSavedData skillData = SkillSavedData.get((ServerLevel) player.level());
        skillData.reset(player.getUUID());
        kingdom.smp.net.ModNetworking.syncSkillsToClient(player);

        src.sendSuccess(
            () -> Component.literal("Cleared class & skill levels for "
                + player.getName().getString() + " → Peasant L1, skill state reset"),
            true);
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

    private static int setClassLevel(CommandSourceStack src, int level) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        applyClassLevel(src, player, level);
        return 1;
    }

    private static int setClassLevelTargets(CommandSourceStack src, int level, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            applyClassLevel(src, p, level);
        }
        return targets.size();
    }

    /**
     * Sets the player's class level directly to {@code level}, resetting xp-into-level to 0
     * and re-applying class stats. Does not auto-promote, broadcast, or play the level-up
     * fanfare — this is an admin/debug shortcut, not a gameplay grant.
     */
    private static void applyClassLevel(CommandSourceStack src, ServerPlayer player, int level) {
        PlayerKingdomRpgData cur = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData next = new PlayerKingdomRpgData(
            cur.kingdomIndex(), cur.classIndex(), level, 0);
        player.setData(ModAttachments.PLAYER_RPG.get(), next);
        kingdom.smp.game.ClassStatHandler.apply(player, next);
        kingdom.smp.game.RpgXpBarSync.sync(player, next);
        kingdom.smp.net.ModNetworking.syncToClient(player);
        src.sendSuccess(
            () -> Component.literal(
                "Set "
                    + player.getName().getString()
                    + " to "
                    + next.playerClass().id()
                    + " L"
                    + level),
            true);
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

    private static int openQuestBoard(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        // Open the menu via SimpleMenuProvider — server creates it with sample
        // quest data; client reconstructs it via the no-arg ctor (which also
        // uses QuestData.sample() so the visuals match).
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (id, playerInv, p) -> new kingdom.smp.quest.QuestBoardMenu(
                id, playerInv, kingdom.smp.quest.QuestData.sample()),
            Component.literal("Quest Board")));
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

    private static int openConsole(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return 0;
        }
        ModNetworking.syncToClient(player);
        PacketDistributor.sendToPlayer(player, new OpenConsolePayload());
        return 1;
    }

    // ── Gear quality debug commands ───────────────────────────────────────────

    private static ItemStack heldDamageable(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return ItemStack.EMPTY;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem()) {
            src.sendFailure(Component.literal("Hold a damageable item in your main hand."));
            return ItemStack.EMPTY;
        }
        return held;
    }

    /** For setquality and info — gear/tools/weapons/ores/ingots only. Plain blocks/food rejected. */
    private static ItemStack heldQualityEligible(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only."));
            return ItemStack.EMPTY;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            src.sendFailure(Component.literal("Hold an item in your main hand."));
            return ItemStack.EMPTY;
        }
        if (!kingdom.smp.gear.QualityScope.isEligible(held)) {
            src.sendFailure(Component.literal("That item is not quality-eligible. "
                    + "Use gear, tools, weapons, or ores/ingots."));
            return ItemStack.EMPTY;
        }
        return held;
    }

    private static int setGearQuality(CommandSourceStack src, String raw) {
        ItemStack held = heldQualityEligible(src);
        if (held.isEmpty()) return 0;
        ItemQuality parsed = null;
        for (ItemQuality q : ItemQuality.values()) {
            if (q.getSerializedName().equalsIgnoreCase(raw)) { parsed = q; break; }
        }
        if (parsed == null) {
            src.sendFailure(Component.literal("Unknown quality: " + raw + " (use standard|fine|mint)"));
            return 0;
        }
        GearComponents.setQuality(held, parsed);
        ItemQuality finalParsed = parsed;
        src.sendSuccess(() -> Component.literal("Set quality → " + finalParsed.displayName()), false);
        return 1;
    }

    private static int setGearPristine(CommandSourceStack src, boolean flag) {
        ItemStack held = heldDamageable(src);
        if (held.isEmpty()) return 0;
        GearComponents.setPristine(held, flag);
        src.sendSuccess(() -> Component.literal("Set pristine → " + flag), false);
        return 1;
    }

    // ── Skill tree debug commands ─────────────────────────────────────────────

    private static Profession parseProfession(CommandSourceStack src, String raw) {
        for (Profession p : Profession.values()) {
            if (p.getSerializedName().equalsIgnoreCase(raw)) return p;
        }
        src.sendFailure(Component.literal("Unknown profession: " + raw));
        return null;
    }

    private static int skillInfo(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        src.sendSuccess(() -> Component.literal(state.summary()), false);
        return 1;
    }

    private static int skillAddPoints(CommandSourceStack src, int amount) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        PlayerSkillState updated = new PlayerSkillState(
                state.unspentProfessionPoints() + amount,
                state.currentRanks(),
                state.milestonesCompleted());
        data.setState(player.getUUID(), updated);
        ModNetworking.syncSkillsToClient(player);
        src.sendSuccess(() -> Component.literal("+" + amount + " points → " + updated.unspentProfessionPoints() + " unspent"), false);
        return 1;
    }

    private static int skillSpend(CommandSourceStack src, String professionRaw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        Profession profession = parseProfession(src, professionRaw);
        if (profession == null) return 0;
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        PlayerSkillState updated = state.trySpendOn(profession);
        if (updated == null) {
            src.sendFailure(Component.literal("Cannot spend on " + profession.displayName()
                    + " — already at Master, or not enough unspent points."));
            return 0;
        }
        data.setState(player.getUUID(), updated);
        ModNetworking.syncSkillsToClient(player);
        ProfessionRank newRank = updated.rankFor(profession);
        src.sendSuccess(() -> Component.literal(profession.displayName() + " → " + newRank.displayName()
                + " (" + updated.unspentProfessionPoints() + " unspent)"), false);
        return 1;
    }

    private static int skillRespec(CommandSourceStack src, String professionRaw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        Profession profession = parseProfession(src, professionRaw);
        if (profession == null) return 0;
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        ProfessionRank current = state.rankFor(profession);
        if (current == null) {
            src.sendFailure(Component.literal("No progress in " + profession.displayName() + " to respec."));
            return 0;
        }
        int refund = Math.max(0, current.cumulativeCost() - 1);
        PlayerSkillState updated = state.respec(profession);
        data.setState(player.getUUID(), updated);
        ModNetworking.syncSkillsToClient(player);
        src.sendSuccess(() -> Component.literal("Respec'd " + profession.displayName()
                + " (was " + current.displayName() + "). Refunded " + refund
                + " points (lossy −1)."), false);
        return 1;
    }

    private static int skillAwardMilestone(CommandSourceStack src, String milestoneId, int points) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        if (state.hasMilestone(milestoneId)) {
            src.sendFailure(Component.literal("Milestone '" + milestoneId + "' already awarded to this player."));
            return 0;
        }
        PlayerSkillState updated = state.withMilestone(milestoneId, points);
        data.setState(player.getUUID(), updated);
        ModNetworking.syncSkillsToClient(player);
        src.sendSuccess(() -> Component.literal("Awarded milestone '" + milestoneId + "' (+" + points
                + " points → " + updated.unspentProfessionPoints() + " unspent)"), false);
        return 1;
    }

    private static int skillSetRank(CommandSourceStack src, String professionRaw, String rankRaw) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        Profession profession = parseProfession(src, professionRaw);
        if (profession == null) return 0;

        // Special-case "none" clears the rank entirely.
        boolean clear = rankRaw.equalsIgnoreCase("none");
        ProfessionRank rank = null;
        if (!clear) {
            for (ProfessionRank r : ProfessionRank.values()) {
                if (r.getSerializedName().equalsIgnoreCase(rankRaw)) { rank = r; break; }
            }
            if (rank == null) {
                src.sendFailure(Component.literal("Unknown rank: " + rankRaw
                    + " (use novice|apprentice|journeyman|expert|master|none)"));
                return 0;
            }
        }

        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        PlayerSkillState state = data.stateFor(player.getUUID());
        java.util.Map<Profession, ProfessionRank> newRanks = new java.util.EnumMap<>(state.currentRanks());
        if (clear) {
            newRanks.remove(profession);
        } else {
            newRanks.put(profession, rank);
        }
        PlayerSkillState updated = new PlayerSkillState(
            state.unspentProfessionPoints(),
            newRanks,
            state.milestonesCompleted());
        data.setState(player.getUUID(), updated);
        ModNetworking.syncSkillsToClient(player);
        final ProfessionRank finalRank = rank;
        src.sendSuccess(() -> Component.literal(
            profession.displayName() + " → " + (clear ? "(cleared)" : finalRank.displayName())
                + " (free admin set; points unchanged)"), false);
        return 1;
    }

    private static int useSkillInfo(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        PlayerUseSkills data = player.getData(ModAttachments.USE_SKILLS.get());
        StringBuilder sb = new StringBuilder();
        for (UseSkill s : UseSkill.values()) {
            float xp = data.xpFor(s);
            int level = data.levelFor(s);
            float into = UseSkillCurve.xpIntoLevel(xp);
            float next = level >= UseSkill.MAX_LEVEL ? 0f : UseSkillCurve.xpForNext(level);
            if (sb.length() > 0) sb.append(" | ");
            sb.append(s.displayName()).append(": L").append(level)
              .append(" (").append(String.format("%.1f", into))
              .append("/").append(String.format("%.1f", next)).append(" xp)");
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int useSkillSetLevel(CommandSourceStack src, String skillRaw, int level) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        UseSkill skill = null;
        for (UseSkill s : UseSkill.values()) {
            if (s.getSerializedName().equalsIgnoreCase(skillRaw)) { skill = s; break; }
        }
        if (skill == null) {
            src.sendFailure(Component.literal("Unknown use-skill: " + skillRaw));
            return 0;
        }
        // Total XP needed = sum of xpForNext for levels 0..level-1, plus a hair
        // to clear the threshold (avoids float-precision rounding to level-1).
        float xpAcc = 0f;
        for (int i = 0; i < level; i++) xpAcc += UseSkillCurve.xpForNext(i);
        if (level > 0) xpAcc += 0.01f;
        final float total = xpAcc;

        PlayerUseSkills cur = player.getData(ModAttachments.USE_SKILLS.get());
        java.util.EnumMap<UseSkill, Float> next = new java.util.EnumMap<>(cur.xp());
        next.put(skill, total);
        player.setData(ModAttachments.USE_SKILLS.get(), new PlayerUseSkills(next));

        final UseSkill finalSkill = skill;
        src.sendSuccess(() -> Component.literal(
            finalSkill.displayName() + " set to L" + level + " ("
                + String.format("%.1f", total) + " total xp)"), false);
        return 1;
    }

    private static int skillReset(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        SkillSavedData data = SkillSavedData.get((ServerLevel) player.level());
        data.reset(player.getUUID());
        ModNetworking.syncSkillsToClient(player);
        src.sendSuccess(() -> Component.literal("Skill state reset to fresh defaults (3 unspent points)."), false);
        return 1;
    }

    // ── Cooking gate debug commands (Phase 0) ─────────────────────────────────

    private static int cookRecipe(CommandSourceStack src, Identifier recipeId) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        CookingService.Result result = CookingService.tryCook(player, recipeId);
        if (result.status() == CookingService.Status.SUCCESS) {
            src.sendSuccess(result::message, false);
            return 1;
        }
        src.sendFailure(result.message());
        return 0;
    }

    private static int cookbookLearn(CommandSourceStack src, Identifier recipeId) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        if (CookingRecipes.get(recipeId) == null) {
            src.sendFailure(Component.literal("Unknown recipe: " + recipeId));
            return 0;
        }
        boolean newlyLearned = CookingService.learn(player, recipeId);
        src.sendSuccess(() -> Component.literal(newlyLearned
                ? "Learned recipe: " + recipeId
                : "Already knew: " + recipeId), false);
        return 1;
    }

    private static int cookbookForget(CommandSourceStack src, Identifier recipeId) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Players only.")); return 0;
        }
        kingdom.smp.food.KnownRecipes known = player.getData(ModAttachments.KNOWN_RECIPES.get());
        if (!known.knows(recipeId)) {
            src.sendFailure(Component.literal("Don't know: " + recipeId));
            return 0;
        }
        java.util.Set<Identifier> next = new java.util.HashSet<>(known.learned());
        next.remove(recipeId);
        player.setData(ModAttachments.KNOWN_RECIPES.get(), new kingdom.smp.food.KnownRecipes(next));
        src.sendSuccess(() -> Component.literal("Forgot recipe: " + recipeId), false);
        return 1;
    }

    private static int gearInfo(CommandSourceStack src) {
        ItemStack held = heldQualityEligible(src);
        if (held.isEmpty()) return 0;
        ItemQuality q = GearComponents.getQuality(held);
        if (!held.isDamageableItem()) {
            // Ore / ingot / non-gear: only quality is meaningful.
            src.sendSuccess(() -> Component.literal(
                    held.getHoverName().getString() + " — Quality: " + q.displayName()
            ), false);
            return 1;
        }
        boolean pristine = GearComponents.isPristine(held);
        ItemCondition cond = ItemCondition.fromStack(held);
        int max = held.getMaxDamage();
        int dmg = held.getDamageValue();
        src.sendSuccess(() -> Component.literal(
                "Quality: " + q.displayName()
                + " | Condition: " + cond.displayName()
                + " | Pristine: " + pristine
                + " | Durability: " + (max - dmg) + "/" + max
        ), false);
        return 1;
    }

    // ── Structure scan / build commands ───────────────────────────────────────

    private static int structScan(CommandSourceStack src, BlockPos from, BlockPos to, String name) {
        ServerLevel level = src.getLevel();
        IscStructure structure;
        try {
            structure = IscScanner.scan(level, from, to);
        } catch (IllegalArgumentException | IllegalStateException e) {
            src.sendFailure(Component.literal("Scan failed: " + e.getMessage()));
            return 0;
        }
        java.nio.file.Path path;
        try {
            path = IscStorage.save(src.getServer(), name, structure);
        } catch (java.io.IOException | IllegalArgumentException e) {
            src.sendFailure(Component.literal("Save failed: " + e.getMessage()));
            return 0;
        }
        int sx = structure.sizeX(), sy = structure.sizeY(), sz = structure.sizeZ();
        java.nio.file.Path worldRoot = src.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        String rel = worldRoot.relativize(path).toString();
        src.sendSuccess(() -> Component.literal(
            "Scanned '" + name + "' (" + sx + "x" + sy + "x" + sz + ", "
                + structure.solidCount() + " solid blocks, "
                + structure.palette().size() + " palette entries) → " + rel),
            true);
        return 1;
    }

    private static int structBuild(CommandSourceStack src, String name, BlockPos at) {
        IscStructure structure;
        try {
            structure = IscStorage.load(src.getServer(), name);
        } catch (java.io.IOException | IllegalArgumentException | IscStructure.IscParseException e) {
            src.sendFailure(Component.literal("Load failed: " + e.getMessage()));
            return 0;
        }
        BlockPos origin = at;
        if (origin == null) {
            if (!(src.getEntity() instanceof ServerPlayer player)) {
                src.sendFailure(Component.literal("Provide an origin position, or run as a player."));
                return 0;
            }
            origin = player.blockPosition();
        }
        int placed = IscScanner.build(src.getLevel(), structure, origin, false);
        final BlockPos finalOrigin = origin;
        src.sendSuccess(() -> Component.literal(
            "Built '" + name + "' at " + finalOrigin.getX() + " " + finalOrigin.getY() + " " + finalOrigin.getZ()
                + " (" + placed + " blocks placed)"),
            true);
        return 1;
    }

    private static int structList(CommandSourceStack src) {
        List<String> names;
        try {
            names = IscStorage.list(src.getServer());
        } catch (java.io.IOException e) {
            src.sendFailure(Component.literal("List failed: " + e.getMessage()));
            return 0;
        }
        if (names.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No saved structures."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal(
            "Saved structures (" + names.size() + "): " + String.join(", ", names)),
            false);
        return names.size();
    }

    private static int structDelete(CommandSourceStack src, String name) {
        boolean removed;
        try {
            removed = IscStorage.delete(src.getServer(), name);
        } catch (java.io.IOException | IllegalArgumentException e) {
            src.sendFailure(Component.literal("Delete failed: " + e.getMessage()));
            return 0;
        }
        if (!removed) {
            src.sendFailure(Component.literal("No structure named '" + name + "'."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Deleted structure '" + name + "'."), true);
        return 1;
    }
}
