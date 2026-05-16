package kingdom.smp.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared widget-position tuner used by every Ironhold reskinned container screen.
 *
 * <p>Each container's per-screen SlotDebug treats its selection index as a unified
 * range over [0 .. SLOT_COUNT + widgetCount). For indices ≥ SLOT_COUNT the operation
 * is routed here. Tuned positions are stored panel-relative (subtracting the screen's
 * leftPos/topPos at capture time) so they survive window resizes.
 *
 * <p>Lifecycle per screen:
 * <ol>
 *   <li>Mixin's init() TAIL injection → {@link #captureWidgets} once. Walks {@code screen.children()},
 *       gives each AbstractWidget a stable label of the form {@code <SimpleClassName>:<orderIndex>}.</li>
 *   <li>Mixin's extractBackground HEAD injection → {@link #applyPositions} every frame. Re-applies any
 *       tuned overrides on top of vanilla's positioning.</li>
 *   <li>Handler routes F4 drag/arrow/print through the SlotDebug, which dispatches widget operations
 *       back into this class when the selected index lies in the widget range.</li>
 * </ol>
 *
 * <p>Stored positions persist across screen open/close (static maps).
 */
public final class ScreenWidgetTuner {
    private ScreenWidgetTuner() {}

    public static final class WidgetEntry {
        public final AbstractWidget widget;
        public final String label;
        public final int origRelX, origRelY;

        WidgetEntry(AbstractWidget w, String label, int origRelX, int origRelY) {
            this.widget = w;
            this.label = label;
            this.origRelX = origRelX;
            this.origRelY = origRelY;
        }
    }

    /** screenKey → ordered list of widgets currently on-screen (rebuilt on screen change). */
    private static final Map<String, List<WidgetEntry>> active = new HashMap<>();
    /** screenKey → label → {x, y} panel-relative tuned positions (set live via F4 drag). */
    private static final Map<String, Map<String, int[]>> tuned = new HashMap<>();
    /** screenKey → label → {x, y} panel-relative DEFAULTS baked into Layout source. Lower priority than tuned. */
    private static final Map<String, Map<String, int[]>> defaults = new HashMap<>();
    /** screenKey → last-captured Screen instance (identity check; resize creates a new init() call but same instance). */
    private static final Map<String, Screen> lastScreen = new HashMap<>();
    /** screenKey → last-captured children() size — recapture if it changes (dynamic widgets like merchant trade list). */
    private static final Map<String, Integer> lastChildCount = new HashMap<>();

    // ── Label tuning (title + inventory labels on AbstractContainerScreen) ─────────────────
    /** Index 0 = title label, index 1 = inventory label. */
    public static final int LABEL_COUNT = 2;
    public static final String[] LABEL_NAMES = { "title_label", "inv_label" };
    /** Screens that override extractLabels and ignore titleLabelX/Y/inventoryLabelX/Y entirely.
     * Tuning those fields is a no-op on these screens, so we drop them from the F4 cycle. */
    private static final java.util.Set<String> labelsDisabledScreens = new java.util.HashSet<>();

    public static void disableLabels(String screenKey) { labelsDisabledScreens.add(screenKey); }
    public static boolean areLabelsDisabled(String screenKey) { return labelsDisabledScreens.contains(screenKey); }

    /** Number of tunable labels for this screen (0 if labels are disabled, else {@link #LABEL_COUNT}). */
    public static int labelCount(String screenKey) {
        return labelsDisabledScreens.contains(screenKey) ? 0 : LABEL_COUNT;
    }

    // ── Sprite/draw-call anchors (arbitrary named tunable points per screen) ───────────────
    /** screenKey → ordered list of anchor names. */
    private static final Map<String, List<String>> anchorOrder = new HashMap<>();
    /** screenKey → name → {x, y} vanilla originals (panel-relative). */
    private static final Map<String, Map<String, int[]>> anchorOriginals = new HashMap<>();
    /** screenKey → name → {x, y} tuned overrides (panel-relative). */
    private static final Map<String, Map<String, int[]>> anchorTuned = new HashMap<>();

    /** screenKey → 2× {x, y} ORIGINAL panel-relative positions captured from vanilla. */
    private static final Map<String, int[][]> labelOriginals = new HashMap<>();
    /** screenKey → 2× {x, y} TUNED overrides (or null entry = use default/original). */
    private static final Map<String, int[][]> labelTuned = new HashMap<>();
    /** screenKey → 2× {x, y} DEFAULTS baked into Layout source (or null entry = use original). */
    private static final Map<String, int[][]> labelDefaults = new HashMap<>();
    /** Last screen instance per key (separate from widget tracking — labels capture during the same hook). */
    private static final Map<String, Screen> labelLastScreen = new HashMap<>();

