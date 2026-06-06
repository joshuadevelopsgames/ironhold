package kingdom.smp.client.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Swaps in the custom {@link MagmaBootsModelDef} geometry when the Magma Boots
 * render on a humanoid, via NeoForge's {@link IClientItemExtensions#getHumanoidArmorModel}.
 * Boots render on {@code LayerType.HUMANOID} (the leg parts of the outer model),
 * so we only override that layer and let everything else pass through.
 * See {@link KnightArmorClientExtensions} for the same pattern on helmets.
 */
public final class MagmaBootsClientExtensions {

    private static HumanoidModel<HumanoidRenderState> bootsModel;

    private MagmaBootsClientExtensions() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MagmaBootsClientExtensions::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
                if (layerType == EquipmentClientInfo.LayerType.HUMANOID) {
                    return boots();
                }
                return original;
            }
        }, kingdom.smp.ModItems.MAGMA_BOOTS.get());
    }

    private static HumanoidModel<HumanoidRenderState> boots() {
        if (bootsModel == null) {
            bootsModel = new HumanoidModel<>(MagmaBootsModelDef.createBootsLayer().bakeRoot());
        }
        return bootsModel;
    }
}
