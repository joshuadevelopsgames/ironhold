package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client-side command to position the anvil forge-hammer click button live.
 *
 * Usage:
 *   /forgebutton &lt;x&gt; &lt;y&gt; &lt;w&gt; &lt;h&gt;   set offset + size (relative to the anvil panel)
 *   /forgebutton print                  show current values
 *
 * Changes apply immediately to an open anvil; reopen if it's closed. Once the
 * zone lines up with the painted hammer, bake the numbers into
 * {@link ForgeButtonDebug}'s defaults.
 */
public final class ForgeButtonCommand {
    private ForgeButtonCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("forgebutton")
            .then(Commands.argument("x", IntegerArgumentType.integer(-512, 512))
            .then(Commands.argument("y", IntegerArgumentType.integer(-512, 512))
            .then(Commands.argument("w", IntegerArgumentType.integer(1, 256))
            .then(Commands.argument("h", IntegerArgumentType.integer(1, 256))
            .executes(ctx -> {
                ForgeButtonDebug.x = IntegerArgumentType.getInteger(ctx, "x");
                ForgeButtonDebug.y = IntegerArgumentType.getInteger(ctx, "y");
                ForgeButtonDebug.w = IntegerArgumentType.getInteger(ctx, "w");
                ForgeButtonDebug.h = IntegerArgumentType.getInteger(ctx, "h");
                ForgeButtonDebug.applyToActive();
                ctx.getSource().sendSuccess(() ->
                    Component.literal("[ForgeButton] " + ForgeButtonDebug.summary()), false);
                return 1;
            })))))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[ForgeButton] " + ForgeButtonDebug.summary()), false);
                    return 1;
                }))
        );
    }
}
