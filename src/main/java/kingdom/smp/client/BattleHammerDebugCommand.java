package kingdom.smp.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live tuning for the Battle Hammer. Run {@code /hammerdebug on} first so the
 * renderer reads these values.
 *
 *   /hammerdebug on|off
 *   /hammerdebug fp pos|rot <x> <y> <z>      /hammerdebug fp scale <s>
 *   /hammerdebug tp pos|rot <x> <y> <z>      /hammerdebug tp scale <s>
 *   /hammerdebug glow mode <0-2>             (0 off, 1 normal, 2 debug-magenta-on-rings)
 *   /hammerdebug glow alpha <a>              (-1 = follow charge)
 *   /hammerdebug glow color <r> <g> <b>      /hammerdebug glow scale <s>
 *   /hammerdebug print
 */
public final class BattleHammerDebugCommand {
    private BattleHammerDebugCommand() {}

    private static int feedback(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + msg), false);
        return 1;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> vec3(
            String name, java.util.function.Consumer<float[]> setter, java.util.function.Supplier<String> summary) {
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

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> scale(
            java.util.function.Consumer<Float> setter, java.util.function.Supplier<String> summary) {
        return Commands.literal("scale").then(Commands.argument("s", FloatArgumentType.floatArg(0.05F, 8.0F))
            .executes(ctx -> { setter.accept(FloatArgumentType.getFloat(ctx, "s")); return feedback(ctx, summary.get()); }));
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var D = (Runnable) () -> { BattleHammerTransformDebug.enabled = true; };

        event.getDispatcher().register(Commands.literal("hammerdebug")
            .then(Commands.literal("on").executes(ctx -> { BattleHammerTransformDebug.enabled = true; return feedback(ctx, "debug ON"); }))
            .then(Commands.literal("off").executes(ctx -> { BattleHammerTransformDebug.enabled = false; return feedback(ctx, "debug OFF (using compiled values)"); }))
            .then(Commands.literal("fp")
                .then(vec3("pos", v -> { D.run(); BattleHammerTransformDebug.fpTransX=v[0]; BattleHammerTransformDebug.fpTransY=v[1]; BattleHammerTransformDebug.fpTransZ=v[2]; }, BattleHammerTransformDebug::fpSummary))
                .then(vec3("rot", v -> { D.run(); BattleHammerTransformDebug.fpRotX=v[0]; BattleHammerTransformDebug.fpRotY=v[1]; BattleHammerTransformDebug.fpRotZ=v[2]; }, BattleHammerTransformDebug::fpSummary))
                .then(scale(s -> { D.run(); BattleHammerTransformDebug.fpScale=s; }, BattleHammerTransformDebug::fpSummary)))
            .then(Commands.literal("tp")
                .then(vec3("pos", v -> { D.run(); BattleHammerTransformDebug.tpTransX=v[0]; BattleHammerTransformDebug.tpTransY=v[1]; BattleHammerTransformDebug.tpTransZ=v[2]; }, BattleHammerTransformDebug::tpSummary))
                .then(vec3("rot", v -> { D.run(); BattleHammerTransformDebug.tpRotX=v[0]; BattleHammerTransformDebug.tpRotY=v[1]; BattleHammerTransformDebug.tpRotZ=v[2]; }, BattleHammerTransformDebug::tpSummary))
                .then(scale(s -> { D.run(); BattleHammerTransformDebug.tpScale=s; }, BattleHammerTransformDebug::tpSummary)))
            .then(Commands.literal("ohfp")
                .then(vec3("pos", v -> { D.run(); BattleHammerTransformDebug.ohFpTransX=v[0]; BattleHammerTransformDebug.ohFpTransY=v[1]; BattleHammerTransformDebug.ohFpTransZ=v[2]; }, BattleHammerTransformDebug::ohFpSummary))
                .then(vec3("rot", v -> { D.run(); BattleHammerTransformDebug.ohFpRotX=v[0]; BattleHammerTransformDebug.ohFpRotY=v[1]; BattleHammerTransformDebug.ohFpRotZ=v[2]; }, BattleHammerTransformDebug::ohFpSummary))
                .then(scale(s -> { D.run(); BattleHammerTransformDebug.ohFpScale=s; }, BattleHammerTransformDebug::ohFpSummary)))
            .then(Commands.literal("ohtp")
                .then(vec3("pos", v -> { D.run(); BattleHammerTransformDebug.ohTpTransX=v[0]; BattleHammerTransformDebug.ohTpTransY=v[1]; BattleHammerTransformDebug.ohTpTransZ=v[2]; }, BattleHammerTransformDebug::ohTpSummary))
                .then(vec3("rot", v -> { D.run(); BattleHammerTransformDebug.ohTpRotX=v[0]; BattleHammerTransformDebug.ohTpRotY=v[1]; BattleHammerTransformDebug.ohTpRotZ=v[2]; }, BattleHammerTransformDebug::ohTpSummary))
                .then(scale(s -> { D.run(); BattleHammerTransformDebug.ohTpScale=s; }, BattleHammerTransformDebug::ohTpSummary)))
            .then(Commands.literal("glow")
                .then(Commands.literal("mode").then(Commands.argument("m", IntegerArgumentType.integer(0,2))
                    .executes(ctx -> { BattleHammerTransformDebug.glowMode=IntegerArgumentType.getInteger(ctx,"m"); return feedback(ctx, BattleHammerTransformDebug.glowSummary()); })))
                .then(Commands.literal("method").then(Commands.argument("m", IntegerArgumentType.integer(0,1))
                    .executes(ctx -> { BattleHammerTransformDebug.glowMethod=IntegerArgumentType.getInteger(ctx,"m"); return feedback(ctx, "glow method=" + BattleHammerTransformDebug.glowMethod + " (0=render, 1=positionAndRender)"); })))
                .then(Commands.literal("alpha").then(Commands.argument("a", FloatArgumentType.floatArg(-1F,1F))
                    .executes(ctx -> { BattleHammerTransformDebug.glowAlpha=FloatArgumentType.getFloat(ctx,"a"); return feedback(ctx, BattleHammerTransformDebug.glowSummary()); })))
                .then(Commands.literal("color")
                    .then(Commands.argument("r", IntegerArgumentType.integer(0,255))
                    .then(Commands.argument("g", IntegerArgumentType.integer(0,255))
                    .then(Commands.argument("b", IntegerArgumentType.integer(0,255))
                    .executes(ctx -> { BattleHammerTransformDebug.glowR=IntegerArgumentType.getInteger(ctx,"r"); BattleHammerTransformDebug.glowG=IntegerArgumentType.getInteger(ctx,"g"); BattleHammerTransformDebug.glowB=IntegerArgumentType.getInteger(ctx,"b"); return feedback(ctx, BattleHammerTransformDebug.glowSummary()); })))))
                .then(Commands.literal("scale").then(Commands.argument("s", FloatArgumentType.floatArg(0.5F,3.0F))
                    .executes(ctx -> { BattleHammerTransformDebug.glowScale=FloatArgumentType.getFloat(ctx,"s"); return feedback(ctx, BattleHammerTransformDebug.glowSummary()); }))))
            .then(Commands.literal("print").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + BattleHammerTransformDebug.fpSummary()), false);
                ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + BattleHammerTransformDebug.tpSummary()), false);
                ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + BattleHammerTransformDebug.ohFpSummary()), false);
                ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + BattleHammerTransformDebug.ohTpSummary()), false);
                ctx.getSource().sendSuccess(() -> Component.literal("[Hammer] " + BattleHammerTransformDebug.glowSummary()), false);
                return 1;
            })));
    }
}
