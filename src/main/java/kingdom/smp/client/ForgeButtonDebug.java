package kingdom.smp.client;

import kingdom.smp.client.gui.InvisibleButton;

/**
 * Live-tunable position + size for the anvil "forge hammer" click button, plus
 * an in-screen edit mode so it can be positioned without closing the anvil
 * (chat/commands are unavailable while a container screen is open).
 *
 * <p>Coordinates are offsets from the anvil screen's top-left
 * ({@code leftPos}/{@code topPos}). Tune in-game (see {@link ForgeButtonDebugHandler}
 * for keys), hit P to print, then bake the values into the defaults here.
 */
public final class ForgeButtonDebug {
    private ForgeButtonDebug() {}

    // Baked from in-game F6 tuning (2026-05-19). Re-tune live and re-bake if the layout changes.
    public static int x = 16;
    public static int y = 6;
    public static int w = 31;
    public static int h = 31;

    /** The live button on the currently-open anvil screen, if any. */
    public static InvisibleButton active;
    /** Screen origin captured when the live button was created (so offsets resolve to absolute). */
    public static int originX, originY;

    // ── Edit-mode state ─────────────────────────────────────────────────────
    private static boolean editMode = false;
    private static boolean dragging = false;
    private static int dragOffX, dragOffY;

    public static boolean isEditMode() { return editMode; }
    public static boolean isDragging() { return dragging; }

    public static void toggleEditMode() {
        editMode = !editMode;
        if (!editMode) dragging = false;
    }

    public static void nudge(int dx, int dy) {
        x += dx;
        y += dy;
        applyToActive();
    }

    public static void resize(int dw, int dh) {
        w = Math.max(4, w + dw);
        h = Math.max(4, h + dh);
        applyToActive();
    }

    /** Begin dragging if the click landed inside the button's screen rect. */
    public static boolean tryStartDrag(int mouseX, int mouseY, int left, int top) {
        int bx = left + x, by = top + y;
        if (mouseX >= bx && mouseX < bx + w && mouseY >= by && mouseY < by + h) {
            dragging = true;
            dragOffX = mouseX - bx;
            dragOffY = mouseY - by;
            return true;
        }
        return false;
    }

    public static void updateDrag(int mouseX, int mouseY, int left, int top) {
        if (!dragging) return;
        x = mouseX - left - dragOffX;
        y = mouseY - top - dragOffY;
        applyToActive();
    }

    public static void endDrag() { dragging = false; }

    /** Push the current coords onto the live button so tweaks apply without reopening. */
    public static void applyToActive() {
        if (active != null) {
            active.setX(originX + x);
            active.setY(originY + y);
            active.setWidth(w);
            active.setHeight(h);
        }
    }

    public static String summary() {
        return "§b[forgebutton] " + "x=" + x + " y=" + y + " w=" + w + " h=" + h;
    }

    /** Bakeable Java for ForgeButtonDebug defaults. */
    public static String printJava() {
        return "    public static int x = " + x + ";\n"
             + "    public static int y = " + y + ";\n"
             + "    public static int w = " + w + ";\n"
             + "    public static int h = " + h + ";";
    }
}
