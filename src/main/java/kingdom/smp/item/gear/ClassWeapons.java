package kingdom.smp.item.gear;

import java.util.ArrayList;
import java.util.List;

import kingdom.smp.rpg.PlayerClass;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers 15 class weapons (5 classes × 3 tiers), steel ingot, and tanzanite gem.
 *
 * <p>Weapon types per class:
 * <ul>
 *   <li>Knight  — longsword  (high damage, slow swing)
 *   <li>Ranger  — shortbow   (BowItem, full charge-and-fire)
 *   <li>Rogue   — dagger     (low damage, very fast)
 *   <li>Cleric  — mace       (medium damage, slow — axe timing)
 *   <li>Wizard  — staff      (placeholder; spell logic added in future step)
 * </ul>
 */
public final class ClassWeapons {
    private ClassWeapons() {}

    public static final List<DeferredItem<Item>> ALL = new ArrayList<>(15);

    // ── Knight longswords ─────────────────────────────────────────────────────────
    public static DeferredItem<Item> KNIGHT_IRON_LONGSWORD;
    public static DeferredItem<Item> KNIGHT_STEEL_LONGSWORD;
    public static DeferredItem<Item> KNIGHT_TANZANITE_LONGSWORD;

    // ── Ranger shortbows ──────────────────────────────────────────────────────────
    public static DeferredItem<Item> RANGER_IRON_SHORTBOW;
    public static DeferredItem<Item> RANGER_STEEL_SHORTBOW;
    public static DeferredItem<Item> RANGER_TANZANITE_SHORTBOW;

    // ── Rogue daggers ─────────────────────────────────────────────────────────────
    public static DeferredItem<Item> ROGUE_IRON_DAGGER;
    public static DeferredItem<Item> ROGUE_STEEL_DAGGER;
    public static DeferredItem<Item> ROGUE_TANZANITE_DAGGER;

    // ── Cleric maces ─────────────────────────────────────────────────────────────
    public static DeferredItem<Item> CLERIC_IRON_MACE;
    public static DeferredItem<Item> CLERIC_STEEL_MACE;
    public static DeferredItem<Item> CLERIC_TANZANITE_MACE;

    // ── Wizard staffs ─────────────────────────────────────────────────────────────
    public static DeferredItem<Item> WIZARD_IRON_STAFF;
    public static DeferredItem<Item> WIZARD_STEEL_STAFF;
    public static DeferredItem<Item> WIZARD_TANZANITE_STAFF;

    // ── Materials ─────────────────────────────────────────────────────────────────
    /** Crafted from iron + blaze powder in the Forge. */
    public static DeferredItem<Item> STEEL_INGOT;
    /** Smelted or blasted from raw tanzanite. */
    public static DeferredItem<Item> TANZANITE_GEM;

    public static void register(DeferredRegister.Items items) {
        STEEL_INGOT     = items.registerSimpleItem("steel_ingot",
            props -> props.rarity(Rarity.UNCOMMON));
        TANZANITE_GEM = items.registerSimpleItem("tanzanite_gem",
            props -> props.rarity(Rarity.RARE));

        // Weapons — longsword: 3.0 bonus dmg, -2.4 spd | dagger: 0.5 bonus, -1.2 | mace: 2.0 bonus, -2.6 | staff: 0 bonus, -2.4
        for (GearTier t : GearTier.values()) {
            net.minecraft.world.item.ToolMaterial mat = toolMat(t);
            Rarity rarity = rarity(t);

            // Use local vars — avoid overwriting named static fields mid-loop
            var knight = sword(items, PlayerClass.KNIGHT, t, mat, rarity, "longsword", 3.0f, -2.4f);
            var ranger = bow(  items, PlayerClass.RANGER,  t, rarity, "shortbow");
            var rogue  = sword(items, PlayerClass.ROGUE,   t, mat, rarity, "dagger",   0.5f, -1.2f);
            var cleric = sword(items, PlayerClass.CLERIC,  t, mat, rarity, "mace",     2.0f, -2.6f);
            var wizard = sword(items, PlayerClass.WIZARD,  t, mat, rarity, "staff",    0.0f, -2.4f);

            assignFields(t, knight, ranger, rogue, cleric, wizard);
            ALL.add(knight); ALL.add(ranger); ALL.add(rogue); ALL.add(cleric); ALL.add(wizard);
        }
    }

    private static DeferredItem<Item> sword(DeferredRegister.Items items,
                                            PlayerClass c, GearTier t,
                                            net.minecraft.world.item.ToolMaterial mat, Rarity rarity,
                                            String type, float dmg, float spd) {
        String id = c.name().toLowerCase() + "_" + t.id + "_" + type;
        return items.registerItem(id,
            props -> new ClassWeaponItem(c, t, props),
            () -> new Item.Properties().sword(mat, dmg, spd).rarity(rarity));
    }

    private static DeferredItem<Item> bow(DeferredRegister.Items items,
                                          PlayerClass c, GearTier t, Rarity rarity, String type) {
        String id = c.name().toLowerCase() + "_" + t.id + "_" + type;
        int durability = switch (t) { case IRON -> 250; case STEEL -> 420; default -> 650; };
        return items.registerItem(id,
            props -> new ClassBowItem(c, t, props),
            () -> new Item.Properties().durability(durability).rarity(rarity));
    }

    private static net.minecraft.world.item.ToolMaterial toolMat(GearTier t) {
        return switch (t) {
            case IRON      -> net.minecraft.world.item.ToolMaterial.IRON;
            case STEEL     -> GearMaterials.STEEL;
            case TANZANITE -> GearMaterials.TANZANITE;
        };
    }

    private static Rarity rarity(GearTier t) {
        return switch (t) {
            case IRON      -> Rarity.COMMON;
            case STEEL     -> Rarity.UNCOMMON;
            case TANZANITE -> Rarity.RARE;
        };
    }

    private static void assignFields(GearTier t,
                                     DeferredItem<Item> knight, DeferredItem<Item> ranger,
                                     DeferredItem<Item> rogue,  DeferredItem<Item> cleric,
                                     DeferredItem<Item> wizard) {
        switch (t) {
            case IRON -> {
                KNIGHT_IRON_LONGSWORD  = knight; RANGER_IRON_SHORTBOW  = ranger;
                ROGUE_IRON_DAGGER      = rogue;  CLERIC_IRON_MACE      = cleric;
                WIZARD_IRON_STAFF      = wizard;
            }
            case STEEL -> {
                KNIGHT_STEEL_LONGSWORD  = knight; RANGER_STEEL_SHORTBOW  = ranger;
                ROGUE_STEEL_DAGGER      = rogue;  CLERIC_STEEL_MACE      = cleric;
                WIZARD_STEEL_STAFF      = wizard;
            }
            case TANZANITE -> {
                KNIGHT_TANZANITE_LONGSWORD  = knight; RANGER_TANZANITE_SHORTBOW  = ranger;
                ROGUE_TANZANITE_DAGGER      = rogue;  CLERIC_TANZANITE_MACE      = cleric;
                WIZARD_TANZANITE_STAFF      = wizard;
            }
        }
    }
}
