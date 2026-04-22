package kingdom.smp;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import kingdom.smp.accessory.AccessoryMenuTypes;
import kingdom.smp.entity.ArcaneBoltEntity;
import kingdom.smp.entity.ArcaneInvokerEntity;
import kingdom.smp.entity.ArcaneOrbEntity;
import kingdom.smp.entity.LunarOrbEntity;
import kingdom.smp.entity.PossessedArmorEntity;
import kingdom.smp.entity.ShipwreckMimicEntity;
import kingdom.smp.entity.SirenEntity;
import kingdom.smp.entity.SolarOrbEntity;
import kingdom.smp.entity.ArcaneWizardEntity;
import kingdom.smp.entity.HexBoltEntity;
import kingdom.smp.entity.MagicMinecartEntity;
import kingdom.smp.entity.TempestArrowEntity;
import kingdom.smp.entity.ArcaneMageEntity;
import kingdom.smp.entity.FilcherEntity;
import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.entity.WardenHalricEntity;
import kingdom.smp.entity.MomPinkDeerEntity;
import kingdom.smp.entity.PinkDeerEntity;
import kingdom.smp.entity.RarePinkDeerEntity;
import kingdom.smp.entity.PurpleAllayEntity;
import kingdom.smp.entity.WillOWispEntity;
import kingdom.smp.entity.WillOWisp2Entity;
import kingdom.smp.entity.VoidInvokerEntity;
import kingdom.smp.entity.BabyMimicEntity;
import kingdom.smp.entity.MiniDragonEntity;
import kingdom.smp.entity.MimicEntity;
import kingdom.smp.entity.NullStalkerEntity;
import kingdom.smp.game.AccessoryTickHandler;
import kingdom.smp.item.ArcaneScepterItem;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.CloudDoubleJumpHandler;
import kingdom.smp.game.AnkhShieldHandler;
import kingdom.smp.game.ClassXpKillRewards;
import kingdom.smp.item.AnkhShieldItem;
import kingdom.smp.item.BandOfRegenerationItem;
import kingdom.smp.item.CloudInABottleItem;
import kingdom.smp.item.HermesBootsItem;
import kingdom.smp.item.MagicMinecartItem;
import kingdom.smp.item.MimicKeyItem;
import kingdom.smp.item.TempestArrowItem;
import kingdom.smp.item.TempestBowItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import kingdom.smp.block.BlueVinesBlock;
import kingdom.smp.block.BlueVinesPlantBlock;
import kingdom.smp.block.EbonyLeavesBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import kingdom.smp.worldgen.BlueVinesFeature;
import kingdom.smp.worldgen.CaveMouthFeature;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import kingdom.smp.game.TanzaniteWorldgenFluidHandler;
import kingdom.smp.net.ModNetworking;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Ironhold.MODID)
public class Ironhold {
    public static final String MODID = "ironhold";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    // ── Sound events ─────────────────────────────────────────────────────────
    // Ebonwood Hollow ambient loop — sound by CreativeMD / AmbientSounds mod (LGPL-3.0)
    // https://github.com/CreativeMD/AmbientSounds — credit required
    public static final DeferredHolder<SoundEvent, SoundEvent> EBONWOOD_AMBIENT =
        SOUND_EVENTS.register("ambient.ebonwood_hollow",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "ambient.ebonwood_hollow")));

    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_AMBIENT =
        SOUND_EVENTS.register("entity.pink_deer.ambient",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.pink_deer.ambient")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_HURT =
        SOUND_EVENTS.register("entity.pink_deer.hurt",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.pink_deer.hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_DEATH =
        SOUND_EVENTS.register("entity.pink_deer.death",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.pink_deer.death")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_MOM_HURT =
        SOUND_EVENTS.register("entity.pink_deer.mom.hurt",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.pink_deer.mom.hurt")));

    // ── Worldgen feature keys ─────────────────────────────────────────────────
    public static final DeferredHolder<Feature<?>, CaveMouthFeature> CAVE_MOUTH_FEATURE =
        FEATURES.register("cave_mouth", () -> new CaveMouthFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, BlueVinesFeature> BLUE_VINES_FEATURE =
        FEATURES.register("blue_vines", () -> new BlueVinesFeature(NoneFeatureConfiguration.CODEC));

    // ── Biome resource keys ───────────────────────────────────────────────────
    /** Resource key for the Ebonwood Hollow biome (data-driven; defined in worldgen/biome/). */
    public static final ResourceKey<Biome> EBONWOOD_HOLLOW = ResourceKey.create(
        Registries.BIOME,
        Identifier.fromNamespaceAndPath(MODID, "ebonwood_hollow")
    );

    public static final DeferredHolder<EntityType<?>, EntityType<MagicMinecartEntity>> MAGIC_MINECART_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "magic_minecart",
            MagicMinecartEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.98F, 0.7F).clientTrackingRange(8).updateInterval(1)
        );

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneWizardEntity>> ARCANE_WIZARD =
        ENTITY_TYPES.registerEntityType(
            "arcane_wizard",
            ArcaneWizardEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneInvokerEntity>> ARCANE_INVOKER =
        ENTITY_TYPES.registerEntityType(
            "arcane_invoker",
            ArcaneInvokerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneMageEntity>> ARCANE_MAGE =
        ENTITY_TYPES.registerEntityType(
            "arcane_mage",
            ArcaneMageEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> ARCANE_MAGE_SPAWN_EGG =
        ITEMS.registerItem(
            "arcane_mage_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(ARCANE_MAGE.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneOrbEntity>> ARCANE_ORB =
        ENTITY_TYPES.registerEntityType(
            "arcane_orb",
            ArcaneOrbEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.35F, 0.35F).clientTrackingRange(6).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBoltEntity>> ARCANE_BOLT =
        ENTITY_TYPES.registerEntityType(
            "arcane_bolt",
            ArcaneBoltEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<SolarOrbEntity>> SOLAR_ORB =
        ENTITY_TYPES.registerEntityType(
            "solar_orb",
            SolarOrbEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.35F, 0.35F).clientTrackingRange(6).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<LunarOrbEntity>> LUNAR_ORB =
        ENTITY_TYPES.registerEntityType(
            "lunar_orb",
            LunarOrbEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.35F, 0.35F).clientTrackingRange(6).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<HexBoltEntity>> HEX_BOLT =
        ENTITY_TYPES.registerEntityType(
            "hex_bolt",
            HexBoltEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.32F, 0.32F).clientTrackingRange(4).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<TempestArrowEntity>> TEMPEST_ARROW_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "tempest_arrow",
            TempestArrowEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidInvokerEntity>> VOID_INVOKER =
        ENTITY_TYPES.registerEntityType(
            "void_invoker",
            VoidInvokerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> VOID_INVOKER_SPAWN_EGG =
        ITEMS.registerItem(
            "void_invoker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(VOID_INVOKER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<NullStalkerEntity>> NULL_STALKER =
        ENTITY_TYPES.registerEntityType(
            "null_stalker",
            NullStalkerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 2.9F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredItem<Item> NULL_STALKER_SPAWN_EGG =
        ITEMS.registerItem(
            "null_stalker_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(NULL_STALKER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<PinkDeerEntity>> PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "pink_deer",
            PinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.7F, 1.1F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredItem<Item> PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(PINK_DEER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<RarePinkDeerEntity>> RARE_PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "rare_pink_deer",
            RarePinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.7F, 1.1F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredItem<Item> RARE_PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "rare_pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(RARE_PINK_DEER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<MomPinkDeerEntity>> MOM_PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "mom_pink_deer",
            MomPinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.95F, 1.45F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredItem<Item> MOM_PINK_DEER_SPAWN_EGG =
        ITEMS.registerItem(
            "mom_pink_deer_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(MOM_PINK_DEER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<PurpleAllayEntity>> PURPLE_ALLAY =
        ENTITY_TYPES.registerEntityType(
            "purple_allay",
            PurpleAllayEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredItem<Item> PURPLE_ALLAY_SPAWN_EGG =
        ITEMS.registerItem(
            "purple_allay_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(PURPLE_ALLAY.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<WillOWispEntity>> WILL_O_WISP =
        ENTITY_TYPES.registerEntityType(
            "will_o_wisp",
            WillOWispEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredItem<Item> WILL_O_WISP_SPAWN_EGG =
        ITEMS.registerItem(
            "will_o_wisp_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(WILL_O_WISP.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<WillOWisp2Entity>> WILL_O_WISP_2 =
        ENTITY_TYPES.registerEntityType(
            "will_o_wisp_2",
            WillOWisp2Entity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredItem<Item> WILL_O_WISP_2_SPAWN_EGG =
        ITEMS.registerItem(
            "will_o_wisp_2_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(WILL_O_WISP_2.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<KingdomVillagerEntity>> KINGDOM_VILLAGER =
        ENTITY_TYPES.registerEntityType(
            "kingdom_villager",
            KingdomVillagerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredItem<Item> KINGDOM_VILLAGER_SPAWN_EGG =
        ITEMS.registerItem(
            "kingdom_villager_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(KINGDOM_VILLAGER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<WardenHalricEntity>> WARDEN_HALRIC =
        ENTITY_TYPES.registerEntityType(
            "warden_halric",
            WardenHalricEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredItem<Item> WARDEN_HALRIC_SPAWN_EGG =
        ITEMS.registerItem(
            "warden_halric_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(WARDEN_HALRIC.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<FilcherEntity>> FILCHER =
        ENTITY_TYPES.registerEntityType(
            "filcher",
            FilcherEntity::new,
            MobCategory.MONSTER,
            // 0.49×0.99 = vanilla baby-zombie dimensions; eye at 0.75 (upper head).
            b -> b.sized(0.49F, 0.99F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> FILCHER_SPAWN_EGG =
        ITEMS.registerItem(
            "filcher_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(FILCHER.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<PossessedArmorEntity>> POSSESSED_ARMOR =
        ENTITY_TYPES.registerEntityType(
            "possessed_armor",
            PossessedArmorEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> POSSESSED_ARMOR_SPAWN_EGG =
        ITEMS.registerItem(
            "possessed_armor_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(POSSESSED_ARMOR.get()).stacksTo(64)));

    // King Enderman — endgame raid boss, ~2x Iron Golem scale
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.KingEndermanEntity>> KING_ENDERMAN =
        ENTITY_TYPES.registerEntityType(
            "king_enderman",
            kingdom.smp.entity.KingEndermanEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(3.0F, 5.5F).eyeHeight(4.9F).clientTrackingRange(12).updateInterval(3).fireImmune());

    public static final DeferredItem<Item> KING_ENDERMAN_SPAWN_EGG =
        ITEMS.registerItem(
            "king_enderman_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(KING_ENDERMAN.get()).stacksTo(64)));

    // Siren — ocean mob that lures players with song
    public static final DeferredHolder<EntityType<?>, EntityType<SirenEntity>> SIREN =
        ENTITY_TYPES.registerEntityType(
            "siren",
            SirenEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredItem<Item> SIREN_SPAWN_EGG =
        ITEMS.registerItem(
            "siren_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(SIREN.get()).stacksTo(64)));

    // Shipwreck Mimic — underwater mimic variant
    public static final DeferredHolder<EntityType<?>, EntityType<ShipwreckMimicEntity>> SHIPWRECK_MIMIC =
        ENTITY_TYPES.registerEntityType(
            "shipwreck_mimic",
            ShipwreckMimicEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 0.9375F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> SHIPWRECK_MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "shipwreck_mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(SHIPWRECK_MIMIC.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<MimicEntity>> MIMIC =
        ENTITY_TYPES.registerEntityType(
            "mimic",
            MimicEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 0.9375F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(MIMIC.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<BabyMimicEntity>> BABY_MIMIC =
        ENTITY_TYPES.registerEntityType(
            "baby_mimic",
            BabyMimicEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.22F, 0.24F).eyeHeight(0.18F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredItem<Item> BABY_MIMIC_SPAWN_EGG =
        ITEMS.registerItem(
            "baby_mimic_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(BABY_MIMIC.get()).stacksTo(64)));

    // Mini Dragon — tiny tameable blue ender dragon pet
    public static final DeferredHolder<EntityType<?>, EntityType<MiniDragonEntity>> MINI_DRAGON =
        ENTITY_TYPES.registerEntityType(
            "mini_dragon",
            MiniDragonEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.75F, 0.6F).eyeHeight(0.45F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredItem<Item> MINI_DRAGON_SPAWN_EGG =
        ITEMS.registerItem(
            "mini_dragon_spawn_egg",
            props -> new SpawnEggItem(props.spawnEgg(MINI_DRAGON.get()).stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.GuillotineSeatEntity>> GUILLOTINE_SEAT_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "guillotine_seat",
            kingdom.smp.entity.GuillotineSeatEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.01F, 0.01F).clientTrackingRange(3).updateInterval(20)
                  .noSummon());

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
                        Identifier.fromNamespaceAndPath(MODID, "scepter_damage"),
                        4.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "scepter_speed"),
                        -2.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "scepter_reach"),
                        1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
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
                        Identifier.fromNamespaceAndPath(MODID, "soluna_damage"),
                        5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "soluna_speed"),
                        -2.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .build()));

    // Pitchfork — throwable spear weapon (trident-like)
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.ThrownPitchforkEntity>> THROWN_PITCHFORK =
        ENTITY_TYPES.registerEntityType(
            "thrown_pitchfork",
            kingdom.smp.entity.ThrownPitchforkEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(2).noSummon());

    public static final DeferredItem<Item> PITCHFORK = ITEMS.registerItem(
        "pitchfork",
        kingdom.smp.item.PitchforkItem::new,
        props -> props
            .durability(400)
            .rarity(net.minecraft.world.item.Rarity.UNCOMMON)
            .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "pitchfork_damage"),
                        5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "pitchfork_speed"),
                        -2.4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "pitchfork_reach"),
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
                        Identifier.fromNamespaceAndPath(MODID, "halberd_damage"),
                        7.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "halberd_speed"),
                        -3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        Identifier.fromNamespaceAndPath(MODID, "halberd_reach"),
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
        props -> new MagicMinecartItem(MAGIC_MINECART_ENTITY.get(), props),
        props -> props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)
    );

    // ── Tanzanite ore ─────────────────────────────────────────────────────────
    // Worldgen: rare lava/water obsidian formations (Y -64..12) — see TanzaniteWorldgenFluidHandler.
    // Slightly slower to mine than obsidian; still needs diamond+ pick for drops.

    public static final DeferredBlock<Block> TANZANITE_ORE = BLOCKS.register(
        "tanzanite_ore",
        id -> new DropExperienceBlock(
            UniformInt.of(4, 9),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .requiresCorrectToolForDrops()
                .strength(52.0f, 1200.0f)
                .sound(SoundType.STONE)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredItem<BlockItem> TANZANITE_ORE_ITEM =
        ITEMS.registerSimpleBlockItem("tanzanite_ore", TANZANITE_ORE);

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

    public static final DeferredBlock<Block> EBONY_LOG = BLOCKS.register(
        "ebony_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredItem<BlockItem> EBONY_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_log", EBONY_LOG);

    public static final DeferredBlock<Block> EBONY_PLANKS = BLOCKS.register(
        "ebony_planks",
        id -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );

    public static final DeferredItem<BlockItem> EBONY_PLANKS_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_planks", EBONY_PLANKS);

    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> STRIPPED_EBONY_LOG = BLOCKS.register(
        "stripped_ebony_log",
        id -> new net.minecraft.world.level.block.RotatedPillarBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> STRIPPED_EBONY_LOG_ITEM =
        ITEMS.registerSimpleBlockItem("stripped_ebony_log", STRIPPED_EBONY_LOG);

    public static final DeferredBlock<net.minecraft.world.level.block.SlabBlock> EBONY_SLAB = BLOCKS.register(
        "ebony_slab",
        id -> new net.minecraft.world.level.block.SlabBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_SLAB_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_slab", EBONY_SLAB);

    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> EBONY_STAIRS = BLOCKS.register(
        "ebony_stairs",
        id -> new net.minecraft.world.level.block.StairBlock(
            EBONY_PLANKS.get().defaultBlockState(),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_STAIRS_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_stairs", EBONY_STAIRS);

    public static final DeferredBlock<net.minecraft.world.level.block.FenceBlock> EBONY_FENCE = BLOCKS.register(
        "ebony_fence",
        id -> new net.minecraft.world.level.block.FenceBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_FENCE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_fence", EBONY_FENCE);

    public static final DeferredBlock<net.minecraft.world.level.block.FenceGateBlock> EBONY_FENCE_GATE = BLOCKS.register(
        "ebony_fence_gate",
        id -> new net.minecraft.world.level.block.FenceGateBlock(
            net.minecraft.world.level.block.state.properties.WoodType.DARK_OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 3.0f)
                .sound(SoundType.WOOD)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_FENCE_GATE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_fence_gate", EBONY_FENCE_GATE);

    public static final DeferredBlock<net.minecraft.world.level.block.DoorBlock> EBONY_DOOR = BLOCKS.register(
        "ebony_door",
        id -> new net.minecraft.world.level.block.DoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_DOOR_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_door", EBONY_DOOR);

    public static final DeferredBlock<net.minecraft.world.level.block.TrapDoorBlock> EBONY_TRAPDOOR = BLOCKS.register(
        "ebony_trapdoor",
        id -> new net.minecraft.world.level.block.TrapDoorBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(3.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_TRAPDOOR_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_trapdoor", EBONY_TRAPDOOR);

    public static final DeferredBlock<net.minecraft.world.level.block.ButtonBlock> EBONY_BUTTON = BLOCKS.register(
        "ebony_button",
        id -> new net.minecraft.world.level.block.ButtonBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            30,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_BUTTON_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_button", EBONY_BUTTON);

    public static final DeferredBlock<net.minecraft.world.level.block.PressurePlateBlock> EBONY_PRESSURE_PLATE = BLOCKS.register(
        "ebony_pressure_plate",
        id -> new net.minecraft.world.level.block.PressurePlateBlock(
            net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noCollision()
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_PRESSURE_PLATE_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_pressure_plate", EBONY_PRESSURE_PLATE);

    // Ebony leaves — generated by ebony trees in the Ebonwood Hollow biome.
    // Fixed-colour texture (dark ashen grey-green); does NOT use biome tint.
    public static final DeferredBlock<EbonyLeavesBlock> EBONY_LEAVES = BLOCKS.register(
        "ebony_leaves",
        id -> new EbonyLeavesBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.2f)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn((s, l, p, e) -> false)
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONY_LEAVES_ITEM =
        ITEMS.registerSimpleBlockItem("ebony_leaves", EBONY_LEAVES);

    // Bat Flower — tall two-block dark flower native to Ebonwood Hollow.
    public static final DeferredBlock<TallFlowerBlock> BAT_FLOWER = BLOCKS.register(
        "bat_flower",
        id -> new TallFlowerBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .noCollision()
                .strength(0.0f)
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> BAT_FLOWER_ITEM =
        ITEMS.registerSimpleBlockItem("bat_flower", BAT_FLOWER);

    // Ebonwood Grass — grass block with mud sides/bottom, generated on biome surface.
    public static final DeferredBlock<net.minecraft.world.level.block.GrassBlock> EBONWOOD_GRASS = BLOCKS.register(
        "ebonwood_grass",
        id -> new net.minecraft.world.level.block.GrassBlock(
            BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> EBONWOOD_GRASS_ITEM =
        ITEMS.registerSimpleBlockItem("ebonwood_grass", EBONWOOD_GRASS);

    // ── Ebonwood Hollow terrain blocks ───────────────────────────────────────

    // Black Sand — replaces sand patches near water in the Ebonwood Hollow biome.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> BLACK_SAND = BLOCKS.register(
        "black_sand",
        id -> new net.minecraft.world.level.block.ColoredFallingBlock(
            new net.minecraft.util.ColorRGBA(0x08070EFF),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(0.5f)
                .sound(SoundType.SAND)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> BLACK_SAND_ITEM =
        ITEMS.registerSimpleBlockItem("black_sand", BLACK_SAND);

    // Dark Gravel — replaces gravel patches near water in the Ebonwood Hollow biome.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> DARK_GRAVEL = BLOCKS.register(
        "dark_gravel",
        id -> new net.minecraft.world.level.block.ColoredFallingBlock(
            new net.minecraft.util.ColorRGBA(0x1E1E26FF),
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.6f)
                .sound(SoundType.GRAVEL)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> DARK_GRAVEL_ITEM =
        ITEMS.registerSimpleBlockItem("dark_gravel", DARK_GRAVEL);

    // Blue Vines — upward-growing vine native to Ebonwood Hollow, deep-blue variant.
    public static final DeferredBlock<BlueVinesBlock> BLUE_VINES = BLOCKS.register(
        "blue_vines",
        id -> new BlueVinesBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .noCollision()
                .randomTicks()
                .instabreak()
                .sound(SoundType.WEEPING_VINES)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    public static final DeferredItem<BlockItem> BLUE_VINES_ITEM =
        ITEMS.registerSimpleBlockItem("blue_vines", BLUE_VINES);

    public static final DeferredBlock<BlueVinesPlantBlock> BLUE_VINES_PLANT = BLOCKS.register(
        "blue_vines_plant",
        id -> new BlueVinesPlantBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .noCollision()
                .instabreak()
                .sound(SoundType.WEEPING_VINES)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .setId(ResourceKey.create(Registries.BLOCK, id))
        )
    );
    // Note: blue_vines_plant has no item — the tip (blue_vines) is the obtainable form.

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

    public static final DeferredItem<MimicKeyItem> MIMIC_KEY =
        ITEMS.registerItem("mimic_key", MimicKeyItem::new,
            props -> props.rarity(net.minecraft.world.item.Rarity.EPIC).stacksTo(1));

    public static final DeferredItem<Item> FOOLS_GOLD = ITEMS.registerSimpleItem(
        "fools_gold",
        props -> props.rarity(net.minecraft.world.item.Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> FILCHER_CROWN = ITEMS.registerSimpleItem(
        "filcher_crown",
        props -> props.rarity(net.minecraft.world.item.Rarity.RARE)
    );

    // ── Guillotine blocks (wood variants) ───────────────────────────────────────
    private static DeferredBlock<Block> guillotineBlock(String name) {
        return BLOCKS.register(name,
            id -> new kingdom.smp.block.GuillotineBlock(
                BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
            )
        );
    }

    public static final DeferredBlock<Block> GUILLOTINE_OAK_BLOCK      = guillotineBlock("guillotine_oak");
    public static final DeferredBlock<Block> GUILLOTINE_SPRUCE_BLOCK   = guillotineBlock("guillotine_spruce");
    public static final DeferredBlock<Block> GUILLOTINE_BIRCH_BLOCK    = guillotineBlock("guillotine_birch");
    public static final DeferredBlock<Block> GUILLOTINE_JUNGLE_BLOCK   = guillotineBlock("guillotine_jungle");
    public static final DeferredBlock<Block> GUILLOTINE_ACACIA_BLOCK   = guillotineBlock("guillotine_acacia");
    public static final DeferredBlock<Block> GUILLOTINE_DARK_OAK_BLOCK = guillotineBlock("guillotine_dark_oak");
    public static final DeferredBlock<Block> GUILLOTINE_MANGROVE_BLOCK = guillotineBlock("guillotine_mangrove");
    public static final DeferredBlock<Block> GUILLOTINE_CHERRY_BLOCK   = guillotineBlock("guillotine_cherry");
    public static final DeferredBlock<Block> GUILLOTINE_CRIMSON_BLOCK  = guillotineBlock("guillotine_crimson");
    public static final DeferredBlock<Block> GUILLOTINE_WARPED_BLOCK   = guillotineBlock("guillotine_warped");
    public static final DeferredBlock<Block> GUILLOTINE_EBONY_BLOCK   = guillotineBlock("guillotine_ebony");

    // Block items
    public static final DeferredItem<BlockItem> GUILLOTINE_OAK_ITEM      = ITEMS.registerSimpleBlockItem("guillotine_oak",      GUILLOTINE_OAK_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_SPRUCE_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_spruce",   GUILLOTINE_SPRUCE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_BIRCH_ITEM    = ITEMS.registerSimpleBlockItem("guillotine_birch",    GUILLOTINE_BIRCH_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_JUNGLE_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_jungle",   GUILLOTINE_JUNGLE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_ACACIA_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_acacia",   GUILLOTINE_ACACIA_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_DARK_OAK_ITEM = ITEMS.registerSimpleBlockItem("guillotine_dark_oak", GUILLOTINE_DARK_OAK_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_MANGROVE_ITEM = ITEMS.registerSimpleBlockItem("guillotine_mangrove", GUILLOTINE_MANGROVE_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_CHERRY_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_cherry",   GUILLOTINE_CHERRY_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_CRIMSON_ITEM  = ITEMS.registerSimpleBlockItem("guillotine_crimson",  GUILLOTINE_CRIMSON_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_WARPED_ITEM   = ITEMS.registerSimpleBlockItem("guillotine_warped",   GUILLOTINE_WARPED_BLOCK);
    public static final DeferredItem<BlockItem> GUILLOTINE_EBONY_ITEM    = ITEMS.registerSimpleBlockItem("guillotine_ebony",    GUILLOTINE_EBONY_BLOCK);

    @SuppressWarnings("unchecked")
    public static final DeferredItem<BlockItem>[] ALL_GUILLOTINES = new DeferredItem[] {
        GUILLOTINE_OAK_ITEM, GUILLOTINE_SPRUCE_ITEM, GUILLOTINE_BIRCH_ITEM, GUILLOTINE_JUNGLE_ITEM, GUILLOTINE_ACACIA_ITEM,
        GUILLOTINE_DARK_OAK_ITEM, GUILLOTINE_MANGROVE_ITEM, GUILLOTINE_CHERRY_ITEM, GUILLOTINE_CRIMSON_ITEM, GUILLOTINE_WARPED_ITEM,
        GUILLOTINE_EBONY_ITEM
    };

    // Block entity type — shared by all guillotine variants
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<kingdom.smp.block.GuillotineBlockEntity>> GUILLOTINE_BLOCK_ENTITY =
        (DeferredHolder) BLOCK_ENTITY_TYPES.register("guillotine",
            () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(
                kingdom.smp.block.GuillotineBlockEntity::new,
                GUILLOTINE_OAK_BLOCK.get(), GUILLOTINE_SPRUCE_BLOCK.get(), GUILLOTINE_BIRCH_BLOCK.get(),
                GUILLOTINE_JUNGLE_BLOCK.get(), GUILLOTINE_ACACIA_BLOCK.get(), GUILLOTINE_DARK_OAK_BLOCK.get(),
                GUILLOTINE_MANGROVE_BLOCK.get(), GUILLOTINE_CHERRY_BLOCK.get(), GUILLOTINE_CRIMSON_BLOCK.get(),
                GUILLOTINE_WARPED_BLOCK.get(), GUILLOTINE_EBONY_BLOCK.get()
            )
        );

    // ── Creative tab (all mod items in one place) ──────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IRONHOLD_TAB =
        CREATIVE_TABS.register("ironhold_tab", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.literal("Kingdom SMP"))
            .icon(() -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD))
            .displayItems((params, output) -> {
                output.accept(KINGDOM_VILLAGER_SPAWN_EGG.get());
                output.accept(WARDEN_HALRIC_SPAWN_EGG.get());
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
                output.accept(MINI_DRAGON_SPAWN_EGG.get());
                output.accept(RARE_PINK_DEER_SPAWN_EGG.get());
                output.accept(MOM_PINK_DEER_SPAWN_EGG.get());
                output.accept(MAGIC_MINECART_ITEM.get());
                output.accept(TEMPEST_BOW.get());
                output.accept(TEMPEST_ARROW.get());
                output.accept(ANKH_SHIELD.get());
                output.accept(ARCANE_SCEPTER.get());
                output.accept(SOLUNA_STAFF.get());
                output.accept(PITCHFORK.get());
                output.accept(HERMES_BOOTS.get());
                output.accept(BAND_OF_REGENERATION.get());
                output.accept(CLOUD_IN_A_BOTTLE.get());
                output.accept(WRAITHS_SIGIL.get());
                output.accept(SIRENS_RING.get());
                output.accept(VENGEFUL_HALBERD.get());
                output.accept(ARMOR_POLISH.get());
                output.accept(RAW_TANZANITE.get());
                output.accept(TANZANITE_GEM.get());
                output.accept(STEEL_INGOT.get());
                output.accept(FOOLS_GOLD.get());
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
                output.accept(BAT_FLOWER_ITEM.get());
                output.accept(EBONWOOD_GRASS_ITEM.get());
                output.accept(BLUE_VINES_ITEM.get());
                output.accept(BLACK_SAND_ITEM.get());
                output.accept(DARK_GRAVEL_ITEM.get());
                for (var g : ALL_GUILLOTINES) output.accept(g.get());
            })
            .build());

    public Ironhold(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(Ironhold::addCreativeTabContents);
        modEventBus.addListener(Ironhold::registerEntityAttributes);
        modEventBus.addListener(Ironhold::registerSpawnPlacements);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        FEATURES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        ModAttachments.register(modEventBus);
        AccessoryMenuTypes.register(modEventBus);
        kingdom.smp.entity.BabyMimicMenuTypes.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(IronholdGameEvents.class);
        NeoForge.EVENT_BUS.register(AnkhShieldHandler.class);
        NeoForge.EVENT_BUS.register(ClassXpKillRewards.class);
        NeoForge.EVENT_BUS.register(AccessoryTickHandler.class);
        NeoForge.EVENT_BUS.register(CloudDoubleJumpHandler.class);
        NeoForge.EVENT_BUS.register(TanzaniteWorldgenFluidHandler.class);
        NeoForge.EVENT_BUS.register(EncumbranceHandler.class);

        modEventBus.addListener(ModNetworking::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Kingdom SMP 2.0 common setup complete.");
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(
            ARCANE_WIZARD.get(),
            Illusioner.createAttributes()
                .add(Attributes.MAX_HEALTH, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.SCALE, 1.0)
                .build());
        event.put(
            ARCANE_INVOKER.get(),
            Illusioner.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.SCALE, 1.0)
                .build());
        event.put(
            VOID_INVOKER.get(),
            Illusioner.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.44)
                .add(Attributes.SCALE, 1.0)
                .build());
        event.put(NULL_STALKER.get(), NullStalkerEntity.createAttributes().build());
        event.put(ARCANE_MAGE.get(), ArcaneMageEntity.createAttributes().build());
        event.put(FILCHER.get(), FilcherEntity.createAttributes().build());
        event.put(POSSESSED_ARMOR.get(), PossessedArmorEntity.createAttributes().build());
        event.put(KING_ENDERMAN.get(), kingdom.smp.entity.KingEndermanEntity.createAttributes().build());
        event.put(SIREN.get(), SirenEntity.createAttributes().build());
        event.put(SHIPWRECK_MIMIC.get(), ShipwreckMimicEntity.createAttributes().build());
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
        event.put(BABY_MIMIC.get(), BabyMimicEntity.createAttributes().build());
        event.put(MINI_DRAGON.get(), MiniDragonEntity.createAttributes().build());
        event.put(KINGDOM_VILLAGER.get(), KingdomVillagerEntity.createAttributes().build());
        event.put(WARDEN_HALRIC.get(), WardenHalricEntity.createAttributes().build());
        event.put(PURPLE_ALLAY.get(), PurpleAllayEntity.createAttributes().build());
        event.put(WILL_O_WISP.get(), WillOWispEntity.createAttributes().build());
        event.put(WILL_O_WISP_2.get(), WillOWispEntity.createAttributes().build());
        event.put(PINK_DEER.get(), PinkDeerEntity.createAttributes().build());
        event.put(RARE_PINK_DEER.get(), PinkDeerEntity.createAttributes().build());
        event.put(MOM_PINK_DEER.get(), MomPinkDeerEntity.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Pink Deer — passive animal, spawns on grass in daylight
        event.register(
            PINK_DEER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            RARE_PINK_DEER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            MOM_PINK_DEER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            ARCANE_MAGE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            FILCHER.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, spawnType, pos, random) -> {
                // Always allow in Ebonwood Hollow (regardless of light)
                if (level.getBiome(pos).is(EBONWOOD_HOLLOW)) {
                    return pos.getY() > level.getSeaLevel();
                }
                return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
            },
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
            MIMIC.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Siren — spawns in water near the surface
        event.register(
            SIREN.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.OCEAN_FLOOR,
            (type, level, spawnType, pos, random) -> {
                // Must be in water and near the surface (within 10 blocks of sea level)
                int seaLevel = level.getSeaLevel();
                return pos.getY() >= seaLevel - 10
                    && pos.getY() <= seaLevel + 2
                    && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
            },
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGIC_MINECART_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ARCANE_MAGE_SPAWN_EGG.get());
            event.accept(FILCHER_SPAWN_EGG.get());
            event.accept(VOID_INVOKER_SPAWN_EGG.get());
            event.accept(NULL_STALKER_SPAWN_EGG.get());
            event.accept(KINGDOM_VILLAGER_SPAWN_EGG.get());
            event.accept(WARDEN_HALRIC_SPAWN_EGG.get());
            event.accept(PURPLE_ALLAY_SPAWN_EGG.get());
            event.accept(WILL_O_WISP_SPAWN_EGG.get());
            event.accept(WILL_O_WISP_2_SPAWN_EGG.get());
            event.accept(PINK_DEER_SPAWN_EGG.get());
            event.accept(RARE_PINK_DEER_SPAWN_EGG.get());
            event.accept(MOM_PINK_DEER_SPAWN_EGG.get());
            event.accept(MIMIC_SPAWN_EGG.get());
            event.accept(BABY_MIMIC_SPAWN_EGG.get());
            event.accept(MINI_DRAGON_SPAWN_EGG.get());
            event.accept(POSSESSED_ARMOR_SPAWN_EGG.get());
            event.accept(SIREN_SPAWN_EGG.get());
            event.accept(SHIPWRECK_MIMIC_SPAWN_EGG.get());
            event.accept(KING_ENDERMAN_SPAWN_EGG.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(TEMPEST_BOW.get());
            event.accept(TEMPEST_ARROW.get());
            event.accept(ANKH_SHIELD.get());
            event.accept(ARCANE_SCEPTER.get());
            event.accept(SOLUNA_STAFF.get());
            event.accept(PITCHFORK.get());
            event.accept(VENGEFUL_HALBERD.get());
            // Accessories
            event.accept(HERMES_BOOTS.get());
            event.accept(BAND_OF_REGENERATION.get());
            event.accept(CLOUD_IN_A_BOTTLE.get());
            event.accept(MIMIC_KEY.get());
            event.accept(SIRENS_RING.get());
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(TANZANITE_ORE_ITEM.get());
            event.accept(EBONY_LOG_ITEM.get());
            event.accept(EBONY_LEAVES_ITEM.get());
            event.accept(BAT_FLOWER_ITEM.get());
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
            event.accept(RAW_TANZANITE.get());
            event.accept(TANZANITE_GEM.get());
            event.accept(STEEL_INGOT.get());
            event.accept(FOOLS_GOLD.get());
            event.accept(FILCHER_CROWN.get());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Kingdom SMP 2.0 server starting.");
    }
}
