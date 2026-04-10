package kingdom.smp;

import kingdom.smp.accessory.AccessoryMenuTypes;
import kingdom.smp.client.ClientNeoForgeEvents;
import kingdom.smp.client.screen.AccessoryScreen;
import kingdom.smp.client.entity.ArcaneBoltRenderer;
import kingdom.smp.client.entity.ArcaneInvokerRenderer;
import kingdom.smp.client.entity.ArcaneOrbRenderer;
import kingdom.smp.client.entity.LunarOrbRenderer;
import kingdom.smp.client.entity.PossessedArmorRenderer;
import kingdom.smp.client.entity.ShipwreckMimicRenderer;
import kingdom.smp.client.entity.SirenModel;
import kingdom.smp.client.entity.SirenRenderer;
import kingdom.smp.client.entity.SolarOrbRenderer;
import kingdom.smp.client.entity.FilcherKingModel;
import kingdom.smp.client.entity.FilcherModel;
import kingdom.smp.client.entity.FilcherRenderer;
import kingdom.smp.client.entity.HexBoltRenderer;
import kingdom.smp.client.entity.BabyMimicModel;
import kingdom.smp.client.entity.BabyMimicRenderer;
import kingdom.smp.client.entity.MimicModel;
import kingdom.smp.client.entity.MimicRenderer;
import kingdom.smp.client.entity.ArcaneMageRenderer;
import kingdom.smp.client.entity.TempestArrowRenderer;
import kingdom.smp.client.entity.ThrownPitchforkRenderer;
import kingdom.smp.client.entity.VoidInvokerRenderer;
import kingdom.smp.client.entity.KingdomVillagerRenderer;
import kingdom.smp.client.entity.NullStalkerRenderer;
import kingdom.smp.client.entity.PinkDeerModel;
import kingdom.smp.client.entity.MomPinkDeerModel;
import kingdom.smp.client.entity.MomPinkDeerRenderer;
import kingdom.smp.client.entity.PinkDeerRenderer;
import kingdom.smp.client.entity.RarePinkDeerRenderer;
import kingdom.smp.client.entity.PurpleAllayRenderer;
import net.minecraft.client.Minecraft;
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
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.register(ClientNeoForgeEvents.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.client.ScepterDebugCommand.class);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FilcherModel.LAYER_LOCATION, FilcherModel::createBodyLayer);
        event.registerLayerDefinition(FilcherKingModel.LAYER_LOCATION, FilcherKingModel::createBodyLayer);
        event.registerLayerDefinition(MimicModel.LAYER_LOCATION, MimicModel::createBodyLayer);
        event.registerLayerDefinition(BabyMimicModel.LAYER_LOCATION, BabyMimicModel::createBodyLayer);
        event.registerLayerDefinition(PinkDeerModel.LAYER_LOCATION, PinkDeerModel::createBodyLayer);
        event.registerLayerDefinition(PinkDeerModel.BABY_LAYER, PinkDeerModel::createBabyLayer);
        event.registerLayerDefinition(MomPinkDeerModel.LAYER_LOCATION, MomPinkDeerModel::createBodyLayer);
        event.registerLayerDefinition(MomPinkDeerModel.BABY_LAYER, MomPinkDeerModel::createBabyLayer);
        event.registerLayerDefinition(SirenModel.LAYER_LOCATION, SirenModel::createBodyLayer);
    }

    @SuppressWarnings("unchecked")
    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Block entity renderers
        event.registerBlockEntityRenderer(
            Ironhold.GUILLOTINE_BLOCK_ENTITY.get(),
            kingdom.smp.client.entity.GuillotineBlockRenderer::new);

        // Entity renderers
        event.registerEntityRenderer(
            Ironhold.MAGIC_MINECART_ENTITY.get(),
            ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
        event.registerEntityRenderer(Ironhold.ARCANE_WIZARD.get(), IllusionerRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_INVOKER.get(), ArcaneInvokerRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_ORB.get(), ArcaneOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_BOLT.get(), ArcaneBoltRenderer::new);
        event.registerEntityRenderer(Ironhold.HEX_BOLT.get(), HexBoltRenderer::new);
        event.registerEntityRenderer(Ironhold.SOLAR_ORB.get(), SolarOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.LUNAR_ORB.get(), LunarOrbRenderer::new);
        event.registerEntityRenderer(Ironhold.POSSESSED_ARMOR.get(), PossessedArmorRenderer::new);
        event.registerEntityRenderer(Ironhold.SIREN.get(), SirenRenderer::new);
        event.registerEntityRenderer(Ironhold.SHIPWRECK_MIMIC.get(), ShipwreckMimicRenderer::new);
        event.registerEntityRenderer(Ironhold.TEMPEST_ARROW_ENTITY.get(), TempestArrowRenderer::new);
        event.registerEntityRenderer(Ironhold.THROWN_PITCHFORK.get(), ThrownPitchforkRenderer::new);
        event.registerEntityRenderer(Ironhold.ARCANE_MAGE.get(), ArcaneMageRenderer::new);
        event.registerEntityRenderer(Ironhold.FILCHER.get(), FilcherRenderer::new);
        event.registerEntityRenderer(Ironhold.VOID_INVOKER.get(), VoidInvokerRenderer::new);
        event.registerEntityRenderer(Ironhold.NULL_STALKER.get(), NullStalkerRenderer::new);
        event.registerEntityRenderer(Ironhold.KINGDOM_VILLAGER.get(), ctx -> new KingdomVillagerRenderer(ctx));
        event.registerEntityRenderer(Ironhold.PURPLE_ALLAY.get(), ctx -> new PurpleAllayRenderer(ctx));
        event.registerEntityRenderer(Ironhold.PINK_DEER.get(), PinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.RARE_PINK_DEER.get(), RarePinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.MOM_PINK_DEER.get(), MomPinkDeerRenderer::new);
        event.registerEntityRenderer(Ironhold.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(Ironhold.BABY_MIMIC.get(), BabyMimicRenderer::new);
        event.registerEntityRenderer(Ironhold.GUILLOTINE_SEAT_ENTITY.get(),
            net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(AccessoryMenuTypes.ACCESSORY_MENU.get(), AccessoryScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Ironhold.LOGGER.info("HELLO FROM CLIENT SETUP");
        Ironhold.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
