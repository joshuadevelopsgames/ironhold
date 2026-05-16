package kingdom.smp.quest;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Plain data carrier for one quest displayed on the {@link QuestBoardMenu}.
 *
 * <p>{@code title} is the heading text on the parchment. {@code description}
 * is one line per element (manual line breaks). {@code tasks} are what the
 * player needs to deliver/collect — each task slot shows an item icon plus a
 * "have / need" count. {@code rewards} are what the player gets on REDEEM —
 * each reward slot is a single ItemStack (with whatever count the stack holds).
 */
public record QuestData(
    String title,
    List<String> description,
    List<Task> tasks,
    List<ItemStack> rewards,
    boolean redeemable
) {
    /** Single task: collect {@code need} of {@code item}, currently have {@code have}. */
    public record Task(ItemStack item, int have, int need) {
        public boolean complete() { return have >= need; }
    }

    /** Empty placeholder for client-side menu construction before server data arrives. */
    public static final QuestData EMPTY = new QuestData(
        "", List.of(), List.of(), List.of(), false);

    /** Hardcoded sample for testing the GUI without a real quest backend. */
    public static QuestData sample() {
        return new QuestData(
            "Hunt the Pack",
            List.of(
                "Wolves prey on villager",
                "livestock. Cull the pack",
                "and bring proof."
            ),
            List.of(
                new Task(new ItemStack(Items.RABBIT_HIDE), 2, 5),
                new Task(new ItemStack(Items.BONE), 0, 3)
            ),
            List.of(
                new ItemStack(Items.EMERALD, 4),
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.COOKED_BEEF, 8)
            ),
            false  // not redeemable until tasks are complete
        );
    }
}
