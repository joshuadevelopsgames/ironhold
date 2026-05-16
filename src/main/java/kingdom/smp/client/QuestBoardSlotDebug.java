package kingdom.smp.client;

/**
 * Live-tunable per-slot positions for the Quest Board GUI's 36 player-inventory
 * slots. Driven by the {@code /qbslot} command and applied every frame by
 * {@link kingdom.smp.client.screen.QuestBoardScreen} (which writes the values
 * onto each {@code Slot.x} / {@code Slot.y} just before rendering).
 *
 * <p>Slot index convention matches the menu's slot order:
 * <ul>
 *   <li>0–26  → main inventory (rows 1, 2, 3 left→right)</li>
 *   <li>27–35 → hotbar</li>
 * </ul>
 *
 * <p>Selected-slot index drives which slot subsequent {@code rot}/{@code pos}
 * commands target. The screen highlights the selected slot with a colored
 * outline so it's obvious which one is being tweaked.
 */
public final class QuestBoardSlotDebug {
    private QuestBoardSlotDebug() {}

    public static final int SLOT_COUNT = 36;

    /** Live positions, one [x, y] per slot. Mutated by the command. */
    private static int[][] positions = makeDefaults();

    /** Index of the slot the next move/pos command will affect. */
    private static int selected = 0;

    private static int[][] makeDefaults() {
        // Derived by scanning the texture for #242323 (painted slot interior).
        // For 232×240 GUI: x0=40, y0=153, pitchX=17, pitchY=16, hotbarGap=3.
        return uniformGrid(40, 153, 17, 16, 3);
    }

    /**
     * Build a uniform 4-row × 9-col grid.
     * @param x0      x of top-left slot
     * @param y0      y of top-left slot (main row 1)
     * @param pitchX  px between slot columns
     * @param pitchY  px between rows within main inventory
     * @param hotbarGap extra px between main row 3 and the hotbar
     */
    public static int[][] uniformGrid(int x0, int y0, int pitchX, int pitchY, int hotbarGap) {
        int[][] out = new int[SLOT_COUNT][2];
        for (int r = 0; r < 3; r++) {
            int y = y0 + r * pitchY;
            for (int c = 0; c < 9; c++) {
                out[r * 9 + c] = new int[]{ x0 + c * pitchX, y };
            }
        }
        int hotbarY = y0 + 3 * pitchY + hotbarGap;
        for (int c = 0; c < 9; c++) {
            out[27 + c] = new int[]{ x0 + c * pitchX, hotbarY };
        }
        return out;
    }

    /** Replace all 36 positions with a fresh uniform grid. */
    public static void setUniformGrid(int x0, int y0, int pitchX, int pitchY, int hotbarGap) {
        positions = uniformGrid(x0, y0, pitchX, pitchY, hotbarGap);
    }

    /** Translate every slot by (dx, dy). */
    public static void nudgeAll(int dx, int dy) {
        for (int[] p : positions) {
            p[0] += dx;
            p[1] += dy;
        }
    }

    public static int[] position(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return new int[]{ 0, 0 };
        return positions[slotIndex];
    }

    public static int selectedIndex() { return selected; }

    public static boolean select(int idx) {
        if (idx < 0 || idx >= SLOT_COUNT) return false;
        selected = idx;
        return true;
    }

    public static void setPos(int idx, int x, int y) {
        if (idx < 0 || idx >= SLOT_COUNT) return;
        positions[idx][0] = x;
        positions[idx][1] = y;
    }

    public static void nudge(int idx, int dx, int dy) {
        if (idx < 0 || idx >= SLOT_COUNT) return;
        positions[idx][0] += dx;
        positions[idx][1] += dy;
    }

    public static void resetAll() {
        positions = makeDefaults();
    }

    /** Pretty-printed Java array, ready to paste over {@code SLOT_POSITIONS} in QuestBoardMenu. */
    public static String printJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("private static final int[][] SLOT_POSITIONS = {\n");
        for (int i = 0; i < SLOT_COUNT; i++) {
            String section = i < 9 ? "main row 1" : i < 18 ? "main row 2"
                : i < 27 ? "main row 3" : "hotbar";
            sb.append(String.format("    /* %2d %-11s */ { %3d, %3d },%n",
                i, section, positions[i][0], positions[i][1]));
        }
        sb.append("};");
        return sb.toString();
    }

    /** Compact summary of the active context for chat feedback. */
    public static String summary() {
        int[] p = positions[selected];
        String section = selected < 9 ? "main row 1" : selected < 18 ? "main row 2"
            : selected < 27 ? "main row 3" : "hotbar";
        return String.format("slot %d (%s) → (%d, %d)", selected, section, p[0], p[1]);
    }
}
