package kingdom.smp.accessory;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the {@link AccessoryMenu} {@link MenuType}. */
public final class AccessoryMenuTypes {
    private AccessoryMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Ironhold.MODID);

    @SuppressWarnings("unused")
    public static final DeferredHolder<MenuType<?>, MenuType<AccessoryMenu>> ACCESSORY_MENU =
            MENUS.register("accessory",
                    () -> new MenuType<>(AccessoryMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
