package kingdom.smp.quest;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * What a quest pays out on redeem.
 *
 * <ul>
 *   <li>{@code skillPoints} — added to the player's unspent profession-point pool
 *       (the same pool spent in the skill tree), via a milestone keyed by quest id
 *       so it can never be claimed twice.</li>
 *   <li>{@code classXp} — granted through the normal class-XP path (may level the player up).</li>
 *   <li>{@code items} — item stacks handed to the player (gold coins go here too).</li>
 * </ul>
 */
public record QuestReward(int skillPoints, int classXp, List<ItemStack> items) {

    public QuestReward {
        items = List.copyOf(items);
    }

    public static QuestReward of(int skillPoints, int classXp, ItemStack... items) {
        return new QuestReward(skillPoints, classXp, List.of(items));
    }

    public boolean hasSkillPoints() { return skillPoints > 0; }
    public boolean hasClassXp() { return classXp > 0; }
}
