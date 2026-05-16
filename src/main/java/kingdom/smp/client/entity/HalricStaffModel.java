package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.item.HalricStaffItem;
import com.geckolib.model.DefaultedItemGeoModel;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib model for Halric's Staff — wires the geo, texture, and idle
 * sway animation. The bone hierarchy is a vertical chain
 * (chain_link_1 → … → chain_link_6) so animations and any future procedural
 * rope physics propagate down the chain naturally.
 */
public class HalricStaffModel extends DefaultedItemGeoModel<HalricStaffItem> {

    public HalricStaffModel() {
        super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "halric_staff"));
    }
}
