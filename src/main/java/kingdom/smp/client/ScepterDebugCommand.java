package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-side commands to adjust scepter third-person transform live.
 *
 * Usage:
 *   /scepterdebug rot <x> <y> <z>
 *   /scepterdebug pos <x> <y> <z>
 *   /scepterdebug scale <s>
 *   /scepterdebug print
 */
public final class ScepterDebugCommand {
    private ScepterDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("scepterdebug")
            .then(Commands.literal("rot")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    ScepterTransformDebug.rotX = FloatArgumentType.getFloat(ctx, "x");
                    ScepterTransformDebug.rotY = FloatArgumentType.getFloat(ctx, "y");
                    ScepterTransformDebug.rotZ = FloatArgumentType.getFloat(ctx, "z");
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[Scepter] " + ScepterTransformDebug.summary()), false);
                    return 1;
                })))))
            .then(Commands.literal("pos")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    ScepterTransformDebug.transX = FloatArgumentType.getFloat(ctx, "x");
                    ScepterTransformDebug.transY = FloatArgumentType.getFloat(ctx, "y");
                    ScepterTransformDebug.transZ = FloatArgumentType.getFloat(ctx, "z");
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[Scepter] " + ScepterTransformDebug.summary()), false);
                    return 1;
                })))))
            .then(Commands.literal("scale")
                .then(Commands.argument("s", FloatArgumentType.floatArg(0.1F, 5.0F))
                .executes(ctx -> {
                    ScepterTransformDebug.scale = FloatArgumentType.getFloat(ctx, "s");
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[Scepter] " + ScepterTransformDebug.summary()), false);
                    return 1;
                })))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[Scepter] " + ScepterTransformDebug.summary()), false);
                    return 1;
                }))
        );
    }
}
