package kingdom.smp.npc;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by NPC entities that opt into the datapack-driven manifest
 * system. Lookup resolves {@link #specificManifestId()} first, then falls back
 * to {@link #fallbackManifestId()}. Both may be null — in that case the entity
 * gets {@link NpcManifest#EMPTY}.
 */
public interface HasNpcManifest {

    /** e.g. {@code ironhold:npc/tobias}. Null if the entity has no specific manifest. */
    @Nullable Identifier specificManifestId();

    /** e.g. {@code ironhold:profession/blacksmith}. Null if the entity has no profession-level manifest. */
    @Nullable Identifier fallbackManifestId();

    default NpcManifest manifest() {
        return NpcManifests.resolve(specificManifestId(), fallbackManifestId());
    }
}
