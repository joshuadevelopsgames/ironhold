package kingdom.smp.mirrors;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * A standalone wall-hung mirror that places like a painting and renders a live planar reflection.
 * Extracted from the Ironhold mod as its own publishable mod.
 */
@Mod(Mirrors.MODID)
public class Mirrors {
    public static final String MODID = "mirrors";

    public Mirrors(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModRegistry::onBuildCreativeTabs);
    }
}
