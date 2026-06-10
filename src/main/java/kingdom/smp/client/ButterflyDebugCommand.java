package kingdom.smp.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import kingdom.smp.entity.ButterflySpecies.ModelShape;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live butterfly model-scale tuning.
 *
 * <pre>
 * /butterflydebug scale 0.8
 * /butterflydebug broad scale 0.7
 * /butterflydebug print
 * /butterflydebug reset
 * </pre>
 */
public final class ButterflyDebugCommand {
    private ButterflyDebugCommand() {}

    private static int feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Butterfly] " + message), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> shapeCommand(ModelShape shape) {
        return Commands.literal(shape.id())
            .then(Commands.literal("scale")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.05F, 8.0F))
                    .executes(ctx -> {
                        ButterflyScaleDebug.set(shape, FloatArgumentType.getFloat(ctx, "value"));
                        return feedback(ctx, ButterflyScaleDebug.summary(shape));
                    })));
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("butterflydebug")
            .then(Commands.literal("scale")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.05F, 8.0F))
                    .executes(ctx -> {
                        ButterflyScaleDebug.setAll(FloatArgumentType.getFloat(ctx, "value"));
                        return print(ctx);
                    })))
            .then(Commands.literal("print").executes(ButterflyDebugCommand::print))
            .then(Commands.literal("reset").executes(ctx -> {
                ButterflyScaleDebug.reset();
                return print(ctx);
            }));

        for (ModelShape shape : ModelShape.values()) {
            root.then(shapeCommand(shape));
        }
        event.getDispatcher().register(root);
    }

    private static int print(CommandContext<CommandSourceStack> ctx) {
        for (ModelShape shape : ModelShape.values()) {
            feedback(ctx, ButterflyScaleDebug.summary(shape));
        }
        return 1;
    }
}
