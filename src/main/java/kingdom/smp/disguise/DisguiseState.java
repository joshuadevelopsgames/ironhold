package kingdom.smp.disguise;

import java.util.Optional;

import net.minecraft.resources.Identifier;

/**
 * The entity a player is currently disguised as, or {@link #NONE}. Held as a transient
 * attachment on the server (it never persists — any damage clears it) and mirrored to
 * clients via {@link kingdom.smp.net.SyncDisguisePayload} into the client disguise cache.
 *
 * @param entityTypeId registry id of the entity type to render the player as, empty when undisguised.
 */
public record DisguiseState(Optional<Identifier> entityTypeId) {

    public static final DisguiseState NONE = new DisguiseState(Optional.empty());

    public static DisguiseState of(Identifier id) {
        return new DisguiseState(Optional.of(id));
    }

    public boolean active() {
        return entityTypeId.isPresent();
    }
}
