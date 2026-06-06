package kingdom.smp.perms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

/**
 * Pure resolution with provenance. Precedence is explicit and deterministic:
 * <ol>
 *   <li>per-player overrides (exact node beats wildcard),</li>
 *   <li>assigned roles by descending rank — first GRANT/DENY wins,</li>
 *   <li>fall back to the perm's vanilla {@link PermissionLevel}.</li>
 * </ol>
 * Every step is recorded into the returned {@link Decision#trace()}, so a live check and {@code /perm why}
 * are the same code path.
 */
public final class PermissionResolver {
    private PermissionResolver() {}

    public static Decision resolve(CommandSourceStack source, Perm perm) {
        List<String> trace = new ArrayList<>();
        ServerPlayer player = source.getPlayer();
        PermissionSet vanilla = source.permissions();

        if (player == null) {
            boolean allowed = vanillaAllows(vanilla, perm.defaultLevel(), trace);
            return new Decision(allowed, "non-player source; vanilla default "
                + perm.defaultLevel().getSerializedName(), trace);
        }

        // 1 + 2. Store resolution (player overrides, then roles by rank).
        PermState store = resolveStore(source.getLevel(), player.getUUID(), player.getName().getString(),
            perm.node(), trace);
        if (store != PermState.UNSET) {
            return new Decision(store == PermState.GRANT, "store decision: " + store, trace);
        }

        // 3. Vanilla default level fallback.
        boolean allowed = vanillaAllows(vanilla, perm.defaultLevel(), trace);
        return new Decision(allowed, "fell back to vanilla default level "
            + perm.defaultLevel().getSerializedName(), trace);
    }

    /**
     * Store-only resolution: player overrides (exact beats wildcard), then assigned roles by descending
     * rank. Returns {@link PermState#UNSET} when the store has nothing to say — callers decide the
     * fallback (the vanilla level for command checks, the base set for the {@link IronholdPermissionSet}).
     * {@code trace} may be null. {@code name} is only used for trace text.
     */
    public static PermState resolveStore(ServerLevel level, UUID id, String name, String node, List<String> trace) {
        PermissionData data = PermissionData.get(level);

        PermState ov = bestEffect(data.overridesOf(id), node);
        if (ov != PermState.UNSET) {
            if (trace != null) {
                trace.add("player override on " + name + " -> " + ov);
            }
            return ov;
        }
        if (trace != null) {
            trace.add("no matching player override");
        }

        List<Role> assigned = new ArrayList<>();
        for (String rid : data.rolesOf(id)) {
            Role r = data.role(rid);
            if (r != null) {
                assigned.add(r);
            }
        }
        if (assigned.isEmpty()) {
            if (trace != null) {
                trace.add("no roles assigned");
            }
            return PermState.UNSET;
        }
        assigned.sort((a, b) -> Integer.compare(b.rank(), a.rank()));
        for (Role r : assigned) {
            PermState eff = r.effectFor(node);
            if (trace != null) {
                trace.add("role '" + r.id() + "' (rank " + r.rank() + ") -> " + eff);
            }
            if (eff != PermState.UNSET) {
                return eff;
            }
        }
        return PermState.UNSET;
    }

    private static PermState bestEffect(Map<String, PermState> rules, String node) {
        PermState exact = rules.get(node);
        if (exact != null) {
            return exact;
        }
        PermState best = null;
        int bestLen = -1;
        for (Map.Entry<String, PermState> e : rules.entrySet()) {
            String pat = e.getKey();
            if ((pat.equals("*") || pat.endsWith(".*")) && ModPermissions.nodeMatches(pat, node) && pat.length() > bestLen) {
                bestLen = pat.length();
                best = e.getValue();
            }
        }
        return best == null ? PermState.UNSET : best;
    }

    private static boolean vanillaAllows(PermissionSet set, PermissionLevel level, List<String> trace) {
        Permission required = vanillaPermissionFor(level);
        if (required == null) {
            trace.add("default level ALL -> granted to everyone");
            return true;
        }
        boolean ok = set.hasPermission(required);
        trace.add("vanilla PermissionSet.hasPermission(" + level.getSerializedName() + ") -> " + ok);
        return ok;
    }

    /** Map a registered default level onto the vanilla command permission constant that gates it. */
    static Permission vanillaPermissionFor(PermissionLevel level) {
        if (level == PermissionLevel.MODERATORS) {
            return Permissions.COMMANDS_MODERATOR;
        }
        if (level == PermissionLevel.GAMEMASTERS) {
            return Permissions.COMMANDS_GAMEMASTER;
        }
        if (level == PermissionLevel.ADMINS) {
            return Permissions.COMMANDS_ADMIN;
        }
        if (level == PermissionLevel.OWNERS) {
            return Permissions.COMMANDS_OWNER;
        }
        return null; // ALL — no permission required
    }
}
