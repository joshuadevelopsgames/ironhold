package kingdom.smp.mirrors.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live calibration of the mirror reflection camera. The nudge is added on top of the planar-reflection
 * transform in mirror-local axes (right across the pane, up = world up, fwd = outward normal).
 *
 * Usage:
 *   /mirrorcam move &lt;right&gt; &lt;up&gt; &lt;fwd&gt;
 *   /mirrorcam rot &lt;dyaw&gt; &lt;dpitch&gt;
 *   /mirrorcam reset
 *   /mirrorcam print        (echoes the last computed camera pos/rot + current nudge)
 */
public final class MirrorCamDebugCommand {
    private MirrorCamDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("mirrorcam")
            .then(Commands.literal("move")
                .then(Commands.argument("right", DoubleArgumentType.doubleArg())
                .then(Commands.argument("up", DoubleArgumentType.doubleArg())
                .then(Commands.argument("fwd", DoubleArgumentType.doubleArg())
                .executes(ctx -> {
                    MirrorReflection.dbgRight = DoubleArgumentType.getDouble(ctx, "right");
                    MirrorReflection.dbgUp = DoubleArgumentType.getDouble(ctx, "up");
                    MirrorReflection.dbgFwd = DoubleArgumentType.getDouble(ctx, "fwd");
                    return print(ctx);
                })))))
            .then(Commands.literal("rot")
                .then(Commands.argument("dyaw", FloatArgumentType.floatArg())
                .then(Commands.argument("dpitch", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    MirrorReflection.dbgYaw = FloatArgumentType.getFloat(ctx, "dyaw");
                    MirrorReflection.dbgPitch = FloatArgumentType.getFloat(ctx, "dpitch");
                    return print(ctx);
                }))))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    MirrorReflection.dbgRight = 0;
                    MirrorReflection.dbgUp = 0;
                    MirrorReflection.dbgFwd = 0;
                    MirrorReflection.dbgYaw = 0;
                    MirrorReflection.dbgPitch = 0;
                    return print(ctx);
                }))
            .then(Commands.literal("print")
                .executes(MirrorCamDebugCommand::print)));
    }

    private static int print(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("[MirrorCam] " + MirrorReflection.lastCamSummary), false);
        return 1;
    }
}
