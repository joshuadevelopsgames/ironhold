package kingdom.smp.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One requirement of a {@link QuestDef}.
 *
 * <ul>
 *   <li>{@link Type#SLAY} — kill {@code count} entities whose type id equals {@code target}.
 *       Progress is a persisted tally, incremented on each matching kill.</li>
 *   <li>{@link Type#COLLECT} — have {@code count} of the item {@code target} anywhere in the
 *       player's inventory. Progress is recomputed live from the inventory; nothing is consumed.</li>
 *   <li>{@link Type#DELIVER} — like COLLECT for completion, but the items are consumed on redeem.</li>
 * </ul>
 *
 * {@code icon} is what the quest board draws in the task slot; {@code label} is the human
 * objective text. Defs are static catalog data, so no codec is needed here.
 */
public record QuestObjective(Type type, Identifier target, int count, ItemStack icon, String label) {

    public enum Type { SLAY, COLLECT, DELIVER }

    /** Slay {@code count} of the given entity type; {@code icon} is a representative display item. */
    public static QuestObjective slay(EntityType<?> entity, int count, Item icon, String label) {
        return new QuestObjective(Type.SLAY, BuiltInRegistries.ENTITY_TYPE.getKey(entity),
                count, new ItemStack(icon), label);
    }

    /** Hold {@code count} of {@code item} in inventory (not consumed). */
    public static QuestObjective collect(Item item, int count, String label) {
        return new QuestObjective(Type.COLLECT, BuiltInRegistries.ITEM.getKey(item),
                count, new ItemStack(item), label);
    }

    /** Hand over {@code count} of {@code item} (consumed on redeem). */
    public static QuestObjective deliver(Item item, int count, String label) {
        return new QuestObjective(Type.DELIVER, BuiltInRegistries.ITEM.getKey(item),
                count, new ItemStack(item), label);
    }

    /** True when this objective is inventory-backed (recomputed live) rather than a kill tally. */
    public boolean inventoryBacked() {
        return type == Type.COLLECT || type == Type.DELIVER;
    }
}
