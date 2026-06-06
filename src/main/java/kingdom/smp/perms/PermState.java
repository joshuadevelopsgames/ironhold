package kingdom.smp.perms;

/** Three-state permission value. DENY is explicit and stops resolution; UNSET defers to the next source. */
public enum PermState {
    GRANT,
    DENY,
    UNSET
}
