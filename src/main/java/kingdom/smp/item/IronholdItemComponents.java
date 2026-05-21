package kingdom.smp.item;

import kingdom.smp.Ironhold;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IronholdItemComponents {
    private IronholdItemComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Ironhold.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlayerTrackerTarget>> PLAYER_TRACKER =
            COMPONENTS.registerComponentType("player_tracker", b -> b
                    .persistent(PlayerTrackerTarget.CODEC)
                    .networkSynchronized(PlayerTrackerTarget.STREAM_CODEC));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
