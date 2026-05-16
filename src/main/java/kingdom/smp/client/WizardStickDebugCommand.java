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
 * Live-tunes the Wizard Stick item's display transforms in-game and prints a
 * paste-ready {@code "display"} block to chat / console.
 *
 * <p>Live preview is wired only for {@code firstperson_righthand} (see
 * {@link WizardStickClientExtensions}). Other contexts are tracked but only
 * apply once you paste the printed JSON into the model file and rebuild.
 *
 * <pre>
 *   /wizardstickdebug context &lt;name&gt;          # switch which context the
 *                                              # next rot/pos/scale edits
 *   /wizardstickdebug rot &lt;x&gt; &lt;y&gt; &lt;z&gt;          # set rotation
 *   /wizardstickdebug pos &lt;x&gt; &lt;y&gt; &lt;z&gt;          # set translation
 *   /wizardstickdebug scale &lt;s&gt;                 # set uniform scale
 *   /wizardstickdebug print                     # print the full display block
 *   /wizardstickdebug summary                   # print the active context
 *   /wizardstickdebug reset                     # reset the active context
 *   /wizardstickdebug reset all                 # reset every context
 * </pre>
 */
public final class WizardStickDebugCommand {
    private WizardStickDebugCommand() {}

    private static final SuggestionProvider<CommandSourceStack> CONTEXT_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(WizardStickTransformDebug.CONTEXTS, builder);

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("wizardstickdebug")
            .then(Commands.literal("context")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CONTEXT_SUGGESTIONS)
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        if (!WizardStickTransformDebug.setActive(name)) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[WS] unknown context: " + name + " (use one of: "
                                    + String.join(", ", WizardStickTransformDebug.CONTEXTS) + ")"));
                            return 0;
                        }
                        sendOk(ctx, "context = " + name + "  " + WizardStickTransformDebug.active().summary());
                        return 1;
                    })))
            .then(Commands.literal("rot")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    var t = WizardStickTransformDebug.active();
                    t.rotX = FloatArgumentType.getFloat(ctx, "x");
                    t.rotY = FloatArgumentType.getFloat(ctx, "y");
                    t.rotZ = FloatArgumentType.getFloat(ctx, "z");
                    sendOk(ctx, WizardStickTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))))
            .then(Commands.literal("pos")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    var t = WizardStickTransformDebug.active();
                    t.transX = FloatArgumentType.getFloat(ctx, "x");
                    t.transY = FloatArgumentType.getFloat(ctx, "y");
                    t.transZ = FloatArgumentType.getFloat(ctx, "z");
                    sendOk(ctx, WizardStickTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))))
            .then(Commands.literal("scale")
                .then(Commands.argument("s", FloatArgumentType.floatArg(0.05F, 5.0F))
                .executes(ctx -> {
                    var t = WizardStickTransformDebug.active();
                    t.scale = FloatArgumentType.getFloat(ctx, "s");
                    sendOk(ctx, WizardStickTransformDebug.activeName() + ": " + t.summary());
                    return 1;
                })))
            .then(Commands.literal("summary")
                .executes(ctx -> {
                    sendOk(ctx, WizardStickTransformDebug.activeName()
                        + ": " + WizardStickTransformDebug.active().summary());
                    return 1;
                }))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    String json = WizardStickTransformDebug.printJson();
                    // Send to player chat AND to server console so it's easy to copy from either.
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§a[WS] Paste this into wizard_stick_3d.json (replace any existing \"display\" block):"
                    ), false);
                    for (String line : json.split("\n")) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    System.out.println("[WizardStickDebug] " + json);
                    return 1;
                }))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    WizardStickTransformDebug.resetActive();
                    sendOk(ctx, "reset " + WizardStickTransformDebug.activeName());
                    return 1;
                })
                .then(Commands.literal("all").executes(ctx -> {
                    WizardStickTransformDebug.resetAll();
                    sendOk(ctx, "reset ALL contexts to defaults");
                    return 1;
                })))
        );
    }

    private static void sendOk(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§b[WS] " + msg), false);
    }
}
