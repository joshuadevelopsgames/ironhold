package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live-tunes Halric's Staff display transforms in-game and prints a
 * paste-ready {@code "display"} block to chat / console.
 *
 * <pre>
 *   /halricstaffdebug context &lt;name&gt;          # which context the next edits affect
 *   /halricstaffdebug rot &lt;x&gt; &lt;y&gt; &lt;z&gt;          # set rotation
 *   /halricstaffdebug pos &lt;x&gt; &lt;y&gt; &lt;z&gt;          # set translation
 *   /halricstaffdebug scale &lt;s&gt;                 # set uniform scale
 *   /halricstaffdebug print                     # print the full display block
 *   /halricstaffdebug summary                   # print the active context
 *   /halricstaffdebug reset                     # reset the active context
 *   /halricstaffdebug reset all                 # reset every context
 * </pre>
 */
public final class HalricStaffDebugCommand {
    private HalricStaffDebugCommand() {}

    private static final SuggestionProvider<CommandSourceStack> CONTEXT_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(HalricStaffTransformDebug.CONTEXTS, builder);

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("halricstaffdebug")
            .then(Commands.literal("context")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CONTEXT_SUGGESTIONS)
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (!HalricStaffTransformDebug.setActive(name)) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[HS] unknown context: " + name + " (use one of: "
                                    + String.join(", ", HalricStaffTransformDebug.CONTEXTS) + ")"));
                            return 0;
                        }
                        sendOk(ctx, "context = " + name + "  " + HalricStaffTransformDebug.active().summary());
                        return 1;
                    })))
            .then(Commands.literal("rot")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    var t = HalricStaffTransformDebug.active();
                    t.rotX = FloatArgumentType.getFloat(ctx, "x");
                    t.rotY = FloatArgumentType.getFloat(ctx, "y");
                    t.rotZ = FloatArgumentType.getFloat(ctx, "z");
                    sendOk(ctx, HalricStaffTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))))
            .then(Commands.literal("pos")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    var t = HalricStaffTransformDebug.active();
                    t.transX = FloatArgumentType.getFloat(ctx, "x");
                    t.transY = FloatArgumentType.getFloat(ctx, "y");
                    t.transZ = FloatArgumentType.getFloat(ctx, "z");
                    sendOk(ctx, HalricStaffTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))))
            .then(Commands.literal("scale")
                .then(Commands.argument("s", FloatArgumentType.floatArg(0.05F, 5.0F))
                .executes(ctx -> {
                    var t = HalricStaffTransformDebug.active();
                    t.scale = FloatArgumentType.getFloat(ctx, "s");
                    sendOk(ctx, HalricStaffTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))
            .then(Commands.literal("summary")
                .executes(ctx -> {
                    sendOk(ctx, HalricStaffTransformDebug.activeName()
                        + ": " + HalricStaffTransformDebug.active().summary());
                    return 1;
                }))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    String json = HalricStaffTransformDebug.printJson();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§a[HS] Paste this into halric_staff.json (replace any existing \"display\" block):"
                    ), false);
                    for (String line : json.split("\n")) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    System.out.println("[HalricStaffDebug] " + json);
                    return 1;
                }))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    HalricStaffTransformDebug.resetActive();
                    sendOk(ctx, "reset " + HalricStaffTransformDebug.activeName());
                    return 1;
                })
                .then(Commands.literal("all").executes(ctx -> {
                    HalricStaffTransformDebug.resetAll();
                    sendOk(ctx, "reset ALL contexts to defaults");
                    return 1;
                })))
        );
    }

    private static void sendOk(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§b[HS] " + msg), false);
    }
}
