package kingdom.smp.mine;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Structure tags maintained by Ironhold. Defined in code so {@link MineLookup}
 * can reference them with compile-time safety; the actual membership is in
 * {@code data/ironhold/tags/worldgen/structure/*.json}.
 */
public final class IronholdStructureTags {
    private IronholdStructureTags() {}

    /**
     * Any structure that should drive the {@link MineGeography#MINE_SHALLOW}/MID/DEEP
     * quality bands at break time. Currently aliases the vanilla mineshaft tag as a
     * bootstrap; Phase B2 will add a dedicated Ironhold Mine structure.
     */
    public static final TagKey<Structure> MINES = TagKey.create(
            Registries.STRUCTURE,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "mines"));
}
