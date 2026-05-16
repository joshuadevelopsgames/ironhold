package kingdom.smp.npc;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Builds the per-turn LLM context block that tells a manifest-aware NPC what
 * the player is currently holding and how the NPC should feel about it.
 *
 * <p>Kept minimal on purpose: only held-item awareness lives here. Full ware
 * and secret lists are not injected — those come into play when the player
 * explicitly invokes a Trade or Pay action.
 */
public final class NpcManifestPrompt {

    private NpcManifestPrompt() {}

    /** Build a per-turn context snippet. Returns empty string if there's nothing to say. */
    public static String build(@Nullable NpcManifest manifest, @Nullable ItemStack held) {
        if (held == null || held.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n[The player is holding: ")
          .append(NpcItemReaction.describeForPrompt(held))
          .append(".");

        if (manifest != null && manifest != NpcManifest.EMPTY) {
            NpcItemReaction.Reaction r = NpcItemReaction.resolve(manifest, held);
            if (r.isPresent()) {
                sb.append(" Your honest gut reaction would be: \"")
                  .append(r.line())
                  .append("\" — express that sentiment in your own words and voice, ")
                  .append("don't quote it verbatim.");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
