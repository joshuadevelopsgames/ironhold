package kingdom.smp.perms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Code-defined registry of every Ironhold permission. Mirrors the {@code ModItems}/{@code ModBlocks}
 * pattern: permissions are registered objects, not magic strings. This makes the node universe finite
 * and enumerable, which is what lets {@link #matching(String)} expand wildcards soundly.
 */
public final class ModPermissions {
    private ModPermissions() {}

    private static final Map<String, Perm> REGISTRY = new LinkedHashMap<>();

    public static Perm register(String path, String description, PermissionLevel defaultLevel, String category) {
        Identifier id = Identifier.fromNamespaceAndPath(Ironhold.MODID, path);
        Perm perm = new Perm(id, description, defaultLevel, category);
        REGISTRY.put(perm.node(), perm);
        return perm;
    }

    // ---- Declared Ironhold permissions ----

    /** Gate for the administrative {@code /k2} command tree. Default ≈ op level 2 (GAMEMASTERS). */
    public static final Perm COMMAND_ADMIN = register(
        "command.admin",
        "Use Ironhold administrative commands (the /k2 tree).",
        PermissionLevel.GAMEMASTERS,
        "command");

    /** Gate for managing the permission system itself. */
    public static final Perm PERM_MANAGE = register(
        "perm.manage",
        "Manage Ironhold permissions: roles, assignments, and overrides.",
        PermissionLevel.ADMINS,
        "command");

    public static Perm byNode(String node) {
        return REGISTRY.get(node);
    }

    public static List<Perm> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    /** Expand a node or wildcard pattern against the registry — the basis for sound wildcards. */
    public static List<Perm> matching(String pattern) {
        List<Perm> out = new ArrayList<>();
        for (Perm p : REGISTRY.values()) {
            if (nodeMatches(pattern, p.node())) {
                out.add(p);
            }
        }
        return out;
    }

    /** {@code *} matches everything; {@code ns:foo.*} matches any node under {@code ns:foo.}; else exact. */
    static boolean nodeMatches(String pattern, String node) {
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1); // keep the trailing dot
            return node.startsWith(prefix);
        }
        return pattern.equals(node);
    }
}
