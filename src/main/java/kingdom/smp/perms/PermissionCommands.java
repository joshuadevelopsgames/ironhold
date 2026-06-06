package kingdom.smp.perms;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /perm} — manage and inspect Ironhold permissions. The headline is {@code /perm why}, which
 * prints the full resolution trace using the same resolver a live check runs.
 */
public final class PermissionCommands {
    private PermissionCommands() {}

    private static final SuggestionProvider<CommandSourceStack> PERM_SUGGESTIONS = (ctx, b) -> {
        String rem = b.getRemaining().toLowerCase();
        for (Perm p : ModPermissions.all()) {
            if (rem.isEmpty() || p.node().toLowerCase().contains(rem)) {
                b.suggest(p.node());
            }
        }
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> STATE_SUGGESTIONS = (ctx, b) -> {
        b.suggest("grant");
        b.suggest("deny");
        b.suggest("unset");
        return b.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> ROLE_SUGGESTIONS = (ctx, b) -> {
        String rem = b.getRemaining().toLowerCase();
        for (Role r : PermissionData.get(ctx.getSource().getLevel()).roles()) {
            if (rem.isEmpty() || r.id().toLowerCase().startsWith(rem)) {
                b.suggest(r.id());
            }
        }
        return b.buildFuture();
    };

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("perm")
                .requires(s -> Perms.check(s, ModPermissions.PERM_MANAGE))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("why")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("perm", StringArgumentType.string())
                            .suggests(PERM_SUGGESTIONS)
                            .executes(c -> why(c.getSource(),
                                EntityArgument.getPlayer(c, "target"),
                                StringArgumentType.getString(c, "perm"))))))
                .then(Commands.literal("set")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("perm", StringArgumentType.string())
                            .suggests(PERM_SUGGESTIONS)
                            .then(Commands.argument("state", StringArgumentType.word())
                                .suggests(STATE_SUGGESTIONS)
                                .executes(c -> setOverride(c.getSource(),
                                    EntityArgument.getPlayer(c, "target"),
                                    StringArgumentType.getString(c, "perm"),
                                    StringArgumentType.getString(c, "state")))))))
                .then(Commands.literal("role")
                    .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .then(Commands.argument("rank", IntegerArgumentType.integer())
                                .executes(c -> roleCreate(c.getSource(),
                                    StringArgumentType.getString(c, "id"),
                                    IntegerArgumentType.getInteger(c, "rank"))))))
                    .then(Commands.literal("rule")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests(ROLE_SUGGESTIONS)
                            .then(Commands.argument("perm", StringArgumentType.string())
                                .suggests(PERM_SUGGESTIONS)
                                .then(Commands.argument("state", StringArgumentType.word())
                                    .suggests(STATE_SUGGESTIONS)
                                    .executes(c -> roleRule(c.getSource(),
                                        StringArgumentType.getString(c, "id"),
                                        StringArgumentType.getString(c, "perm"),
                                        StringArgumentType.getString(c, "state")))))))
                    .then(Commands.literal("assign")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ROLE_SUGGESTIONS)
                                .executes(c -> roleAssign(c.getSource(),
                                    EntityArgument.getPlayer(c, "target"),
                                    StringArgumentType.getString(c, "id"))))))
                    .then(Commands.literal("unassign")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ROLE_SUGGESTIONS)
                                .executes(c -> roleUnassign(c.getSource(),
                                    EntityArgument.getPlayer(c, "target"),
                                    StringArgumentType.getString(c, "id"))))))
                    .then(Commands.literal("list").executes(c -> roleList(c.getSource())))));
    }

    // ---- handlers ----

    private static int list(CommandSourceStack src) {
        List<Perm> perms = ModPermissions.all();
        src.sendSuccess(() -> Component.literal("Ironhold permissions (" + perms.size() + "):")
            .withStyle(ChatFormatting.GOLD), false);
        for (Perm p : perms) {
            src.sendSuccess(() -> Component.literal("  " + p.node()
                + " [" + p.defaultLevel().getSerializedName() + "] - " + p.description())
                .withStyle(ChatFormatting.GRAY), false);
        }
        return perms.size();
    }

    private static int why(CommandSourceStack src, ServerPlayer target, String node) {
        Perm perm = resolvePerm(node);
        if (perm == null) {
            src.sendFailure(Component.literal("Unknown permission: " + node + " (try /perm list)"));
            return 0;
        }
        Decision d = PermissionResolver.resolve(target.createCommandSourceStack(), perm);
        String name = target.getName().getString();
        src.sendSuccess(() -> Component.literal(name + (d.allowed() ? " MAY " : " MAY NOT ") + perm.node())
            .withStyle(d.allowed() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        src.sendSuccess(() -> Component.literal("  decision: " + d.summary())
            .withStyle(ChatFormatting.YELLOW), false);
        for (String step : d.trace()) {
            src.sendSuccess(() -> Component.literal("   - " + step).withStyle(ChatFormatting.GRAY), false);
        }
        return d.allowed() ? 1 : 0;
    }

    private static int setOverride(CommandSourceStack src, ServerPlayer target, String node, String stateStr) {
        Perm perm = resolvePerm(node);
        String stored = perm != null ? perm.node() : normalizeNode(node);
        PermState state = parseState(stateStr);
        PermissionData.get(src.getLevel()).setOverride(target.getUUID(), stored, state);
        String name = target.getName().getString();
        src.sendSuccess(() -> Component.literal("Override " + stored + " = " + state + " for " + name)
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int roleCreate(CommandSourceStack src, String id, int rank) {
        PermissionData data = PermissionData.get(src.getLevel());
        if (data.role(id) != null) {
            src.sendFailure(Component.literal("Role already exists: " + id));
            return 0;
        }
        data.createRole(id, rank);
        src.sendSuccess(() -> Component.literal("Created role '" + id + "' (rank " + rank + ")")
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int roleRule(CommandSourceStack src, String id, String node, String stateStr) {
        PermissionData data = PermissionData.get(src.getLevel());
        if (data.role(id) == null) {
            src.sendFailure(Component.literal("No such role: " + id));
            return 0;
        }
        Perm perm = resolvePerm(node);
        String stored = perm != null ? perm.node() : normalizeNode(node); // allow wildcards / raw nodes
        PermState state = parseState(stateStr);
        data.setRoleRule(id, stored, state);
        src.sendSuccess(() -> Component.literal("Role '" + id + "': " + stored + " = " + state)
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int roleAssign(CommandSourceStack src, ServerPlayer target, String id) {
        PermissionData data = PermissionData.get(src.getLevel());
        if (data.role(id) == null) {
            src.sendFailure(Component.literal("No such role: " + id));
            return 0;
        }
        data.assignRole(target.getUUID(), id);
        String name = target.getName().getString();
        src.sendSuccess(() -> Component.literal("Assigned role '" + id + "' to " + name)
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int roleUnassign(CommandSourceStack src, ServerPlayer target, String id) {
        PermissionData.get(src.getLevel()).unassignRole(target.getUUID(), id);
        String name = target.getName().getString();
        src.sendSuccess(() -> Component.literal("Unassigned role '" + id + "' from " + name)
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int roleList(CommandSourceStack src) {
        List<Role> roles = new ArrayList<>(PermissionData.get(src.getLevel()).roles());
        roles.sort((a, b) -> Integer.compare(b.rank(), a.rank()));
        src.sendSuccess(() -> Component.literal("Roles (" + roles.size() + "), highest rank first:")
            .withStyle(ChatFormatting.GOLD), false);
        for (Role r : roles) {
            src.sendSuccess(() -> Component.literal("  " + r.id()
                + " (rank " + r.rank() + ", " + r.rules().size() + " rules)")
                .withStyle(ChatFormatting.GRAY), false);
        }
        return roles.size();
    }

    // ---- helpers ----

    private static Perm resolvePerm(String node) {
        Perm p = ModPermissions.byNode(node);
        if (p == null && !node.contains(":")) {
            p = ModPermissions.byNode("ironhold:" + node);
        }
        return p;
    }

    private static String normalizeNode(String node) {
        return node.contains(":") ? node : "ironhold:" + node;
    }

    private static PermState parseState(String s) {
        return switch (s.toLowerCase()) {
            case "grant" -> PermState.GRANT;
            case "deny" -> PermState.DENY;
            default -> PermState.UNSET;
        };
    }
}
