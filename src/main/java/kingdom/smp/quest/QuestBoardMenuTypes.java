package kingdom.smp.quest;

import kingdom.smp.Ironhold;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the {@link QuestBoardMenu} {@link MenuType}. The client factory decodes the real
 * {@link QuestData} the server sent as menu open data, so both sides render the same quest.
 */
public final class QuestBoardMenuTypes {
    private QuestBoardMenuTypes() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Ironhold.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<QuestBoardMenu>> QUEST_BOARD_MENU =
        MENUS.register("quest_board",
            () -> IMenuTypeExtension.create((id, inv, buf) ->
                new QuestBoardMenu(id, inv, QuestData.STREAM_CODEC.decode(buf))));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
