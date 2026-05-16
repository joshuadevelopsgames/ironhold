package kingdom.smp.client;

import kingdom.smp.inventory.ChestLayout;

/**
 * Live-tunable layout for the Ironhold 3-row chest screen. 63 slots:
 * <ul>
 *   <li>0–26  → chest grid (3×9)</li>
 *   <li>27–53 → 3×9 main inventory</li>
 *   <li>54–62 → hotbar</li>
 * </ul>
 */
public final class ChestSlotDebug {
    private ChestSlotDebug() {}

    public static final int SLOT_COUNT = 63;

    private static int[][] positions = makeDefaults();
    private static int selected = 0;
    private static boolean editMode = false;
    private static int dragSlot = -1;
    private static int dragOffsetX, dragOffsetY;

    private static int[][] makeDefaults() {
        int[][] p = new int[SLOT_COUNT][2];
        // 0–26: chest grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                p[row * 9 + col] = new int[]{ChestLayout.COL_X[col], ChestLayout.CHEST_ROW_Y[row]};
            }
        }
        // 27–53: inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                p[27 + row * 9 + col] = new int[]{ChestLayout.COL_X[col], ChestLayout.INV_ROW_Y[row]};
            }
        }
        // 54–62: hotbar
        for (int col = 0; col < 9; col++) {
            p[54 + col] = new int[]{ChestLayout.COL_X[col], ChestLayout.HOTBAR_Y};
        }
        return p;
    }

    public static boolean isEditMode() { return editMode; }
    public static void toggleEditMode() { editMode = !editMode; }
    public static int selectedIndex() { return selected; }
    public static void cycleSelection(int delta) {
        selected = ((selected + delta) % SLOT_COUNT + SLOT_COUNT) % SLOT_COUNT;
    }
    public static int[] slotPos(int idx) {
        if (idx < 0 || idx >= SLOT_COUNT) return new int[]{0, 0};
        return positions[idx];
    }
    public static void nudge(int dx, int dy) {
        positions[selected][0] += dx; positions[selected][1] += dy;
    }
    public static boolean tryStartDrag(int mx, int my, int leftPos, int topPos) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = leftPos + positions[i][0];
            int sy = topPos + positions[i][1];
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                dragSlot = i;
                dragOffsetX = mx - sx;
                dragOffsetY = my - sy;
                selected = i;
                return true;
            }
        }
        return false;
    }
    public static boolean isDragging() { return dragSlot >= 0; }
    public static void updateDrag(int mx, int my, int leftPos, int topPos) {
        if (dragSlot < 0) return;
        positions[dragSlot][0] = mx - leftPos - dragOffsetX;
        positions[dragSlot][1] = my - topPos - dragOffsetY;
    }
    public static void endDrag() { dragSlot = -1; }
    public static void resetAll() { positions = makeDefaults(); }

    private static String slotName(int i) {
        if (i <= 26) return "chest[" + i + "]";
        if (i <= 53) return "inv[" + (i - 27) + "]";
        return "hotbar[" + (i - 54) + "]";
    }

    public static String summary() {
        int[] p = positions[selected];
        return String.format("[edit] CHEST slot %d (%s) → (%d, %d)",
                selected, slotName(selected), p[0], p[1]);
    }

    public static String printJava() {
        StringBuilder sb = new StringBuilder("=== Ironhold chest layout ===\n");
        for (int i = 0; i < SLOT_COUNT; i++) {
            sb.append(String.format("/* %2d %-12s */ { %3d, %3d },%n",
                    i, slotName(i), positions[i][0], positions[i][1]));
        }
        return sb.toString();
    }
}
