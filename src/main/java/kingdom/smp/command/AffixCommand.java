package kingdom.smp.command;

import java.util.List;

import com.mojang.brigadier.Command;

import kingdom.smp.gear.AffixData;
import kingdom.smp.gear.AffixRoller;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test/admin command for the affix system: {@code /affix roll} rolls fresh affixes on the held item
 * (count = its quality tier), {@code /affix clear} removes them. Production rolling (loot / blacksmith
 * reroll) is the follow-up increment. Registered to the game bus in {@code Ironhold}.
 */
public final class AffixCommand {
    private AffixCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("affix").requires(src -> kingdom.smp.perms.Perms.check(
                    src, kingdom.smp.perms.ModPermissions.COMMAND_ADMIN))
                .then(Commands.literal("roll").executes(ctx -> roll(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    private static int roll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        AffixRoller.roll(held);
        src.sendSuccess(() -> Component.literal(
            "Rolled " + AffixData.get(held).size() + " affix(es) on the held item."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        AffixData.set(player.getMainHandItem(), List.of());
        src.sendSuccess(() -> Component.literal("Cleared affixes on the held item."), false);
        return Command.SINGLE_SUCCESS;
    }
}
