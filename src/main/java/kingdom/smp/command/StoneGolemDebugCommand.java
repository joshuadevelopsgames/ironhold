package kingdom.smp.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kingdom.smp.entity.StoneGolemEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;

/**
 * Dev command to replay a stone golem's action animation on demand, so the attack pose can be tuned
 * without waiting for the AI to swing.
 *
 * <pre>/golemattack slam | sweep | stagger</pre>
 * Triggers the named one-shot on the nearest stone golem within 40 blocks (animation only — no damage).
 */
public final class StoneGolemDebugCommand {
    private StoneGolemDebugCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("golemattack")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(play("slam"))
                .then(play("sweep"))
                .then(play("stagger"))
                .then(Commands.literal("speed")
                    .then(Commands.argument("s", com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.05f))
                    .executes(ctx -> {
                        float s = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "s");
                        StoneGolemEntity.DEBUG_ACTION_SPEED = s;
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("[Golem] attack anim speed = " + s + " (1.0 = normal)"), false);
                        return 1;
                    }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> play(String anim) {
        return Commands.literal(anim).executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            ServerLevel level = src.getLevel();
            Vec3 pos = src.getPosition();
            StoneGolemEntity golem = level.getEntitiesOfClass(
                    StoneGolemEntity.class, new AABB(pos, pos).inflate(40))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(pos)))
                .orElse(null);
            if (golem == null) {
                src.sendFailure(Component.literal("No stone golem within 40 blocks."));
                return 0;
            }
            golem.triggerAnim("action", anim);
            src.sendSuccess(() -> Component.literal("[Golem] played \"" + anim + "\""), false);
            return 1;
        });
    }
}
