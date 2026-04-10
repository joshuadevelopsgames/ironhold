package kingdom.smp.item.gear;

import java.util.EnumMap;
import java.util.Map;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * All armor materials (5 classes × 3 tiers = 15) and tool materials (Steel, Tanzanite).
 *
 * <p>Defense values by class philosophy:
 * <ul>
 *   <li>Knight  — heaviest plate, highest defense + toughness
 *   <li>Cleric  — heavy holy, just below Knight
 *   <li>Ranger  — medium leather-plate hybrid
 *   <li>Rogue   — light, agile, low defense
 *   <li>Wizard  — cloth, minimal defense
 * </ul>
 */
public final class GearMaterials {
    private GearMaterials() {}

    // ── Repair tag keys ──────────────────────────────────────────────────────────
    public static final TagKey<Item> REPAIRS_STEEL     =
        TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "repairs_steel_gear"));
    public static final TagKey<Item> REPAIRS_TANZANITE =
        TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "repairs_tanzanite_gear"));

    // ── Tool materials ────────────────────────────────────────────────────────────
    /** Steel — between iron and diamond in durability and damage. */
    public static final ToolMaterial STEEL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_IRON_TOOL,
        400, 7.5f, 1.5f, 11, REPAIRS_STEEL);

    /** Tanzanite — close to netherite tier, magic-touched. */
    public static final ToolMaterial TANZANITE = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        600, 9.0f, 2.5f, 14, REPAIRS_TANZANITE);

    // ── Armor materials ───────────────────────────────────────────────────────────

    // Knight
    public static final ArmorMaterial KNIGHT_IRON      = make(PlayerClass.KNIGHT, GearTier.IRON);
    public static final ArmorMaterial KNIGHT_STEEL     = make(PlayerClass.KNIGHT, GearTier.STEEL);
    public static final ArmorMaterial KNIGHT_TANZANITE = make(PlayerClass.KNIGHT, GearTier.TANZANITE);

    // Ranger
    public static final ArmorMaterial RANGER_IRON      = make(PlayerClass.RANGER, GearTier.IRON);
    public static final ArmorMaterial RANGER_STEEL     = make(PlayerClass.RANGER, GearTier.STEEL);
    public static final ArmorMaterial RANGER_TANZANITE = make(PlayerClass.RANGER, GearTier.TANZANITE);

    // Rogue
    public static final ArmorMaterial ROGUE_IRON       = make(PlayerClass.ROGUE, GearTier.IRON);
    public static final ArmorMaterial ROGUE_STEEL      = make(PlayerClass.ROGUE, GearTier.STEEL);
    public static final ArmorMaterial ROGUE_TANZANITE  = make(PlayerClass.ROGUE, GearTier.TANZANITE);

    // Cleric
    public static final ArmorMaterial CLERIC_IRON      = make(PlayerClass.CLERIC, GearTier.IRON);
    public static final ArmorMaterial CLERIC_STEEL     = make(PlayerClass.CLERIC, GearTier.STEEL);
    public static final ArmorMaterial CLERIC_TANZANITE = make(PlayerClass.CLERIC, GearTier.TANZANITE);

    // Wizard
    public static final ArmorMaterial WIZARD_IRON      = make(PlayerClass.WIZARD, GearTier.IRON);
    public static final ArmorMaterial WIZARD_STEEL     = make(PlayerClass.WIZARD, GearTier.STEEL);
    public static final ArmorMaterial WIZARD_TANZANITE = make(PlayerClass.WIZARD, GearTier.TANZANITE);

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private static ArmorMaterial make(PlayerClass c, GearTier t) {
        String assetId = c.name().toLowerCase() + "_" + t.id;
        ResourceKey<EquipmentAsset> assetKey = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, assetId));
        return new ArmorMaterial(
            durability(t),
            defense(c, t),
            enchantability(c, t),
            equipSound(c, t),
            toughness(c, t),
            knockbackResistance(c, t),
            repairTag(t),
            assetKey);
    }

    private static int durability(GearTier t) {
        return switch (t) {
            case IRON      -> 15;
            case STEEL     -> 22;
            case TANZANITE -> 32;
        };
    }

    /** Defense values (boots / leggings / chestplate / helmet) by class+tier. */
    private static Map<ArmorType, Integer> defense(PlayerClass c, GearTier t) {
        int[] bchh = switch (c) {
            case KNIGHT  -> switch (t) { case IRON -> new int[]{2,5,7,3}; case STEEL -> new int[]{3,6,8,3}; default -> new int[]{3,6,9,4}; };
            case CLERIC  -> switch (t) { case IRON -> new int[]{2,5,6,3}; case STEEL -> new int[]{3,6,7,3}; default -> new int[]{3,6,8,4}; };
            case RANGER  -> switch (t) { case IRON -> new int[]{2,4,5,2}; case STEEL -> new int[]{2,5,6,2}; default -> new int[]{2,5,7,2}; };
            case ROGUE   -> switch (t) { case IRON -> new int[]{1,3,4,1}; case STEEL -> new int[]{2,4,5,2}; default -> new int[]{2,5,6,2}; };
            case WIZARD  -> switch (t) { case IRON -> new int[]{1,2,3,1}; case STEEL -> new int[]{1,3,4,1}; default -> new int[]{2,4,5,2}; };
            case PEASANT -> new int[]{1,2,3,1};
            default -> new int[]{1,3,4,1};
        };
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        m.put(ArmorType.BOOTS,      bchh[0]);
        m.put(ArmorType.LEGGINGS,   bchh[1]);
        m.put(ArmorType.CHESTPLATE, bchh[2]);
        m.put(ArmorType.HELMET,     bchh[3]);
        m.put(ArmorType.BODY,       bchh[2]);
        return m;
    }

    private static int enchantability(PlayerClass c, GearTier t) {
        // Wizard gear is more enchantable; T3 gets a slight bump across the board
        int base = (c == PlayerClass.WIZARD) ? 14 : 9;
        return base + (t == GearTier.TANZANITE ? 2 : 0);
    }

    private static float toughness(PlayerClass c, GearTier t) {
        if (t == GearTier.IRON) return 0.0f;
        return switch (c) {
            case KNIGHT, CLERIC -> t == GearTier.STEEL ? 1.0f : 2.0f;
            case RANGER         -> t == GearTier.STEEL ? 0.5f : 1.0f;
            case ROGUE, WIZARD  -> t == GearTier.STEEL ? 0.0f : 0.5f;
            case PEASANT        -> 0.0f;
            default             -> t == GearTier.STEEL ? 0.5f : 1.0f;
        };
    }

    private static float knockbackResistance(PlayerClass c, GearTier t) {
        if (t != GearTier.TANZANITE) return 0.0f;
        return switch (c) {
            case KNIGHT  -> 0.10f;
            case CLERIC  -> 0.05f;
            default      -> 0.0f;
        };
    }

    private static Holder<SoundEvent> equipSound(PlayerClass c, GearTier t) {
        if (t == GearTier.TANZANITE) return SoundEvents.ARMOR_EQUIP_NETHERITE;
        return switch (c) {
            case KNIGHT, CLERIC -> SoundEvents.ARMOR_EQUIP_IRON;
            default             -> SoundEvents.ARMOR_EQUIP_LEATHER;
        };
    }

    private static TagKey<Item> repairTag(GearTier t) {
        return switch (t) {
            case IRON      -> ItemTags.REPAIRS_IRON_ARMOR;
            case STEEL     -> REPAIRS_STEEL;
            case TANZANITE -> REPAIRS_TANZANITE;
        };
    }

    /** Look up the pre-built ArmorMaterial for a class + tier. */
    public static ArmorMaterial get(PlayerClass c, GearTier t) {
        return switch (c) {
            case KNIGHT  -> switch (t) { case IRON -> KNIGHT_IRON;  case STEEL -> KNIGHT_STEEL;  default -> KNIGHT_TANZANITE; };
            case RANGER  -> switch (t) { case IRON -> RANGER_IRON;  case STEEL -> RANGER_STEEL;  default -> RANGER_TANZANITE; };
            case ROGUE   -> switch (t) { case IRON -> ROGUE_IRON;   case STEEL -> ROGUE_STEEL;   default -> ROGUE_TANZANITE;  };
            case CLERIC  -> switch (t) { case IRON -> CLERIC_IRON;  case STEEL -> CLERIC_STEEL;  default -> CLERIC_TANZANITE; };
            case WIZARD  -> switch (t) { case IRON -> WIZARD_IRON;  case STEEL -> WIZARD_STEEL;  default -> WIZARD_TANZANITE; };
            case PEASANT -> KNIGHT_IRON; // fallback
            default -> KNIGHT_IRON;
        };
    }
}
