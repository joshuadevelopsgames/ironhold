package kingdom.smp;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import kingdom.smp.accessory.AccessoryMenuTypes;
import kingdom.smp.game.AccessoryTickHandler;
import kingdom.smp.game.EncumbranceHandler;
import kingdom.smp.game.CloudDoubleJumpHandler;
import kingdom.smp.game.AnkhShieldHandler;
import kingdom.smp.game.ClassXpKillRewards;
import kingdom.smp.game.TanzaniteWorldgenFluidHandler;
import kingdom.smp.net.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Ironhold.MODID)
public class Ironhold {
    public static final String MODID = "ironhold";
    public static final Logger LOGGER = LogUtils.getLogger();


    public Ironhold(IEventBus modEventBus, ModContainer modContainer) {

        // SERVER type: the API keys in Config are never synced to connecting clients.
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, Config.SPEC);

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEffects.register(modEventBus);
        kingdom.smp.enchant.ModEnchantments.register(modEventBus);
        kingdom.smp.moon.ModMoonDimensions.register(modEventBus);

        // Let PlagueEffect resolve its own Holder lazily after registration.
        kingdom.smp.effect.PlagueEffect.setHolderSupplier(() -> ModEffects.PLAGUE_EFFECT);

        ModAttachments.register(modEventBus);
        AccessoryMenuTypes.register(modEventBus);
        kingdom.smp.entity.BabyMimicMenuTypes.register(modEventBus);
        kingdom.smp.quest.QuestBoardMenuTypes.register(modEventBus);
        kingdom.smp.gear.GearComponents.register(modEventBus);
        kingdom.smp.item.IronholdItemComponents.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(IronholdGameEvents.class);
        NeoForge.EVENT_BUS.register(AnkhShieldHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.item.BattleHammerCombatHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.block.wardheart.EndCrystalShieldHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.block.TripwireRackHandler.class);
        NeoForge.EVENT_BUS.register(ClassXpKillRewards.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.effect.PlagueHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.effect.BleedingHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.effect.LunarLevityHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.entity.CatVsRatHandler.class);
        NeoForge.EVENT_BUS.register(AccessoryTickHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.disguise.DisguiseEventHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.disguise.DisguiseSoundHandler.class);
        NeoForge.EVENT_BUS.register(CloudDoubleJumpHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.moon.MoonGravityHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.moon.MoonAnimalConversionHandler.class);
        NeoForge.EVENT_BUS.register(TanzaniteWorldgenFluidHandler.class);
        NeoForge.EVENT_BUS.register(EncumbranceHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.gear.GearTooltipHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.gear.GearAttributeHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.skill.SkillEventHandlers.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.quest.QuestEventHandlers.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.food.CookingInteractionHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.skill.useskill.PickpocketHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.npc.NpcGiftHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.game.MimicChestSpawner.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.skill.useskill.SneakDetectionTracker.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.skill.useskill.VillagerStealthHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.skill.useskill.SneakStealthHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.entity.VillageKnightSpawner.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.mine.MineDropQualityHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.game.FoolsGoldOreHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.game.EnhancedPickaxeSmeltHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.game.LockProtectionHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.trade.GoldCoinTradeHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.rpg.ability.AbilityEffects.class);
        // Seasons: cycle advance + client sync, crop-growth gating, and seasonal snow/melt.
        NeoForge.EVENT_BUS.register(kingdom.smp.seasons.SeasonTickHandler.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.seasons.CropGrowthGate.class);
        NeoForge.EVENT_BUS.register(kingdom.smp.seasons.SeasonWeatherHandler.class);
        // Honeycomb: wax terrain against erosion, and seal armor stands (equip-lock + owner-only invincibility).
        NeoForge.EVENT_BUS.register(kingdom.smp.honeycomb.HoneycombWax.class);

        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(kingdom.smp.seasons.network.SeasonsNetworking::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Kingdom SMP 2.0 common setup complete.");
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Kingdom SMP 2.0 server starting.");
    }
}
