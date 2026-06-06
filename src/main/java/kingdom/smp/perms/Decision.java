package kingdom.smp.perms;

import java.util.List;

/**
 * The result of a permission check: the boolean outcome plus the provenance that produced it. The
 * resolver returns this for every check, so {@code /perm why} uses the exact same code path as a live
 * check rather than a separate "verbose" reimplementation — explainability is built in, not bolted on.
 */
public record Decision(boolean allowed, String summary, List<String> trace) {}
