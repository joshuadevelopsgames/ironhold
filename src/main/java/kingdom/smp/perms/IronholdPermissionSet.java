package kingdom.smp.perms;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;

/**
 * A {@link PermissionSet} that overlays the server's role/override store on top of the vanilla
 * level-based set. This is the seam that makes Ironhold a server-wide permissions manager: because
 * MC 1.26 routes every permission check (vanilla commands and modded alike) through a player's
 * {@code PermissionSet}, owning that set lets the store grant or deny <em>any</em> mod's
 * {@link Permission.Atom} — identified by {@link net.minecraft.resources.Identifier} — not just Ironhold's.
 *
 * <p>Resolution for an atom: store GRANT/DENY wins; on UNSET, a registered {@link Perm} falls back to its
 * declared default level, and an unknown atom falls back to the vanilla base set. Non-atom permissions
 * (e.g. {@code HasCommandLevel}) always defer to the base, so default behavior is unchanged for any
 * player with no roles or overrides.
 */
public final class IronholdPermissionSet implements PermissionSet {

    private final ServerLevel level;
    private final UUID player;
    private final PermissionSet base;

    public IronholdPermissionSet(ServerLevel level, UUID player, PermissionSet base) {
        this.level = level;
        this.player = player;
        this.base = base;
    }

    public PermissionSet base() {
        return base;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        if (perm instanceof Permission.Atom atom) {
            String node = atom.id().toString();
            PermState s = PermissionResolver.resolveStore(level, player, "", node, null);
            if (s == PermState.GRANT) {
                return true;
            }
            if (s == PermState.DENY) {
                return false;
            }
            // UNSET: honor a registered perm's declared default level, else defer to vanilla.
            Perm known = ModPermissions.byNode(node);
            if (known != null) {
                Permission required = PermissionResolver.vanillaPermissionFor(known.defaultLevel());
                return required == null || base.hasPermission(required);
            }
        }
        return base.hasPermission(perm);
    }
}
