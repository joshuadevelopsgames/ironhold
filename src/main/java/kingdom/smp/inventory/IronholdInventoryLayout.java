package kingdom.smp.inventory;

/**
 * Layout for the Ironhold custom inventory screen.
 *
 * <p>All slot positions are in game-pixels relative to the screen's
 * {@code (leftPos, topPos)} origin (the top-left of the main panel texture).
 *
 * <p>Vanilla constructs slots 0–45 at its own coordinates, then
 * {@code InventoryMenuMixin} rewrites their {@code x}/{@code y} to these values.
 * Slots 46–54 (5 accessory + 4 vanity) are appended by the same mixin.
 *
 * <p>Values were tuned in-game with the F4 drag overlay, then dumped via the
 * P key. To re-tune: open inventory, F4, drag elements, P to dump, paste here.
 */
public final class IronholdInventoryLayout {
    private IronholdInventoryLayout() {}

    // ── Main panel size ───────────────────────────────────────────────────────

    public static final int MAIN_W = 226;
    public static final int MAIN_H = 266;

    // ── Crafting (vanilla 0–4) ────────────────────────────────────────────────

    public static final int CRAFT_RESULT_X = 190;
    public static final int CRAFT_RESULT_Y = 58;

    public static final int CRAFT_GRID_X0 = 125;
    public static final int CRAFT_GRID_Y0 = 47;
    public static final int CRAFT_GRID_PITCH = 21;

    // ── Armor (vanilla 5–8) ───────────────────────────────────────────────────

    public static final int ARMOR_X = 20;
    /** Per-slot y values (head, chest, legs, feet) — drag-tuned, slight non-uniform pitch. */
    public static final int[] ARMOR_Y = {34, 54, 77, 97};

    // ── Offhand (vanilla 45) ──────────────────────────────────────────────────

    public static final int OFFHAND_X = 111;
    public static final int OFFHAND_Y = 97;

    // ── 3×9 inventory + hotbar (vanilla 9–44) ─────────────────────────────────

    /** Per-column x for the 4×9 inventory grid (drag-tuned, non-uniform pitch ~21–22). */
    public static final int[] INV_COL_X = {18, 40, 62, 83, 105, 127, 148, 170, 192};
    /** Per-row y for the 3×9 main inventory rows (used for vanilla slots 9–35). */
    public static final int[] INV_ROW_Y = {123, 143, 163};
    /** Hotbar row y (vanilla slots 36–44). */
    public static final int HOTBAR_Y = 188;

    // ── Accessories (mixin-added 46–50) ──────────────────────────────────────

    /** Indices of accessory slots in the menu (after vanilla appends 0–45). */
    public static final int ACCESSORY_SLOT_FIRST = 46;
    /** Exclusive end of the 5 accessory slots. */
    public static final int ACCESSORY_SLOT_END = 51;
    public static final int ACCESSORY_Y = 237;
    public static final int[] ACCESSORY_X = {62, 85, 109, 132, 156};

    // ── Vanity panel (separate texture, left of main, 51–54) ─────────────────

    /** Exclusive end of the 4 vanity slots (after accessories). */
    public static final int VANITY_SLOT_END = 55;
    public static final int VANITY_PANEL_W = 64;
    public static final int VANITY_PANEL_H = 147;
    public static final int VANITY_PANEL_OFFSET_X = -65;
    public static final int VANITY_PANEL_OFFSET_Y = 36;

    /** Vanity slot x within the vanity panel. */
    public static final int VANITY_SLOT_X_LOCAL = 23;
    /** Vanity slot y values within the vanity panel (head, chest, legs, feet). */
    public static final int[] VANITY_SLOT_Y_LOCAL = {33, 58, 83, 108};

    /** Slot.x as the screen sees it: panel offset + local. */
    public static final int VANITY_SLOT_X = VANITY_PANEL_OFFSET_X + VANITY_SLOT_X_LOCAL;

    // ── Skills panel (separate texture, right of main) ────────────────────────

    public static final int SKILLS_PANEL_W = 79;
    public static final int SKILLS_PANEL_H = 42;
    public static final int SKILLS_PANEL_OFFSET_X = 229;
    public static final int SKILLS_PANEL_OFFSET_Y = 178;

    // ── Paperdoll (player render in central cutout) ──────────────────────────

    public static final int PAPERDOLL_X0 = 45;
    public static final int PAPERDOLL_Y0 = 34;
    public static final int PAPERDOLL_X1 = 101;
    public static final int PAPERDOLL_Y1 = 112;
    public static final int PAPERDOLL_SIZE = 32;
}
