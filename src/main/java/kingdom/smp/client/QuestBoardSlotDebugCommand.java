package kingdom.smp.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Live-tunes the Quest Board GUI's per-slot positions in-game. Mirrors the
 * structure of {@link WizardStickDebugCommand}.
 *
 * <pre>
 *   /qbslot select &lt;0-35&gt;        # pick which slot subsequent moves edit
 *   /qbslot pos    &lt;x&gt; &lt;y&gt;       # set absolute position
 *   /qbslot move   &lt;dx&gt; &lt;dy&gt;     # nudge by dx,dy
 *   /qbslot info                  # current selection + position
 *   /qbslot print                 # full 36-element Java array
 *   /qbslot reset                 # restore defaults
 * </pre>
 *
 * <p>The currently-selected slot is outlined in cyan in the GUI so it's
 * obvious which one is being moved.
 */
public final class QuestBoardSlotDebugCommand {
    private QuestBoardSlotDebugCommand() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("qbslot")
            .then(Commands.literal("select")
                .then(Commands.argument("idx", IntegerArgumentType.integer(0, 35))
                    .executes(ctx -> {
                        int idx = IntegerArgumentType.getInteger(ctx, "idx");
                        if (!QuestBoardSlotDebug.select(idx)) {
                            ctx.getSource().sendFailure(Component.literal(
                                "[QB] invalid slot index: " + idx));
                            return 0;
                        }
                        sendOk(ctx, "selected " + QuestBoardSlotDebug.summary());
                        return 1;
                    })))
            .then(Commands.literal("pos")
                .then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int x = IntegerArgumentType.getInteger(ctx, "x");
                        int y = IntegerArgumentType.getInteger(ctx, "y");
                        QuestBoardSlotDebug.setPos(QuestBoardSlotDebug.selectedIndex(), x, y);
                        sendOk(ctx, QuestBoardSlotDebug.summary());
                        return 1;
                    }))))
            .then(Commands.literal("move")
                .then(Commands.argument("dx", IntegerArgumentType.integer())
                .then(Commands.argument("dy", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int dx = IntegerArgumentType.getInteger(ctx, "dx");
                        int dy = IntegerArgumentType.getInteger(ctx, "dy");
                        QuestBoardSlotDebug.nudge(QuestBoardSlotDebug.selectedIndex(), dx, dy);
                        sendOk(ctx, QuestBoardSlotDebug.summary());
                        return 1;
                    }))))
            .then(Commands.literal("info")
                .executes(ctx -> {
                    sendOk(ctx, QuestBoardSlotDebug.summary());
                    return 1;
                }))
            .then(Commands.literal("print")
                .executes(ctx -> {
                    String java = QuestBoardSlotDebug.printJava();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§a[QB] Paste this over SLOT_POSITIONS in QuestBoardMenu.java:"
                    ), false);
                    for (String line : java.split("\n")) {
                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    System.out.println("[QuestBoardSlotDebug] " + java);
                    return 1;
                }))
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    QuestBoardSlotDebug.resetAll();
                    sendOk(ctx, "all slots reset to defaults");
                    return 1;
                }))
            .then(Commands.literal("all")
                .then(Commands.argument("dx", IntegerArgumentType.integer())
                .then(Commands.argument("dy", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        int dx = IntegerArgumentType.getInteger(ctx, "dx");
                        int dy = IntegerArgumentType.getInteger(ctx, "dy");
                        QuestBoardSlotDebug.nudgeAll(dx, dy);
                        sendOk(ctx, "nudged ALL slots by (" + dx + ", " + dy + ")");
                        return 1;
                    }))))
            .then(Commands.literal("grid")
                .then(Commands.argument("x0", IntegerArgumentType.integer())
                .then(Commands.argument("y0", IntegerArgumentType.integer())
                .then(Commands.argument("pitchX", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("pitchY", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("hotbarGap", IntegerArgumentType.integer(0, 64))
                    .executes(ctx -> {
                        int x0 = IntegerArgumentType.getInteger(ctx, "x0");
                        int y0 = IntegerArgumentType.getInteger(ctx, "y0");
                        int px = IntegerArgumentType.getInteger(ctx, "pitchX");
                        int py = IntegerArgumentType.getInteger(ctx, "pitchY");
                        int hg = IntegerArgumentType.getInteger(ctx, "hotbarGap");
                        QuestBoardSlotDebug.setUniformGrid(x0, y0, px, py, hg);
                        sendOk(ctx, String.format(
                            "rebuilt uniform grid: x0=%d y0=%d pitchX=%d pitchY=%d hotbarGap=%d",
                            x0, y0, px, py, hg));
                        return 1;
                    }))))))) );
    }

    private static void sendOk(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§b[QB] " + msg), false);
    }
}
