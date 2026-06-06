package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-side live tuning for the stone golem's grip on the battle hammer.
 *
 * <pre>
 *   /golemhammer pos &lt;x&gt; &lt;y&gt; &lt;z&gt;     translation (model pixels)
 *   /golemhammer rot &lt;x&gt; &lt;y&gt; &lt;z&gt;     rotation (degrees)
 *   /golemhammer scale &lt;s&gt;            uniform scale
 *   /golemhammer hud                  toggle the on-screen readout
 *   /golemhammer print                dump current values to chat (to bake into the bone)
 * </pre>
 * Values feed {@link StoneGolemHammerTuning} and apply instantly to every stone golem on screen.
 */
public final class StoneGolemHammerDebugCommand {
    private StoneGolemHammerDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("golemhammer")
            .then(Commands.literal("pos")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    StoneGolemHammerTuning.posX = FloatArgumentType.getFloat(ctx, "x");
                    StoneGolemHammerTuning.posY = FloatArgumentType.getFloat(ctx, "y");
                    StoneGolemHammerTuning.posZ = FloatArgumentType.getFloat(ctx, "z");
                    return echo(ctx);
                })))))
            .then(Commands.literal("rot")
                .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg())
                .then(Commands.argument("z", FloatArgumentType.floatArg())
                .executes(ctx -> {
                    StoneGolemHammerTuning.rotX = FloatArgumentType.getFloat(ctx, "x");
                    StoneGolemHammerTuning.rotY = FloatArgumentType.getFloat(ctx, "y");
                    StoneGolemHammerTuning.rotZ = FloatArgumentType.getFloat(ctx, "z");
                    return echo(ctx);
                })))))
            .then(Commands.literal("scale")
                .then(Commands.argument("s", FloatArgumentType.floatArg(0.05f))
                .executes(ctx -> {
                    StoneGolemHammerTuning.scale = FloatArgumentType.getFloat(ctx, "s");
                    return echo(ctx);
                })))
            .then(Commands.literal("hud").executes(ctx -> {
                StoneGolemHammerTuning.hudVisible = !StoneGolemHammerTuning.hudVisible;
                return echo(ctx);
            }))
            .then(Commands.literal("print").executes(StoneGolemHammerDebugCommand::echo)));
    }

    private static int echo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
            () -> Component.literal("[GolemHammer] " + StoneGolemHammerTuning.summary()), false);
        return 1;
    }
}
