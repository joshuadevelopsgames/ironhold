package kingdom.smp.client;

import kingdom.smp.inventory.CraftingTableLayout;

/**
 * Live-tunable layout for the Ironhold crafting-table screen. Mirrors
 * {@link IronholdSlotDebug} but with 46 slots and no paperdoll/vanity/skills.
 *
 * <p>Slot index convention (matches {@link net.minecraft.world.inventory.CraftingMenu}):
 * <ul>
 *   <li>0     → result</li>
 *   <li>1–9   → 3×3 crafting grid (row-major)</li>
 *   <li>10–36 → 3×9 main inventory</li>
 *   <li>37–45 → hotbar</li>
 * </ul>
 */
public final class CraftingSlotDebug {
    private CraftingSlotDebug() {}

    public static final int SLOT_COUNT = 46;

    private static int[][] positions = makeDefaults();
    private static int selected = 0;
    private static boolean editMode = false;

    /** Active drag state. -1 when not dragging. */
    private static int dragSlot = -1;
    private static int dragOffsetX, dragOffsetY;

    private static int[][] makeDefaults() {
        int[][] p = new int[SLOT_COUNT][2];

        // 0: result
        p[0] = new int[]{CraftingTableLayout.CRAFT_RESULT_X, CraftingTableLayout.CRAFT_RESULT_Y};

        // 1–9: 3×3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                p[1 + row * 3 + col] = new int[]{
                        CraftingTableLayout.CRAFT_GRID_X0 + col * CraftingTableLayout.CRAFT_GRID_PITCH,
                        CraftingTableLayout.CRAFT_GRID_Y0 + row * CraftingTableLayout.CRAFT_GRID_PITCH};
            }
        }

        // 10–36: inventory 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                p[10 + row * 9 + col] = new int[]{
                        CraftingTableLayout.INV_COL_X[col],
                        CraftingTableLayout.INV_ROW_Y[row]};
            }
        }

        // 37–45: hotbar
        for (int col = 0; col < 9; col++) {
            p[37 + col] = new int[]{
                    CraftingTableLayout.INV_COL_X[col],
                    CraftingTableLayout.HOTBAR_Y};
        }

        return p;
    }

    // ── Edit mode + selection ────────────────────────────────────────────────

    public static boolean isEditMode() { return editMode; }
    public static void toggleEditMode() { editMode = !editMode; }

    public static int selectedIndex() { return selected; }
    public static void cycleSelection(int delta) {
        selected = ((selected + delta) % SLOT_COUNT + SLOT_COUNT) % SLOT_COUNT;
    }
    public static void selectSlot(int idx) {
        if (idx >= 0 && idx < SLOT_COUNT) selected = idx;
    }

    // ── Position queries ──────────────────────────────────────────────────────

    public static int[] slotPos(int idx) {
        if (idx < 0 || idx >= SLOT_COUNT) return new int[]{0, 0};
        return positions[idx];
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    public static void nudge(int dx, int dy) {
        positions[selected][0] += dx;
        positions[selected][1] += dy;
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    /** Start dragging the slot under the cursor (if any). */
    public static boolean tryStartDrag(int mouseX, int mouseY, int leftPos, int topPos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = leftPos + positions[i][0];
            int sy = topPos + positions[i][1];
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                dragSlot = i;
                dragOffsetX = mouseX - sx;
                dragOffsetY = mouseY - sy;
                selected = i;
                return true;
            }
        }
        return false;
    }

    public static boolean isDragging() { return dragSlot >= 0; }

    public static void updateDrag(int mouseX, int mouseY, int leftPos, int topPos) {
        if (dragSlot < 0) return;
        positions[dragSlot][0] = mouseX - leftPos - dragOffsetX;
        positions[dragSlot][1] = mouseY - topPos - dragOffsetY;
    }

    public static void endDrag() {
        dragSlot = -1;
    }

    public static void resetAll() {
        positions = makeDefaults();
    }

    // ── Pretty-print ─────────────────────────────────────────────────────────

    private static String slotName(int i) {
        if (i == 0) return "result";
        if (i <= 9) return "craft[" + (i - 1) + "]";
        if (i <= 36) return "inv[" + (i - 10) + "]";
        return "hotbar[" + (i - 37) + "]";
    }

    public static String summary() {
        int[] p = positions[selected];
        return String.format("[edit] CRAFTING slot %d (%s) → (%d, %d)",
                selected, slotName(selected), p[0], p[1]);
    }

    public static String printJava() {
        StringBuilder sb = new StringBuilder("=== Ironhold crafting layout ===\n");
        for (int i = 0; i < SLOT_COUNT; i++) {
            sb.append(String.format("/* %2d %-12s */ { %3d, %3d },%n",
                    i, slotName(i), positions[i][0], positions[i][1]));
        }
        return sb.toString();
    }
}
