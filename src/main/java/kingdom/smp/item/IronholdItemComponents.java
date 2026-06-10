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

    /** Battle Hammer forge-power charge (crit-combo level + last-crit time). Synced so the
     *  client renders the inner-ring glow stage from it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ForgeCharge>> FORGE_CHARGE =
            COMPONENTS.registerComponentType("forge_charge", b -> b
                    .persistent(ForgeCharge.CODEC)
                    .networkSynchronized(ForgeCharge.STREAM_CODEC));

    /** Battle Hammer trim material key (e.g. "emerald"), applied via smithing. Recolors the
     *  forge glow; synced so the client picks the matching tinted glow texture set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> FORGE_TRIM =
            COMPONENTS.registerComponentType("forge_trim", b -> b
                    .persistent(com.mojang.serialization.Codec.STRING)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8));

    /** Butterfly species ids stored in a placed-then-broken butterfly terrarium (block),
     *  so breaking the jar drops an item that still holds its butterflies. Max 3. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<String>>> BUTTERFLY_JAR_CONTENTS =
            COMPONENTS.registerComponentType("butterfly_jar_contents", b -> b
                    .persistent(com.mojang.serialization.Codec.STRING.listOf())
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8
                            .apply(net.minecraft.network.codec.ByteBufCodecs.list())));

    /** Coin Purse stored balance (loose gold coins banked into the purse item). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COIN_BALANCE =
            COMPONENTS.registerComponentType("coin_balance", b -> b
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
