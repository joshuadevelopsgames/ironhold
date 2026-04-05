package kingdom.smp.item.gear;

import java.util.ArrayList;
import java.util.List;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all 60 class armor items (5 classes × 3 tiers × 4 pieces).
 *
 * <p>Item IDs follow the pattern: {@code <class>_<tier>_<piece>}
 * e.g. {@code knight_iron_chestplate}, {@code ranger_tanzanite_boots}.
 */
public final class ClassArmor {
    private ClassArmor() {}

    /** All registered armor items — added to the COMBAT creative tab. */
    public static final List<DeferredItem<Item>> ALL = new ArrayList<>(60);

    // ── Knight ────────────────────────────────────────────────────────────────────
    public static DeferredItem<Item> KNIGHT_IRON_HELMET,      KNIGHT_IRON_CHESTPLATE,      KNIGHT_IRON_LEGGINGS,      KNIGHT_IRON_BOOTS;
    public static DeferredItem<Item> KNIGHT_STEEL_HELMET,     KNIGHT_STEEL_CHESTPLATE,     KNIGHT_STEEL_LEGGINGS,     KNIGHT_STEEL_BOOTS;
    public static DeferredItem<Item> KNIGHT_TANZANITE_HELMET, KNIGHT_TANZANITE_CHESTPLATE, KNIGHT_TANZANITE_LEGGINGS, KNIGHT_TANZANITE_BOOTS;

    // ── Ranger ────────────────────────────────────────────────────────────────────
    public static DeferredItem<Item> RANGER_IRON_HELMET,      RANGER_IRON_CHESTPLATE,      RANGER_IRON_LEGGINGS,      RANGER_IRON_BOOTS;
    public static DeferredItem<Item> RANGER_STEEL_HELMET,     RANGER_STEEL_CHESTPLATE,     RANGER_STEEL_LEGGINGS,     RANGER_STEEL_BOOTS;
    public static DeferredItem<Item> RANGER_TANZANITE_HELMET, RANGER_TANZANITE_CHESTPLATE, RANGER_TANZANITE_LEGGINGS, RANGER_TANZANITE_BOOTS;

    // ── Rogue ─────────────────────────────────────────────────────────────────────
    public static DeferredItem<Item> ROGUE_IRON_HELMET,      ROGUE_IRON_CHESTPLATE,      ROGUE_IRON_LEGGINGS,      ROGUE_IRON_BOOTS;
    public static DeferredItem<Item> ROGUE_STEEL_HELMET,     ROGUE_STEEL_CHESTPLATE,     ROGUE_STEEL_LEGGINGS,     ROGUE_STEEL_BOOTS;
    public static DeferredItem<Item> ROGUE_TANZANITE_HELMET, ROGUE_TANZANITE_CHESTPLATE, ROGUE_TANZANITE_LEGGINGS, ROGUE_TANZANITE_BOOTS;

    // ── Cleric ────────────────────────────────────────────────────────────────────
    public static DeferredItem<Item> CLERIC_IRON_HELMET,      CLERIC_IRON_CHESTPLATE,      CLERIC_IRON_LEGGINGS,      CLERIC_IRON_BOOTS;
    public static DeferredItem<Item> CLERIC_STEEL_HELMET,     CLERIC_STEEL_CHESTPLATE,     CLERIC_STEEL_LEGGINGS,     CLERIC_STEEL_BOOTS;
    public static DeferredItem<Item> CLERIC_TANZANITE_HELMET, CLERIC_TANZANITE_CHESTPLATE, CLERIC_TANZANITE_LEGGINGS, CLERIC_TANZANITE_BOOTS;

    // ── Wizard ────────────────────────────────────────────────────────────────────
    public static DeferredItem<Item> WIZARD_IRON_HELMET,      WIZARD_IRON_CHESTPLATE,      WIZARD_IRON_LEGGINGS,      WIZARD_IRON_BOOTS;
    public static DeferredItem<Item> WIZARD_STEEL_HELMET,     WIZARD_STEEL_CHESTPLATE,     WIZARD_STEEL_LEGGINGS,     WIZARD_STEEL_BOOTS;
    public static DeferredItem<Item> WIZARD_TANZANITE_HELMET, WIZARD_TANZANITE_CHESTPLATE, WIZARD_TANZANITE_LEGGINGS, WIZARD_TANZANITE_BOOTS;

    public static void register(DeferredRegister.Items items) {
        for (PlayerClass c : new PlayerClass[]{ PlayerClass.KNIGHT, PlayerClass.RANGER,
                                                PlayerClass.ROGUE, PlayerClass.CLERIC, PlayerClass.WIZARD }) {
            for (GearTier t : GearTier.values()) {
                registerSet(items, c, t);
            }
        }
    }

    private static void registerSet(DeferredRegister.Items items, PlayerClass c, GearTier t) {
        String prefix = c.name().toLowerCase() + "_" + t.id + "_";
        Rarity rarity = rarity(t);

        DeferredItem<Item> helmet      = register(items, prefix + "helmet",      c, t, ArmorType.HELMET,      rarity);
        DeferredItem<Item> chestplate  = register(items, prefix + "chestplate",  c, t, ArmorType.CHESTPLATE,  rarity);
        DeferredItem<Item> leggings    = register(items, prefix + "leggings",    c, t, ArmorType.LEGGINGS,    rarity);
        DeferredItem<Item> boots       = register(items, prefix + "boots",       c, t, ArmorType.BOOTS,       rarity);

        // Assign to named fields for direct reference
        assignFields(c, t, helmet, chestplate, leggings, boots);
        ALL.add(helmet); ALL.add(chestplate); ALL.add(leggings); ALL.add(boots);
    }

