package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom chest screen (3-row chests + barrels).
 *
 * <p>Vanilla {@link net.minecraft.world.inventory.ChestMenu} slot order for a 3-row chest:
 * <ul>
 *   <li>0–26  → 3×9 chest grid (top)</li>
 *   <li>27–53 → 3×9 main inventory</li>
 *   <li>54–62 → hotbar</li>
 * </ul>
 *
 * <p>Initial values are rough estimates — drag-tune in-game with F4.
 */
public final class ChestLayout {
    private ChestLayout() {}

    public static final int MAIN_W = 285;
    public static final int MAIN_H = 295;

    /** Per-column x for the 9-wide grid (used by both chest + inv + hotbar rows). */
    public static final int[] COL_X =
            {22, 49, 76, 104, 131, 158, 186, 213, 240};

    /** Per-row y for the 3-row chest grid (slots 0–26). */
    public static final int[] CHEST_ROW_Y = {30, 52, 74};

    /** Per-row y for the 3×9 main inventory rows (slots 27–53). */
    public static final int[] INV_ROW_Y = {148, 170, 192};

    /** Hotbar row y (slots 54–62). */
    public static final int HOTBAR_Y = 224;
}
