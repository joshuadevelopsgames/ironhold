package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kingdom.smp.client.block.TripwireRackTuning;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live-tunes where a tripwire-hook item rack draws its held item, so it can be nudged into the
 * hook's ring in-game (e.g. with a golden sword in the rack) without recompiling. Backs
 * {@link TripwireRackTuning}, which {@link kingdom.smp.client.block.TripwireRackRenderer} reads
 * every frame.
 *
 * <pre>
 *   /rackdebug set &lt;dial&gt; &lt;value&gt;   # dial: hooky | hang | outlift | pitch | roll | scale
 *   /rackdebug show                      # print the current values
 *   /rackdebug reset                     # back to defaults
 *   /rackdebug print                     # paste-ready DEF_* constants for TripwireRackTuning
 * </pre>
 *
 * Examples: {@code /rackdebug set hooky 0.7}, {@code /rackdebug set hang 0.02}.
 */
public final class TripwireRackDebugCommand {
    private TripwireRackDebugCommand() {}

    private static final SuggestionProvider<CommandSourceStack> DIAL_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(TripwireRackTuning.dials(), builder);

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("rackdebug")
            .then(Commands.literal("set")
                .then(Commands.argument("dial", StringArgumentType.word())
                    .suggests(DIAL_SUGGESTIONS)
                    .then(Commands.argument("value", FloatArgumentType.floatArg())
                        .executes(ctx -> {
                            String dial = StringArgumentType.getString(ctx, "dial");
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            if (!TripwireRackTuning.set(dial, value)) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "[rack] unknown dial: " + dial + " (use one of: "
                                        + String.join(", ", TripwireRackTuning.dials()) + ")"));
                                return 0;
                            }
                            sendOk(ctx, dial + " = " + value + "    " + TripwireRackTuning.summary());
                            return 1;
                        }))))
            .then(Commands.literal("show")
                .executes(ctx -> {
                    sendOk(ctx, TripwireRackTuning.summary());
                    return 1;
                }))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    TripwireRackTuning.reset();
                    sendOk(ctx, "reset → " + TripwireRackTuning.summary());
                    return 1;
                }))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    String java = TripwireRackTuning.printJava();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§a[rack] Paste into TripwireRackTuning (replace the DEF_* block):"), false);
                    for (String line : java.split("\n")) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    System.out.println("[TripwireRackDebug]\n" + java);
                    return 1;
                }))
        );
    }

    private static void sendOk(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§b[rack] " + msg), false);
    }
}
