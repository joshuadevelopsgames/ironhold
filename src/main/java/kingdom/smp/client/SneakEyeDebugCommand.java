package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kingdom.smp.client.hud.SneakEyeConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-only command to live-tune the sneak-eye HUD position and scale.
 *
 * Usage:
 *   /sneakeye pos &lt;x&gt; &lt;y&gt;    — set offset from screen center (px)
 *   /sneakeye scale &lt;s&gt;        — set size multiplier (0.1–6.0)
 *   /sneakeye nudge &lt;dx&gt; &lt;dy&gt; — adjust position by a delta
 *   /sneakeye reset             — restore defaults
 *   /sneakeye print             — show current values
 */
public final class SneakEyeDebugCommand {
    private SneakEyeDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("sneakeye")
            .then(Commands.literal("pos")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                .executes(ctx -> {
                    SneakEyeConfig.loadIfNeeded();
                    SneakEyeConfig.offsetX = IntegerArgumentType.getInteger(ctx, "x");
                    SneakEyeConfig.offsetY = IntegerArgumentType.getInteger(ctx, "y");
                    SneakEyeConfig.save();
                    reply(ctx);
                    return 1;
                }))))
            .then(Commands.literal("nudge")
                .then(Commands.argument("dx", IntegerArgumentType.integer())
                .then(Commands.argument("dy", IntegerArgumentType.integer())
                .executes(ctx -> {
                    SneakEyeConfig.loadIfNeeded();
                    SneakEyeConfig.offsetX += IntegerArgumentType.getInteger(ctx, "dx");
                    SneakEyeConfig.offsetY += IntegerArgumentType.getInteger(ctx, "dy");
                    SneakEyeConfig.save();
                    reply(ctx);
                    return 1;
                }))))
            .then(Commands.literal("scale")
                .then(Commands.argument("s", FloatArgumentType.floatArg(0.1F, 6.0F))
                .executes(ctx -> {
                    SneakEyeConfig.loadIfNeeded();
                    SneakEyeConfig.scale = FloatArgumentType.getFloat(ctx, "s");
                    SneakEyeConfig.save();
                    reply(ctx);
                    return 1;
                })))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    SneakEyeConfig.resetDefaults();
                    reply(ctx);
                    return 1;
                }))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    SneakEyeConfig.loadIfNeeded();
                    reply(ctx);
                    return 1;
                }))
        );
    }

    private static void reply(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("[SneakEye] " + SneakEyeConfig.summary()), false);
    }
}
