package kingdom.smp.client;

import kingdom.smp.inventory.IronholdInventoryLayout;

/**
 * Live-tunable layout for the Ironhold custom inventory screen.
 *
 * <p>Driven by keyboard shortcuts handled in {@code ClientNeoForgeEvents}:
 * F4 toggles edit mode; Tab cycles selected slot; arrows nudge; number keys
 * switch which "group" is being tuned (slot / paperdoll / vanity panel / skills panel).
 * P prints Java-ready code to chat.
 *
 * <p>Slot index convention (matches {@link net.minecraft.world.inventory.InventoryMenu}
 * + Ironhold extra slots):
 * <ul>
 *   <li>0     → craft result</li>
 *   <li>1–4   → 2×2 craft grid (TL, TR, BL, BR)</li>
 *   <li>5–8   → armor (HEAD, CHEST, LEGS, FEET)</li>
 *   <li>9–35  → 3×9 main inventory</li>
 *   <li>36–44 → hotbar</li>
 *   <li>45    → offhand</li>
 *   <li>46–50 → accessories</li>
 *   <li>51–54 → vanity</li>
 * </ul>
 */
public final class IronholdSlotDebug {
    private IronholdSlotDebug() {}

    public static final int SLOT_COUNT = 55;

    public enum Group { SLOT, PAPERDOLL, VANITY_PANEL, SKILLS_PANEL }

    /** Per-slot positions (relative to leftPos, topPos). */
    private static int[][] positions = makeDefaults();
    private static int selected = 14; // first inventory slot
    private static boolean editMode = false;
    private static Group group = Group.SLOT;

    /** Active drag state. -1 when not dragging. */
    private static int dragSlot = -1;
    private static Group dragGroup = null;
    /** Offset from cursor to top-left of dragged element at drag start. */
    private static int dragOffsetX, dragOffsetY;

    /** Paperdoll bounding box (x0, y0, x1, y1, size) — relative to leftPos, topPos. */
    private static int[] paperdoll = defaultPaperdoll();

    private static int[] defaultPaperdoll() {
        return new int[]{
                IronholdInventoryLayout.PAPERDOLL_X0, IronholdInventoryLayout.PAPERDOLL_Y0,
                IronholdInventoryLayout.PAPERDOLL_X1, IronholdInventoryLayout.PAPERDOLL_Y1,
                IronholdInventoryLayout.PAPERDOLL_SIZE};
    }

    /** Vanity panel offset (x, y) — relative to leftPos, topPos. */
    private static int[] vanityPanel = {
            IronholdInventoryLayout.VANITY_PANEL_OFFSET_X,
            IronholdInventoryLayout.VANITY_PANEL_OFFSET_Y};

    /** Skills panel offset (x, y) — relative to leftPos, topPos. */
    private static int[] skillsPanel = {
            IronholdInventoryLayout.SKILLS_PANEL_OFFSET_X,
            IronholdInventoryLayout.SKILLS_PANEL_OFFSET_Y};

