package kingdom.smp.inventory;

/**
 * Gui-relative coordinates for extra slots on the vanilla survival inventory screen
 * ({@code InventoryScreen} / {@code InventoryMenu}). Matches vanilla 176×166 layout:
 * hotbar occupies y 142–159; the accessory row sits below the main panel.
 */
public final class IronholdInventoryLayout {
    private IronholdInventoryLayout() {}

    /**
     * Indices of slots appended after vanilla {@link net.minecraft.world.inventory.InventoryMenu}
     * (vanilla uses 0–45). Keep in sync with {@code InventoryMenuMixin}.
     */
    public static final int ACCESSORY_SLOT_FIRST = 46;
    /** Exclusive end of the 5 accessory slots. */
    public static final int ACCESSORY_SLOT_END = 51;
    /** Exclusive end of the 4 vanity slots (after accessories). */
    public static final int VANITY_SLOT_END = 55;

    /** Five 18px slots centered in the 176px-wide inventory (176 − 90) / 2. */
    public static final int ACCESSORY_SLOT_X0 = 43;
    /** Width of the 5-slot row (5×18). */
    public static final int ACCESSORY_SLOT_ROW_WIDTH = 5 * 18;
    /** Horizontal padding between panel frame and slot row. */
    public static final int ACCESSORY_PANEL_PAD_X = 4;
    /** First accessory slot y (hotbar ends ~160; row sits fully below vanilla texture). */
    public static final int ACCESSORY_SLOT_Y = 182;

    /** Horizontally centered in the 32px-wide vanity island (see {@code InventoryScreenMixin}). */
    public static final int VANITY_SLOT_X = -25;
    public static final int VANITY_SLOT_Y0 = 8;
}
