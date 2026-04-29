package kingdom.smp.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Per-player profession-tree state.
 *
 * <ul>
 *   <li>{@code unspentProfessionPoints} — points awarded by milestones that haven't been spent yet.
 *       Starts at 3 (per spec §3.1 starting allocation).</li>
 *   <li>{@code currentRanks} — highest unlocked rank per profession. A profession not in the map
 *       has not yet had any points spent in it.</li>
 *   <li>{@code milestonesCompleted} — set of milestone IDs already awarded to this player. Used
 *       to prevent double-awarding per-character unlocks.</li>
 * </ul>
 *
 * Class-tree state is intentionally NOT in this record — it lives in the existing
 * {@link kingdom.smp.rpg.PlayerKingdomRpgData} attachment alongside class XP. Profession state
 * is server-level SavedData (per spec §8.5) so admin tooling and server-first milestones have
 * a single source of truth.
 *
 * @see <a href="../../../../specs/profession-skill-system.md">profession-skill-system.md §8.5</a>
 */
public record PlayerSkillState(
        int unspentProfessionPoints,
        Map<Profession, ProfessionRank> currentRanks,
        Set<String> milestonesCompleted) {

    public static final int STARTING_POINTS = 3;

    public static final Codec<PlayerSkillState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("unspent", STARTING_POINTS).forGetter(PlayerSkillState::unspentProfessionPoints),
            Codec.unboundedMap(Profession.CODEC, ProfessionRank.CODEC)
                    .optionalFieldOf("ranks", Map.of()).forGetter(PlayerSkillState::currentRanks),
            Codec.STRING.listOf()
                    .optionalFieldOf("milestones", java.util.List.of())
                    .forGetter(s -> new java.util.ArrayList<>(s.milestonesCompleted()))
    ).apply(i, (unspent, ranks, milestones) -> new PlayerSkillState(unspent, ranks, new HashSet<>(milestones))));

    /** Defensive copy on construction so the maps/sets aren't shared references. */
    public PlayerSkillState {
        currentRanks = new EnumMap<>(currentRanks);
        milestonesCompleted = new HashSet<>(milestonesCompleted);
    }

    public static PlayerSkillState fresh() {
        return new PlayerSkillState(STARTING_POINTS, new EnumMap<>(Profession.class), new HashSet<>());
    }

    /** Highest rank unlocked in the given profession, or null if no points have been spent there. */
    public ProfessionRank rankFor(Profession profession) {
        return currentRanks.get(profession);
    }

    /** Total points spent across all professions. */
    public int totalPointsSpent() {
        return currentRanks.values().stream().mapToInt(ProfessionRank::cumulativeCost).sum();
    }

    public boolean hasMilestone(String milestoneId) {
        return milestonesCompleted.contains(milestoneId);
    }

    /**
     * Attempt to spend a point to advance the next rank in the given profession.
     * Returns the new state on success, or {@code null} if not enough points are available
     * or the profession is already at Master.
     */
    public PlayerSkillState trySpendOn(Profession profession) {
        ProfessionRank current = currentRanks.get(profession);
        ProfessionRank next = current == null ? ProfessionRank.NOVICE : nextRank(current);
        if (next == null) return null;
        if (unspentProfessionPoints < next.pointCost()) return null;

        Map<Profession, ProfessionRank> newRanks = new EnumMap<>(currentRanks);
        newRanks.put(profession, next);
        return new PlayerSkillState(
                unspentProfessionPoints - next.pointCost(),
                newRanks,
                milestonesCompleted);
    }

    /** Award a milestone — grants its point value and records the milestone ID so it can't repeat. */
    public PlayerSkillState withMilestone(String milestoneId, int pointValue) {
        if (milestonesCompleted.contains(milestoneId)) return this;
        Set<String> newSet = new HashSet<>(milestonesCompleted);
        newSet.add(milestoneId);
        return new PlayerSkillState(
                unspentProfessionPoints + pointValue,
                currentRanks,
                newSet);
    }

    /**
     * Lossy respec of a single profession — refunds {@code (rank.cumulativeCost − 1)} points
     * and clears progress in the given profession. Used by debug commands; the player UI uses
     * {@link #respecAll()} instead.
     */
    public PlayerSkillState respec(Profession profession) {
        ProfessionRank current = currentRanks.get(profession);
        if (current == null) return this;
        int refund = Math.max(0, current.cumulativeCost() - 1);
        Map<Profession, ProfessionRank> newRanks = new EnumMap<>(currentRanks);
        newRanks.remove(profession);
        return new PlayerSkillState(
                unspentProfessionPoints + refund,
                newRanks,
                milestonesCompleted);
    }

    /**
     * Global respec — clears every profession's rank and refunds {@code (totalSpent − 1)}
     * points. The −1 is a single global penalty (not per-profession), so respeccing two
     * professions costs the same as respeccing one. If nothing is spent, returns {@code this}.
     */
    public PlayerSkillState respecAll() {
        int totalSpent = totalPointsSpent();
        if (totalSpent <= 0) return this;
        int refund = Math.max(0, totalSpent - 1);
        return new PlayerSkillState(
                unspentProfessionPoints + refund,
                new EnumMap<>(Profession.class),
                milestonesCompleted);
    }

    private static ProfessionRank nextRank(ProfessionRank current) {
        int nextOrdinal = current.ordinal() + 1;
        ProfessionRank[] values = ProfessionRank.values();
        return nextOrdinal >= values.length ? null : values[nextOrdinal];
    }

    /** Compact summary string for /k2 skill info output. */
    public String summary() {
        String ranks = currentRanks.entrySet().stream()
                .map(e -> e.getKey().displayName() + "=" + e.getValue().displayName())
                .collect(Collectors.joining(", "));
        if (ranks.isEmpty()) ranks = "(none)";
        return "Unspent: " + unspentProfessionPoints
                + " | Spent: " + totalPointsSpent()
                + " | Ranks: " + ranks
                + " | Milestones: " + milestonesCompleted.size();
    }
}