    /**
     * Idempotent: only re-walks children() if the screen instance changed or the widget count differs.
     * Safe to call every frame from extractBackground. Replaces the previous {@code @Inject(method="init")}
     * approach, which failed for screens that don't redeclare {@code init()} on the subclass.
     */
    public static void ensureCaptured(String screenKey, Screen screen, int leftPos, int topPos) {
        int childCount = 0;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget) childCount++;
        }
        if (lastScreen.get(screenKey) == screen
                && Integer.valueOf(childCount).equals(lastChildCount.get(screenKey))) {
            return;
        }
        captureWidgets(screenKey, screen, leftPos, topPos);
        lastScreen.put(screenKey, screen);
        lastChildCount.put(screenKey, childCount);
    }

    /** Walk screen.children(); pick up AbstractWidget instances; assign stable order labels. */
    public static void captureWidgets(String screenKey, Screen screen, int leftPos, int topPos) {
        List<WidgetEntry> list = new ArrayList<>();
        Map<String, Integer> classCounters = new HashMap<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget w) {
                String cls = w.getClass().getSimpleName();
                int n = classCounters.merge(cls, 1, Integer::sum) - 1;
                String label = cls + ":" + n;
                int relX = w.getX() - leftPos;
                int relY = w.getY() - topPos;
                list.add(new WidgetEntry(w, label, relX, relY));
            }
        }
        active.put(screenKey, list);
    }

    /** Apply tuned > default > vanilla positions to live widgets. Call every frame. */
    public static void applyPositions(String screenKey, int leftPos, int topPos) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null) return;
        Map<String, int[]> screenTuned = tuned.get(screenKey);
        Map<String, int[]> screenDefaults = defaults.get(screenKey);
        for (WidgetEntry e : list) {
            int[] relXY = null;
            if (screenTuned != null) relXY = screenTuned.get(e.label);
            if (relXY == null && screenDefaults != null) relXY = screenDefaults.get(e.label);
            if (relXY != null) {
                e.widget.setX(leftPos + relXY[0]);
                e.widget.setY(topPos + relXY[1]);
            }
            // else: leave at vanilla position
        }
    }

    public static int widgetCount(String screenKey) {
        List<WidgetEntry> list = active.get(screenKey);
        return list == null ? 0 : list.size();
    }

    public static String widgetLabel(String screenKey, int i) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null || i < 0 || i >= list.size()) return "?";
        return list.get(i).label;
    }

    /** Panel-relative pos lookup order: tuned (live drag) → default (Layout-baked) → original (vanilla). */
    public static int[] widgetPos(String screenKey, int i) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null || i < 0 || i >= list.size()) return new int[]{0, 0};
        WidgetEntry e = list.get(i);
        Map<String, int[]> screenTuned = tuned.get(screenKey);
        if (screenTuned != null) {
            int[] xy = screenTuned.get(e.label);
            if (xy != null) return new int[]{xy[0], xy[1]};
        }
        Map<String, int[]> screenDefaults = defaults.get(screenKey);
        if (screenDefaults != null) {
            int[] xy = screenDefaults.get(e.label);
            if (xy != null) return new int[]{xy[0], xy[1]};
        }
        return new int[]{e.origRelX, e.origRelY};
    }

    /**
     * Register a Layout-baked default for one widget. Called from each Layout's static initializer.
     * Lower priority than live-tuned positions; higher than vanilla originals.
     */
    public static void setDefaultWidgetPos(String screenKey, String widgetLabel, int relX, int relY) {
        defaults.computeIfAbsent(screenKey, k -> new HashMap<>()).put(widgetLabel, new int[]{relX, relY});
    }

    public static void setWidgetRelPos(String screenKey, int i, int relX, int relY) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null || i < 0 || i >= list.size()) return;
        WidgetEntry e = list.get(i);
        tuned.computeIfAbsent(screenKey, k -> new HashMap<>()).put(e.label, new int[]{relX, relY});
    }

    public static void nudgeWidget(String screenKey, int i, int dx, int dy) {
        int[] xy = widgetPos(screenKey, i);
        setWidgetRelPos(screenKey, i, xy[0] + dx, xy[1] + dy);
    }

    /** Returns widget index hit, or -1. Hit-test uses absolute screen coords (widget.getX/Y). */
    public static int tryStartDragWidget(String screenKey, int mx, int my) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            AbstractWidget w = list.get(i).widget;
            int x = w.getX(), y = w.getY();
            int ww = w.getWidth(), wh = w.getHeight();
            if (mx >= x && mx < x + ww && my >= y && my < y + wh) {
                return i;
            }
        }
        return -1;
    }

    /** Convenience: get the widget's current absolute origin (for computing drag offset). */
    public static int[] widgetAbsPos(String screenKey, int i) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null || i < 0 || i >= list.size()) return new int[]{0, 0};
        AbstractWidget w = list.get(i).widget;
        return new int[]{w.getX(), w.getY()};
    }

    /** Returns {x, y, width, height} in absolute screen coords. */
    public static int[] widgetBox(String screenKey, int i) {
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null || i < 0 || i >= list.size()) return new int[]{0, 0, 0, 0};
        AbstractWidget w = list.get(i).widget;
        return new int[]{w.getX(), w.getY(), w.getWidth(), w.getHeight()};
    }

    public static void resetScreen(String screenKey) {
        tuned.remove(screenKey);
        // restore originals on live widgets
        List<WidgetEntry> list = active.get(screenKey);
        AbstractContainerScreen<?> cs = null; // we don't store the screen; widgets remember leftPos
        if (list != null) {
            // Original positions were captured panel-relative, so we need current leftPos/topPos.
            // We don't have access here, so just clear tuned — applyPositions next frame falls back to originals.
        }
    }

    // ── Labels ─────────────────────────────────────────────────────────────────

    /**
     * Capture the screen's vanilla label positions once per screen instance.
     * Call from extractBackground BEFORE applyLabelPositions overwrites them.
     */
    public static void ensureLabelsCaptured(String screenKey, Screen screen,
                                            int titleX, int titleY, int invX, int invY) {
        if (labelLastScreen.get(screenKey) == screen) return;
        int[][] orig = new int[LABEL_COUNT][2];
        orig[0][0] = titleX; orig[0][1] = titleY;
        orig[1][0] = invX;   orig[1][1] = invY;
        labelOriginals.put(screenKey, orig);
        labelLastScreen.put(screenKey, screen);
    }

    /** Panel-relative pos lookup order: tuned → default → original. */
    public static int[] labelPos(String screenKey, int i) {
        if (i < 0 || i >= LABEL_COUNT) return new int[]{0, 0};
        int[][] t = labelTuned.get(screenKey);
        if (t != null && t[i] != null) return new int[]{t[i][0], t[i][1]};
        int[][] d = labelDefaults.get(screenKey);
        if (d != null && d[i] != null) return new int[]{d[i][0], d[i][1]};
        int[][] o = labelOriginals.get(screenKey);
        if (o != null) return new int[]{o[i][0], o[i][1]};
        return new int[]{0, 0};
    }

    /** Register a Layout-baked default label position. Called from each Layout's static initializer. */
    public static void setDefaultLabelPos(String screenKey, int i, int relX, int relY) {
        if (i < 0 || i >= LABEL_COUNT) return;
        int[][] d = labelDefaults.computeIfAbsent(screenKey, k -> new int[LABEL_COUNT][]);
        d[i] = new int[]{relX, relY};
    }

    public static void setLabelRelPos(String screenKey, int i, int x, int y) {
        if (i < 0 || i >= LABEL_COUNT) return;
        int[][] t = labelTuned.computeIfAbsent(screenKey, k -> new int[LABEL_COUNT][]);
        t[i] = new int[]{x, y};
    }

    public static void nudgeLabel(String screenKey, int i, int dx, int dy) {
        int[] xy = labelPos(screenKey, i);
        setLabelRelPos(screenKey, i, xy[0] + dx, xy[1] + dy);
    }

    /** Synthetic hit-box for label drag (~60×10 around label origin). Returns index or -1. */
    public static int tryStartDragLabel(String screenKey, int mx, int my, int leftPos, int topPos) {
        for (int i = 0; i < LABEL_COUNT; i++) {
            int[] rel = labelPos(screenKey, i);
            int absX = leftPos + rel[0];
            int absY = topPos + rel[1];
            // generous bbox since we don't have Font access here
            if (mx >= absX - 2 && mx < absX + 60 && my >= absY - 2 && my < absY + 12) {
                return i;
            }
        }
        return -1;
    }

    /** Returns {absX, absY, bboxW, bboxH} — approximate, for the edit-mode outline. */
    public static int[] labelBox(String screenKey, int i, int leftPos, int topPos) {
        int[] rel = labelPos(screenKey, i);
        return new int[]{ leftPos + rel[0] - 2, topPos + rel[1] - 2, 60, 12 };
    }

    public static String labelName(int i) {
        if (i < 0 || i >= LABEL_COUNT) return "?";
        return LABEL_NAMES[i];
    }

    // ── Anchors (arbitrary tunable sprite/draw-call positions) ─────────────────

    /**
     * Register a named anchor with its vanilla original position (panel-relative).
     * Call from Layout static init. Idempotent: same (screenKey, name) registers once.
     */
    public static void registerAnchor(String screenKey, String name, int origX, int origY) {
        Map<String, int[]> originals = anchorOriginals.computeIfAbsent(screenKey, k -> new HashMap<>());
        if (!originals.containsKey(name)) {
            originals.put(name, new int[]{origX, origY});
            anchorOrder.computeIfAbsent(screenKey, k -> new ArrayList<>()).add(name);
        }
    }

    public static int anchorCount(String screenKey) {
        List<String> order = anchorOrder.get(screenKey);
        return order == null ? 0 : order.size();
    }

    public static String anchorName(String screenKey, int i) {
        List<String> order = anchorOrder.get(screenKey);
        if (order == null || i < 0 || i >= order.size()) return "?";
        return order.get(i);
    }

    /** Panel-relative pos: tuned > original. */
    public static int[] anchorPos(String screenKey, int i) {
        String name = anchorName(screenKey, i);
        if ("?".equals(name)) return new int[]{0, 0};
        return anchorPos(screenKey, name);
    }

    public static int[] anchorPos(String screenKey, String name) {
        Map<String, int[]> t = anchorTuned.get(screenKey);
        if (t != null) {
            int[] xy = t.get(name);
            if (xy != null) return new int[]{xy[0], xy[1]};
        }
        Map<String, int[]> o = anchorOriginals.get(screenKey);
        if (o != null) {
            int[] xy = o.get(name);
            if (xy != null) return new int[]{xy[0], xy[1]};
        }
        return new int[]{0, 0};
    }

    public static void setAnchorRelPos(String screenKey, int i, int relX, int relY) {
        String name = anchorName(screenKey, i);
        if ("?".equals(name)) return;
        anchorTuned.computeIfAbsent(screenKey, k -> new HashMap<>())
                   .put(name, new int[]{relX, relY});
    }

    public static void nudgeAnchor(String screenKey, int i, int dx, int dy) {
        int[] xy = anchorPos(screenKey, i);
        setAnchorRelPos(screenKey, i, xy[0] + dx, xy[1] + dy);
    }

    /** 24×24 synthetic hit-box centered on the anchor origin (sprite size unknown). */
    public static int tryStartDragAnchor(String screenKey, int mx, int my, int leftPos, int topPos) {
        int n = anchorCount(screenKey);
        for (int i = 0; i < n; i++) {
            int[] rel = anchorPos(screenKey, i);
            int absX = leftPos + rel[0];
            int absY = topPos + rel[1];
            if (mx >= absX - 4 && mx < absX + 24 && my >= absY - 4 && my < absY + 24) {
                return i;
            }
        }
        return -1;
    }

    public static int[] anchorBox(String screenKey, int i, int leftPos, int topPos) {
        int[] rel = anchorPos(screenKey, i);
        return new int[]{ leftPos + rel[0] - 4, topPos + rel[1] - 4, 24, 24 };
    }

    public static String printJava(String screenKey) {
        StringBuilder sb = new StringBuilder("=== " + screenKey + " widgets (panel-relative) ===\n");
        List<WidgetEntry> list = active.get(screenKey);
        if (list == null) {
            sb.append("(none captured)\n");
        } else {
            for (int i = 0; i < list.size(); i++) {
                WidgetEntry e = list.get(i);
                int[] xy = widgetPos(screenKey, i);
                sb.append(String.format("/* W%2d %-30s */ { %3d, %3d },  // orig (%3d,%3d)%n",
                        i, e.label, xy[0], xy[1], e.origRelX, e.origRelY));
            }
        }
        sb.append("=== ").append(screenKey).append(" labels ===\n");
        int[][] orig = labelOriginals.get(screenKey);
        for (int i = 0; i < LABEL_COUNT; i++) {
            int[] xy = labelPos(screenKey, i);
            int origX = (orig != null) ? orig[i][0] : 0;
            int origY = (orig != null) ? orig[i][1] : 0;
            sb.append(String.format("/* L%d  %-14s */ { %3d, %3d },  // orig (%3d,%3d)%n",
                    i, LABEL_NAMES[i], xy[0], xy[1], origX, origY));
        }
        List<String> aOrder = anchorOrder.get(screenKey);
        if (aOrder != null && !aOrder.isEmpty()) {
            sb.append("=== ").append(screenKey).append(" anchors ===\n");
            Map<String, int[]> aOrig = anchorOriginals.get(screenKey);
            for (int i = 0; i < aOrder.size(); i++) {
                String name = aOrder.get(i);
                int[] xy = anchorPos(screenKey, i);
                int[] o2 = (aOrig != null) ? aOrig.get(name) : null;
                int origX = (o2 != null) ? o2[0] : 0;
                int origY = (o2 != null) ? o2[1] : 0;
                sb.append(String.format("/* A%d  %-14s */ { %3d, %3d },  // orig (%3d,%3d)%n",
                        i, name, xy[0], xy[1], origX, origY));
            }
        }
        return sb.toString();
    }
}
