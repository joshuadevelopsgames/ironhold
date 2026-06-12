package kingdom.smp;

import kingdom.smp.accessory.AccessoryMenuTypes;
import kingdom.smp.client.ClientNeoForgeEvents;
import kingdom.smp.client.screen.AccessoryScreen;
import kingdom.smp.client.entity.ArcaneBoltRenderer;
import kingdom.smp.client.entity.ArcaneInvokerRenderer;
import kingdom.smp.client.entity.ArcaneOrbRenderer;
import kingdom.smp.client.entity.LunarOrbRenderer;
import kingdom.smp.client.entity.PiglinVillagerRenderer;
import kingdom.smp.client.entity.KnightRenderer;
import kingdom.smp.client.entity.PossessedArmorRenderer;
import kingdom.smp.client.entity.ShipwreckMimicRenderer;
import kingdom.smp.client.entity.ShipwreckMimicModel;
import kingdom.smp.client.entity.ShulkerHerderModel;
import kingdom.smp.client.entity.ShulkerHerderRenderer;
import kingdom.smp.client.entity.WhiteShulkerRenderer;
import kingdom.smp.client.entity.BlackShulkerRenderer;
import kingdom.smp.client.entity.SirenModel;
import kingdom.smp.client.entity.SirenRenderer;
import kingdom.smp.client.entity.SolarOrbRenderer;
import kingdom.smp.client.entity.FilcherModel;
import kingdom.smp.client.entity.FilcherRenderer;
import kingdom.smp.client.entity.HexBoltRenderer;
import kingdom.smp.client.entity.HoplingModel;
import kingdom.smp.client.entity.HoplingRenderer;
import kingdom.smp.client.entity.RatRenderer;
import kingdom.smp.client.entity.SpellBeamRenderer;
import kingdom.smp.client.entity.BabyMimicModel;
import kingdom.smp.client.entity.BabyMimicRenderer;
import kingdom.smp.client.entity.MimicModel;
import kingdom.smp.client.entity.MimicRenderer;
import kingdom.smp.client.entity.ArcaneMageRenderer;
import kingdom.smp.client.entity.TempestArrowRenderer;
import kingdom.smp.client.entity.ThrownPitchforkRenderer;
import kingdom.smp.client.entity.VoidInvokerRenderer;
import kingdom.smp.client.entity.KangarudeRenderer;
import kingdom.smp.client.entity.KingdomVillagerRenderer;
import kingdom.smp.client.entity.WardenHalricRenderer;
import kingdom.smp.client.entity.NullStalkerRenderer;
import kingdom.smp.client.entity.PinkDeerModel;
import kingdom.smp.client.entity.MomPinkDeerModel;
import kingdom.smp.client.entity.MomPinkDeerRenderer;
import kingdom.smp.client.entity.PinkDeerRenderer;
import kingdom.smp.client.entity.RarePinkDeerRenderer;
import kingdom.smp.client.entity.PurpleAllayRenderer;
import kingdom.smp.client.entity.WillOWispRenderer;
import kingdom.smp.client.entity.WillOWisp2Model;
import kingdom.smp.client.entity.WillOWisp2Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.IllusionerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Ironhold.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Ironhold.MODID, value = Dist.CLIENT)
public class IronholdClient {
    public IronholdClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(IronholdClient::registerLayerDefinitions);
        modEventBus.addListener(IronholdClient::registerEntityRenderers);
        modEventBus.addListener(IronholdClient::onAddPlayerLayers);
        modEventBus.addListener(IronholdClient::registerMenuScreens);
        modEventBus.addListener(IronholdClient::registerKeyMappings);
        modEventBus.addListener(IronholdClient::registerRangeSelectItemModelProperties);
        modEventBus.addListener(IronholdClient::registerParticleProviders);
        // Register IClientItemExtensions per knight helm item so each helmet gets its
        // matching custom HumanoidModel (kettlehat / bascinet / barbute / crusader / armet)
        // via NeoForge's getHumanoidArmorModel hook — same approach as Epic Knights' own
        // NeoForge port, and the call site lives in EquipmentLayerRenderer.renderLayers.
        kingdom.smp.client.entity.KnightArmorClientExtensions.register(modEventBus);
        // Magma Boots: custom 3D worn model (obsidian shaft + lava sole) swapped in
        // for the FEET/HUMANOID armor layer via the same getHumanoidArmorModel hook.
        kingdom.smp.client.entity.MagmaBootsClientExtensions.register(modEventBus);
        // Dyed water: per-colour fluid models (vanilla water sprites + constant tint) and the
        // colour-reading block tint for the dyed cauldron.
        kingdom.smp.dyewater.DyedWaterClientEvents.register(modEventBus);
        // Battle Hammer: custom third-person charge arm pose (arm cocks up/back as the
        // hammer winds up). Registers an IClientItemExtensions returning the enum-extended
        // ArmPose while the hammer is in use.
        kingdom.smp.client.entity.BattleHammerArmPose.register(modEventBus);
        // Built-in dynamic lights (held light-source items, registered wands, glowing mobs).
        // Reload-listener registration is a mod-bus event; tick/logout are on the game bus.
        modEventBus.addListener(kingdom.smp.dynlight.DynamicLightsClientEvents::onAddClientReloadListeners);
        // Seasons: client-side color resolver swap + sub-season change rebuild. Static-field swap
        // is installed at FML client setup; per-tick state advance and sync are on the game bus.
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) ->
            e.enqueueWork(kingdom.smp.seasons.client.SeasonColorHandlers::install));
        NeoForge.EVENT_BUS.register(kingdom.smp.seasons.client.SeasonColorHandlers.class);
        // Stardew-style crop season tooltips (which season(s) a seed grows in + live in/out indicator).
        NeoForge.EVENT_BUS.register(kingdom.smp.seasons.client.CropSeasonTooltipHandler.class);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientNeoForgeEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.CoinPurseHoldHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.DisguiseClient.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.entity.DiamondScepterFieldWorldRenderer.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.WaxOverlayRenderer.class);
        // Reactive music: per-tick trigger evaluation + SelectMusicEvent override + PvP escalation.
        NeoForge.EVENT_BUS.register(kingdom.smp.music.ReactiveMusicHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.dynlight.DynamicLightsClientEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ScepterDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.BattleHammerDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ClubDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ButterflyDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ButterflyJarDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ForgeButtonCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ForgeButtonDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.SneakEyeDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.SneakHoldInputHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.MirrorReflectionEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.portal.client.PortalClientEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.portal.client.PortalRenderEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.MirrorCamDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.WizardStickDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.HalricStaffDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.QuestBoardSlotDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.StoneGolemHammerDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.TripwireRackDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.IronholdSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.CraftingSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.FurnaceSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ChestSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.LargeChestSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.EnderChestSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.AnvilSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.BeaconSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.BlastFurnaceSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.BrewingStandSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.CrafterSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.DispenserSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.GrindstoneSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.HopperSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ShulkerSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.SmithingSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.StonecutterSlotDebugHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.VillagerSlotDebugHandler.class);
        // Live transform tuning for ALL display contexts is handled by
        // ItemModelResolverMixin (overrides itemTransform per-render). The old
        // RenderHandEvent override is gone now — the mixin covers first-person too.
    }

    /** Adds player-body layers (Diamond Scepter charge field, worn Shroomcap) to every player skin renderer. */
    private static void onAddPlayerLayers(EntityRenderersEvent.AddLayers event) {
        // Bake the cosmetic meshes once and share the baked roots across every skin's layer.
        net.minecraft.client.model.geom.ModelPart shroomcapRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.ShroomcapLayer.LAYER_LOCATION);
        net.minecraft.client.model.geom.ModelPart catEarsRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.CatEarsLayer.LAYER_LOCATION);
        net.minecraft.client.model.geom.ModelPart haloRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.HaloLayer.LAYER_LOCATION);
        net.minecraft.client.model.geom.ModelPart angelWingsRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.AngelWingsLayer.LAYER_LOCATION);
        net.minecraft.client.model.geom.ModelPart headAppendagesRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.HeadAppendageLayer.LAYER_LOCATION);
        net.minecraft.client.model.geom.ModelPart backCosmeticsRoot =
            event.getEntityModels().bakeLayer(kingdom.smp.client.entity.BackCosmeticLayer.LAYER_LOCATION);
        for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new kingdom.smp.client.entity.DiamondScepterFieldLayer(renderer));
                renderer.addLayer(new kingdom.smp.client.entity.ShroomcapLayer(renderer, shroomcapRoot));
                renderer.addLayer(new kingdom.smp.client.entity.CatEarsLayer(renderer, catEarsRoot));
                renderer.addLayer(new kingdom.smp.client.entity.HaloLayer(renderer, haloRoot));
                renderer.addLayer(new kingdom.smp.client.entity.AngelWingsLayer(renderer, angelWingsRoot));
                renderer.addLayer(new kingdom.smp.client.entity.HeadAppendageLayer(renderer, headAppendagesRoot));
                renderer.addLayer(new kingdom.smp.client.entity.BackCosmeticLayer(renderer, backCosmeticsRoot));
            }
        }
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FilcherModel.LAYER_LOCATION, FilcherModel::createBodyLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.PlagueDoctorModel.LAYER_LOCATION,
            kingdom.smp.client.entity.PlagueDoctorModel::createBodyLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.MinerDunstanModel.LAYER_LOCATION,
            kingdom.smp.client.entity.MinerDunstanModel::createBodyLayer);
        event.registerLayerDefinition(MimicModel.LAYER_LOCATION, MimicModel::createBodyLayer);
        event.registerLayerDefinition(BabyMimicModel.LAYER_LOCATION, BabyMimicModel::createBodyLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.SlimePetModel.LAYER_LOCATION,
            kingdom.smp.client.entity.SlimePetModel::createBodyLayer);
        event.registerLayerDefinition(PinkDeerModel.LAYER_LOCATION, PinkDeerModel::createBodyLayer);
        event.registerLayerDefinition(PinkDeerModel.BABY_LAYER, PinkDeerModel::createBabyLayer);
        event.registerLayerDefinition(MomPinkDeerModel.LAYER_LOCATION, MomPinkDeerModel::createBodyLayer);
        event.registerLayerDefinition(MomPinkDeerModel.BABY_LAYER, MomPinkDeerModel::createBabyLayer);
        event.registerLayerDefinition(SirenModel.LAYER_LOCATION, SirenModel::createBodyLayer);
        event.registerLayerDefinition(ShulkerHerderModel.LAYER_LOCATION, ShulkerHerderModel::createBodyLayer);
        event.registerLayerDefinition(ShipwreckMimicModel.LAYER_LOCATION, ShipwreckMimicModel::createBodyLayer);
        event.registerLayerDefinition(WillOWisp2Model.LAYER_LOCATION, WillOWisp2Model::createBodyLayer);
        event.registerLayerDefinition(HoplingModel.LAYER_LOCATION, HoplingModel::createBodyLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.ShroomlingModel.LAYER_LOCATION,
            kingdom.smp.client.entity.ShroomlingModel::createBodyLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.ShroomcapLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.ShroomcapLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.CatEarsLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.CatEarsLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.HaloLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.HaloLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.AngelWingsLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.AngelWingsLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.HeadAppendageLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.HeadAppendageLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.StatueBaseLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.StatueBaseLayer::createLayer);
        event.registerLayerDefinition(
            kingdom.smp.client.entity.BackCosmeticLayer.LAYER_LOCATION,
            kingdom.smp.client.entity.BackCosmeticLayer::createLayer);
        // MoonHopling is now rendered via GeckoLib — no LayerDefinition needed.
        event.registerLayerDefinition(
            kingdom.smp.client.entity.KingEndermanModel.LAYER_LOCATION,
            kingdom.smp.client.entity.KingEndermanModel::createBodyLayer);
        // PiglinVillager is now rendered via GeckoLib — no LayerDefinition needed.
    }

    @SuppressWarnings("unchecked")
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Block entity renderers
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.GUILLOTINE_BLOCK_ENTITY.get(),
            kingdom.smp.client.entity.GuillotineBlockRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.WARDHEART_BLOCK_ENTITY.get(),
            kingdom.smp.client.entity.WardheartBlockRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.CLASS_STONE_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.ClassStoneRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.TRIPWIRE_RACK_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.TripwireRackRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.BUTTERFLY_TERRARIUM_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.ButterflyJarRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.CHALICE_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.ChaliceRenderer::new);
        event.registerBlockEntityRenderer(
            kingdom.smp.ModBlocks.STATUE_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.StatueBlockRenderer::new);

        // Entity renderers
        event.registerEntityRenderer(
            kingdom.smp.ModEntities.BUTTERFLY.get(),
            kingdom.smp.client.entity.ButterflyRenderer::new);
        event.registerEntityRenderer(
            kingdom.smp.ModEntities.MIRROR.get(),
            kingdom.smp.client.entity.MirrorRenderer::new);
        event.registerEntityRenderer(
            kingdom.smp.ModEntities.MAGIC_MIRROR.get(),
            kingdom.smp.client.entity.MagicMirrorRenderer::new);
        event.registerEntityRenderer(
            kingdom.smp.ModEntities.MAGIC_MINECART_ENTITY.get(),
            kingdom.smp.client.entity.MagicMinecartRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCANE_WIZARD.get(), IllusionerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCANE_INVOKER.get(), ArcaneInvokerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCANE_ORB.get(), ArcaneOrbRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCANE_BOLT.get(), ArcaneBoltRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.HEX_BOLT.get(), HexBoltRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.KING_ENDER_EYE.get(),
            kingdom.smp.client.entity.KingEnderEyeRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SPELL_BEAM.get(), SpellBeamRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SLAM_DEBRIS.get(),
            kingdom.smp.client.entity.SlamDebrisRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SOLAR_ORB.get(), SolarOrbRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.LUNAR_ORB.get(), LunarOrbRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.POSSESSED_ARMOR.get(), PossessedArmorRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_RECRUIT.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_recruit.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createKettlehat()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_MAN_AT_ARMS.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_man_at_arms.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createGrandBascinet()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_CROSSBOWMAN.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_crossbowman.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createBarbute()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_ARMORED.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_armored.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createMaximilianHelmet()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_CRUSADER.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_crusader.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createCrusader()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_GOTHIC.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_gothic.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createArmet()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_GOLD.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_gold.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createArmet()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_JOUSTER.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_jouster.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createStechhelm()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KNIGHT_VETERAN.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_veteran.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createSallet()));
        event.registerEntityRenderer(kingdom.smp.ModEntities.SIREN.get(), SirenRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SHIPWRECK_MIMIC.get(), ShipwreckMimicRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.TEMPEST_ARROW_ENTITY.get(), TempestArrowRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.THROWN_PITCHFORK.get(), ThrownPitchforkRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCANE_MAGE.get(), ArcaneMageRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.FILCHER.get(), FilcherRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.VOID_INVOKER.get(), VoidInvokerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.NULL_STALKER.get(), NullStalkerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.KINGDOM_VILLAGER.get(), ctx -> new KingdomVillagerRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KANGARUDE.get(), KangarudeRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.ENDER_VILLAGER.get(),
            kingdom.smp.client.entity.EnderVillagerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SHULKER_HERDER.get(), ShulkerHerderRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.WHITE_SHULKER.get(), WhiteShulkerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.BLACK_SHULKER.get(), BlackShulkerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.WARDEN_HALRIC.get(), ctx -> new WardenHalricRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.TALLYKEEPER.get(),
            ctx -> new kingdom.smp.client.entity.TallykeeperRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.CEMETERY_WATCHER.get(),
            ctx -> new kingdom.smp.client.entity.CemeteryWatcherRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.MIRA_INNKEEPER.get(),
            ctx -> new kingdom.smp.client.entity.MiraInnkeeperRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.BLACKSMITH_TOBIAS.get(),
            ctx -> new kingdom.smp.client.entity.BlacksmithTobiasRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.PLAGUE_DOCTOR.get(),
            ctx -> new kingdom.smp.client.entity.PlagueDoctorRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.PRIEST_CEDRIC.get(),
            ctx -> new kingdom.smp.client.entity.PriestCedricRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.MINER_DUNSTAN.get(),
            ctx -> new kingdom.smp.client.entity.MinerDunstanRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.OLD_HESTA.get(),
            ctx -> new kingdom.smp.client.entity.OldHestaRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.OLD_BEREN.get(),
            ctx -> new kingdom.smp.client.entity.OldBerenRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.CAPTAIN_ROSELIND.get(),
            ctx -> new kingdom.smp.client.entity.CaptainRoselindRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.KANGARUDE_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.KANGARUDE_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.HAALINA_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.HAALINA_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.FACELACES_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.FACELACES_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.RED_RAICHU_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.RED_RAICHU_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.TWOHRD_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.TWOHRD_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.ARCATHEONE_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.ARCATHEONE_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.CHEAKIE_STATUE.get(),
            ctx -> new kingdom.smp.client.entity.StoneStatueRenderer(ctx,
                kingdom.smp.client.entity.StoneStatueRenderer.CHEAKIE_TEXTURE));
        event.registerEntityRenderer(kingdom.smp.ModEntities.LOREMASTER_EILAN.get(),
            ctx -> new kingdom.smp.client.entity.LoremasterEilanRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.SISTER_WREN.get(),
            ctx -> new kingdom.smp.client.entity.SisterWrenRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.BRAM_BARD.get(),
            ctx -> new kingdom.smp.client.entity.BramBardRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.PIPPA_URCHIN.get(),
            ctx -> new kingdom.smp.client.entity.PippaUrchinRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.PIGLIN_VILLAGER.get(), PiglinVillagerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.STONE_GOLEM.get(),
            kingdom.smp.client.entity.StoneGolemRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.GARGOYLE.get(),
            kingdom.smp.client.entity.GargoyleRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.PURPLE_ALLAY.get(), ctx -> new PurpleAllayRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.HOPLING.get(), HoplingRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SHROOMLING.get(),
            kingdom.smp.client.entity.ShroomlingRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.MOON_HOPLING.get(),
            kingdom.smp.client.entity.MoonHoplingRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.MOONSHROOM.get(),
            kingdom.smp.client.entity.MoonshroomRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.RAT.get(), RatRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.VAMPIRE_BAT.get(), kingdom.smp.client.entity.VampireBatRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.WILL_O_WISP.get(), ctx -> new WillOWispRenderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.WILL_O_WISP_2.get(), ctx -> new WillOWisp2Renderer(ctx));
        event.registerEntityRenderer(kingdom.smp.ModEntities.PINK_DEER.get(), PinkDeerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.RARE_PINK_DEER.get(), RarePinkDeerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.MOM_PINK_DEER.get(), MomPinkDeerRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.BABY_MIMIC.get(), BabyMimicRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SLIME_PET_JE11IE.get(),
            kingdom.smp.client.entity.SlimePetRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.SLIME_PET_CHEAKIE.get(),
            kingdom.smp.client.entity.SlimePetRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.GUILLOTINE_SEAT_ENTITY.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.STAFF_ZONE.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(kingdom.smp.ModEntities.KING_ENDERMAN.get(),
            kingdom.smp.client.entity.KingEndermanRenderer::new);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(AccessoryMenuTypes.ACCESSORY_MENU.get(), AccessoryScreen::new);
        event.register(kingdom.smp.quest.QuestBoardMenuTypes.QUEST_BOARD_MENU.get(),
            kingdom.smp.client.screen.QuestBoardScreen::new);
    }

    private static void registerRangeSelectItemModelProperties(
            net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "player_compass_angle"),
            kingdom.smp.client.item.PlayerCompassAngle.MAP_CODEC);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.IRON_SPARK.get(),
            kingdom.smp.client.particle.IronSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.IRON_SPARK_FLARE.get(),
            kingdom.smp.client.particle.IronSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.IRON_SPARK_PULSE.get(),
            kingdom.smp.client.particle.IronSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PLAGUE_FLEA.get(),
            kingdom.smp.client.particle.PlagueParticle.FleaProvider::new);
        event.registerSpriteSet(ModParticles.PLAGUE_SPORE.get(),
            kingdom.smp.client.particle.PlagueParticle.SporeProvider::new);
        event.registerSpriteSet(ModParticles.DIAMOND_SCEPTER_SPARK.get(),
            kingdom.smp.client.particle.DiamondScepterParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SHROOMLING_SPORE.get(),
            kingdom.smp.client.particle.ShroomlingSporeParticle.BlueProvider::new);
        event.registerSpriteSet(ModParticles.ORANGE_SHROOMLING_SPORE.get(),
            kingdom.smp.client.particle.ShroomlingSporeParticle.OrangeProvider::new);
        event.registerSpriteSet(ModParticles.LUNAR_LEVITY_MOTE.get(),
            kingdom.smp.client.particle.LunarLevityParticle.Provider::new);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(kingdom.smp.client.IronholdKeys.SIREN_LURE);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_1);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_2);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_3);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_4);
        event.register(kingdom.smp.client.IronholdKeys.KANGARUDE_PTT);
        event.register(kingdom.smp.client.IronholdKeys.SEASHELL_DASH);
        event.register(kingdom.smp.client.IronholdKeys.EMOTE_POINT);
        event.register(kingdom.smp.client.IronholdKeys.PARRY);
        event.register(kingdom.smp.client.IronholdKeys.DODGE);
        event.register(kingdom.smp.client.IronholdKeys.ACCESSORY_ACTIVE);
        event.register(kingdom.smp.client.IronholdKeys.GOLEM_HAMMER_CYCLE);
        event.register(kingdom.smp.client.IronholdKeys.GOLEM_HAMMER_DEC);
        event.register(kingdom.smp.client.IronholdKeys.GOLEM_HAMMER_INC);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Ironhold.LOGGER.info("HELLO FROM CLIENT SETUP");
        Ironhold.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
