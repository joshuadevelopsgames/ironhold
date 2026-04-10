package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BabyMimicMenuTypes {
    private BabyMimicMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Ironhold.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<BabyMimicMenu>> BABY_MIMIC_MENU =
        MENUS.register("baby_mimic",
            () -> new MenuType<>(BabyMimicMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