    private static DeferredItem<Item> register(DeferredRegister.Items items, String id,
                                               PlayerClass c, GearTier t, ArmorType type, Rarity rarity) {
        var mat = GearMaterials.get(c, t);
        return items.registerItem(id,
            props -> new ClassArmorItem(c, t, props),
            () -> new Item.Properties()
                    .humanoidArmor(mat, type)
                    .rarity(rarity));
    }

    private static Rarity rarity(GearTier t) {
        return switch (t) {
            case IRON      -> Rarity.COMMON;
            case STEEL     -> Rarity.UNCOMMON;
            case TANZANITE -> Rarity.RARE;
        };
    }

    @SuppressWarnings("java:S1479") // long switch is fine for explicit field assignment
    private static void assignFields(PlayerClass c, GearTier t,
                                     DeferredItem<Item> h, DeferredItem<Item> ch,
                                     DeferredItem<Item> l, DeferredItem<Item> b) {
        switch (c) {
            case KNIGHT -> {
                switch (t) {
                    case IRON      -> { KNIGHT_IRON_HELMET=h; KNIGHT_IRON_CHESTPLATE=ch; KNIGHT_IRON_LEGGINGS=l; KNIGHT_IRON_BOOTS=b; }
                    case STEEL     -> { KNIGHT_STEEL_HELMET=h; KNIGHT_STEEL_CHESTPLATE=ch; KNIGHT_STEEL_LEGGINGS=l; KNIGHT_STEEL_BOOTS=b; }
                    case TANZANITE -> { KNIGHT_TANZANITE_HELMET=h; KNIGHT_TANZANITE_CHESTPLATE=ch; KNIGHT_TANZANITE_LEGGINGS=l; KNIGHT_TANZANITE_BOOTS=b; }
                }
            }
            case RANGER -> {
                switch (t) {
                    case IRON      -> { RANGER_IRON_HELMET=h; RANGER_IRON_CHESTPLATE=ch; RANGER_IRON_LEGGINGS=l; RANGER_IRON_BOOTS=b; }
                    case STEEL     -> { RANGER_STEEL_HELMET=h; RANGER_STEEL_CHESTPLATE=ch; RANGER_STEEL_LEGGINGS=l; RANGER_STEEL_BOOTS=b; }
                    case TANZANITE -> { RANGER_TANZANITE_HELMET=h; RANGER_TANZANITE_CHESTPLATE=ch; RANGER_TANZANITE_LEGGINGS=l; RANGER_TANZANITE_BOOTS=b; }
                }
            }
            case ROGUE -> {
                switch (t) {
                    case IRON      -> { ROGUE_IRON_HELMET=h; ROGUE_IRON_CHESTPLATE=ch; ROGUE_IRON_LEGGINGS=l; ROGUE_IRON_BOOTS=b; }
                    case STEEL     -> { ROGUE_STEEL_HELMET=h; ROGUE_STEEL_CHESTPLATE=ch; ROGUE_STEEL_LEGGINGS=l; ROGUE_STEEL_BOOTS=b; }
                    case TANZANITE -> { ROGUE_TANZANITE_HELMET=h; ROGUE_TANZANITE_CHESTPLATE=ch; ROGUE_TANZANITE_LEGGINGS=l; ROGUE_TANZANITE_BOOTS=b; }
                }
            }
            case CLERIC -> {
                switch (t) {
                    case IRON      -> { CLERIC_IRON_HELMET=h; CLERIC_IRON_CHESTPLATE=ch; CLERIC_IRON_LEGGINGS=l; CLERIC_IRON_BOOTS=b; }
                    case STEEL     -> { CLERIC_STEEL_HELMET=h; CLERIC_STEEL_CHESTPLATE=ch; CLERIC_STEEL_LEGGINGS=l; CLERIC_STEEL_BOOTS=b; }
                    case TANZANITE -> { CLERIC_TANZANITE_HELMET=h; CLERIC_TANZANITE_CHESTPLATE=ch; CLERIC_TANZANITE_LEGGINGS=l; CLERIC_TANZANITE_BOOTS=b; }
                }
            }
            case WIZARD -> {
                switch (t) {
                    case IRON      -> { WIZARD_IRON_HELMET=h; WIZARD_IRON_CHESTPLATE=ch; WIZARD_IRON_LEGGINGS=l; WIZARD_IRON_BOOTS=b; }
                    case STEEL     -> { WIZARD_STEEL_HELMET=h; WIZARD_STEEL_CHESTPLATE=ch; WIZARD_STEEL_LEGGINGS=l; WIZARD_STEEL_BOOTS=b; }
                    case TANZANITE -> { WIZARD_TANZANITE_HELMET=h; WIZARD_TANZANITE_CHESTPLATE=ch; WIZARD_TANZANITE_LEGGINGS=l; WIZARD_TANZANITE_BOOTS=b; }
                }
            }
            default -> {} // PEASANT — no class armor
        }
    }
}
