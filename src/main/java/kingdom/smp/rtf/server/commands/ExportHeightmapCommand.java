package kingdom.smp.rtf.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;

public class ExportHeightmapCommand {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext) {
    	commandDispatcher.register(
    		Commands.literal("rtf").requires((stack) -> stack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(
    			Commands.literal("export").then(
    				Commands.literal("heightmap").then(
    					Commands.argument("x", IntegerArgumentType.integer()).then(
        					Commands.argument("z", IntegerArgumentType.integer()).then(
    	        				Commands.argument("size", IntegerArgumentType.integer()).executes((ctx) -> {
    	        					throw new UnsupportedOperationException("TODO");
    	        				})
    						)
    					)
        			)
    			)
    		)
    	);
    }
}