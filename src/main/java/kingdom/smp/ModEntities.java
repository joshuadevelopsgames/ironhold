package kingdom.smp;

import kingdom.smp.entity.ArcaneBoltEntity;
import kingdom.smp.entity.ArcaneInvokerEntity;
import kingdom.smp.entity.ArcaneOrbEntity;
import kingdom.smp.entity.LunarOrbEntity;
import kingdom.smp.entity.PiglinVillagerEntity;
import kingdom.smp.entity.KnightArmoredEntity;
import kingdom.smp.entity.KnightCrossbowmanEntity;
import kingdom.smp.entity.KnightCrusaderEntity;
import kingdom.smp.entity.KnightGoldEntity;
import kingdom.smp.entity.KnightGothicEntity;
import kingdom.smp.entity.KnightJousterEntity;
import kingdom.smp.entity.KnightManAtArmsEntity;
import kingdom.smp.entity.KnightRecruitEntity;
import kingdom.smp.entity.KnightVeteranEntity;
import kingdom.smp.entity.PossessedArmorEntity;
import kingdom.smp.entity.ShipwreckMimicEntity;
import kingdom.smp.entity.ShulkerHerderEntity;
import kingdom.smp.entity.WhiteShulkerEntity;
import kingdom.smp.entity.BlackShulkerEntity;
import kingdom.smp.entity.SirenEntity;
import kingdom.smp.entity.SolarOrbEntity;
import kingdom.smp.entity.ArcaneWizardEntity;
import kingdom.smp.entity.HexBoltEntity;
import kingdom.smp.entity.HoplingEntity;
import kingdom.smp.entity.MoonHoplingEntity;
import kingdom.smp.entity.ShroomlingEntity;
import kingdom.smp.entity.MoonshroomEntity;
import kingdom.smp.entity.KangarudeEntity;
import kingdom.smp.entity.SpellBeamEntity;
import kingdom.smp.entity.MagicMinecartEntity;
import kingdom.smp.entity.TempestArrowEntity;
import kingdom.smp.entity.StaffZoneEntity;
import kingdom.smp.entity.ArcaneMageEntity;
import kingdom.smp.entity.FilcherEntity;
import kingdom.smp.entity.EnderVillagerEntity;
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
import kingdom.smp.entity.MimicEntity;
import kingdom.smp.entity.NullStalkerEntity;
import kingdom.smp.entity.StoneGolemEntity;
import kingdom.smp.entity.GargoyleEntity;
import kingdom.smp.entity.ButterflyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity-type registrations plus their attribute and spawn-placement setup,
 *  split out of {@link Ironhold}. Spawn-egg items still live in Ironhold and
 *  reference these holders. */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Ironhold.MODID);

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

    public static final DeferredHolder<EntityType<?>, EntityType<SpellBeamEntity>> SPELL_BEAM =
        ENTITY_TYPES.registerEntityType(
            "spell_beam",
            SpellBeamEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.1F, 0.1F).clientTrackingRange(64).updateInterval(20).noSummon());

    public static final DeferredHolder<EntityType<?>, EntityType<StaffZoneEntity>> STAFF_ZONE =
        ENTITY_TYPES.registerEntityType(
            "staff_zone",
            StaffZoneEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.1F, 0.1F).clientTrackingRange(64).updateInterval(2).noSummon());

    // Visual-only terrain chunks flung up by the Battle Hammer ground slam.
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.SlamDebrisEntity>> SLAM_DEBRIS =
        ENTITY_TYPES.registerEntityType(
            "slam_debris",
            kingdom.smp.entity.SlamDebrisEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.85F, 0.85F).clientTrackingRange(8).updateInterval(20).noSummon());

    public static final DeferredHolder<EntityType<?>, EntityType<TempestArrowEntity>> TEMPEST_ARROW_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "tempest_arrow",
            TempestArrowEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.KingEnderEyeEntity>> KING_ENDER_EYE =
        ENTITY_TYPES.registerEntityType(
            "king_ender_eye",
            kingdom.smp.entity.KingEnderEyeEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidInvokerEntity>> VOID_INVOKER =
        ENTITY_TYPES.registerEntityType(
            "void_invoker",
            VoidInvokerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<NullStalkerEntity>> NULL_STALKER =
        ENTITY_TYPES.registerEntityType(
            "null_stalker",
            NullStalkerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 2.9F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<StoneGolemEntity>> STONE_GOLEM =
        ENTITY_TYPES.registerEntityType(
            "stone_golem",
            StoneGolemEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.6F, 3.9F).clientTrackingRange(10).updateInterval(3));

    /** Gargoyle — hostile winged stone flyer; swoops like a Vex but collides with walls. */
    public static final DeferredHolder<EntityType<?>, EntityType<GargoyleEntity>> GARGOYLE =
        ENTITY_TYPES.registerEntityType(
            "gargoyle",
            GargoyleEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.7F, 1.2F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<PinkDeerEntity>> PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "pink_deer",
            PinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.7F, 1.1F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<RarePinkDeerEntity>> RARE_PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "rare_pink_deer",
            RarePinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.7F, 1.1F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<MomPinkDeerEntity>> MOM_PINK_DEER =
        ENTITY_TYPES.registerEntityType(
            "mom_pink_deer",
            MomPinkDeerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.95F, 1.45F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<PurpleAllayEntity>> PURPLE_ALLAY =
        ENTITY_TYPES.registerEntityType(
            "purple_allay",
            PurpleAllayEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<HoplingEntity>> HOPLING =
        ENTITY_TYPES.registerEntityType(
            "hopling",
            HoplingEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.9F, 0.9F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<MoonHoplingEntity>> MOON_HOPLING =
        ENTITY_TYPES.registerEntityType(
            "moon_hopling",
            MoonHoplingEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.9F, 1.1F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<ShroomlingEntity>> SHROOMLING =
        ENTITY_TYPES.registerEntityType(
            "shroomling",
            ShroomlingEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.8F, 0.6F).clientTrackingRange(8).updateInterval(2));

    /** Moonshroom — light-blue lunar mooshroom variant; spawns on moon regolith. */
    public static final DeferredHolder<EntityType<?>, EntityType<MoonshroomEntity>> MOONSHROOM =
        ENTITY_TYPES.registerEntityType(
            "moonshroom",
            MoonshroomEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.9F, 1.4F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.RatEntity>> RAT =
        ENTITY_TYPES.registerEntityType(
            "rat",
            kingdom.smp.entity.RatEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.2F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.VampireBatEntity>> VAMPIRE_BAT =
        ENTITY_TYPES.registerEntityType(
            "vampire_bat",
            kingdom.smp.entity.VampireBatEntity::new,
            MobCategory.AMBIENT,
            b -> b.sized(0.5F, 0.9F).clientTrackingRange(8).updateInterval(2));

    /** Butterfly — fragile ambient flyer; 23 species via variant, captured for fishing bait. */
    public static final DeferredHolder<EntityType<?>, EntityType<ButterflyEntity>> BUTTERFLY =
        ENTITY_TYPES.registerEntityType(
            "butterfly",
            ButterflyEntity::new,
            MobCategory.AMBIENT,
            b -> b.sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<WillOWispEntity>> WILL_O_WISP =
        ENTITY_TYPES.registerEntityType(
            "will_o_wisp",
            WillOWispEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<WillOWisp2Entity>> WILL_O_WISP_2 =
        ENTITY_TYPES.registerEntityType(
            "will_o_wisp_2",
            WillOWisp2Entity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.35F, 0.6F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<KingdomVillagerEntity>> KINGDOM_VILLAGER =
        ENTITY_TYPES.registerEntityType(
            "kingdom_villager",
            KingdomVillagerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Kangarude — humanoid NPC with a player skin, local-LLM brain, and ElevenLabs voice via SVC. */
    public static final DeferredHolder<EntityType<?>, EntityType<KangarudeEntity>> KANGARUDE =
        ENTITY_TYPES.registerEntityType(
            "kangarude",
            KangarudeEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(12).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<EnderVillagerEntity>> ENDER_VILLAGER =
        ENTITY_TYPES.registerEntityType(
            "ender_villager",
            EnderVillagerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<ShulkerHerderEntity>> SHULKER_HERDER =
        ENTITY_TYPES.registerEntityType(
            "shulker_herder",
            ShulkerHerderEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** White Shulker — healer/priest caste; weak attack, heals nearby End mobs. */
    public static final DeferredHolder<EntityType<?>, EntityType<WhiteShulkerEntity>> WHITE_SHULKER =
        ENTITY_TYPES.registerEntityType(
            "white_shulker",
            WhiteShulkerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 1.0F).clientTrackingRange(10));

    /** Black Shulker — void assassin; teleport-strikes and blinds with bullets. */
    public static final DeferredHolder<EntityType<?>, EntityType<BlackShulkerEntity>> BLACK_SHULKER =
        ENTITY_TYPES.registerEntityType(
            "black_shulker",
            BlackShulkerEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 1.0F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<WardenHalricEntity>> WARDEN_HALRIC =
        ENTITY_TYPES.registerEntityType(
            "warden_halric",
            WardenHalricEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** The Tallykeeper — royal records-herald; daily stat-ranking coin reward. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.TallykeeperEntity>> TALLYKEEPER =
        ENTITY_TYPES.registerEntityType(
            "tallykeeper",
            kingdom.smp.entity.TallykeeperEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Vesper — wither-skeleton cemetery watcher. Peaceful, immobile, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.CemeteryWatcherEntity>> CEMETERY_WATCHER =
        ENTITY_TYPES.registerEntityType(
            "cemetery_watcher",
            kingdom.smp.entity.CemeteryWatcherEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.7F, 2.4F).clientTrackingRange(10).updateInterval(3));

    /** Mira — village innkeeper at The Wandering Wolf. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.MiraInnkeeperEntity>> MIRA_INNKEEPER =
        ENTITY_TYPES.registerEntityType(
            "mira_innkeeper",
            kingdom.smp.entity.MiraInnkeeperEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Master Tobias — village blacksmith at The Iron Hearth. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.BlacksmithTobiasEntity>> BLACKSMITH_TOBIAS =
        ENTITY_TYPES.registerEntityType(
            "blacksmith_tobias",
            kingdom.smp.entity.BlacksmithTobiasEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Doctor Corvus — raven-masked plague doctor. Sells buboes, cleanses afflictions for relics. Voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.PlagueDoctorEntity>> PLAGUE_DOCTOR =
        ENTITY_TYPES.registerEntityType(
            "plague_doctor",
            kingdom.smp.entity.PlagueDoctorEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Brother Cedric — village priest at the Chapel of the Old Light. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.PriestCedricEntity>> PRIEST_CEDRIC =
        ENTITY_TYPES.registerEntityType(
            "priest_cedric",
            kingdom.smp.entity.PriestCedricEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Foreman Dunstan — pit-foreman of the Old Shafts, Mining-profession mentor. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.MinerDunstanEntity>> MINER_DUNSTAN =
        ENTITY_TYPES.registerEntityType(
            "miner_dunstan",
            kingdom.smp.entity.MinerDunstanEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Old Hesta — village seer at the Hollow Shrine. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.OldHestaEntity>> OLD_HESTA =
        ENTITY_TYPES.registerEntityType(
            "old_hesta",
            kingdom.smp.entity.OldHestaEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Old Beren — disgraced veteran on the tavern steps. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.OldBerenEntity>> OLD_BEREN =
        ENTITY_TYPES.registerEntityType(
            "old_beren",
            kingdom.smp.entity.OldBerenEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Captain Roselind — village watch captain at the Barracks. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.CaptainRoselindEntity>> CAPTAIN_ROSELIND =
        ENTITY_TYPES.registerEntityType(
            "captain_roselind",
            kingdom.smp.entity.CaptainRoselindEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Kangarude statue — a frozen humanoid carved from rock; pure decoration, no AI. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> KANGARUDE_STATUE =
        ENTITY_TYPES.registerEntityType(
            "kangarude_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Haalina statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> HAALINA_STATUE =
        ENTITY_TYPES.registerEntityType(
            "haalina_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** facelaces statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> FACELACES_STATUE =
        ENTITY_TYPES.registerEntityType(
            "facelaces_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** red raichu statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> RED_RAICHU_STATUE =
        ENTITY_TYPES.registerEntityType(
            "red_raichu_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** twohrd statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> TWOHRD_STATUE =
        ENTITY_TYPES.registerEntityType(
            "twohrd_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** arcatheone statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> ARCATHEONE_STATUE =
        ENTITY_TYPES.registerEntityType(
            "arcatheone_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** cheakie statue — same carved-stone humanoid, different stonified skin. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.StoneStatueEntity>> CHEAKIE_STATUE =
        ENTITY_TYPES.registerEntityType(
            "cheakie_statue",
            kingdom.smp.entity.StoneStatueEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Loremaster Eilan — village scribe at the Library. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.LoremasterEilanEntity>> LOREMASTER_EILAN =
        ENTITY_TYPES.registerEntityType(
            "loremaster_eilan",
            kingdom.smp.entity.LoremasterEilanEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Sister Wren — village apothecary at the Herb Garden. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.SisterWrenEntity>> SISTER_WREN =
        ENTITY_TYPES.registerEntityType(
            "sister_wren",
            kingdom.smp.entity.SisterWrenEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Bram — village bard on The Wandering Wolf's stage. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.BramBardEntity>> BRAM_BARD =
        ENTITY_TYPES.registerEntityType(
            "bram_bard",
            kingdom.smp.entity.BramBardEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    /** Pippa — village street kid (girl, ~10) in the market alleys. Peaceful, voiced. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.PippaUrchinEntity>> PIPPA_URCHIN =
        ENTITY_TYPES.registerEntityType(
            "pippa_urchin",
            kingdom.smp.entity.PippaUrchinEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.4F, 1.0F).clientTrackingRange(10).updateInterval(3));

    /** Piglin-villager crossbreed: passive trader, lives in Overworld/Nether border settlements. */
    public static final DeferredHolder<EntityType<?>, EntityType<PiglinVillagerEntity>> PIGLIN_VILLAGER =
        ENTITY_TYPES.registerEntityType(
            "piglin_villager",
            PiglinVillagerEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<FilcherEntity>> FILCHER =
        ENTITY_TYPES.registerEntityType(
            "filcher",
            FilcherEntity::new,
            MobCategory.MONSTER,
            // 0.49×0.99 = vanilla baby-zombie dimensions; eye at 0.75 (upper head).
            b -> b.sized(0.49F, 0.99F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<PossessedArmorEntity>> POSSESSED_ARMOR =
        ENTITY_TYPES.registerEntityType(
            "possessed_armor",
            PossessedArmorEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    // King Enderman — endgame raid boss, ~2x Iron Golem scale
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.KingEndermanEntity>> KING_ENDERMAN =
        ENTITY_TYPES.registerEntityType(
            "king_enderman",
            kingdom.smp.entity.KingEndermanEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(3.0F, 5.5F).eyeHeight(4.9F).clientTrackingRange(12).updateInterval(3).fireImmune());

    // Siren — ocean mob that lures players with song
    public static final DeferredHolder<EntityType<?>, EntityType<SirenEntity>> SIREN =
        ENTITY_TYPES.registerEntityType(
            "siren",
            SirenEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).updateInterval(3));

    // ── Knight variants (trial chamber medievalization) ────────────────────────
    public static final DeferredHolder<EntityType<?>, EntityType<KnightRecruitEntity>> KNIGHT_RECRUIT =
        ENTITY_TYPES.registerEntityType("knight_recruit", KnightRecruitEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightManAtArmsEntity>> KNIGHT_MAN_AT_ARMS =
        ENTITY_TYPES.registerEntityType("knight_man_at_arms", KnightManAtArmsEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightCrossbowmanEntity>> KNIGHT_CROSSBOWMAN =
        ENTITY_TYPES.registerEntityType("knight_crossbowman", KnightCrossbowmanEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightArmoredEntity>> KNIGHT_ARMORED =
        ENTITY_TYPES.registerEntityType("knight_armored", KnightArmoredEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightCrusaderEntity>> KNIGHT_CRUSADER =
        ENTITY_TYPES.registerEntityType("knight_crusader", KnightCrusaderEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightGothicEntity>> KNIGHT_GOTHIC =
        ENTITY_TYPES.registerEntityType("knight_gothic", KnightGothicEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightGoldEntity>> KNIGHT_GOLD =
        ENTITY_TYPES.registerEntityType("knight_gold", KnightGoldEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightJousterEntity>> KNIGHT_JOUSTER =
        ENTITY_TYPES.registerEntityType("knight_jouster", KnightJousterEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<KnightVeteranEntity>> KNIGHT_VETERAN =
        ENTITY_TYPES.registerEntityType("knight_veteran", KnightVeteranEntity::new, MobCategory.MONSTER,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).updateInterval(3));

    // Shipwreck Mimic — underwater mimic variant
    public static final DeferredHolder<EntityType<?>, EntityType<ShipwreckMimicEntity>> SHIPWRECK_MIMIC =
        ENTITY_TYPES.registerEntityType(
            "shipwreck_mimic",
            ShipwreckMimicEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 0.9375F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<MimicEntity>> MIMIC =
        ENTITY_TYPES.registerEntityType(
            "mimic",
            MimicEntity::new,
            MobCategory.MONSTER,
            b -> b.sized(1.0F, 0.9375F).eyeHeight(0.75F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<BabyMimicEntity>> BABY_MIMIC =
        ENTITY_TYPES.registerEntityType(
            "baby_mimic",
            BabyMimicEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.22F, 0.24F).eyeHeight(0.18F).clientTrackingRange(8).updateInterval(3));

    // Slime Pets — tiny floating player-head companions that bite for half a heart and Slime the target.
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.SlimePetEntity>> SLIME_PET_JE11IE =
        ENTITY_TYPES.registerEntityType(
            "slime_pet_je11ie",
            kingdom.smp.entity.SlimePetEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.45F, 0.55F).eyeHeight(0.4F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.SlimePetEntity>> SLIME_PET_CHEAKIE =
        ENTITY_TYPES.registerEntityType(
            "slime_pet_cheakie",
            kingdom.smp.entity.SlimePetEntity::new,
            MobCategory.CREATURE,
            b -> b.sized(0.45F, 0.55F).eyeHeight(0.4F).clientTrackingRange(8).updateInterval(3));

    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.GuillotineSeatEntity>> GUILLOTINE_SEAT_ENTITY =
        ENTITY_TYPES.registerEntityType(
            "guillotine_seat",
            kingdom.smp.entity.GuillotineSeatEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.01F, 0.01F).clientTrackingRange(3).updateInterval(20)
                  .noSummon());

    // Pitchfork — throwable spear weapon (trident-like)
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.ThrownPitchforkEntity>> THROWN_PITCHFORK =
        ENTITY_TYPES.registerEntityType(
            "thrown_pitchfork",
            kingdom.smp.entity.ThrownPitchforkEntity::new,
            MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(2).noSummon());

    /** Wall-hung mirror — painting-like decoration entity; reflection rendered client-side. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.MirrorEntity>> MIRROR =
        ENTITY_TYPES.registerEntityType(
            "mirror",
            kingdom.smp.entity.MirrorEntity::new,
            MobCategory.MISC,
            b -> b.sized(kingdom.smp.entity.MirrorEntity.DEFAULT_WIDTH, kingdom.smp.entity.MirrorEntity.DEFAULT_HEIGHT)
                  .eyeHeight(0.0F)
                  .clientTrackingRange(10)
                  .updateInterval(Integer.MAX_VALUE));

    /** Glowing-text variant of the mirror; see {@link kingdom.smp.entity.MagicMirrorEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<kingdom.smp.entity.MagicMirrorEntity>> MAGIC_MIRROR =
        ENTITY_TYPES.registerEntityType(
            "magic_mirror",
            kingdom.smp.entity.MagicMirrorEntity::new,
            MobCategory.MISC,
            b -> b.sized(kingdom.smp.entity.MirrorEntity.DEFAULT_WIDTH, kingdom.smp.entity.MirrorEntity.DEFAULT_HEIGHT)
                  .eyeHeight(0.0F)
                  .clientTrackingRange(10)
                  .updateInterval(Integer.MAX_VALUE));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerEntityAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
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
        event.put(STONE_GOLEM.get(), StoneGolemEntity.createAttributes().build());
        event.put(GARGOYLE.get(), GargoyleEntity.createAttributes().build());
        event.put(ARCANE_MAGE.get(), ArcaneMageEntity.createAttributes().build());
        event.put(FILCHER.get(), FilcherEntity.createAttributes().build());
        event.put(POSSESSED_ARMOR.get(), PossessedArmorEntity.createAttributes().build());
        event.put(KNIGHT_RECRUIT.get(), KnightRecruitEntity.createAttributes().build());
        event.put(KNIGHT_MAN_AT_ARMS.get(), KnightManAtArmsEntity.createAttributes().build());
        event.put(KNIGHT_CROSSBOWMAN.get(), KnightCrossbowmanEntity.createAttributes().build());
        event.put(KNIGHT_ARMORED.get(), KnightArmoredEntity.createAttributes().build());
        event.put(KNIGHT_CRUSADER.get(), KnightCrusaderEntity.createAttributes().build());
        event.put(KNIGHT_GOTHIC.get(), KnightGothicEntity.createAttributes().build());
        event.put(KNIGHT_GOLD.get(), KnightGoldEntity.createAttributes().build());
        event.put(KNIGHT_JOUSTER.get(), KnightJousterEntity.createAttributes().build());
        event.put(KNIGHT_VETERAN.get(), KnightVeteranEntity.createAttributes().build());
        event.put(KING_ENDERMAN.get(), kingdom.smp.entity.KingEndermanEntity.createAttributes().build());
        event.put(SIREN.get(), SirenEntity.createAttributes().build());
        event.put(SHIPWRECK_MIMIC.get(), ShipwreckMimicEntity.createAttributes().build());
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
        event.put(BABY_MIMIC.get(), BabyMimicEntity.createAttributes().build());
        event.put(SLIME_PET_JE11IE.get(), kingdom.smp.entity.SlimePetEntity.createAttributes().build());
        event.put(SLIME_PET_CHEAKIE.get(), kingdom.smp.entity.SlimePetEntity.createAttributes().build());
        event.put(KINGDOM_VILLAGER.get(), KingdomVillagerEntity.createAttributes().build());
        event.put(KANGARUDE.get(), KangarudeEntity.createAttributes().build());
        event.put(ENDER_VILLAGER.get(), EnderVillagerEntity.createAttributes().build());
        event.put(SHULKER_HERDER.get(), ShulkerHerderEntity.createAttributes().build());
        event.put(WHITE_SHULKER.get(), WhiteShulkerEntity.createAttributes().build());
        event.put(BLACK_SHULKER.get(), BlackShulkerEntity.createAttributes().build());
        event.put(WARDEN_HALRIC.get(), WardenHalricEntity.createAttributes().build());
        event.put(TALLYKEEPER.get(), kingdom.smp.entity.TallykeeperEntity.createAttributes().build());
        event.put(CEMETERY_WATCHER.get(), kingdom.smp.entity.CemeteryWatcherEntity.createAttributes().build());
        event.put(MIRA_INNKEEPER.get(), kingdom.smp.entity.MiraInnkeeperEntity.createAttributes().build());
        event.put(BLACKSMITH_TOBIAS.get(), kingdom.smp.entity.BlacksmithTobiasEntity.createAttributes().build());
        event.put(PLAGUE_DOCTOR.get(), kingdom.smp.entity.PlagueDoctorEntity.createAttributes().build());
        event.put(PRIEST_CEDRIC.get(), kingdom.smp.entity.PriestCedricEntity.createAttributes().build());
        event.put(MINER_DUNSTAN.get(), kingdom.smp.entity.MinerDunstanEntity.createAttributes().build());
        event.put(OLD_HESTA.get(), kingdom.smp.entity.OldHestaEntity.createAttributes().build());
        event.put(OLD_BEREN.get(), kingdom.smp.entity.OldBerenEntity.createAttributes().build());
        event.put(CAPTAIN_ROSELIND.get(), kingdom.smp.entity.CaptainRoselindEntity.createAttributes().build());
        event.put(KANGARUDE_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(HAALINA_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(FACELACES_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(RED_RAICHU_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(TWOHRD_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(ARCATHEONE_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(CHEAKIE_STATUE.get(), kingdom.smp.entity.StoneStatueEntity.createAttributes().build());
        event.put(LOREMASTER_EILAN.get(), kingdom.smp.entity.LoremasterEilanEntity.createAttributes().build());
        event.put(SISTER_WREN.get(), kingdom.smp.entity.SisterWrenEntity.createAttributes().build());
        event.put(BRAM_BARD.get(), kingdom.smp.entity.BramBardEntity.createAttributes().build());
        event.put(PIPPA_URCHIN.get(), kingdom.smp.entity.PippaUrchinEntity.createAttributes().build());
        event.put(PIGLIN_VILLAGER.get(),
            net.minecraft.world.entity.npc.villager.Villager.createAttributes().build());
        event.put(PURPLE_ALLAY.get(), PurpleAllayEntity.createAttributes().build());
        event.put(WILL_O_WISP.get(), WillOWispEntity.createAttributes().build());
        event.put(WILL_O_WISP_2.get(), WillOWispEntity.createAttributes().build());
        event.put(PINK_DEER.get(), PinkDeerEntity.createAttributes().build());
        event.put(RARE_PINK_DEER.get(), PinkDeerEntity.createAttributes().build());
        event.put(MOM_PINK_DEER.get(), MomPinkDeerEntity.createAttributes().build());
        event.put(HOPLING.get(), HoplingEntity.createAttributes().build());
        event.put(MOON_HOPLING.get(), MoonHoplingEntity.createAttributes().build());
        event.put(SHROOMLING.get(), ShroomlingEntity.createAttributes().build());
        event.put(MOONSHROOM.get(), net.minecraft.world.entity.animal.cow.Cow.createAttributes().build());
        event.put(RAT.get(), kingdom.smp.entity.RatEntity.createAttributes().build());
        event.put(VAMPIRE_BAT.get(), kingdom.smp.entity.VampireBatEntity.createAttributes().build());
        event.put(BUTTERFLY.get(), ButterflyEntity.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Hopling — small End-dimension creature; rare ground spawn on End islands
        event.register(
            HOPLING.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Moon Hopling — rarer celestial cousin; same End-island ground spawn rules
        event.register(
            MOON_HOPLING.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Shroomling — passive cave mushroom; ground spawn at any light level
        event.register(
            SHROOMLING.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ShroomlingEntity::checkShroomlingSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Moonshroom — lunar mooshroom; ground spawn on moon regolith at any light level
        event.register(
            MOONSHROOM.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            MoonshroomEntity::checkMoonshroomSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Rat — vermin, ground spawn at night under standard animal rules
        event.register(
            RAT.get(),
            SpawnPlacementTypes.ON_GROUND,
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            kingdom.smp.entity.RatEntity::checkRatSpawnRules,
            net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Vampire Bat — flying mob in dark caves
        event.register(
            VAMPIRE_BAT.get(),
            SpawnPlacementTypes.NO_RESTRICTIONS,
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            kingdom.smp.entity.VampireBatEntity::checkVampireBatSpawnRules,
            net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Butterfly — surface ambient flyer, daytime light
        event.register(
            BUTTERFLY.get(),
            SpawnPlacementTypes.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ButterflyEntity::checkButterflySpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
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
                if (level.getBiome(pos).is(ModWorldgen.EBONWOOD_HOLLOW)) {
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
}
