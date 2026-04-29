package kingdom.smp.game;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Datapack tags under {@code data/ironhold/tags/entity_type/}. */
public final class KingdomEntityTags {
    public static final TagKey<EntityType<?>> RANGER_FAVORED =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Ironhold.MODID, "ranger_favored"));
    public static final TagKey<EntityType<?>> CLERIC_FAVORED =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Ironhold.MODID, "cleric_favored"));
    public static final TagKey<EntityType<?>> WIZARD_FAVORED =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizard_favored"));
    public static final TagKey<EntityType<?>> KNIGHT_FAVORED =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_favored"));
    public static final TagKey<EntityType<?>> PATROL_BUFF_CANDIDATES =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Ironhold.MODID, "patrol_buff_candidates"));

    private KingdomEntityTags() {}
}
