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
import kingdom.smp.gear.GearComponents;
import kingdom.smp.gear.ItemCondition;
import kingdom.smp.gear.ItemQuality;
import kingdom.smp.gear.RepairFatigue;
import kingdom.smp.skill.PlayerSkillState;
import kingdom.smp.skill.Profession;
import kingdom.smp.skill.ProfessionRank;
import kingdom.smp.skill.SkillSavedData;
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

    private static final SuggestionProvider<CommandSourceStack> PROFESSION_NAME_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase();
        for (Profession p : Profession.values()) {
            String id = p.getSerializedName();
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
                                            StringArgumentType.getString(ctx, "profession")))))
                    .then(
                        Commands.literal("gear")
                            .then(Commands.literal("info").executes(ctx -> gearInfo(ctx.getSource())))
                            .then(Commands.literal("setquality")
                                .then(Commands.argument("quality", StringArgumentType.word())
                                    .suggests(QUALITY_SUGGESTIONS)
                                    .executes(ctx -> setGearQuality(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "quality")))))
                            .then(Commands.literal("setfatigue")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, RepairFatigue.MAX_LEVEL))
                                    .executes(ctx -> setGearFatigue(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "level")))))
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
                            .then(Commands.literal("reset").executes(ctx -> skillReset(ctx.getSource())))));

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

    private static int setGearFatigue(CommandSourceStack src, int level) {
        ItemStack held = heldDamageable(src);
        if (held.isEmpty()) return 0;
        GearComponents.setFatigue(held, new RepairFatigue(level));
        src.sendSuccess(() -> Component.literal("Set fatigue → " + level), false);
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
        RepairFatigue f = GearComponents.getFatigue(held);
        boolean pristine = GearComponents.isPristine(held);
        ItemCondition cond = ItemCondition.fromStack(held);
        int max = held.getMaxDamage();
        int dmg = held.getDamageValue();
        src.sendSuccess(() -> Component.literal(
                "Quality: " + q.displayName()
                + " | Condition: " + cond.displayName()
                + " | Fatigue: " + f.level() + "/" + RepairFatigue.MAX_LEVEL
                + " | Pristine: " + pristine
                + " | Durability: " + (max - dmg) + "/" + max
        ), false);
        return 1;
    }
}
