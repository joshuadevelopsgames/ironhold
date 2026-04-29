package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.item.WizardStaffItem;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class WizardStaffModel extends DefaultedItemGeoModel<WizardStaffItem> {

    /** Custom ticket injected by WizardStaffRenderer during captureDefaultRenderState. */
    public static final DataTicket<Integer> GEM_INDEX =
        DataTickets.create("wizard_staff_gem_index", Integer.class);

    /** Maps gem index → model file suffix. Index 0 (no gem) uses the base model. */
    public static final Map<Integer, String> GEM_NAMES = Map.ofEntries(
        Map.entry(1,  "diamond"),
        Map.entry(2,  "emerald"),
        Map.entry(3,  "amethyst"),
        Map.entry(4,  "lapis"),
        Map.entry(5,  "quartz"),
        Map.entry(6,  "prismarine"),
        Map.entry(7,  "ender_pearl"),
        Map.entry(8,  "nether_star"),
        Map.entry(9,  "heart_of_the_sea"),
        Map.entry(10, "echo_shard"),
        Map.entry(11, "iron"),
        Map.entry(12, "gold"),
        Map.entry(13, "copper"),
        Map.entry(14, "netherite"),
        Map.entry(15, "blaze_rod"),
        Map.entry(16, "bone")
    );

    public WizardStaffModel() {
        super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "wizard_staff"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        int gemIdx = renderState.getOrDefaultGeckolibData(GEM_INDEX, 0);
        String suffix = GEM_NAMES.get(gemIdx);
        String modelName = (suffix != null) ? "wizard_staff_" + suffix : "wizard_staff";
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, "item/" + modelName);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        int gemIdx = renderState.getOrDefaultGeckolibData(GEM_INDEX, 0);
        String suffix = GEM_NAMES.get(gemIdx);
        String texName = (suffix != null) ? "wizard_staff_" + suffix : "wizard_staff";
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/item/" + texName + ".png");
    }
}
