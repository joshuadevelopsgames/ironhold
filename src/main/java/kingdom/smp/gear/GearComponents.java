package kingdom.smp.gear;

import com.mojang.serialization.Codec;
import kingdom.smp.Ironhold;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers and accesses the per-item DataComponents used by the gear quality system:
 * quality (Standard/Fine/Mint) and the Pristine refinement flag.
 *
 * Items without these components fall back to Fine quality and not-Pristine,
 * so unmodded vanilla items behave as Fine baseline without explicit migration.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md</a>
 */
public final class GearComponents {
    private GearComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Ironhold.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemQuality>> QUALITY =
            COMPONENTS.registerComponentType("quality", builder -> builder
                    .persistent(ItemQuality.CODEC)
                    .networkSynchronized(ItemQuality.STREAM_CODEC));

    /**
     * Pristine flag — boolean component. Present-and-true means the item has been refined
     * to Pristine. Absent or false means Worn/Damaged/etc. by durability.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PRISTINE =
            COMPONENTS.registerComponentType("pristine", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static ItemQuality getQuality(ItemStack stack) {
        return stack.getOrDefault(QUALITY.get(), ItemQuality.defaultQuality());
    }

    public static void setQuality(ItemStack stack, ItemQuality quality) {
        if (quality == ItemQuality.defaultQuality()) {
            stack.remove(QUALITY.get());
        } else {
            stack.set(QUALITY.get(), quality);
        }
    }

    public static boolean isPristine(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(PRISTINE.get()));
    }

    public static void setPristine(ItemStack stack, boolean pristine) {
        if (pristine) {
            stack.set(PRISTINE.get(), Boolean.TRUE);
        } else {
            stack.remove(PRISTINE.get());
        }
    }
}
