package kingdom.smp;

import kingdom.smp.item.ArcaneScepterItem;
import kingdom.smp.item.AnkhShieldItem;
import kingdom.smp.item.BandOfRegenerationItem;
import kingdom.smp.item.CloudInABottleItem;
import kingdom.smp.item.DisguiseTomeItem;
import kingdom.smp.item.EnhancedPickaxeItem;
import kingdom.smp.item.HermesBootsItem;
import kingdom.smp.item.MagicMinecartItem;
import kingdom.smp.item.MimicKeyItem;
import kingdom.smp.item.MirrorItem;
import kingdom.smp.item.PinkSlimeBallItem;
import kingdom.smp.item.TempestArrowItem;
import kingdom.smp.item.TempestBowItem;
import kingdom.smp.item.VanityCosmeticItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import kingdom.smp.game.TanzaniteWorldgenFluidHandler;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registrations (incl. spawn eggs, gear, knight equipment, block items)
 *  and the creative tab, split out of {@link Ironhold}. References ModEntities
 *  (spawn eggs) and ModBlocks (block items). */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ironhold.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ironhold.MODID);

    // Status effects → ModEffects; sound events → ModSounds; worldgen feature + biome keys → ModWorldgen.





    public static final DeferredItem<Item> ARCANE_MAGE_SPAWN_EGG =
        ITEMS.registerItem(
            "arcane_mage_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.ARCANE_MAGE.get()).stacksTo(64)));










    public static final DeferredItem<Item> VOID_INVOKER_SPAWN_EGG =
        ITEMS.registerItem(
            "void_invoker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.VOID_INVOKER.get()).stacksTo(64)));


    public static final DeferredItem<Item> NULL_STALKER_SPAWN_EGG =
        ITEMS.registerItem(
            "null_stalker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.NULL_STALKER.get()).stacksTo(64)));


    public static final DeferredItem<Item> PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PINK_DEER.get()).stacksTo(64)));


    public static final DeferredItem<Item> RARE_PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "rare_pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.RARE_PINK_DEER.get()).stacksTo(64)));


    public static final DeferredItem<Item> MOM_PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "mom_pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.MOM_PINK_DEER.get()).stacksTo(64)));




    public static final DeferredItem<Item> RAT_SPAWN_EGG =
        ITEMS.registerItem(
            "rat_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.RAT.get()).stacksTo(64)));

    public static final DeferredItem<Item> SHROOMLING_SPAWN_EGG =
        ITEMS.registerItem(
            "shroomling_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SHROOMLING.get()).stacksTo(64)));

    public static final DeferredItem<Item> MOONSHROOM_SPAWN_EGG =
        ITEMS.registerItem(
            "moonshroom_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.MOONSHROOM.get()).stacksTo(64)));

    public static final DeferredItem<Item> PURPLE_ALLAY_SPAWN_EGG =
        ITEMS.registerItem(
            "purple_allay_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PURPLE_ALLAY.get()).stacksTo(64)));


    public static final DeferredItem<Item> WILL_O_WISP_SPAWN_EGG =
        ITEMS.registerItem(
            "will_o_wisp_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.WILL_O_WISP.get()).stacksTo(64)));


    public static final DeferredItem<Item> WILL_O_WISP_2_SPAWN_EGG =
        ITEMS.registerItem(
            "will_o_wisp_2_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.WILL_O_WISP_2.get()).stacksTo(64)));


    public static final DeferredItem<Item> KINGDOM_VILLAGER_SPAWN_EGG =
        ITEMS.registerItem(
            "kingdom_villager_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KINGDOM_VILLAGER.get()).stacksTo(64)));


    public static final DeferredItem<Item> KANGARUDE_SPAWN_EGG =
        ITEMS.registerItem(
            "kangarude_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KANGARUDE.get()).stacksTo(64)));


    public static final DeferredItem<Item> ENDER_VILLAGER_SPAWN_EGG =
        ITEMS.registerItem(
            "ender_villager_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.ENDER_VILLAGER.get()).stacksTo(64)));


    public static final DeferredItem<Item> SHULKER_HERDER_SPAWN_EGG =
        ITEMS.registerItem(
            "shulker_herder_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SHULKER_HERDER.get()).stacksTo(64)));


    public static final DeferredItem<Item> WHITE_SHULKER_SPAWN_EGG =
        ITEMS.registerItem(
            "white_shulker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.WHITE_SHULKER.get()).stacksTo(64)));


    public static final DeferredItem<Item> BLACK_SHULKER_SPAWN_EGG =
        ITEMS.registerItem(
            "black_shulker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.BLACK_SHULKER.get()).stacksTo(64)));


    public static final DeferredItem<Item> WARDEN_HALRIC_SPAWN_EGG =
        ITEMS.registerItem(
            "warden_halric_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.WARDEN_HALRIC.get()).stacksTo(64)));


    public static final DeferredItem<Item> TALLYKEEPER_SPAWN_EGG =
        ITEMS.registerItem(
            "tallykeeper_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.TALLYKEEPER.get()).stacksTo(64)));


    public static final DeferredItem<Item> CEMETERY_WATCHER_SPAWN_EGG =
        ITEMS.registerItem(
            "cemetery_watcher_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.CEMETERY_WATCHER.get()).stacksTo(64)));


    public static final DeferredItem<Item> MIRA_INNKEEPER_SPAWN_EGG =
        ITEMS.registerItem(
            "mira_innkeeper_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.MIRA_INNKEEPER.get()).stacksTo(64)));


    public static final DeferredItem<Item> BLACKSMITH_TOBIAS_SPAWN_EGG =
        ITEMS.registerItem(
            "blacksmith_tobias_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.BLACKSMITH_TOBIAS.get()).stacksTo(64)));


    public static final DeferredItem<Item> PLAGUE_DOCTOR_SPAWN_EGG =
        ITEMS.registerItem(
            "plague_doctor_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PLAGUE_DOCTOR.get()).stacksTo(64)));


    public static final DeferredItem<Item> PRIEST_CEDRIC_SPAWN_EGG =
        ITEMS.registerItem(
            "priest_cedric_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PRIEST_CEDRIC.get()).stacksTo(64)));


    public static final DeferredItem<Item> OLD_HESTA_SPAWN_EGG =
        ITEMS.registerItem(
            "old_hesta_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.OLD_HESTA.get()).stacksTo(64)));


    public static final DeferredItem<Item> OLD_BEREN_SPAWN_EGG =
        ITEMS.registerItem(
            "old_beren_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.OLD_BEREN.get()).stacksTo(64)));


    public static final DeferredItem<Item> CAPTAIN_ROSELIND_SPAWN_EGG =
        ITEMS.registerItem(
            "captain_roselind_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.CAPTAIN_ROSELIND.get()).stacksTo(64)));


    public static final DeferredItem<Item> LOREMASTER_EILAN_SPAWN_EGG =
        ITEMS.registerItem(
            "loremaster_eilan_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.LOREMASTER_EILAN.get()).stacksTo(64)));


    public static final DeferredItem<Item> SISTER_WREN_SPAWN_EGG =
        ITEMS.registerItem(
            "sister_wren_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SISTER_WREN.get()).stacksTo(64)));


    public static final DeferredItem<Item> BRAM_BARD_SPAWN_EGG =
        ITEMS.registerItem(
            "bram_bard_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.BRAM_BARD.get()).stacksTo(64)));


    public static final DeferredItem<Item> PIPPA_URCHIN_SPAWN_EGG =
        ITEMS.registerItem(
            "pippa_urchin_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PIPPA_URCHIN.get()).stacksTo(64)));


    public static final DeferredItem<Item> PIGLIN_VILLAGER_SPAWN_EGG =
        ITEMS.registerItem(
            "piglin_villager_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.PIGLIN_VILLAGER.get()).stacksTo(64)));


    public static final DeferredItem<Item> FILCHER_SPAWN_EGG =
        ITEMS.registerItem(
            "filcher_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.FILCHER.get()).stacksTo(64)));


    public static final DeferredItem<Item> POSSESSED_ARMOR_SPAWN_EGG =
        ITEMS.registerItem(
            "possessed_armor_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.POSSESSED_ARMOR.get()).stacksTo(64)));


    public static final DeferredItem<Item> KING_ENDERMAN_SPAWN_EGG =
        ITEMS.registerItem(
            "king_enderman_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KING_ENDERMAN.get()).stacksTo(64)));


    public static final DeferredItem<Item> SIREN_SPAWN_EGG =
        ITEMS.registerItem(
            "siren_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SIREN.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_RECRUIT_SPAWN_EGG =
        ITEMS.registerItem("knight_recruit_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_RECRUIT.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_MAN_AT_ARMS_SPAWN_EGG =
        ITEMS.registerItem("knight_man_at_arms_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_MAN_AT_ARMS.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_CROSSBOWMAN_SPAWN_EGG =
        ITEMS.registerItem("knight_crossbowman_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_CROSSBOWMAN.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_ARMORED_SPAWN_EGG =
        ITEMS.registerItem("knight_armored_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_ARMORED.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_CRUSADER_SPAWN_EGG =
        ITEMS.registerItem("knight_crusader_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_CRUSADER.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_GOTHIC_SPAWN_EGG =
        ITEMS.registerItem("knight_gothic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_GOTHIC.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_GOLD_SPAWN_EGG =
        ITEMS.registerItem("knight_gold_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_GOLD.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_JOUSTER_SPAWN_EGG =
        ITEMS.registerItem("knight_jouster_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_JOUSTER.get()).stacksTo(64)));

    public static final DeferredItem<Item> KNIGHT_VETERAN_SPAWN_EGG =
        ITEMS.registerItem("knight_veteran_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KNIGHT_VETERAN.get()).stacksTo(64)));

    // ── Knight equipment assets (map to assets/ironhold/equipment/*.json) ────────
    // Helmet equipment assets (one per knight) — texture matches the custom helmet model's UV layout.
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_RECRUIT =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_recruit"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_MAN_AT_ARMS =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_man_at_arms"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_CROSSBOWMAN =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_crossbowman"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_ARMORED =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_armored"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_CRUSADER =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_crusader"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_GOTHIC =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_gothic"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_GOLD =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_gold"));

    // Body equipment assets — used by chest/legs/boots items. The helmet textures from
    // Epic Knights only paint helmet UV regions, so chest/legs/boots need separate textures
    // (gambeson for the lighter sets, maximilian for the armored set, full-set textures
    // for crusader/gothic/gold) to avoid garbled chestplates and broken arms.
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_RECRUIT_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_recruit_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_MAN_AT_ARMS_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_man_at_arms_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_CROSSBOWMAN_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_crossbowman_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_ARMORED_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_armored_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_CRUSADER_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_crusader_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_GOTHIC_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_gothic_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_GOLD_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_gold_body"));

    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_JOUSTER =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_jouster"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_JOUSTER_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_jouster_body"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_VETERAN =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_veteran"));
    public static final ResourceKey<EquipmentAsset> EQ_KNIGHT_VETERAN_BODY =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "knight_veteran_body"));

    // ── Knight armor items (mob-only; 4 slots per tier, no drops) ─────────────
    // Helper: builds Equippable for the given slot + asset, no player-facing features
    private static Equippable knightEquip(EquipmentSlot slot, ResourceKey<EquipmentAsset> asset) {
        return Equippable.builder(slot).setAsset(asset)
            .setDispensable(false).setSwappable(false).setDamageOnHurt(false).build();
    }

    // Knight Recruit
    public static final DeferredItem<Item> KNIGHT_RECRUIT_HELM   = ITEMS.registerItem("knight_recruit_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_RECRUIT))));
    public static final DeferredItem<Item> KNIGHT_RECRUIT_CHEST  = ITEMS.registerItem("knight_recruit_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_RECRUIT_BODY))));
    public static final DeferredItem<Item> KNIGHT_RECRUIT_LEGS   = ITEMS.registerItem("knight_recruit_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_RECRUIT_BODY))));
    public static final DeferredItem<Item> KNIGHT_RECRUIT_BOOTS  = ITEMS.registerItem("knight_recruit_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_RECRUIT_BODY))));

    // Knight Man-at-Arms
    public static final DeferredItem<Item> KNIGHT_MAN_AT_ARMS_HELM   = ITEMS.registerItem("knight_man_at_arms_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_MAN_AT_ARMS))));
    public static final DeferredItem<Item> KNIGHT_MAN_AT_ARMS_CHEST  = ITEMS.registerItem("knight_man_at_arms_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_MAN_AT_ARMS_BODY))));
    public static final DeferredItem<Item> KNIGHT_MAN_AT_ARMS_LEGS   = ITEMS.registerItem("knight_man_at_arms_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_MAN_AT_ARMS_BODY))));
    public static final DeferredItem<Item> KNIGHT_MAN_AT_ARMS_BOOTS  = ITEMS.registerItem("knight_man_at_arms_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_MAN_AT_ARMS_BODY))));

    // Knight Crossbowman
    public static final DeferredItem<Item> KNIGHT_CROSSBOWMAN_HELM   = ITEMS.registerItem("knight_crossbowman_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_CROSSBOWMAN))));
    public static final DeferredItem<Item> KNIGHT_CROSSBOWMAN_CHEST  = ITEMS.registerItem("knight_crossbowman_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_CROSSBOWMAN_BODY))));
    public static final DeferredItem<Item> KNIGHT_CROSSBOWMAN_LEGS   = ITEMS.registerItem("knight_crossbowman_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_CROSSBOWMAN_BODY))));
    public static final DeferredItem<Item> KNIGHT_CROSSBOWMAN_BOOTS  = ITEMS.registerItem("knight_crossbowman_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_CROSSBOWMAN_BODY))));

    // Knight Armored
    public static final DeferredItem<Item> KNIGHT_ARMORED_HELM   = ITEMS.registerItem("knight_armored_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_ARMORED))));
    public static final DeferredItem<Item> KNIGHT_ARMORED_CHEST  = ITEMS.registerItem("knight_armored_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_ARMORED_BODY))));
    public static final DeferredItem<Item> KNIGHT_ARMORED_LEGS   = ITEMS.registerItem("knight_armored_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_ARMORED_BODY))));
    public static final DeferredItem<Item> KNIGHT_ARMORED_BOOTS  = ITEMS.registerItem("knight_armored_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_ARMORED_BODY))));

    // Knight Crusader
    public static final DeferredItem<Item> KNIGHT_CRUSADER_HELM   = ITEMS.registerItem("knight_crusader_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_CRUSADER))));
    public static final DeferredItem<Item> KNIGHT_CRUSADER_CHEST  = ITEMS.registerItem("knight_crusader_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_CRUSADER_BODY))));
    public static final DeferredItem<Item> KNIGHT_CRUSADER_LEGS   = ITEMS.registerItem("knight_crusader_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_CRUSADER_BODY))));
    public static final DeferredItem<Item> KNIGHT_CRUSADER_BOOTS  = ITEMS.registerItem("knight_crusader_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_CRUSADER_BODY))));

    // Knight Gothic
    public static final DeferredItem<Item> KNIGHT_GOTHIC_HELM   = ITEMS.registerItem("knight_gothic_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_GOTHIC))));
    public static final DeferredItem<Item> KNIGHT_GOTHIC_CHEST  = ITEMS.registerItem("knight_gothic_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_GOTHIC_BODY))));
    public static final DeferredItem<Item> KNIGHT_GOTHIC_LEGS   = ITEMS.registerItem("knight_gothic_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_GOTHIC_BODY))));
    public static final DeferredItem<Item> KNIGHT_GOTHIC_BOOTS  = ITEMS.registerItem("knight_gothic_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_GOTHIC_BODY))));

    // Knight Gold
    public static final DeferredItem<Item> KNIGHT_GOLD_HELM   = ITEMS.registerItem("knight_gold_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_GOLD))));
    public static final DeferredItem<Item> KNIGHT_GOLD_CHEST  = ITEMS.registerItem("knight_gold_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_GOLD_BODY))));
    public static final DeferredItem<Item> KNIGHT_GOLD_LEGS   = ITEMS.registerItem("knight_gold_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_GOLD_BODY))));
    public static final DeferredItem<Item> KNIGHT_GOLD_BOOTS  = ITEMS.registerItem("knight_gold_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_GOLD_BODY))));

    // Knight Jouster — Stechhelm + platemail
    public static final DeferredItem<Item> KNIGHT_JOUSTER_HELM   = ITEMS.registerItem("knight_jouster_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_JOUSTER))));
    public static final DeferredItem<Item> KNIGHT_JOUSTER_CHEST  = ITEMS.registerItem("knight_jouster_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_JOUSTER_BODY))));
    public static final DeferredItem<Item> KNIGHT_JOUSTER_LEGS   = ITEMS.registerItem("knight_jouster_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_JOUSTER_BODY))));
    public static final DeferredItem<Item> KNIGHT_JOUSTER_BOOTS  = ITEMS.registerItem("knight_jouster_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_JOUSTER_BODY))));

    // Knight Veteran — Sallet + chainmail
    public static final DeferredItem<Item> KNIGHT_VETERAN_HELM   = ITEMS.registerItem("knight_veteran_helm",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.HEAD,  EQ_KNIGHT_VETERAN))));
    public static final DeferredItem<Item> KNIGHT_VETERAN_CHEST  = ITEMS.registerItem("knight_veteran_chest",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.CHEST, EQ_KNIGHT_VETERAN_BODY))));
    public static final DeferredItem<Item> KNIGHT_VETERAN_LEGS   = ITEMS.registerItem("knight_veteran_legs",   p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.LEGS,  EQ_KNIGHT_VETERAN_BODY))));
    public static final DeferredItem<Item> KNIGHT_VETERAN_BOOTS  = ITEMS.registerItem("knight_veteran_boots",  p -> new Item(p.stacksTo(1).component(DataComponents.EQUIPPABLE, knightEquip(EquipmentSlot.FEET,  EQ_KNIGHT_VETERAN_BODY))));


    public static final DeferredItem<Item> SHIPWRECK_MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "shipwreck_mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SHIPWRECK_MIMIC.get()).stacksTo(64)));


    public static final DeferredItem<Item> MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.MIMIC.get()).stacksTo(64)));


    public static final DeferredItem<Item> BABY_MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "baby_mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.BABY_MIMIC.get()).stacksTo(64)));



    public static final DeferredItem<Item> SLIME_PET_JE11IE_SPAWN_EGG =
        ITEMS.registerItem(
            "slime_pet_je11ie_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SLIME_PET_JE11IE.get()).stacksTo(64)));

    public static final DeferredItem<Item> SLIME_PET_CHEAKIE_SPAWN_EGG =
        ITEMS.registerItem(
            "slime_pet_cheakie_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.SLIME_PET_CHEAKIE.get()).stacksTo(64)));


    public static final DeferredItem<Item> KINGDOM_DRAGON_SPAWN_EGG =
        ITEMS.registerItem(
            "kingdom_dragon_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ModEntities.KINGDOM_DRAGON.get()).stacksTo(64)));


    public static final DeferredItem<TempestArrowItem> TEMPEST_ARROW =
        ITEMS.registerItem("tempest_arrow", TempestArrowItem::new, props -> props.stacksTo(64));

    public static final DeferredItem<TempestBowItem> TEMPEST_BOW =
        ITEMS.registerItem(
            "tempest_bow",
            TempestBowItem::new,
            props -> props.durability(384).rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredItem<AnkhShieldItem> ANKH_SHIELD =
        ITEMS.registerItem("ankh_shield", AnkhShieldItem::new, AnkhShieldItem::applyAnkhProperties);

    // Arcane Scepter — spear-like wizard weapon with extended reach, shoots arcane orbs
    public static final DeferredItem<Item> ARCANE_SCEPTER = ITEMS.registerItem(
        "arcane_scepter",
        ArcaneScepterItem::new,
        props -> props
            .durability(500)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "scepter_damage"),
                        4.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "scepter_speed"),
                        -2.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "scepter_reach"),
                        1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Battle Hammer — netherite war hammer; charge "forge power" then ground-slam to launch foes
    public static final DeferredItem<Item> BATTLE_HAMMER = ITEMS.registerItem(
        "battle_hammer",
        kingdom.smp.item.BattleHammerItem::new,
        props -> props
            .durability(900)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .fireResistant()
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer_damage"),
                        8.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer_speed"),
                        -3.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Diamond Scepter — crystalline variant of the Arcane Scepter; fires a chilling crystal lance
    public static final DeferredItem<Item> DIAMOND_SCEPTER = ITEMS.registerItem(
        "diamond_scepter",
        kingdom.smp.item.DiamondScepterItem::new,
        props -> props
            .durability(600)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "diamond_scepter_damage"),
                        5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "diamond_scepter_speed"),
                        -2.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Soluna Staff — wizard weapon that shifts between sun/moon texture by time of day
    public static final DeferredItem<Item> SOLUNA_STAFF = ITEMS.registerItem(
        "soluna_staff",
        kingdom.smp.item.SolunaStaffItem::new,
        props -> props
            .durability(600)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "soluna_damage"),
                        5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "soluna_speed"),
                        -2.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Wizard Staff — gem-socket staff; gem/metal in offhand loads it and primes a spell
    public static final DeferredItem<Item> WIZARD_STAFF = ITEMS.registerItem(
        "wizard_staff",
        kingdom.smp.item.WizardStaffItem::new,
        props -> props
            .durability(500)
            .rarity(net.minecraft.world.item.Rarity.RARE)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizard_staff_damage"),
                        3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizard_staff_speed"),
                        -2.4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Masquerade — invisible plant left by reverse-pickpocket; hover reveals what + who
    public static final DeferredItem<Item> MASQUERADE = ITEMS.registerItem(
        "masquerade",
        kingdom.smp.item.MasqueradeItem::new,
        props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));


    /**
     * Wizard Stick — entry-level wand. Power scales with the wielder's wizard class
     * (see {@link kingdom.smp.item.WizardStickItem}); pitiful for non-mages, decent
     * for Sorcerer Supreme and the tier-4 mage hybrids.
     */
    public static final DeferredItem<Item> WIZARD_STICK = ITEMS.registerItem(
        "wizard_stick",
        kingdom.smp.item.WizardStickItem::new,
        props -> props.stacksTo(1));

    /**
     * Halric's Staff — Warden Halric's ceremonial weapon. 3D model with hanging lantern.
     */
    public static final DeferredItem<Item> HALRIC_STAFF = ITEMS.registerItem(
        "halric_staff",
        kingdom.smp.item.HalricStaffItem::new,
        props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredItem<Item> PITCHFORK = ITEMS.registerItem(
        "pitchfork",
        kingdom.smp.item.PitchforkItem::new,
        props -> props
            .durability(400)
            .rarity(net.minecraft.world.item.Rarity.UNCOMMON)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "pitchfork_damage"),
                        5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "pitchfork_speed"),
                        -2.4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "pitchfork_reach"),
                        1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // ── Possessed Armor drops ──────────────────────────────────────────────

    // Vengeful Halberd — legendary long-reach weapon, more damage at low HP
    public static final DeferredItem<Item> VENGEFUL_HALBERD = ITEMS.registerItem(
        "vengeful_halberd",
        kingdom.smp.item.VengefulHalberdItem::new,
        props -> props
            .durability(1200)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "halberd_damage"),
                        7.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "halberd_speed"),
                        -3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "halberd_reach"),
                        1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Armor Polish — consumable that permanently increases armor durability by 20%
    public static final DeferredItem<Item> ARMOR_POLISH = ITEMS.registerItem(
        "armor_polish",
        kingdom.smp.item.ArmorPolishItem::new,
        props -> props.stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE));

    // Wraith's Sigil — accessory that grants a spectral dash
    public static final DeferredItem<Item> WRAITHS_SIGIL = ITEMS.registerItem(
        "wraiths_sigil",
        kingdom.smp.item.WraithsSigilItem::new,
        props -> props.rarity(net.minecraft.world.item.Rarity.EPIC));

    // Siren's Ring — accessory that lures a nearby target toward you
    public static final DeferredItem<Item> SIRENS_RING = ITEMS.registerItem(
        "sirens_ring",
        kingdom.smp.item.SirensRingItem::new,
        props -> props.rarity(net.minecraft.world.item.Rarity.RARE));


    public static final DeferredItem<Item> MAGIC_MINECART_ITEM = ITEMS.registerItem(
        "magic_minecart",
        props -> new MagicMinecartItem(ModEntities.MAGIC_MINECART_ENTITY.get(), props),
        props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)
    );

    // Wall-hung mirror — places like a painting (auto-fits the largest shape the wall allows) and
    // renders a live reflection. A single item; the placed shape is chosen on placement.
    public static final DeferredItem<Item> MIRROR = ITEMS.registerItem(
        "mirror",
        MirrorItem::new,
        props -> props.stacksTo(16)
    );

    /** Consumable ownership lock for vanilla chests, shelves, and armor stands. */
    public static final DeferredItem<Item> LOCK = ITEMS.registerSimpleItem(
        "lock",
        props -> props.stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );

    // Invisible Item Frame — places a vanilla frame flagged invisible (only the held item shows).
    // Crafted from an item frame + a glass pane. Same inventory texture as a normal frame, but
    // always enchant-glinted (forced via ENCHANTMENT_GLINT_OVERRIDE) so the two are easy to tell
    // apart. See InvisibleItemFrameItem and ItemFrameInvisibleDropMixin.
    public static final DeferredItem<Item> INVISIBLE_ITEM_FRAME = ITEMS.registerItem(
        "invisible_item_frame",
        kingdom.smp.item.InvisibleItemFrameItem::new,
        props -> props.stacksTo(64).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
    );

    // ── Tanzanite ore ─────────────────────────────────────────────────────────
    // Worldgen: rare lava/water obsidian formations (Y -64..12) — see TanzaniteWorldgenFluidHandler.
    // Slightly slower to mine than obsidian; still needs diamond+ pick for drops.


    public static final DeferredItem<BlockItem> TANZANITE_ORE_ITEM =
        ITEMS.registerSimpleBlockItem("tanzanite_ore", ModBlocks.TANZANITE_ORE);


    public static final DeferredItem<BlockItem> FOOLS_GOLD_ORE_ITEM =
        ITEMS.registerSimpleBlockItem("fools_gold_ore", ModBlocks.FOOLS_GOLD_ORE);

    public static final DeferredItem<BlockItem> DEEPSLATE_FOOLS_GOLD_ORE_ITEM =
        ITEMS.registerSimpleBlockItem("deepslate_fools_gold_ore", ModBlocks.DEEPSLATE_FOOLS_GOLD_ORE);

    // ── Moon Dimension ────────────────────────────────────────────────────────
    public static final DeferredItem<BlockItem> MOON_STONE_ITEM =
        ITEMS.registerSimpleBlockItem("moon_stone", ModBlocks.MOON_STONE);

    public static final DeferredItem<BlockItem> MOON_DUST_ITEM =
        ITEMS.registerSimpleBlockItem("moon_dust", ModBlocks.MOON_DUST);

    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage",
        kingdom.smp.item.BandageItem::new);

    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem(
        "gold_coin",
        props -> props.stacksTo(64)
    );

    public static final DeferredItem<Item> RAW_TANZANITE = ITEMS.registerSimpleItem(
        "raw_tanzanite",
        props -> props.rarity(net.minecraft.world.item.Rarity.RARE)
    );

    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem(
        "steel_ingot",
        props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> TANZANITE_GEM = ITEMS.registerSimpleItem(
        "tanzanite_gem",
        props -> props.rarity(net.minecraft.world.item.Rarity.RARE)
    );

    // ── Ebony wood (near-black dark oak variants) ─────────────────────────────


    public static final DeferredItem<BlockItem> EBONY_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_log", ModBlocks.EBONY_LOG);


    public static final DeferredItem<BlockItem> EBONY_PLANKS_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_planks", ModBlocks.EBONY_PLANKS);

    public static final DeferredItem<BlockItem> STRIPPED_EBONY_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("stripped_ebony_log", ModBlocks.STRIPPED_EBONY_LOG);

    public static final DeferredItem<BlockItem> EBONY_SLAB_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_slab", ModBlocks.EBONY_SLAB);

    public static final DeferredItem<BlockItem> EBONY_STAIRS_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_stairs", ModBlocks.EBONY_STAIRS);

    public static final DeferredItem<BlockItem> EBONY_FENCE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_fence", ModBlocks.EBONY_FENCE);

    public static final DeferredItem<BlockItem> EBONY_FENCE_GATE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_fence_gate", ModBlocks.EBONY_FENCE_GATE);

    public static final DeferredItem<BlockItem> EBONY_DOOR_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_door", ModBlocks.EBONY_DOOR);

    public static final DeferredItem<BlockItem> EBONY_TRAPDOOR_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_trapdoor", ModBlocks.EBONY_TRAPDOOR);

    public static final DeferredItem<BlockItem> EBONY_BUTTON_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_button", ModBlocks.EBONY_BUTTON);

    public static final DeferredItem<BlockItem> EBONY_PRESSURE_PLATE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_pressure_plate", ModBlocks.EBONY_PRESSURE_PLATE);

    public static final DeferredItem<BlockItem> EBONY_LEAVES_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_leaves", ModBlocks.EBONY_LEAVES);

    // ── Chinese Cedar wood (pink heartwood, tan birch-style bark) ─────────────
    public static final DeferredItem<BlockItem> CHINESE_CEDAR_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_log", ModBlocks.CHINESE_CEDAR_LOG);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_PLANKS_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_planks", ModBlocks.CHINESE_CEDAR_PLANKS);

    public static final DeferredItem<BlockItem> STRIPPED_CHINESE_CEDAR_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("stripped_chinese_cedar_log", ModBlocks.STRIPPED_CHINESE_CEDAR_LOG);

    public static final DeferredItem<BlockItem> STRIPPED_CHINESE_CEDAR_WOOD_ITEM =
        ITEMS.registerSimpleBlockItem("stripped_chinese_cedar_wood", ModBlocks.STRIPPED_CHINESE_CEDAR_WOOD);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_SLAB_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_slab", ModBlocks.CHINESE_CEDAR_SLAB);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_STAIRS_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_stairs", ModBlocks.CHINESE_CEDAR_STAIRS);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_FENCE_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_fence", ModBlocks.CHINESE_CEDAR_FENCE);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_FENCE_GATE_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_fence_gate", ModBlocks.CHINESE_CEDAR_FENCE_GATE);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_DOOR_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_door", ModBlocks.CHINESE_CEDAR_DOOR);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_TRAPDOOR_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_trapdoor", ModBlocks.CHINESE_CEDAR_TRAPDOOR);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_BUTTON_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_button", ModBlocks.CHINESE_CEDAR_BUTTON);

    public static final DeferredItem<BlockItem> CHINESE_CEDAR_PRESSURE_PLATE_ITEM =
        ITEMS.registerSimpleBlockItem("chinese_cedar_pressure_plate", ModBlocks.CHINESE_CEDAR_PRESSURE_PLATE);

    public static final DeferredItem<BlockItem> BAT_FLOWER_ITEM =
        ITEMS.registerSimpleBlockItem("bat_flower", ModBlocks.BAT_FLOWER);

    public static final DeferredItem<BlockItem> ORANGE_GLOWSHROOMS_ITEM =
        ITEMS.registerSimpleBlockItem("orange_glowshrooms", ModBlocks.ORANGE_GLOWSHROOMS);

    public static final DeferredItem<BlockItem> BLUE_GLOWSHROOMS_ITEM =
        ITEMS.registerSimpleBlockItem("blue_glowshrooms", ModBlocks.BLUE_GLOWSHROOMS);

    public static final DeferredItem<BlockItem> EBONWOOD_GRASS_ITEM =
        ITEMS.registerSimpleBlockItem("ebonwood_grass", ModBlocks.EBONWOOD_GRASS);

    // ── Ebonwood Hollow terrain blocks ───────────────────────────────────────

    public static final DeferredItem<BlockItem> BLACK_SAND_ITEM =
        ITEMS.registerSimpleBlockItem("black_sand", ModBlocks.BLACK_SAND);

    public static final DeferredItem<BlockItem> DARK_GRAVEL_ITEM =
        ITEMS.registerSimpleBlockItem("dark_gravel", ModBlocks.DARK_GRAVEL);

    public static final DeferredItem<BlockItem> BLUE_VINES_ITEM =
        ITEMS.registerSimpleBlockItem("blue_vines", ModBlocks.BLUE_VINES);

    // Note: blue_vines_plant has no item — the tip (blue_vines) is the obtainable form.

    // ── Magma Boots (feet armor, custom 3D worn model) ────────────────────────
    // Worn model + texture are client-side: MagmaBootsModelDef / MagmaBootsClientExtensions
    // swap in the obsidian-shaft + lava-sole geometry for the HUMANOID armor layer,
    // and the equipment asset JSON (assets/ironhold/equipment/magma_boots.json) points
    // at textures/entity/equipment/humanoid/magma_boots.png.
    public static final ResourceKey<EquipmentAsset> EQ_MAGMA_BOOTS =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ironhold.MODID, "magma_boots"));

    /** Netherite-tier defensive boots with a fiery theme. assetId drives the worn texture/model. */
    public static final ArmorMaterial MAGMA_BOOTS_MATERIAL = new ArmorMaterial(
        37,                                       // base durability units (×ArmorType multiplier)
        Map.of(ArmorType.BOOTS, 3),               // defense (netherite-boot parity)
        15,                                       // enchantment value
        SoundEvents.ARMOR_EQUIP_NETHERITE,        // equip sound
        3.0f,                                     // toughness
        0.1f,                                     // knockback resistance
        ItemTags.REPAIRS_NETHERITE_ARMOR,         // repair ingredient (netherite ingot)
        EQ_MAGMA_BOOTS);

    public static final DeferredItem<Item> MAGMA_BOOTS =
        ITEMS.registerItem("magma_boots",
            p -> new Item(p.humanoidArmor(MAGMA_BOOTS_MATERIAL, ArmorType.BOOTS)
                .rarity(net.minecraft.world.item.Rarity.RARE)
                .fireResistant()));

    // ── Accessories (Terraria-inspired) ───────────────────────────────────────

    public static final DeferredItem<HermesBootsItem> HERMES_BOOTS =
        ITEMS.registerItem("hermes_boots", HermesBootsItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredItem<BandOfRegenerationItem> BAND_OF_REGENERATION =
        ITEMS.registerItem("band_of_regeneration", BandOfRegenerationItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredItem<CloudInABottleItem> CLOUD_IN_A_BOTTLE =
        ITEMS.registerItem("cloud_in_a_bottle", CloudInABottleItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE));

    /** Seashell — accessory granting an underwater dash. See {@link kingdom.smp.item.SeashellItem}. */
    public static final DeferredItem<kingdom.smp.item.SeashellItem> SEASHELL =
        ITEMS.registerItem("seashell", kingdom.smp.item.SeashellItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1));

    public static final DeferredItem<MimicKeyItem> MIMIC_KEY =
        ITEMS.registerItem("mimic_key", MimicKeyItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1));

    /** Accessory: summons the Je11ie & Cheakie slime-head pets while equipped. */
    public static final DeferredItem<PinkSlimeBallItem> PINK_SLIME_BALL =
        ITEMS.registerItem("pink_slime_ball", PinkSlimeBallItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1));

    /** Compass that tracks a player you right-click on. Spins when out-of-dimension. */
    public static final DeferredItem<kingdom.smp.item.PlayerCompassItem> PLAYER_COMPASS =
        ITEMS.registerItem("player_compass", kingdom.smp.item.PlayerCompassItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE).stacksTo(1));

    public static final DeferredItem<Item> FOOLS_GOLD = ITEMS.registerSimpleItem(
        "fools_gold",
        props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> FILCHER_CROWN = ITEMS.registerSimpleItem(
        "filcher_crown",
        props -> props.rarity(net.minecraft.world.item.Rarity.RARE)
    );

    /** Enhanced Pickaxe — automatically smelts ores as they are mined.
     *  Drops smelted results (e.g., Iron Ingot) instead of raw ore blocks. */
    public static final DeferredItem<EnhancedPickaxeItem> ENHANCED_PICKAXE = ITEMS.registerItem(
        "enhanced_pickaxe",
        EnhancedPickaxeItem::new,
        props -> props.durability(250)
            .rarity(net.minecraft.world.item.Rarity.UNCOMMON)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "enhanced_pickaxe_damage"),
                        6.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(Ironhold.MODID, "enhanced_pickaxe_speed"),
                        -2.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Block items
    public static final DeferredItem<BlockItem> GUILLOTINE_OAK_ITEM      = ITEMS.registerSimpleBlockItem("guillotine_oak",      ModBlocks.GUILLOTINE_OAK_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_SPRUCE_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_spruce",   ModBlocks.GUILLOTINE_SPRUCE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_BIRCH_ITEM    = ITEMS.registerSimpleBlockItem("guillotine_birch",    ModBlocks.GUILLOTINE_BIRCH_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_JUNGLE_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_jungle",   ModBlocks.GUILLOTINE_JUNGLE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_ACACIA_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_acacia",   ModBlocks.GUILLOTINE_ACACIA_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_DARK_OAK_ITEM = ITEMS.registerSimpleBlockItem("guillotine_dark_oak", ModBlocks.GUILLOTINE_DARK_OAK_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_MANGROVE_ITEM = ITEMS.registerSimpleBlockItem("guillotine_mangrove", ModBlocks.GUILLOTINE_MANGROVE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_CHERRY_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_cherry",   ModBlocks.GUILLOTINE_CHERRY_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_CRIMSON_ITEM  = ITEMS.registerSimpleBlockItem("guillotine_crimson",  ModBlocks.GUILLOTINE_CRIMSON_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_WARPED_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_warped",   ModBlocks.GUILLOTINE_WARPED_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_EBONY_ITEM    = ITEMS.registerSimpleBlockItem("guillotine_ebony",    ModBlocks.GUILLOTINE_EBONY_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_CHINESE_CEDAR_ITEM = ITEMS.registerSimpleBlockItem("guillotine_chinese_cedar", ModBlocks.GUILLOTINE_CHINESE_CEDAR_BLOCK);

    @SuppressWarnings("unchecked")
    public static final DeferredItem<BlockItem>[] ALL_GUILLOTINES = new DeferredItem[] {
        GUILLOTINE_OAK_ITEM, GUILLOTINE_SPRUCE_ITEM, GUILLOTINE_BIRCH_ITEM, GUILLOTINE_JUNGLE_ITEM, GUILLOTINE_ACACIA_ITEM,
        GUILLOTINE_DARK_OAK_ITEM, GUILLOTINE_MANGROVE_ITEM, GUILLOTINE_CHERRY_ITEM, GUILLOTINE_CRIMSON_ITEM, GUILLOTINE_WARPED_ITEM,
        GUILLOTINE_EBONY_ITEM, GUILLOTINE_CHINESE_CEDAR_ITEM
    };




    public static final DeferredItem<BlockItem> WARDHEART_ITEM =
        ITEMS.registerSimpleBlockItem("wardheart", ModBlocks.WARDHEART_BLOCK,
            () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC));



    public static final DeferredItem<BlockItem> CLASS_STONE_ITEM =
        ITEMS.registerSimpleBlockItem("class_stone", ModBlocks.CLASS_STONE_BLOCK,
            () -> new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));


    public static final DeferredItem<kingdom.smp.item.ChorusChargeItem> CHORUS_CHARGE =
        ITEMS.registerItem("chorus_charge", kingdom.smp.item.ChorusChargeItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON).stacksTo(64));

    public static final DeferredItem<Item> END_CRYSTAL_SHARD =
        ITEMS.registerSimpleItem("end_crystal_shard",
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE));

    /** Cheese wedge — primary food for taming rats; also a snack restoring 4 hunger.
     *  Crafted from milk + wheat (placeholder recipe, see data/recipe/cheese_wedge.json). */
    public static final DeferredItem<Item> CHEESE_WEDGE =
        ITEMS.registerItem("cheese_wedge",
            props -> new Item(props.food(
                new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(4).saturationModifier(0.6F).build())));

    /** Swollen lymph node — drops from cows/players that die while plagued. Sole non-renewable
     *  cure ingredient for the Plague Tonic. */
    public static final DeferredItem<Item> PLAGUE_BUBO =
        ITEMS.registerSimpleItem("plague_bubo",
            props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    /** Drinkable cure for the Plague effect at any stage. Crafted from milk + honey + golden
     *  apple + plague bubo (returns empty bucket on craft). */
    public static final DeferredItem<Item> PLAGUE_TONIC =
        ITEMS.registerItem("plague_tonic",
            props -> new kingdom.smp.item.PlagueTonicItem(
                props.rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Drop from the White Shulker — used in magical warding items. */
    public static final DeferredItem<Item> PURIFIED_SHELL =
        ITEMS.registerSimpleItem("purified_shell",
            props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    /** Drop from the Black Shulker — used for teleport daggers / stealth gear. */
    public static final DeferredItem<Item> VOID_CORE =
        ITEMS.registerSimpleItem("void_core",
            props -> props.rarity(net.minecraft.world.item.Rarity.RARE));

    /** Common Shroomling drop — a faintly glowing blue cave mushroom. */
    public static final DeferredItem<Item> GLOWSHROOM =
        ITEMS.registerSimpleItem("glowshroom");

    /** Moonshroom Stew — the moon's take on mushroom stew. Eating it steeps you in the lunar pull:
     *  3 minutes of {@link kingdom.smp.effect.LunarLevityEffect Lunar Levity} (~1/6 g). Restores the
     *  same hunger as vanilla stew and, like it, leaves an empty bowl. */
    public static final DeferredItem<Item> MOONSHROOM_STEW =
        ITEMS.registerItem("moonshroom_stew",
            props -> new Item(props.stacksTo(1)
                // 26.1.2 dropped Item.Properties#usingConvertsTo; the eat-to-bowl behaviour now lives
                // on the USE_REMAINDER data component (UseRemainder takes an ItemStackTemplate).
                .component(net.minecraft.core.component.DataComponents.USE_REMAINDER,
                    new net.minecraft.world.item.component.UseRemainder(
                        new net.minecraft.world.item.ItemStackTemplate(net.minecraft.world.item.Items.BOWL)))
                .food(
                    new net.minecraft.world.food.FoodProperties.Builder()
                        .nutrition(6).saturationModifier(0.6F).build(),
                    // 1.21.2+: eat-time effects live on the Consumable component, not FoodProperties.
                    net.minecraft.world.item.component.Consumable.builder()
                        .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                            new net.minecraft.world.effect.MobEffectInstance(
                                ModEffects.LUNAR_LEVITY_EFFECT, 20 * 60 * 3, 0), 1.0F))
                        .build())));

    /** Wearable vanity cap lifted from a (blue) Shroomling — worn atop the head as the 3D cap.
     *  A {@link VanityCosmeticItem} (no Equippable component): it lives only in the HEAD vanity
     *  slot and is drawn by {@code ShroomcapLayer}, never by vanilla's armor/head-item layers. */
    public static final DeferredItem<Item> SHROOMCAP =
        ITEMS.registerItem("shroomcap",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Wearable vanity cap lifted from the rare glowing-orange Shroomling. */
    public static final DeferredItem<Item> SHROOMCAP_ORANGE =
        ITEMS.registerItem("shroomcap_orange",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Wearable vanity cat ears — a pair of 3D ears worn atop the head.
     *  A {@link VanityCosmeticItem} (no Equippable component): it lives only in the HEAD vanity
     *  slot and is drawn by {@code CatEarsLayer}, never by vanilla's armor/head-item layers. */
    public static final DeferredItem<Item> CAT_EARS =
        ITEMS.registerItem("cat_ears",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Cat-ear colour variants — same geometry as {@link #CAT_EARS}, differ only by texture
     *  (picked per-item in {@code CatEarsLayer}). */
    public static final DeferredItem<Item> CAT_EARS_BLACK =
        ITEMS.registerItem("cat_ears_black",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CAT_EARS_WHITE =
        ITEMS.registerItem("cat_ears_white",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CAT_EARS_CALICO =
        ITEMS.registerItem("cat_ears_calico",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CAT_EARS_SIAMESE =
        ITEMS.registerItem("cat_ears_siamese",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Head-appendage vanity cosmetics — distinct 3D geometry drawn by {@code HeadAppendageLayer}
     *  (one shared layer; each item selects its baked part + texture). HEAD vanity slot. */
    public static final DeferredItem<Item> BUNNY_EARS =
        ITEMS.registerItem("bunny_ears",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FOX_EARS =
        ITEMS.registerItem("fox_ears",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> DEVIL_HORNS =
        ITEMS.registerItem("devil_horns",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> UNICORN_HORN =
        ITEMS.registerItem("unicorn_horn",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> RAM_HORNS =
        ITEMS.registerItem("ram_horns",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ANTENNAE =
        ITEMS.registerItem("antennae",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ELF_EARS =
        ITEMS.registerItem("elf_ears",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Wearable vanity halo — a small glowing ring floating above the head.
     *  Additive overlay: shown on top of any worn helmet rather than replacing it. */
    public static final DeferredItem<Item> HALO =
        ITEMS.registerItem("halo",
            props -> new VanityCosmeticItem(EquipmentSlot.HEAD, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Wearable vanity angel wings — feathered wings worn on the back.
     *  Additive overlay: shown on top of any worn chestplate rather than replacing it. */
    public static final DeferredItem<Item> ANGEL_WINGS =
        ITEMS.registerItem("angel_wings",
            props -> new VanityCosmeticItem(EquipmentSlot.CHEST, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Wearable vanity cape — a cloth drape hanging down the back. Additive CHEST overlay
     *  (shown on top of any worn chestplate), drawn by {@code BackCosmeticLayer}. */
    public static final DeferredItem<Item> CAPE =
        ITEMS.registerItem("cape",
            props -> new VanityCosmeticItem(EquipmentSlot.CHEST, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** Vanity tails — additive LEGS overlays (so a tail and a cape can be worn together),
     *  drawn by {@code BackCosmeticLayer}. */
    public static final DeferredItem<Item> CAT_TAIL =
        ITEMS.registerItem("cat_tail",
            props -> new VanityCosmeticItem(EquipmentSlot.LEGS, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> FOX_TAIL =
        ITEMS.registerItem("fox_tail",
            props -> new VanityCosmeticItem(EquipmentSlot.LEGS, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> DRAGON_TAIL =
        ITEMS.registerItem("dragon_tail",
            props -> new VanityCosmeticItem(EquipmentSlot.LEGS, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> DEVIL_TAIL =
        ITEMS.registerItem("devil_tail",
            props -> new VanityCosmeticItem(EquipmentSlot.LEGS, true,
                props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Right-click a creature you're looking at to take its form; right-click empty air to revert. */
    public static final DeferredItem<Item> MASTER_OF_DISGUISE_TOME =
        ITEMS.registerItem("master_of_disguise_tome",
            props -> new DisguiseTomeItem(props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ── Creative tab (all mod items in one place) ──────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IRONHOLD_TAB =
        CREATIVE_TABS.register("ironhold_tab", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.literal("Kingdom SMP"))
            .icon(() -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD))
            .displayItems((params, output) -> {
                output.accept(MASTER_OF_DISGUISE_TOME.get());
                output.accept(KINGDOM_VILLAGER_SPAWN_EGG.get());
                output.accept(WARDEN_HALRIC_SPAWN_EGG.get());
                output.accept(TALLYKEEPER_SPAWN_EGG.get());
                output.accept(CEMETERY_WATCHER_SPAWN_EGG.get());
                output.accept(MIRA_INNKEEPER_SPAWN_EGG.get());
                output.accept(BLACKSMITH_TOBIAS_SPAWN_EGG.get());
                output.accept(PLAGUE_DOCTOR_SPAWN_EGG.get());
                output.accept(PRIEST_CEDRIC_SPAWN_EGG.get());
                output.accept(OLD_HESTA_SPAWN_EGG.get());
                output.accept(OLD_BEREN_SPAWN_EGG.get());
                output.accept(CAPTAIN_ROSELIND_SPAWN_EGG.get());
                output.accept(LOREMASTER_EILAN_SPAWN_EGG.get());
                output.accept(SISTER_WREN_SPAWN_EGG.get());
                output.accept(BRAM_BARD_SPAWN_EGG.get());
                output.accept(PIPPA_URCHIN_SPAWN_EGG.get());
                output.accept(PURPLE_ALLAY_SPAWN_EGG.get());
                output.accept(WILL_O_WISP_SPAWN_EGG.get());
                output.accept(WILL_O_WISP_2_SPAWN_EGG.get());
                output.accept(PINK_DEER_SPAWN_EGG.get());
                output.accept(ARCANE_MAGE_SPAWN_EGG.get());
                output.accept(FILCHER_SPAWN_EGG.get());
                output.accept(VOID_INVOKER_SPAWN_EGG.get());
                output.accept(NULL_STALKER_SPAWN_EGG.get());
                output.accept(POSSESSED_ARMOR_SPAWN_EGG.get());
                output.accept(SIREN_SPAWN_EGG.get());
                output.accept(SHIPWRECK_MIMIC_SPAWN_EGG.get());
                output.accept(MIMIC_SPAWN_EGG.get());
                output.accept(BABY_MIMIC_SPAWN_EGG.get());
                output.accept(SLIME_PET_JE11IE_SPAWN_EGG.get());
                output.accept(SLIME_PET_CHEAKIE_SPAWN_EGG.get());
                output.accept(KINGDOM_DRAGON_SPAWN_EGG.get());
                output.accept(RARE_PINK_DEER_SPAWN_EGG.get());
                output.accept(KNIGHT_RECRUIT_SPAWN_EGG.get());
                output.accept(KNIGHT_MAN_AT_ARMS_SPAWN_EGG.get());
                output.accept(KNIGHT_CROSSBOWMAN_SPAWN_EGG.get());
                output.accept(KNIGHT_ARMORED_SPAWN_EGG.get());
                output.accept(KNIGHT_CRUSADER_SPAWN_EGG.get());
                output.accept(KNIGHT_GOTHIC_SPAWN_EGG.get());
                output.accept(KNIGHT_GOLD_SPAWN_EGG.get());
                output.accept(KNIGHT_JOUSTER_SPAWN_EGG.get());
                output.accept(KNIGHT_VETERAN_SPAWN_EGG.get());
                output.accept(MOM_PINK_DEER_SPAWN_EGG.get());
                output.accept(RAT_SPAWN_EGG.get());
                output.accept(SHROOMLING_SPAWN_EGG.get());
                output.accept(MOONSHROOM_SPAWN_EGG.get());
                output.accept(SHULKER_HERDER_SPAWN_EGG.get());
                output.accept(WHITE_SHULKER_SPAWN_EGG.get());
                output.accept(BLACK_SHULKER_SPAWN_EGG.get());
                output.accept(PURIFIED_SHELL.get());
                output.accept(VOID_CORE.get());
                output.accept(GLOWSHROOM.get());
                output.accept(MOONSHROOM_STEW.get());
                output.accept(SHROOMCAP.get());
                output.accept(SHROOMCAP_ORANGE.get());
                output.accept(CAT_EARS.get());
                output.accept(CAT_EARS_BLACK.get());
                output.accept(CAT_EARS_WHITE.get());
                output.accept(CAT_EARS_CALICO.get());
                output.accept(CAT_EARS_SIAMESE.get());
                output.accept(BUNNY_EARS.get());
                output.accept(FOX_EARS.get());
                output.accept(DEVIL_HORNS.get());
                output.accept(UNICORN_HORN.get());
                output.accept(RAM_HORNS.get());
                output.accept(ANTENNAE.get());
                output.accept(ELF_EARS.get());
                output.accept(HALO.get());
                output.accept(ANGEL_WINGS.get());
                output.accept(CAPE.get());
                output.accept(CAT_TAIL.get());
                output.accept(FOX_TAIL.get());
                output.accept(DRAGON_TAIL.get());
                output.accept(DEVIL_TAIL.get());
                output.accept(CHEESE_WEDGE.get());
                output.accept(PLAGUE_BUBO.get());
                output.accept(PLAGUE_TONIC.get());
                output.accept(BANDAGE.get());
                output.accept(MAGIC_MINECART_ITEM.get());
                output.accept(MIRROR.get());
                output.accept(LOCK.get());
                output.accept(INVISIBLE_ITEM_FRAME.get());
                output.accept(TEMPEST_BOW.get());
                output.accept(TEMPEST_ARROW.get());
                output.accept(ANKH_SHIELD.get());
                output.accept(ARCANE_SCEPTER.get());
                output.accept(DIAMOND_SCEPTER.get());
                output.accept(BATTLE_HAMMER.get());
                output.accept(SOLUNA_STAFF.get());
                output.accept(WIZARD_STAFF.get());
                output.accept(WIZARD_STICK.get());
                output.accept(HALRIC_STAFF.get());
                output.accept(CLASS_STONE_ITEM.get());
                output.accept(PITCHFORK.get());
                output.accept(MAGMA_BOOTS.get());
                output.accept(HERMES_BOOTS.get());
                output.accept(PLAYER_COMPASS.get());
                output.accept(BAND_OF_REGENERATION.get());
                output.accept(CLOUD_IN_A_BOTTLE.get());
                output.accept(WRAITHS_SIGIL.get());
                output.accept(SEASHELL.get());
                output.accept(SIRENS_RING.get());
                output.accept(VENGEFUL_HALBERD.get());
                output.accept(ARMOR_POLISH.get());
                output.accept(GOLD_COIN.get());
                output.accept(RAW_TANZANITE.get());
                output.accept(TANZANITE_GEM.get());
                output.accept(STEEL_INGOT.get());
                output.accept(FOOLS_GOLD.get());
                output.accept(FOOLS_GOLD_ORE_ITEM.get());
                output.accept(DEEPSLATE_FOOLS_GOLD_ORE_ITEM.get());
                output.accept(FILCHER_CROWN.get());
                output.accept(TANZANITE_ORE_ITEM.get());
                output.accept(EBONY_LOG_ITEM.get());
                output.accept(STRIPPED_EBONY_LOG_ITEM.get());
                output.accept(EBONY_PLANKS_ITEM.get());
                output.accept(EBONY_SLAB_ITEM.get());
                output.accept(EBONY_STAIRS_ITEM.get());
                output.accept(EBONY_FENCE_ITEM.get());
                output.accept(EBONY_FENCE_GATE_ITEM.get());
                output.accept(EBONY_DOOR_ITEM.get());
                output.accept(EBONY_TRAPDOOR_ITEM.get());
                output.accept(EBONY_BUTTON_ITEM.get());
                output.accept(EBONY_PRESSURE_PLATE_ITEM.get());
                output.accept(EBONY_LEAVES_ITEM.get());
                output.accept(CHINESE_CEDAR_LOG_ITEM.get());
                output.accept(STRIPPED_CHINESE_CEDAR_LOG_ITEM.get());
                output.accept(STRIPPED_CHINESE_CEDAR_WOOD_ITEM.get());
                output.accept(CHINESE_CEDAR_PLANKS_ITEM.get());
                output.accept(CHINESE_CEDAR_SLAB_ITEM.get());
                output.accept(CHINESE_CEDAR_STAIRS_ITEM.get());
                output.accept(CHINESE_CEDAR_FENCE_ITEM.get());
                output.accept(CHINESE_CEDAR_FENCE_GATE_ITEM.get());
                output.accept(CHINESE_CEDAR_DOOR_ITEM.get());
                output.accept(CHINESE_CEDAR_TRAPDOOR_ITEM.get());
                output.accept(CHINESE_CEDAR_BUTTON_ITEM.get());
                output.accept(CHINESE_CEDAR_PRESSURE_PLATE_ITEM.get());
                output.accept(BAT_FLOWER_ITEM.get());
                output.accept(ORANGE_GLOWSHROOMS_ITEM.get());
                output.accept(BLUE_GLOWSHROOMS_ITEM.get());
                output.accept(EBONWOOD_GRASS_ITEM.get());
                output.accept(BLUE_VINES_ITEM.get());
                output.accept(BLACK_SAND_ITEM.get());
                output.accept(DARK_GRAVEL_ITEM.get());
                for (var g : ALL_GUILLOTINES) output.accept(g.get());
                output.accept(WARDHEART_ITEM.get());
                output.accept(CHORUS_CHARGE.get());
                output.accept(END_CRYSTAL_SHARD.get());
            })
            .build());


    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(ModItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGIC_MINECART_ITEM.get());
            event.accept(LOCK.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ARCANE_MAGE_SPAWN_EGG.get());
            event.accept(FILCHER_SPAWN_EGG.get());
            event.accept(VOID_INVOKER_SPAWN_EGG.get());
            event.accept(NULL_STALKER_SPAWN_EGG.get());
            event.accept(KNIGHT_RECRUIT_SPAWN_EGG.get());
            event.accept(KNIGHT_MAN_AT_ARMS_SPAWN_EGG.get());
            event.accept(KNIGHT_CROSSBOWMAN_SPAWN_EGG.get());
            event.accept(KNIGHT_ARMORED_SPAWN_EGG.get());
            event.accept(KNIGHT_CRUSADER_SPAWN_EGG.get());
            event.accept(KNIGHT_GOTHIC_SPAWN_EGG.get());
            event.accept(KNIGHT_GOLD_SPAWN_EGG.get());
            event.accept(KNIGHT_JOUSTER_SPAWN_EGG.get());
            event.accept(KNIGHT_VETERAN_SPAWN_EGG.get());
            event.accept(KINGDOM_VILLAGER_SPAWN_EGG.get());
            event.accept(KANGARUDE_SPAWN_EGG.get());
            event.accept(WARDEN_HALRIC_SPAWN_EGG.get());
            event.accept(TALLYKEEPER_SPAWN_EGG.get());
            event.accept(CEMETERY_WATCHER_SPAWN_EGG.get());
            event.accept(MIRA_INNKEEPER_SPAWN_EGG.get());
            event.accept(BLACKSMITH_TOBIAS_SPAWN_EGG.get());
            event.accept(PLAGUE_DOCTOR_SPAWN_EGG.get());
            event.accept(PRIEST_CEDRIC_SPAWN_EGG.get());
            event.accept(OLD_HESTA_SPAWN_EGG.get());
            event.accept(OLD_BEREN_SPAWN_EGG.get());
            event.accept(CAPTAIN_ROSELIND_SPAWN_EGG.get());
            event.accept(LOREMASTER_EILAN_SPAWN_EGG.get());
            event.accept(SISTER_WREN_SPAWN_EGG.get());
            event.accept(BRAM_BARD_SPAWN_EGG.get());
            event.accept(PIPPA_URCHIN_SPAWN_EGG.get());
            event.accept(PURPLE_ALLAY_SPAWN_EGG.get());
            event.accept(WILL_O_WISP_SPAWN_EGG.get());
            event.accept(WILL_O_WISP_2_SPAWN_EGG.get());
            event.accept(PINK_DEER_SPAWN_EGG.get());
            event.accept(RARE_PINK_DEER_SPAWN_EGG.get());
            event.accept(MOM_PINK_DEER_SPAWN_EGG.get());
            event.accept(RAT_SPAWN_EGG.get());
            event.accept(SHROOMLING_SPAWN_EGG.get());
            event.accept(MOONSHROOM_SPAWN_EGG.get());
            event.accept(MIMIC_SPAWN_EGG.get());
            event.accept(BABY_MIMIC_SPAWN_EGG.get());
            event.accept(SLIME_PET_JE11IE_SPAWN_EGG.get());
            event.accept(SLIME_PET_CHEAKIE_SPAWN_EGG.get());
            event.accept(KINGDOM_DRAGON_SPAWN_EGG.get());
            event.accept(POSSESSED_ARMOR_SPAWN_EGG.get());
            event.accept(SIREN_SPAWN_EGG.get());
            event.accept(SHIPWRECK_MIMIC_SPAWN_EGG.get());
            event.accept(KING_ENDERMAN_SPAWN_EGG.get());
            event.accept(WHITE_SHULKER_SPAWN_EGG.get());
            event.accept(BLACK_SHULKER_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TEMPEST_BOW.get());
            event.accept(TEMPEST_ARROW.get());
            event.accept(ANKH_SHIELD.get());
            event.accept(ARCANE_SCEPTER.get());
            event.accept(DIAMOND_SCEPTER.get());
            event.accept(BATTLE_HAMMER.get());
            event.accept(SOLUNA_STAFF.get());
            event.accept(WIZARD_STAFF.get());
            event.accept(WIZARD_STICK.get());
            event.accept(HALRIC_STAFF.get());
            event.accept(PITCHFORK.get());
            event.accept(VENGEFUL_HALBERD.get());
            event.accept(MAGMA_BOOTS.get());
            // Accessories
            event.accept(HERMES_BOOTS.get());
            event.accept(BAND_OF_REGENERATION.get());
            event.accept(CLOUD_IN_A_BOTTLE.get());
            event.accept(SEASHELL.get());
            event.accept(MIMIC_KEY.get());
            event.accept(PINK_SLIME_BALL.get());
            event.accept(SIRENS_RING.get());
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(TANZANITE_ORE_ITEM.get());
            event.accept(EBONY_LOG_ITEM.get());
            event.accept(EBONY_LEAVES_ITEM.get());
            event.accept(BAT_FLOWER_ITEM.get());
            event.accept(ORANGE_GLOWSHROOMS_ITEM.get());
            event.accept(BLUE_GLOWSHROOMS_ITEM.get());
            event.accept(EBONWOOD_GRASS_ITEM.get());
            event.accept(BLUE_VINES_ITEM.get());
            event.accept(BLACK_SAND_ITEM.get());
            event.accept(DARK_GRAVEL_ITEM.get());
            event.accept(STRIPPED_EBONY_LOG_ITEM.get());
            event.accept(EBONY_PLANKS_ITEM.get());
            event.accept(EBONY_SLAB_ITEM.get());
            event.accept(EBONY_STAIRS_ITEM.get());
            event.accept(EBONY_FENCE_ITEM.get());
            event.accept(EBONY_FENCE_GATE_ITEM.get());
            event.accept(EBONY_DOOR_ITEM.get());
            event.accept(EBONY_TRAPDOOR_ITEM.get());
            event.accept(EBONY_BUTTON_ITEM.get());
            event.accept(EBONY_PRESSURE_PLATE_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(GOLD_COIN.get());
            event.accept(RAW_TANZANITE.get());
            event.accept(TANZANITE_GEM.get());
            event.accept(STEEL_INGOT.get());
            event.accept(FOOLS_GOLD.get());
            event.accept(FOOLS_GOLD_ORE_ITEM.get());
            event.accept(DEEPSLATE_FOOLS_GOLD_ORE_ITEM.get());
            event.accept(FILCHER_CROWN.get());
            event.accept(GLOWSHROOM.get());
            event.accept(SHROOMCAP.get());
            event.accept(SHROOMCAP_ORANGE.get());
            event.accept(CAT_EARS.get());
            event.accept(CAT_EARS_BLACK.get());
            event.accept(CAT_EARS_WHITE.get());
            event.accept(CAT_EARS_CALICO.get());
            event.accept(CAT_EARS_SIAMESE.get());
            event.accept(BUNNY_EARS.get());
            event.accept(FOX_EARS.get());
            event.accept(DEVIL_HORNS.get());
            event.accept(UNICORN_HORN.get());
            event.accept(RAM_HORNS.get());
            event.accept(ANTENNAE.get());
            event.accept(ELF_EARS.get());
            event.accept(HALO.get());
            event.accept(ANGEL_WINGS.get());
            event.accept(CAPE.get());
            event.accept(CAT_TAIL.get());
            event.accept(FOX_TAIL.get());
            event.accept(DRAGON_TAIL.get());
            event.accept(DEVIL_TAIL.get());
        }
    }
}
