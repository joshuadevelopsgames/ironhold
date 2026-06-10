package kingdom.smp.quest;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Plain data carrier for one quest displayed on the {@link QuestBoardMenu}.
 *
 * <p>{@code questId} ties the board back to the {@link Quests} catalog entry so REDEEM can call
 * {@link QuestService#redeem}; empty for informational boards (no active quest / sample).
 * {@code title} is the heading text on the parchment. {@code description}
 * is one line per element (manual line breaks). {@code tasks} are what the
 * player needs to deliver/collect — each task slot shows an item icon plus a
 * "have / need" count. {@code rewards} are what the player gets on REDEEM —
 * each reward slot is a single ItemStack (with whatever count the stack holds).
 *
 * <p>Synced server → client as the menu's extra open data via {@link #STREAM_CODEC}.
 */
public record QuestData(
    String questId,
    String title,
    List<String> description,
    List<Task> tasks,
    List<ItemStack> rewards,
    boolean redeemable
) {
    /** Single task: collect {@code need} of {@code item}, currently have {@code have}. */
    public record Task(ItemStack item, int have, int need) {
        public boolean complete() { return have >= need; }

        public static final StreamCodec<RegistryFriendlyByteBuf, Task> STREAM_CODEC =
            StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Task::item,
                ByteBufCodecs.VAR_INT, Task::have,
                ByteBufCodecs.VAR_INT, Task::need,
                Task::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestData> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestData::questId,
            ByteBufCodecs.STRING_UTF8, QuestData::title,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestData::description,
            Task.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestData::tasks,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), QuestData::rewards,
            ByteBufCodecs.BOOL, QuestData::redeemable,
            QuestData::new);

    /** Empty placeholder for client-side menu construction before server data arrives. */
    public static final QuestData EMPTY = new QuestData(
        "", "", List.of(), List.of(), List.of(), false);

    /** Informational board shown when the player has no accepted quest. */
    public static QuestData noActiveQuest() {
        return new QuestData(
            "",
            "No Active Quest",
            List.of(
                "The board is bare.",
                "Seek out the kingdom's",
                "folk for work."
            ),
            List.of(), List.of(), false);
    }

    /** Hardcoded sample for testing the GUI without a real quest backend. */
    public static QuestData sample() {
        return new QuestData(
            "",
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
