package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom ender-chest screen.
 *
 * <p>Vanilla {@link net.minecraft.world.inventory.ChestMenu} (3-row, backed by
 * {@link net.minecraft.world.inventory.PlayerEnderChestContainer}) slot order:
 * <ul>
 *   <li>0–26  → 3×9 ender-chest grid (top)</li>
 *   <li>27–53 → 3×9 main inventory</li>
 *   <li>54–62 → hotbar</li>
 * </ul>
 *
 * <p>Initial values are rough estimates — drag-tune in-game with F4.
 */
public final class EnderChestLayout {
    private EnderChestLayout() {}

    public static final int MAIN_W = 268;
    public static final int MAIN_H = 303;

    /** Per-column x for the 9-wide grid. */
    public static final int[] COL_X =
            {18, 45, 72, 99, 126, 153, 180, 207, 234};

    /** Per-row y for the 3-row ender-chest grid (slots 0–26). */
    public static final int[] CHEST_ROW_Y = {30, 52, 74};

    /** Per-row y for the 3×9 main inventory rows (slots 27–53). */
    public static final int[] INV_ROW_Y = {154, 176, 198};

    /** Hotbar row y (slots 54–62). */
    public static final int HOTBAR_Y = 232;
}
