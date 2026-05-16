package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom furnace screen.
 *
 * <p>Vanilla {@link net.minecraft.world.inventory.FurnaceMenu} slot order:
 * <ul>
 *   <li>0     → input (top)</li>
 *   <li>1     → fuel (bottom, with painted flames above)</li>
 *   <li>2     → result (right of arrow)</li>
 *   <li>3–29  → 3×9 main inventory</li>
 *   <li>30–38 → hotbar</li>
 * </ul>
 *
 * <p>Drag-tuned in-game with F4 + drag.
 */
public final class FurnaceLayout {
    private FurnaceLayout() {}

    public static final int MAIN_W = 268;
    public static final int MAIN_H = 310;

    // ── Furnace slots (0–2) ──────────────────────────────────────────────────

    public static final int INPUT_X  = 90;
    public static final int INPUT_Y  = 48;
    public static final int FUEL_X   = 90;
    public static final int FUEL_Y   = 118;
    public static final int RESULT_X = 198;
    public static final int RESULT_Y = 82;

    // ── Burn-progress arrow (vanilla animated sprite over painted arrow) ─────
    /** Top-left of the painted arrow in the texture. Vanilla burnProgressSprite is 24×16. */
    public static final int ARROW_X = 130;
    public static final int ARROW_Y = 82;
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 16;

    // ── 3×9 inventory + hotbar (3–38) ────────────────────────────────────────

    /** Per-column x for the 4×9 inventory grid. */
    public static final int[] INV_COL_X =
            {22, 49, 74, 100, 125, 151, 177, 203, 228};
    /** Per-row y for the 3×9 main inventory rows (slots 3–29). */
    public static final int[] INV_ROW_Y = {177, 202, 227};
    /** Hotbar row y (slots 30–38). */
    public static final int HOTBAR_Y = 262;
}
