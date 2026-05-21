package kingdom.smp.seasons;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kingdom.smp.seasons.network.SyncSeasonPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/**
 * {@code /season} — inspect and adjust the current season cycle (op-only).
 *
 * <ul>
 *   <li>{@code /season} — print current sub-season, day-of-year, ticks remaining</li>
 *   <li>{@code /season set <subseason>} — jump to the start of a given sub-season</li>
 *   <li>{@code /season advance <days>} — push the cycle forward by N in-game days</li>
 * </ul>
 */
public final class SeasonsCommand {
    private SeasonsCommand() {}

    private static final SuggestionProvider<CommandSourceStack> SUB_SEASON_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(Season.SubSeason.VALUES).map(s -> s.name().toLowerCase()), builder);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("season")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("set")
                    .then(Commands.argument("subseason", StringArgumentType.word())
                        .suggests(SUB_SEASON_SUGGESTIONS)
                        .executes(ctx -> setSub(ctx.getSource(), StringArgumentType.getString(ctx, "subseason")))))
                .then(Commands.literal("advance")
                    .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                        .executes(ctx -> advance(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "days")))))
        );
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        SeasonSavedData data = Seasons.serverData(level);
        SeasonState state = SeasonTime.of(data.cycleTicks());
        int subTicksInto = data.cycleTicks() % SeasonConfig.SUB_SEASON_DURATION_TICKS;
        int subTicksRemaining = SeasonConfig.SUB_SEASON_DURATION_TICKS - subTicksInto;
        int daysRemaining = subTicksRemaining / SeasonConfig.DAY_DURATION_TICKS;

        source.sendSuccess(() -> Component.literal("§6Season: §f" + state.subSeason().name().toLowerCase()
            + " §7(" + state.season().name().toLowerCase() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Day " + (state.day() + 1) + " of "
            + (SeasonConfig.CYCLE_DURATION_TICKS / SeasonConfig.DAY_DURATION_TICKS)
            + " — " + daysRemaining + " day(s) until next sub-season"), false);
        return 1;
    }

    private static int setSub(CommandSourceStack source, String name) throws CommandSyntaxException {
        Season.SubSeason target;
        try {
            target = Season.SubSeason.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown sub-season: " + name).withStyle(ChatFormatting.RED));
            return 0;
        }
        ServerLevel level = source.getLevel();
        SeasonSavedData data = Seasons.serverData(level);
        data.setCycleTicks(target.ordinal() * SeasonConfig.SUB_SEASON_DURATION_TICKS);
        broadcast(level, data.cycleTicks());
        source.sendSuccess(() -> Component.literal("§6Jumped to §f" + target.name().toLowerCase()), true);
        return 1;
    }

    private static int advance(CommandSourceStack source, int days) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        SeasonSavedData data = Seasons.serverData(level);
        data.setCycleTicks(data.cycleTicks() + days * SeasonConfig.DAY_DURATION_TICKS);
        broadcast(level, data.cycleTicks());
        SeasonState after = SeasonTime.of(data.cycleTicks());
        source.sendSuccess(() -> Component.literal("§6Advanced " + days + " day(s). Now §f"
            + after.subSeason().name().toLowerCase()), true);
        return 1;
    }

    private static void broadcast(ServerLevel level, int ticks) {
        SyncSeasonPayload payload = new SyncSeasonPayload(level.dimension(), ticks);
        for (ServerPlayer sp : level.players()) {
            PacketDistributor.sendToPlayer(sp, payload);
        }
    }
}
