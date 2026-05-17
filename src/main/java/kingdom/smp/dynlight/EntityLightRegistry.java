package kingdom.smp.dynlight;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

/**
 * Entity-type → luminance lookup loaded from {@code assets/<ns>/dynamiclights/entity/*.json}.
 * Only entities whose {@link EntityType} appears in the map glow on their own — this is the
 * "glowing mobs" half of the user-facing scope rule.
 */
public final class EntityLightRegistry {
    private EntityLightRegistry() {}

    /** Replaced atomically by {@link DynamicLightLoader} on resource reload. */
    private static volatile Map<Identifier, Integer> registered = Map.of();

    static void setRegistered(Map<Identifier, Integer> next) {
        registered = next;
    }

    public static int luminanceOf(Entity entity) {
        if (entity == null) return 0;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Integer v = id == null ? null : registered.get(id);
        return v == null ? 0 : Math.min(15, Math.max(0, v));
    }

    public static int registeredCount() { return registered.size(); }
}
