package kingdom.smp.npc;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Pure helpers for resolving a player's held item into an NPC reaction —
 * scripted line from the manifest, structured context for an LLM prompt, etc.
 *
 * <p>Stateless: callers apply the rapport delta and dispatch the line however
 * fits their dialogue paradigm (one-shot bubble, LLM injection, etc.).
 */
public final class NpcItemReaction {

    private NpcItemReaction() {}

    public record Reaction(String line, int rapportDelta, Kind kind) {
        public enum Kind { TASTE, DISDAIN, NEUTRAL, NONE }
        public boolean isPresent() { return kind != Kind.NONE; }
    }

    public static final Reaction NONE = new Reaction("", 0, Reaction.Kind.NONE);

    /** Returns the manifest reaction for a held item, or {@link #NONE} if empty/no match. */
    public static Reaction resolve(NpcManifest manifest, ItemStack held) {
        if (manifest == null || manifest == NpcManifest.EMPTY) return NONE;
        if (held == null || held.isEmpty()) return NONE;

        String id = itemId(held);
        if (id == null) return NONE;

        NpcManifest.Taste taste = manifest.tasteFor(id);
        if (taste != null) {
            return new Reaction(taste.reaction(), taste.rapportDelta(), Reaction.Kind.TASTE);
        }
        NpcManifest.Disdain disdain = manifest.disdainFor(id);
        if (disdain != null) {
            return new Reaction(disdain.reaction(), disdain.rapportDelta(), Reaction.Kind.DISDAIN);
        }
        return NONE;
    }

    /** Short description suitable for inclusion in an LLM system/user prompt. */
    public static String describeForPrompt(ItemStack held) {
        if (held == null || held.isEmpty()) return "(empty hand)";
        String id = itemId(held);
        int count = held.getCount();
        int dmg = held.getDamageValue();
        int maxDmg = held.getMaxDamage();
        boolean hasName = held.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);

        StringBuilder sb = new StringBuilder();
        sb.append(id == null ? "unknown" : id);
        if (count > 1) sb.append(" x").append(count);
        if (maxDmg > 0) {
            int durabilityPct = Math.round(100f * (maxDmg - dmg) / maxDmg);
            sb.append(" (").append(durabilityPct).append("% durability)");
        }
        if (hasName) {
            String name = held.getHoverName().getString();
            sb.append(" — named \"").append(name).append("\"");
        }
        return sb.toString();
    }

    public static @Nullable String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }
}
