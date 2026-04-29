package kingdom.smp.skill;

import kingdom.smp.net.SyncSkillStatePayload;

import java.util.Collections;
import java.util.Map;

/**
 * Client-side mirror of the player's profession-skill state. Updated by
 * {@link kingdom.smp.net.ModNetworking}'s S2C handler whenever the server pushes a
 * {@link SyncSkillStatePayload}. Read by {@link kingdom.smp.client.screen.SkillTreeScreen}
 * for rendering.
 *
 * Single static field, since there's only one local player on the client at a time.
 */
public final class ClientSkillData {
    private ClientSkillData() {}

    private static int unspentPoints = 0;
    private static Map<Profession, ProfessionRank> ranks = Collections.emptyMap();
    private static int milestoneCount = 0;
    private static boolean received = false;

    public static void receive(SyncSkillStatePayload payload) {
        unspentPoints = payload.unspentPoints();
        ranks = Map.copyOf(payload.ranks());
        milestoneCount = payload.milestoneCount();
        received = true;
    }

    public static int unspentPoints() { return unspentPoints; }
    public static Map<Profession, ProfessionRank> ranks() { return ranks; }
    public static int milestoneCount() { return milestoneCount; }
    public static boolean hasReceived() { return received; }

    public static ProfessionRank rankFor(Profession profession) {
        return ranks.get(profession);
    }

    /** Highest reachable rank if the player spent now (next rank, or null if at Master). */
    public static ProfessionRank nextRankFor(Profession profession) {
        ProfessionRank current = ranks.get(profession);
        if (current == null) return ProfessionRank.NOVICE;
        int nextOrdinal = current.ordinal() + 1;
        ProfessionRank[] values = ProfessionRank.values();
        return nextOrdinal >= values.length ? null : values[nextOrdinal];
    }

    /** Whether the player can afford the next rank in the given profession. */
    public static boolean canAffordNext(Profession profession) {
        ProfessionRank next = nextRankFor(profession);
        return next != null && unspentPoints >= next.pointCost();
    }
}
