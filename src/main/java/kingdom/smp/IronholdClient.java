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
import kingdom.smp.client.entity.dragon.KingdomDragonRenderer;
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
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
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
        modEventBus.addListener(IronholdClient::registerMenuScreens);
        modEventBus.addListener(IronholdClient::registerKeyMappings);
        // Register IClientItemExtensions per knight helm item so each helmet gets its
        // matching custom HumanoidModel (kettlehat / bascinet / barbute / crusader / armet)
        // via NeoForge's getHumanoidArmorModel hook — same approach as Epic Knights' own
        // NeoForge port, and the call site lives in EquipmentLayerRenderer.renderLayers.
        kingdom.smp.client.entity.KnightArmorClientExtensions.register(modEventBus);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientNeoForgeEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ScepterDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.SneakEyeDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.SneakHoldInputHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.WizardStickDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.HalricStaffDebugCommand.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.QuestBoardSlotDebugCommand.class);
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

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FilcherModel.LAYER_LOCATION, FilcherModel::createBodyLayer);
        event.registerLayerDefinition(MimicModel.LAYER_LOCATION, MimicModel::createBodyLayer);
        event.registerLayerDefinition(BabyMimicModel.LAYER_LOCATION, BabyMimicModel::createBodyLayer);
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
            kingdom.smp.client.entity.KingEndermanModel.LAYER_LOCATION,
            kingdom.smp.client.entity.KingEndermanModel::createBodyLayer);
        // PiglinVillager is now rendered via GeckoLib — no LayerDefinition needed.
    }

    @SuppressWarnings("unchecked")
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Block entity renderers
        event.registerBlockEntityRenderer(
            Ironhold.GUILLOTINE_BLOCK_ENTITY.get(),
            kingdom.smp.client.entity.GuillotineBlockRenderer::new);
        event.registerBlockEntityRenderer(
            Ironhold.WARDHEART_BLOCK_ENTITY.get(),
            kingdom.smp.client.entity.WardheartBlockRenderer::new);
        event.registerBlockEntityRenderer(
            Ironhold.CLASS_STONE_BLOCK_ENTITY.get(),
            kingdom.smp.client.block.ClassStoneRenderer::new);

        // Entity renderers
        event.registerEntityRenderer(
            Ironhold.MAGIC_MINECART_ENTITY.get(),
            ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        event.registerEntityRenderer(Ironhold.ARCANE_WIZARD.get(), IllusionerRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_INVOKER.get(), ArcaneInvokerRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_ORB.get(), ArcaneOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_BOLT.get(), ArcaneBoltRenderer::new);
        event.registerEntityRenderer(Ironhold.HEX_BOLT.get(), HexBoltRenderer::new);
        event.registerEntityRenderer(Ironhold.KING_ENDER_EYE.get(),
            kingdom.smp.client.entity.KingEnderEyeRenderer::new);
        event.registerEntityRenderer(Ironhold.SPELL_BEAM.get(), SpellBeamRenderer::new);
        event.registerEntityRenderer(Ironhold.SOLAR_ORB.get(), SolarOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.LUNAR_ORB.get(), LunarOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.POSSESSED_ARMOR.get(), PossessedArmorRenderer::new);
        event.registerEntityRenderer(Ironhold.KNIGHT_RECRUIT.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_recruit.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createKettlehat()));
        event.registerEntityRenderer(Ironhold.KNIGHT_MAN_AT_ARMS.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_man_at_arms.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createGrandBascinet()));
        event.registerEntityRenderer(Ironhold.KNIGHT_CROSSBOWMAN.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_crossbowman.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createBarbute()));
        event.registerEntityRenderer(Ironhold.KNIGHT_ARMORED.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_armored.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createMaximilianHelmet()));
        event.registerEntityRenderer(Ironhold.KNIGHT_CRUSADER.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_crusader.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createCrusader()));
        event.registerEntityRenderer(Ironhold.KNIGHT_GOTHIC.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_gothic.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createArmet()));
        event.registerEntityRenderer(Ironhold.KNIGHT_GOLD.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_gold.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createArmet()));
        event.registerEntityRenderer(Ironhold.KNIGHT_JOUSTER.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_jouster.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createStechhelm()));
        event.registerEntityRenderer(Ironhold.KNIGHT_VETERAN.get(),
            ctx -> new KnightRenderer(ctx,
                Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/knight_veteran.png"),
                kingdom.smp.client.entity.KnightArmorModelDefs.createSallet()));
        event.registerEntityRenderer(Ironhold.SIREN.get(), SirenRenderer::new);
        event.registerEntityRenderer(Ironhold.SHIPWRECK_MIMIC.get(), ShipwreckMimicRenderer::new);
        event.registerEntityRenderer(Ironhold.TEMPEST_ARROW_ENTITY.get(), TempestArrowRenderer::new);
        event.registerEntityRenderer(Ironhold.THROWN_PITCHFORK.get(), ThrownPitchforkRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_MAGE.get(), ArcaneMageRenderer::new);
        event.registerEntityRenderer(Ironhold.FILCHER.get(), FilcherRenderer::new);
        event.registerEntityRenderer(Ironhold.VOID_INVOKER.get(), VoidInvokerRenderer::new);
        event.registerEntityRenderer(Ironhold.NULL_STALKER.get(), NullStalkerRenderer::new);
        event.registerEntityRenderer(Ironhold.KINGDOM_VILLAGER.get(), ctx -> new KingdomVillagerRenderer(ctx));
        event.registerEntityRenderer(Ironhold.KANGARUDE.get(), KangarudeRenderer::new);
        event.registerEntityRenderer(Ironhold.ENDER_VILLAGER.get(),
            kingdom.smp.client.entity.EnderVillagerRenderer::new);
        event.registerEntityRenderer(Ironhold.SHULKER_HERDER.get(), ShulkerHerderRenderer::new);
        event.registerEntityRenderer(Ironhold.WHITE_SHULKER.get(), WhiteShulkerRenderer::new);
        event.registerEntityRenderer(Ironhold.BLACK_SHULKER.get(), BlackShulkerRenderer::new);
        event.registerEntityRenderer(Ironhold.WARDEN_HALRIC.get(), ctx -> new WardenHalricRenderer(ctx));
        event.registerEntityRenderer(Ironhold.CEMETERY_WATCHER.get(),
            ctx -> new kingdom.smp.client.entity.CemeteryWatcherRenderer(ctx));
        event.registerEntityRenderer(Ironhold.MIRA_INNKEEPER.get(),
            ctx -> new kingdom.smp.client.entity.MiraInnkeeperRenderer(ctx));
        event.registerEntityRenderer(Ironhold.BLACKSMITH_TOBIAS.get(),
            ctx -> new kingdom.smp.client.entity.BlacksmithTobiasRenderer(ctx));
        event.registerEntityRenderer(Ironhold.PRIEST_CEDRIC.get(),
            ctx -> new kingdom.smp.client.entity.PriestCedricRenderer(ctx));
        event.registerEntityRenderer(Ironhold.OLD_HESTA.get(),
            ctx -> new kingdom.smp.client.entity.OldHestaRenderer(ctx));
        event.registerEntityRenderer(Ironhold.OLD_BEREN.get(),
            ctx -> new kingdom.smp.client.entity.OldBerenRenderer(ctx));
        event.registerEntityRenderer(Ironhold.CAPTAIN_ROSELIND.get(),
            ctx -> new kingdom.smp.client.entity.CaptainRoselindRenderer(ctx));
        event.registerEntityRenderer(Ironhold.LOREMASTER_EILAN.get(),
            ctx -> new kingdom.smp.client.entity.LoremasterEilanRenderer(ctx));
        event.registerEntityRenderer(Ironhold.SISTER_WREN.get(),
            ctx -> new kingdom.smp.client.entity.SisterWrenRenderer(ctx));
        event.registerEntityRenderer(Ironhold.BRAM_BARD.get(),
            ctx -> new kingdom.smp.client.entity.BramBardRenderer(ctx));
        event.registerEntityRenderer(Ironhold.PIPPA_URCHIN.get(),
            ctx -> new kingdom.smp.client.entity.PippaUrchinRenderer(ctx));
        event.registerEntityRenderer(Ironhold.PIGLIN_VILLAGER.get(), PiglinVillagerRenderer::new);
        event.registerEntityRenderer(Ironhold.PURPLE_ALLAY.get(), ctx -> new PurpleAllayRenderer(ctx));
        event.registerEntityRenderer(Ironhold.HOPLING.get(), HoplingRenderer::new);
        event.registerEntityRenderer(Ironhold.RAT.get(), RatRenderer::new);
        event.registerEntityRenderer(Ironhold.WILL_O_WISP.get(), ctx -> new WillOWispRenderer(ctx));
        event.registerEntityRenderer(Ironhold.WILL_O_WISP_2.get(), ctx -> new WillOWisp2Renderer(ctx));
        event.registerEntityRenderer(Ironhold.PINK_DEER.get(), PinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.RARE_PINK_DEER.get(), RarePinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.MOM_PINK_DEER.get(), MomPinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(Ironhold.BABY_MIMIC.get(), BabyMimicRenderer::new);
        event.registerEntityRenderer(Ironhold.KINGDOM_DRAGON.get(), KingdomDragonRenderer::new);
        event.registerEntityRenderer(Ironhold.GUILLOTINE_SEAT_ENTITY.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(Ironhold.KING_ENDERMAN.get(),
            kingdom.smp.client.entity.KingEndermanRenderer::new);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(AccessoryMenuTypes.ACCESSORY_MENU.get(), AccessoryScreen::new);
        event.register(kingdom.smp.quest.QuestBoardMenuTypes.QUEST_BOARD_MENU.get(),
            kingdom.smp.client.screen.QuestBoardScreen::new);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(kingdom.smp.client.IronholdKeys.SIREN_LURE);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_1);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_2);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_3);
        event.register(kingdom.smp.client.IronholdKeys.ABILITY_4);
        event.register(kingdom.smp.client.IronholdKeys.KANGARUDE_PTT);
        event.register(kingdom.smp.client.IronholdKeys.SEASHELL_DASH);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Ironhold.LOGGER.info("HELLO FROM CLIENT SETUP");
        Ironhold.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
