package kingdom.smp.quest;

import java.util.List;

/**
 * Static definition of one quest in the {@link Quests} catalog. Immutable content data —
 * per-player state lives in {@link QuestProgress}.
 *
 * @param id           stable unique id ({@code "tobias_proving_the_forge"}); also the milestone
 *                     key for the skill-point reward, so it must never change once shipped.
 * @param giver        key of the NPC that offers this quest ({@code "blacksmith_tobias"});
 *                     {@code "board"} for quest-board-only quests with no specific giver.
 * @param title        heading shown on the parchment.
 * @param description  flavor text, one entry per rendered line.
 * @param objectives   what must be done (1+). Quest completes when every objective is met.
 * @param reward       paid out on redeem.
 * @param durationTicks time limit once accepted, in ticks (20/sec). Every quest is timed.
 */
public record QuestDef(
        String id,
        String giver,
        String title,
        List<String> description,
        List<QuestObjective> objectives,
        QuestReward reward,
        int durationTicks) {

    public QuestDef {
        description = List.copyOf(description);
        objectives = List.copyOf(objectives);
    }

    /** Milestone id used to make the skill-point reward idempotent. */
    public String milestoneId() {
        return "quest_" + id;
    }

    public int durationSeconds() {
        return durationTicks / 20;
    }
}
