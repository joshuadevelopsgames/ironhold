package kingdom.smp.quest;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the {@link QuestBoardMenu} {@link MenuType}. */
public final class QuestBoardMenuTypes {
    private QuestBoardMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Ironhold.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<QuestBoardMenu>> QUEST_BOARD_MENU =
        MENUS.register("quest_board",
            () -> new MenuType<>(QuestBoardMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
