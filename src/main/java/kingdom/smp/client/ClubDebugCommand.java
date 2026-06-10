package kingdom.smp.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import kingdom.smp.client.ClubTransformDebug.Pose;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Live tuning for the Club family's per-display-context transforms (shared by plain / spiked /
 * ribbed). The renderer reads {@link ClubTransformDebug} every frame, so changes apply instantly.
 *
 *   /clubdebug gui|ground|fixed|fp|tp pos <x> <y> <z>
 *   /clubdebug gui|ground|fixed|fp|tp rot <x> <y> <z>
 *   /clubdebug gui|ground|fixed|fp|tp scale <s>
 *   /clubdebug print
 *
 * Dial in the inventory look with the {@code gui} context, then {@code /clubdebug print} and
 * paste the numbers to bake them into {@link ClubTransformDebug}'s defaults.
 */
public final class ClubDebugCommand {
    private ClubDebugCommand() {}

    private static int feedback(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Club] " + msg), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vec3(
            String name, Consumer<float[]> setter, Supplier<String> summary) {
        return Commands.literal(name)
            .then(Commands.argument("x", FloatArgumentType.floatArg())
            .then(Commands.argument("y", FloatArgumentType.floatArg())
            .then(Commands.argument("z", FloatArgumentType.floatArg())
            .executes(ctx -> {
                setter.accept(new float[]{
                    FloatArgumentType.getFloat(ctx, "x"),
                    FloatArgumentType.getFloat(ctx, "y"),
                    FloatArgumentType.getFloat(ctx, "z")});
                return feedback(ctx, summary.get());
            }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> scale(
            Consumer<Float> setter, Supplier<String> summary) {
        return Commands.literal("scale").then(Commands.argument("s", FloatArgumentType.floatArg(0.05F, 8.0F))
            .executes(ctx -> { setter.accept(FloatArgumentType.getFloat(ctx, "s")); return feedback(ctx, summary.get()); }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> poseCmd(String name, Pose p) {
        Supplier<String> sum = () -> p.summary(name);
        return Commands.literal(name)
            .then(vec3("pos", v -> { p.tx = v[0]; p.ty = v[1]; p.tz = v[2]; }, sum))
            .then(vec3("rot", v -> { p.rx = v[0]; p.ry = v[1]; p.rz = v[2]; }, sum))
            .then(scale(s -> p.scale = s, sum));
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("clubdebug")
            .then(poseCmd("fp", ClubTransformDebug.FP))
            .then(poseCmd("tp", ClubTransformDebug.TP))
            .then(poseCmd("gui", ClubTransformDebug.GUI))
            .then(poseCmd("ground", ClubTransformDebug.GROUND))
            .then(poseCmd("fixed", ClubTransformDebug.FIXED))
            .then(Commands.literal("print").executes(ctx -> {
                feedback(ctx, ClubTransformDebug.FP.summary("fp"));
                feedback(ctx, ClubTransformDebug.TP.summary("tp"));
                feedback(ctx, ClubTransformDebug.GUI.summary("gui"));
                feedback(ctx, ClubTransformDebug.GROUND.summary("ground"));
                feedback(ctx, ClubTransformDebug.FIXED.summary("fixed"));
                return 1;
            })));
    }
}
