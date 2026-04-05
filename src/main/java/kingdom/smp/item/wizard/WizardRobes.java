package kingdom.smp.item.wizard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import kingdom.smp.Ironhold;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wizard robes from Cyano's Wonderful Wands (GPL-3.0) — textures/equipment adapted for MC 1.21.11.
 *
 * @see <a href="https://github.com/MinecraftModDevelopmentMods/WonderfulWands">WonderfulWands</a>
 */
public final class WizardRobes {
    private WizardRobes() {}

    public static final String[] COLORS = {
        "black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink",
        "lime", "yellow", "light_blue", "magenta", "orange", "white"
    };

    private static final Map<ArmorType, Integer> ROBE_DEFENSE = new EnumMap<>(ArmorType.class);
    static {
        ROBE_DEFENSE.put(ArmorType.BOOTS, 1);
        ROBE_DEFENSE.put(ArmorType.LEGGINGS, 1);
        ROBE_DEFENSE.put(ArmorType.CHESTPLATE, 1);
        ROBE_DEFENSE.put(ArmorType.HELMET, 1);
        ROBE_DEFENSE.put(ArmorType.BODY, 1);
    }

    public static final List<DeferredItem<Item>> ALL_ROBES = new ArrayList<>(64);

    public static DeferredItem<Item> WIZARDS_HAT;

    private static Holder<SoundEvent> leatherEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    private static Map<ArmorType, Integer> hatDefense() {
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        for (ArmorType t : ArmorType.values()) {
            m.put(t, t == ArmorType.HELMET ? 1 : 0);
        }
        return m;
    }

    private static ArmorMaterial materialWizardsHat() {
        ResourceKey<EquipmentAsset> assetKey = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizards_hat"));
        return new ArmorMaterial(
            15,
            hatDefense(),
            40,
            leatherEquipSound(),
            0.0F,
            0.0F,
            ItemTags.WOOL,
            assetKey
        );
    }

    private static ArmorMaterial materialForColor(String color) {
        ResourceKey<EquipmentAsset> assetKey = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizard_robes_" + color));
        return new ArmorMaterial(
            15,
            ROBE_DEFENSE,
            40,
            leatherEquipSound(),
            0.0F,
            0.0F,
            ItemTags.WOOL,
            assetKey
        );
    }

    public static void register(DeferredRegister.Items items) {
        for (String color : COLORS) {
            ArmorMaterial mat = materialForColor(color);
            registerPiece(items, mat, color, ArmorType.BOOTS, "boots");
            registerPiece(items, mat, color, ArmorType.LEGGINGS, "leggings");
            registerPiece(items, mat, color, ArmorType.CHESTPLATE, "chestplate");
            registerPiece(items, mat, color, ArmorType.HELMET, "helmet");
        }

        WIZARDS_HAT = items.registerItem(
            "wizards_hat",
            props -> new Item(props.humanoidArmor(materialWizardsHat(), ArmorType.HELMET)),
            () -> new Item.Properties().stacksTo(1));
    }

    private static void registerPiece(
        DeferredRegister.Items items,
        ArmorMaterial mat,
        String color,
        ArmorType type,
        String suffix
    ) {
        String id = "robes_" + color + "_" + suffix;
        DeferredItem<Item> def = items.registerItem(
            id,
            props -> new Item(props.humanoidArmor(mat, type)),
            () -> new Item.Properties()
        );
        ALL_ROBES.add(def);
    }
}
