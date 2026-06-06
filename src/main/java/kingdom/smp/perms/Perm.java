package kingdom.smp.perms;

import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * A registered, typed permission. Unlike LuckPerms' stringly-typed nodes, every Perm is a code-defined
 * object with a description and a vanilla {@link PermissionLevel} fallback, so typos are compile errors,
 * the full node universe is enumerable, and wildcards can be expanded soundly against the registry.
 */
public record Perm(Identifier id, String description, PermissionLevel defaultLevel, String category) {

    /** The canonical node string, e.g. {@code ironhold:command.admin}. */
    public String node() {
        return id.toString();
    }

    /** The vanilla {@link Permission} atom for this perm — usable directly with a {@code PermissionSet}. */
    public Permission atom() {
        return Permission.Atom.create(id);
    }
}
