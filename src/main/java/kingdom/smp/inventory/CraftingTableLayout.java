package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom crafting-table screen.
 *
 * <p>Vanilla {@link net.minecraft.world.inventory.CraftingMenu} slot order:
 * <ul>
 *   <li>0     → result</li>
 *   <li>1–9   → 3×3 crafting grid (row-major)</li>
 *   <li>10–36 → 3×9 main inventory</li>
 *   <li>37–45 → hotbar</li>
 * </ul>
 *
 * <p>Drag-tuned in-game with F4 + drag.
 */
public final class CraftingTableLayout {
    private CraftingTableLayout() {}

    // ── Main panel size ──────────────────────────────────────────────────────

    public static final int MAIN_W = 278;
    public static final int MAIN_H = 291;

    // ── Crafting result + 3×3 grid (vanilla 0–9) ─────────────────────────────

    public static final int CRAFT_RESULT_X = 207;
    public static final int CRAFT_RESULT_Y = 73;

    /** Top-left of the 3×3 crafting grid; pitch is uniform 30. */
    public static final int CRAFT_GRID_X0 = 58;
    public static final int CRAFT_GRID_Y0 = 44;
    public static final int CRAFT_GRID_PITCH = 30;

    // ── 3×9 inventory + hotbar (vanilla 10–45) ───────────────────────────────

    /** Per-column x for the 4×9 inventory grid. */
    public static final int[] INV_COL_X =
            {22, 49, 77, 104, 131, 159, 186, 213, 240};
    /** Per-row y for the 3×9 main inventory rows (slots 10–36). */
    public static final int[] INV_ROW_Y = {159, 187, 215};
    /** Hotbar row y (slots 37–45). */
    public static final int HOTBAR_Y = 249;
}
