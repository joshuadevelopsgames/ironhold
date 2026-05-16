package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom large chest screen (6-row, double chest).
 *
 * <p>Vanilla {@link net.minecraft.world.inventory.ChestMenu} slot order for a 6-row chest:
 * <ul>
 *   <li>0–53  → 6×9 chest grid (top)</li>
 *   <li>54–80 → 3×9 main inventory</li>
 *   <li>81–89 → hotbar</li>
 * </ul>
 *
 * <p>Initial values are rough — drag-tune in-game with F4.
 */
public final class LargeChestLayout {
    private LargeChestLayout() {}

    public static final int MAIN_W = 270;
    public static final int MAIN_H = 322;

    /** Per-column x for the 9-wide grid (used by chest + inv + hotbar rows). */
    public static final int[] COL_X =
            {18, 45, 72, 99, 126, 153, 180, 207, 234};

    /** Per-row y for the 6-row chest grid (slots 0–53). */
    public static final int[] CHEST_ROW_Y = {28, 50, 72, 94, 116, 138};

    /** Per-row y for the 3×9 main inventory rows (slots 54–80). */
    public static final int[] INV_ROW_Y = {180, 202, 224};

    /** Hotbar row y (slots 81–89). */
    public static final int HOTBAR_Y = 256;
}
