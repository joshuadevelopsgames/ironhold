package milkucha.trmt;

import milkucha.trmt.network.TRMTPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TRMT — The Roads More Travelled. NeoForge port of milkucha/trmt
 * (CC BY-NC 4.0). See docs/third-party/trmt.md for attribution and the
 * upstream → NeoForge mapping table.
 *
 * <p>Most event handlers live in {@code milkucha.trmt.handler.*} and are
 * auto-registered via {@code @EventBusSubscriber(modid = TRMT.MOD_ID)}.
 * Only mod-bus listeners that need direct access to the {@code IEventBus}
 * (DeferredRegister wiring, payload registration, field resolution) are
 * attached here.
 */
@Mod(TRMT.MOD_ID)
public final class TRMT {
    public static final String MOD_ID = "trmt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TRMT(IEventBus modEventBus, ModContainer modContainer) {
        TRMTConfig.load();

        TRMTBlocks.register(modEventBus);
        TRMTEffects.register(modEventBus);
        TRMTPotions.register(modEventBus);

        modEventBus.addListener(TRMTPayloads::register);

        // Resolve `public static Block ERODED_X` / `LIGHTNESS_ENTRY` /
        // `LIGHTNESS` fields once registries are populated, so legacy
        // upstream code can read them without churning every callsite.
        modEventBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            TRMTBlocks.resolve();
            TRMTEffects.resolve();
            TRMTPotions.resolve();
        }));

        // TODO(port): version-check handshake (RegisterConfigurationTasksEvent)
        //   — upstream sends a configuration-phase packet to the client and
        //     disconnects mismatched versions. Soft feature; the mod works
        //     without it. Port when client side lands.

        LOGGER.info("TRMT NeoForge port: registering ({})", modContainer.getModId());
    }

    /** Convenience for namespaced identifiers within the {@code trmt} mod. */
    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
