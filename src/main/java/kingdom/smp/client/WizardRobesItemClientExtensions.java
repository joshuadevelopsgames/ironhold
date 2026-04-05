package kingdom.smp.client;

import kingdom.smp.item.wizard.WizardRobes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * {@link net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer} skips a layer when its tint is
 * {@code 0}. For some equipment / dye combinations that resolves to zero and nothing is drawn (looks like
 * missing armor). Force a visible tint for wizard robe items.
 */
public final class WizardRobesItemClientExtensions {
    private WizardRobesItemClientExtensions() {}

    private static final IClientItemExtensions INSTANCE = new IClientItemExtensions() {
        @Override
        public int getArmorLayerTintColor(
            ItemStack stack,
            EquipmentClientInfo.Layer layer,
            int layerIndex,
            int defaultDyeColor
        ) {
            int c = IClientItemExtensions.super.getArmorLayerTintColor(stack, layer, layerIndex, defaultDyeColor);
            return c == 0 ? 0xFFFFFFFF : c;
        }
    };

    public static void register(RegisterClientExtensionsEvent event) {
        for (var def : WizardRobes.ALL_ROBES) {
            event.registerItem(INSTANCE, def.get());
        }
        event.registerItem(INSTANCE, WizardRobes.WIZARDS_HAT.get());
    }
}
