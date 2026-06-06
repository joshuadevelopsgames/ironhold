package kingdom.smp.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/**
 * A purely cosmetic, vanity-only wearable.
 *
 * <p>Unlike vanilla armor it carries <strong>no</strong>
 * {@link net.minecraft.core.component.DataComponents#EQUIPPABLE} component, so it can
 * never be slotted into a real armor slot, auto-equipped on right-click, or drawn by
 * vanilla's armor/head-item layers. It is accepted only by
 * {@link kingdom.smp.accessory.VanitySlot} for its declared {@link #vanitySlot()}, and its
 * on-body appearance is supplied by a dedicated render layer (e.g.
 * {@code ShroomcapLayer}) — not a vanilla equipment asset.
 */
public class VanityCosmeticItem extends Item {

    private final EquipmentSlot vanitySlot;
    private final boolean overlaysArmor;

    public VanityCosmeticItem(EquipmentSlot vanitySlot, Properties props) {
        this(vanitySlot, false, props);
    }

    public VanityCosmeticItem(EquipmentSlot vanitySlot, boolean overlaysArmor, Properties props) {
        super(props);
        this.vanitySlot = vanitySlot;
        this.overlaysArmor = overlaysArmor;
    }

    /** The vanity slot this cosmetic belongs in (HEAD / CHEST / LEGS / FEET). */
    public EquipmentSlot vanitySlot() {
        return this.vanitySlot;
    }

    /**
     * Whether this cosmetic is an additive overlay drawn <em>on top of</em> the real
     * armor (a halo, wings) rather than a replacement for the armor's look. Additive
     * cosmetics are not substituted into {@code getItemBySlot}; they reach their render
     * layer through {@link kingdom.smp.client.VanityAccessoryRenderState} instead.
     */
    public boolean overlaysArmor() {
        return this.overlaysArmor;
    }
}
