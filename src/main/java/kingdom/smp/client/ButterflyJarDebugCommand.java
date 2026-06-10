package kingdom.smp.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live tuning for the butterflies displayed inside a placed Butterfly Jar block.
 *
 * <pre>
 * /jardebug scale 0.8           # display scale of the in-jar butterflies
 * /jardebug offset 0 -0.05 0    # nudge all butterflies inside the jar (x y z)
 * /jardebug print
 * /jardebug reset
 * </pre>
 *
 * Changes apply instantly to all placed jars in view — no rebuild needed.
 */
public final class ButterflyJarDebugCommand {
    private ButterflyJarDebugCommand() {}

    private static int feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal("[ButterflyJar] " + message), false);
        return 1;
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var root = Commands.literal("jardebug")
            .then(Commands.literal("scale")
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.05F, 8.0F))
                    .executes(ctx -> {
                        ButterflyJarScaleDebug.setScale(FloatArgumentType.getFloat(ctx, "value"));
                        return feedback(ctx, ButterflyJarScaleDebug.summary());
                    })))
            .then(Commands.literal("offset")
                .then(Commands.argument("x", FloatArgumentType.floatArg(-1.0F, 1.0F))
                    .then(Commands.argument("y", FloatArgumentType.floatArg(-1.0F, 1.0F))
                        .then(Commands.argument("z", FloatArgumentType.floatArg(-1.0F, 1.0F))
                            .executes(ctx -> {
                                ButterflyJarScaleDebug.setOffset(
                                    FloatArgumentType.getFloat(ctx, "x"),
                                    FloatArgumentType.getFloat(ctx, "y"),
                                    FloatArgumentType.getFloat(ctx, "z"));
                                return feedback(ctx, ButterflyJarScaleDebug.summary());
                            })))))
            .then(Commands.literal("print")
                .executes(ctx -> feedback(ctx, ButterflyJarScaleDebug.summary())))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    ButterflyJarScaleDebug.reset();
                    return feedback(ctx, ButterflyJarScaleDebug.summary());
                }));
        event.getDispatcher().register(root);
    }
}
