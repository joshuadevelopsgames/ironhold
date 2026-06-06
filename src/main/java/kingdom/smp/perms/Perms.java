package kingdom.smp.perms;

import net.minecraft.commands.CommandSourceStack;

/** Facade for command {@code .requires(...)} gates: {@code .requires(s -> Perms.check(s, MY_PERM))}. */
public final class Perms {
    private Perms() {}

    public static boolean check(CommandSourceStack source, Perm perm) {
        return PermissionResolver.resolve(source, perm).allowed();
    }
}
