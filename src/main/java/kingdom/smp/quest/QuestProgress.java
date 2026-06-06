package kingdom.smp.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Per-player progress on a single quest.
 *
 * @param questId     id of the {@link QuestDef} this tracks.
 * @param status      current lifecycle state.
 * @param deadlineTick overworld game-time tick at which the quest expires (failed if not complete).
 * @param killCounts  per-objective kill tallies, parallel to {@link QuestDef#objectives()}.
 *                    Only {@link QuestObjective.Type#SLAY} objectives use this; inventory-backed
 *                    objectives are recomputed live, so their slot here stays 0.
 */
public record QuestProgress(String questId, QuestStatus status, long deadlineTick, List<Integer> killCounts) {

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("quest").forGetter(QuestProgress::questId),
            QuestStatus.CODEC.optionalFieldOf("status", QuestStatus.ACTIVE).forGetter(QuestProgress::status),
            Codec.LONG.optionalFieldOf("deadline", 0L).forGetter(QuestProgress::deadlineTick),
            Codec.INT.listOf().optionalFieldOf("kills", List.of()).forGetter(QuestProgress::killCounts)
    ).apply(i, QuestProgress::new));

    public QuestProgress {
        killCounts = List.copyOf(killCounts);
    }

    /** Fresh ACTIVE progress for a def, with zeroed kill tallies and the given deadline. */
    public static QuestProgress start(QuestDef def, long deadlineTick) {
        Integer[] zeros = new Integer[def.objectives().size()];
        java.util.Arrays.fill(zeros, 0);
        return new QuestProgress(def.id(), QuestStatus.ACTIVE, deadlineTick, List.of(zeros));
    }

    public QuestProgress withStatus(QuestStatus newStatus) {
        return new QuestProgress(questId, newStatus, deadlineTick, killCounts);
    }

    /** Returns a copy with objective {@code index}'s kill tally incremented by one (capped at need by callers). */
    public QuestProgress incrementKill(int index) {
        List<Integer> next = new java.util.ArrayList<>(killCounts);
        while (next.size() <= index) next.add(0);
        next.set(index, next.get(index) + 1);
        return new QuestProgress(questId, status, deadlineTick, next);
    }

    public int killCount(int index) {
        return index >= 0 && index < killCounts.size() ? killCounts.get(index) : 0;
    }
}