    private static int[][] makeDefaults() {
        int[][] p = new int[SLOT_COUNT][2];

        // 0: craft result
        p[0] = new int[]{IronholdInventoryLayout.CRAFT_RESULT_X, IronholdInventoryLayout.CRAFT_RESULT_Y};

        // 1–4: 2×2 craft grid (TL, TR, BL, BR)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                p[1 + row * 2 + col] = new int[]{
                        IronholdInventoryLayout.CRAFT_GRID_X0 + col * IronholdInventoryLayout.CRAFT_GRID_PITCH,
                        IronholdInventoryLayout.CRAFT_GRID_Y0 + row * IronholdInventoryLayout.CRAFT_GRID_PITCH};
            }
        }

        // 5–8: armor (per-slot y)
        for (int i = 0; i < 4; i++) {
            p[5 + i] = new int[]{
                    IronholdInventoryLayout.ARMOR_X,
                    IronholdInventoryLayout.ARMOR_Y[i]};
        }

        // 9–35: inventory 3×9 (per-row y, per-col x)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                p[9 + row * 9 + col] = new int[]{
                        IronholdInventoryLayout.INV_COL_X[col],
                        IronholdInventoryLayout.INV_ROW_Y[row]};
            }
        }

        // 36–44: hotbar
        for (int col = 0; col < 9; col++) {
            p[36 + col] = new int[]{
                    IronholdInventoryLayout.INV_COL_X[col],
                    IronholdInventoryLayout.HOTBAR_Y};
        }

        // 45: offhand
        p[45] = new int[]{IronholdInventoryLayout.OFFHAND_X, IronholdInventoryLayout.OFFHAND_Y};

        // 46–50: accessories
        for (int i = 0; i < 5; i++) {
            p[46 + i] = new int[]{
                    IronholdInventoryLayout.ACCESSORY_X[i],
                    IronholdInventoryLayout.ACCESSORY_Y};
        }

        // 51–54: vanity (slot.x already includes panel offset)
        for (int i = 0; i < 4; i++) {
            p[51 + i] = new int[]{
                    IronholdInventoryLayout.VANITY_SLOT_X,
                    IronholdInventoryLayout.VANITY_PANEL_OFFSET_Y + IronholdInventoryLayout.VANITY_SLOT_Y_LOCAL[i]};
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

    public static Group group() { return group; }
    public static void setGroup(Group g) { group = g; }

    // ── Position queries ──────────────────────────────────────────────────────

    public static int[] slotPos(int idx) {
        if (idx < 0 || idx >= SLOT_COUNT) return new int[]{0, 0};
        return positions[idx];
    }

    public static int[] paperdoll() { return paperdoll; }
    public static int[] vanityPanelOffset() { return vanityPanel; }
    public static int[] skillsPanelOffset() { return skillsPanel; }

    // ── Mutations (called by key handler) ────────────────────────────────────

    /** Nudge whatever's selected by (dx, dy). */
    public static void nudge(int dx, int dy) {
        switch (group) {
            case SLOT -> {
                positions[selected][0] += dx;
                positions[selected][1] += dy;
            }
            case PAPERDOLL -> {
                paperdoll[0] += dx; paperdoll[1] += dy;
                paperdoll[2] += dx; paperdoll[3] += dy;
            }
            case VANITY_PANEL -> {
                vanityPanel[0] += dx; vanityPanel[1] += dy;
            }
            case SKILLS_PANEL -> {
                skillsPanel[0] += dx; skillsPanel[1] += dy;
            }
        }
    }

    /** Resize paperdoll: (dW, dH) grow/shrink the bounding box; (dSize) adjusts entity scale. */
    public static void resizePaperdoll(int dW, int dH, int dSize) {
        paperdoll[2] += dW;
        paperdoll[3] += dH;
        paperdoll[4] += dSize;
        if (paperdoll[4] < 4) paperdoll[4] = 4;
    }

    // ── Drag (called by mouse handler) ────────────────────────────────────────

    /**
     * Identify what's under the cursor and start dragging it.
     * Auto-sets {@link #selected} and {@link #group} so arrow-key fine-tuning
     * targets the same element after release.
     *
     * @return true if something was grabbed
     */
    public static boolean tryStartDrag(int mouseX, int mouseY, int leftPos, int topPos) {
        // Slot hit test (16×16 area at slot.x, slot.y)
        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = leftPos + positions[i][0];
            int sy = topPos + positions[i][1];
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                dragSlot = i;
                dragGroup = Group.SLOT;
                dragOffsetX = mouseX - sx;
                dragOffsetY = mouseY - sy;
                selected = i;
                group = Group.SLOT;
                return true;
            }
        }
        // Paperdoll bbox
        int px0 = leftPos + paperdoll[0], py0 = topPos + paperdoll[1];
        int px1 = leftPos + paperdoll[2], py1 = topPos + paperdoll[3];
        if (mouseX >= px0 && mouseX < px1 && mouseY >= py0 && mouseY < py1) {
            dragGroup = Group.PAPERDOLL;
            dragOffsetX = mouseX - px0;
            dragOffsetY = mouseY - py0;
            group = Group.PAPERDOLL;
            return true;
        }
        // Vanity panel bbox
        int vx0 = leftPos + vanityPanel[0], vy0 = topPos + vanityPanel[1];
        if (mouseX >= vx0 && mouseX < vx0 + IronholdInventoryLayout.VANITY_PANEL_W
                && mouseY >= vy0 && mouseY < vy0 + IronholdInventoryLayout.VANITY_PANEL_H) {
            dragGroup = Group.VANITY_PANEL;
            dragOffsetX = mouseX - vx0;
            dragOffsetY = mouseY - vy0;
            group = Group.VANITY_PANEL;
            return true;
        }
        // Skills panel bbox
        int kx0 = leftPos + skillsPanel[0], ky0 = topPos + skillsPanel[1];
        if (mouseX >= kx0 && mouseX < kx0 + IronholdInventoryLayout.SKILLS_PANEL_W
                && mouseY >= ky0 && mouseY < ky0 + IronholdInventoryLayout.SKILLS_PANEL_H) {
            dragGroup = Group.SKILLS_PANEL;
            dragOffsetX = mouseX - kx0;
            dragOffsetY = mouseY - ky0;
            group = Group.SKILLS_PANEL;
            return true;
        }
        return false;
    }

    public static boolean isDragging() { return dragGroup != null; }

    /** Update the dragged element's position to track the cursor. */
    public static void updateDrag(int mouseX, int mouseY, int leftPos, int topPos) {
        if (dragGroup == null) return;
        int newX = mouseX - leftPos - dragOffsetX;
        int newY = mouseY - topPos - dragOffsetY;
        switch (dragGroup) {
            case SLOT -> {
                if (dragSlot >= 0) {
                    positions[dragSlot][0] = newX;
                    positions[dragSlot][1] = newY;
                }
            }
            case PAPERDOLL -> {
                int w = paperdoll[2] - paperdoll[0];
                int h = paperdoll[3] - paperdoll[1];
                paperdoll[0] = newX;
                paperdoll[1] = newY;
                paperdoll[2] = newX + w;
                paperdoll[3] = newY + h;
            }
            case VANITY_PANEL -> {
                vanityPanel[0] = newX;
                vanityPanel[1] = newY;
            }
            case SKILLS_PANEL -> {
                skillsPanel[0] = newX;
                skillsPanel[1] = newY;
            }
        }
    }

    public static void endDrag() {
        dragSlot = -1;
        dragGroup = null;
    }

    public static void resetAll() {
        positions = makeDefaults();
        paperdoll = defaultPaperdoll();
        vanityPanel = new int[]{
                IronholdInventoryLayout.VANITY_PANEL_OFFSET_X,
                IronholdInventoryLayout.VANITY_PANEL_OFFSET_Y};
        skillsPanel = new int[]{
                IronholdInventoryLayout.SKILLS_PANEL_OFFSET_X,
                IronholdInventoryLayout.SKILLS_PANEL_OFFSET_Y};
    }

    // ── Pretty-print ──────────────────────────────────────────────────────────

    public static String summary() {
        return switch (group) {
            case SLOT -> {
                int[] p = positions[selected];
                yield String.format("[edit] SLOT %d (%s) → (%d, %d)",
                        selected, slotName(selected), p[0], p[1]);
            }
            case PAPERDOLL -> String.format(
                    "[edit] PAPERDOLL → (%d, %d) → (%d, %d), size=%d",
                    paperdoll[0], paperdoll[1], paperdoll[2], paperdoll[3], paperdoll[4]);
            case VANITY_PANEL -> String.format(
                    "[edit] VANITY_PANEL offset → (%d, %d)", vanityPanel[0], vanityPanel[1]);
            case SKILLS_PANEL -> String.format(
                    "[edit] SKILLS_PANEL offset → (%d, %d)", skillsPanel[0], skillsPanel[1]);
        };
    }

    private static String slotName(int i) {
        if (i == 0) return "result";
        if (i <= 4) return "craft" + (i - 1);
        if (i <= 8) return new String[]{"head", "chest", "legs", "feet"}[i - 5];
        if (i <= 35) return "inv[" + (i - 9) + "]";
        if (i <= 44) return "hotbar[" + (i - 36) + "]";
        if (i == 45) return "offhand";
        if (i <= 50) return "accessory[" + (i - 46) + "]";
        return "vanity[" + (i - 51) + "]";
    }

    /** Java code dump that can be pasted into IronholdInventoryLayout. */
    public static String printJava() {
        StringBuilder sb = new StringBuilder("=== Ironhold inventory layout ===\n");
        sb.append(String.format("// Paperdoll bounds: (%d, %d) → (%d, %d), size=%d%n",
                paperdoll[0], paperdoll[1], paperdoll[2], paperdoll[3], paperdoll[4]));
        sb.append(String.format("// VANITY_PANEL_OFFSET = (%d, %d)%n",
                vanityPanel[0], vanityPanel[1]));
        sb.append(String.format("// SKILLS_PANEL_OFFSET = (%d, %d)%n",
                skillsPanel[0], skillsPanel[1]));
        sb.append("// Slot positions:\n");
        for (int i = 0; i < SLOT_COUNT; i++) {
            sb.append(String.format("/* %2d %-12s */ { %3d, %3d },%n",
                    i, slotName(i), positions[i][0], positions[i][1]));
        }
        return sb.toString();
    }
}
