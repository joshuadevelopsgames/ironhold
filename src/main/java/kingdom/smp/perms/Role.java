package kingdom.smp.perms;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A named bag of grant rules with an explicit integer rank. Resolution scans roles by descending rank,
 * so precedence is something an operator can predict by reading — no LuckPerms-style weight arithmetic.
 */
public final class Role {

    public static final Codec<PermState> STATE_CODEC =
        Codec.STRING.xmap(PermState::valueOf, PermState::name);

    public static final Codec<Role> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(r -> r.id),
        Codec.INT.fieldOf("rank").forGetter(r -> r.rank),
        Codec.unboundedMap(Codec.STRING, STATE_CODEC).optionalFieldOf("rules", Map.of())
            .forGetter(r -> r.rules)
    ).apply(i, Role::new));

    private final String id;
    private int rank;
    private final Map<String, PermState> rules;

    public Role(String id, int rank, Map<String, PermState> rules) {
        this.id = id;
        this.rank = rank;
        this.rules = new LinkedHashMap<>(rules);
    }

    public String id() {
        return id;
    }

    public int rank() {
        return rank;
    }

    public Map<String, PermState> rules() {
        return rules;
    }

    public void setRule(String node, PermState state) {
        if (state == PermState.UNSET) {
            rules.remove(node);
        } else {
            rules.put(node, state);
        }
    }

    /** This role's effect on a node: an exact rule wins; otherwise the most specific matching wildcard. */
    public PermState effectFor(String node) {
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
}
