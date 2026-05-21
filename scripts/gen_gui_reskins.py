#!/usr/bin/env python3
"""
Generate Ironhold GUI-reskin boilerplate (Layout / SlotDebug / SlotDebugHandler / ScreenMixin)
for the 12 vanilla containers the user dropped textures for. Idempotent — re-run after spec edits.

Output paths (relative to ironhold/src/main/java/kingdom/smp/):
  inventory/<Name>Layout.java
  client/<Name>SlotDebug.java
  client/<Name>SlotDebugHandler.java
  mixin/<Name>ScreenMixin.java
"""
import os, sys, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "java" / "kingdom" / "smp"
MODID_CONST = "Ironhold.MODID"

# -------- spec --------
# fields:
#   key         — texture folder + filename stem (textures/gui/<key>/<key>_main.png)
#   pascal      — class-name root (e.g. Hopper, BlastFurnace)
#   screen_pkg  — fully-qualified vanilla Screen class
#   menu_pkg    — fully-qualified vanilla Menu class
#   main_w/main_h — in-game pixel size (we DOWNSCALE source via blit dest size)
#   src_w/src_h — native source PNG resolution (for blit srcSprite dims)
#   slot_count  — total slot count = container + 36 (inv 27 + hotbar 9)
#   container_slots — list of (label, x, y) for the container-side slots, in slot-index order
#   inv_origin  — (x, y) top-left of the 3×9 main-inv grid
#   hotbar_y    — y coordinate of the hotbar row
#   inv_col_step / inv_row_step — pixel step between inv slots (usually equal, ~22 for a 1.6× scaled grid)
SPECS = [
    {
        "key": "anvil", "pascal": "Anvil",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.AnvilScreen",
        "menu_pkg":   "net.minecraft.world.inventory.AnvilMenu",
        "main_w": 280, "main_h": 280, "src_w": 700, "src_h": 698,
        "container_slots": [("in_left", 60, 70), ("in_right", 110, 70), ("out", 195, 70)],
        "inv_origin": (24, 158), "hotbar_y": 230, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "beacon", "pascal": "Beacon",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.BeaconScreen",
        "menu_pkg":   "net.minecraft.world.inventory.BeaconMenu",
        "main_w": 290, "main_h": 287, "src_w": 833, "src_h": 826,
        # Tuned values from in-game F4 sessions — last update chat log 2026-05-13 01:37.
        # Slot grid kept on a clean formula (±1px drag variance smoothed out).
        "container_slots": [("payment", 169, 132)],
        "inv_origin": (42, 165), "hotbar_y": 245, "inv_col_step": 24, "inv_row_step": 24,
        "widget_defaults": {
            "BeaconPowerButton:0":         ( 64,  31),
            "BeaconPowerButton:1":         ( 93,  31),
            "BeaconPowerButton:2":         ( 64,  59),
            "BeaconPowerButton:3":         ( 93,  59),
            "BeaconPowerButton:4":         ( 79,  87),
            "BeaconPowerButton:5":         (196,  59),
            "BeaconUpgradePowerButton:0":  (168,  47),
            "BeaconConfirmButton:0":       (201, 129),
            "BeaconCancelButton:0":        (229, 129),
        },
        # BeaconScreen overrides extractLabels and ignores title/inv label fields entirely.
        # It draws "Primary Power" and "Secondary Power" at hardcoded coords instead.
        # Disable the generic labels and expose the two real ones as named anchors below.
        "disable_labels": True,
        "anchors": [
            ("primary_label",    62, 10),  # PRIMARY_EFFECT_LABEL centeredText origin
            ("secondary_label", 169, 10),  # SECONDARY_EFFECT_LABEL centeredText origin
        ],
    },
    {
        "key": "blast_furnace", "pascal": "BlastFurnace",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen",
        "menu_pkg":   "net.minecraft.world.inventory.BlastFurnaceMenu",
        "main_w": 280, "main_h": 275, "src_w": 366, "src_h": 360,
        "container_slots": [("input", 90, 30), ("fuel", 90, 80), ("output", 180, 55)],
        "inv_origin": (24, 154), "hotbar_y": 226, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "brewing_stand", "pascal": "BrewingStand",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.BrewingStandScreen",
        "menu_pkg":   "net.minecraft.world.inventory.BrewingStandMenu",
        # Tuned values from chat log 2026-05-13 02:16 (±1px drag noise smoothed).
        "main_w": 280, "main_h": 282, "src_w": 1106, "src_h": 1115,
        "container_slots": [
            ("bottle0",    104,  95),
            ("bottle1",    144, 106),
            ("bottle2",    183,  95),
            ("ingredient", 145,  41),
            ("fuel",        46,  41),
        ],
        "inv_origin": (26, 147), "hotbar_y": 236, "inv_col_step": 26, "inv_row_step": 26,
        "label_defaults": {
            "title_label": (103,  24),
            "inv_label":   ( 26, 125),
        },
    },
    {
        "key": "crafter", "pascal": "Crafter",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.CrafterScreen",
        "menu_pkg":   "net.minecraft.world.inventory.CrafterMenu",
        "main_w": 280, "main_h": 282, "src_w": 1106, "src_h": 1115,
        # 3×3 grid + 1 output
        "container_slots": (
            [(f"grid{r*3+c}", 60 + c*22, 30 + r*22) for r in range(3) for c in range(3)]
            + [("out", 200, 52)]
        ),
        "inv_origin": (24, 160), "hotbar_y": 232, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "dispenser", "pascal": "Dispenser",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.DispenserScreen",
        "menu_pkg":   "net.minecraft.world.inventory.DispenserMenu",
        "main_w": 280, "main_h": 282, "src_w": 1106, "src_h": 1115,
        "container_slots": [(f"grid{r*3+c}", 110 + c*22, 30 + r*22) for r in range(3) for c in range(3)],
        "inv_origin": (24, 160), "hotbar_y": 232, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "grindstone", "pascal": "Grindstone",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.GrindstoneScreen",
        "menu_pkg":   "net.minecraft.world.inventory.GrindstoneMenu",
        "main_w": 280, "main_h": 278, "src_w": 1101, "src_h": 1094,
        "container_slots": [("in_top", 80, 30), ("in_bot", 80, 60), ("out", 200, 45)],
        "inv_origin": (24, 156), "hotbar_y": 228, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "hopper", "pascal": "Hopper",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.HopperScreen",
        "menu_pkg":   "net.minecraft.world.inventory.HopperMenu",
        # Texture updated 2026-05-13: 1221×1109 (was 1150×1150). MAIN scaled to preserve aspect.
        # Tuned values from chat log 2026-05-13 02:12 (±1px drag noise smoothed to clean formula).
        "main_w": 290, "main_h": 263, "src_w": 1221, "src_h": 1109,
        "container_slots": [(f"hop{i}", 83 + i*26, 48) for i in range(5)],
        "inv_origin": (32, 101), "hotbar_y": 203, "inv_col_step": 26, "inv_row_step": 27,
        "label_defaults": {
            "title_label": (115,  27),
            "inv_label":   ( 28,  79),
        },
    },
    {
        "key": "shulker", "pascal": "Shulker",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen",
        "menu_pkg":   "net.minecraft.world.inventory.ShulkerBoxMenu",
        # Texture updated 2026-05-13: 1071×984 (was 1150×1150). MAIN widened to match aspect.
        # Tuned values from chat log 2026-05-13 02:50 (±1px drag noise smoothed to clean formula).
        "main_w": 315, "main_h": 290, "src_w": 1071, "src_h": 984,
        # 3×9 shulker grid
        "container_slots": [
            (f"sh{r*9+c}", 35 + c*28, 42 + r*28) for r in range(3) for c in range(9)
        ],
        "inv_origin": (35, 144), "hotbar_y": 241, "inv_col_step": 28, "inv_row_step": 28,
    },
    {
        "key": "smithing", "pascal": "Smithing",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.SmithingScreen",
        "menu_pkg":   "net.minecraft.world.inventory.SmithingMenu",
        "main_w": 280, "main_h": 280, "src_w": 1150, "src_h": 1150,
        "container_slots": [
            ("template", 30, 50), ("base", 60, 50),
            ("addition", 90, 50), ("out", 200, 50),
        ],
        "inv_origin": (24, 158), "hotbar_y": 230, "inv_col_step": 26, "inv_row_step": 22,
    },
    {
        "key": "stonecutter", "pascal": "Stonecutter",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.StonecutterScreen",
        "menu_pkg":   "net.minecraft.world.inventory.StonecutterMenu",
        # Tuned values from chat log 2026-05-13 02:55.
        "main_w": 280, "main_h": 280, "src_w": 1150, "src_h": 1150,
        "container_slots": [("in", 48, 50), ("out", 213, 47)],
        "inv_origin": (33, 137), "hotbar_y": 219, "inv_col_step": 24, "inv_row_step": 25,
        "label_defaults": {
            "title_label": ( 28,  27),
            "inv_label":   ( 28, 119),
        },
        # Vanilla draws these as raw sprites inside extractBackground. Anchors expose them to F4.
        # @ModifyArgs in StonecutterScreenMixin reads anchorPos and adjusts the draw-call args.
        "anchors": [
            ("scroller",    119, 15),  # SCROLLER_SPRITE blit base
            ("recipe_grid",  52, 14),  # extractButtons + extractRecipes recipeX, recipeY origin
        ],
    },
    {
        "key": "villager", "pascal": "Villager",
        "screen_pkg": "net.minecraft.client.gui.screens.inventory.MerchantScreen",
        "menu_pkg":   "net.minecraft.world.inventory.MerchantMenu",
        "main_w": 320, "main_h": 234, "src_w": 1192, "src_h": 871,
        "container_slots": [("trade_in1", 142, 50), ("trade_in2", 175, 50), ("out", 245, 50)],
        "inv_origin": (115, 105), "hotbar_y": 178, "inv_col_step": 22, "inv_row_step": 22,
    },
]

# -------- templates --------
LAYOUT_TMPL = '''package kingdom.smp.inventory;

import kingdom.smp.client.ScreenWidgetTuner;

/**
 * Layout for the Ironhold {pascal} screen reskin. Generated by scripts/gen_gui_reskins.py.
 *
 * <p>Source texture {key}_main.png ({src_w}×{src_h}) is blit-scaled to {main_w}×{main_h} in-game.
 *
 * <p>Widget + label defaults below are baked in from F4-tuned in-game sessions. To update them,
 * tune in-game, hit P, copy values into scripts/gen_gui_reskins.py SPECS for "{key}", regenerate.
 */
public final class {pascal}Layout {{
    private {pascal}Layout() {{}}

    public static final int MAIN_W = {main_w};
    public static final int MAIN_H = {main_h};
    public static final int SRC_W  = {src_w};
    public static final int SRC_H  = {src_h};

    /** Container-side slot positions (in slot-index order). */
    public static final int[][] CONTAINER_SLOTS = new int[][] {{
{container_slots_body}    }};

    /** 3×9 main inventory top-left. */
    public static final int INV_X = {inv_x};
    public static final int INV_Y = {inv_y};
    public static final int HOTBAR_Y = {hotbar_y};
    public static final int INV_COL_STEP = {inv_col_step};
    public static final int INV_ROW_STEP = {inv_row_step};

    /** Static-init block registers widget + label defaults and anchor origins so they apply on first screen open. */
    static {{
{disable_labels_body}{widget_defaults_body}{label_defaults_body}{anchors_body}    }}
}}
'''

SLOTDEBUG_TMPL = '''package kingdom.smp.client;

import kingdom.smp.inventory.{pascal}Layout;

/**
 * Live-tunable layout for the Ironhold {pascal} screen reskin.
 * Generated by scripts/gen_gui_reskins.py.
 *
 * <p>Selection index spans a unified range [0 .. SLOT_COUNT + widgetCount):
 * <ul>
 *   <li>0..CONTAINER_COUNT-1 → container-side slots</li>
 *   <li>CONTAINER_COUNT..SLOT_COUNT-1 → player inventory + hotbar</li>
 *   <li>SLOT_COUNT.. → captured AbstractWidgets routed through {{@link ScreenWidgetTuner}}</li>
 * </ul>
 */
public final class {pascal}SlotDebug {{
    private {pascal}SlotDebug() {{}}

    public static final String SCREEN_KEY = "{key}";
    public static final int CONTAINER_COUNT = {container_count};
    public static final int SLOT_COUNT = {total};

    private static final String[] CONTAINER_LABELS = new String[] {{
{labels_body}    }};

    private static int[][] positions = makeDefaults();
    private static int selected = 0;
    private static boolean editMode = false;
    /** -1 = no drag; 0..SLOT_COUNT-1 = slot index; ≥SLOT_COUNT = widget index + SLOT_COUNT. */
    private static int dragIndex = -1;
    private static int dragOffsetX, dragOffsetY;

    private static int[][] makeDefaults() {{
        int[][] p = new int[SLOT_COUNT][2];
        for (int i = 0; i < CONTAINER_COUNT; i++) {{
            p[i] = new int[]{{ {pascal}Layout.CONTAINER_SLOTS[i][0], {pascal}Layout.CONTAINER_SLOTS[i][1] }};
        }}
        for (int row = 0; row < 3; row++) {{
            for (int col = 0; col < 9; col++) {{
                p[CONTAINER_COUNT + row * 9 + col] = new int[]{{
                    {pascal}Layout.INV_X + col * {pascal}Layout.INV_COL_STEP,
                    {pascal}Layout.INV_Y + row * {pascal}Layout.INV_ROW_STEP
                }};
            }}
        }}
        for (int col = 0; col < 9; col++) {{
            p[CONTAINER_COUNT + 27 + col] = new int[]{{
                {pascal}Layout.INV_X + col * {pascal}Layout.INV_COL_STEP,
                {pascal}Layout.HOTBAR_Y
            }};
        }}
        return p;
    }}

    private static int totalCount() {{
        return SLOT_COUNT
             + ScreenWidgetTuner.widgetCount(SCREEN_KEY)
             + ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY)
             + ScreenWidgetTuner.anchorCount(SCREEN_KEY);
    }}

    /** Returns 0=slot, 1=widget, 2=label, 3=anchor for the unified index. */
    private static int rangeOf(int idx) {{
        int wc = ScreenWidgetTuner.widgetCount(SCREEN_KEY);
        if (idx < SLOT_COUNT) return 0;
        if (idx < SLOT_COUNT + wc) return 1;
        if (idx < SLOT_COUNT + wc + ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY)) return 2;
        return 3;
    }}

    private static int widgetIdx(int idx) {{ return idx - SLOT_COUNT; }}
    private static int labelIdx(int idx)  {{ return idx - SLOT_COUNT - ScreenWidgetTuner.widgetCount(SCREEN_KEY); }}
    private static int anchorIdx(int idx) {{ return idx - SLOT_COUNT - ScreenWidgetTuner.widgetCount(SCREEN_KEY) - ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY); }}

    public static boolean isEditMode() {{ return editMode; }}
    public static void toggleEditMode() {{ editMode = !editMode; }}
    public static int selectedIndex() {{ return selected; }}

    public static void cycleSelection(int delta) {{
        int total = totalCount();
        if (total <= 0) return;
        selected = ((selected + delta) % total + total) % total;
    }}

    public static int[] slotPos(int idx) {{
        if (idx < 0) return new int[]{{0, 0}};
        switch (rangeOf(idx)) {{
            case 0:  return positions[idx];
            case 1:  return ScreenWidgetTuner.widgetPos(SCREEN_KEY, widgetIdx(idx));
            case 2:  return ScreenWidgetTuner.labelPos(SCREEN_KEY, labelIdx(idx));
            default: return ScreenWidgetTuner.anchorPos(SCREEN_KEY, anchorIdx(idx));
        }}
    }}

    public static void nudge(int dx, int dy) {{
        switch (rangeOf(selected)) {{
            case 0 -> {{ positions[selected][0] += dx; positions[selected][1] += dy; }}
            case 1 -> ScreenWidgetTuner.nudgeWidget(SCREEN_KEY, widgetIdx(selected), dx, dy);
            case 2 -> ScreenWidgetTuner.nudgeLabel(SCREEN_KEY, labelIdx(selected), dx, dy);
            default -> ScreenWidgetTuner.nudgeAnchor(SCREEN_KEY, anchorIdx(selected), dx, dy);
        }}
    }}

    public static boolean tryStartDrag(int mx, int my, int leftPos, int topPos) {{
        // 1. Slot hit-boxes (16×16, panel-relative).
        for (int i = 0; i < SLOT_COUNT; i++) {{
            int sx = leftPos + positions[i][0];
            int sy = topPos + positions[i][1];
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {{
                dragIndex = i;
                dragOffsetX = mx - sx;
                dragOffsetY = my - sy;
                selected = i;
                return true;
            }}
        }}
        // 2. Widget hit-boxes (absolute).
        int wi = ScreenWidgetTuner.tryStartDragWidget(SCREEN_KEY, mx, my);
        if (wi >= 0) {{
            int[] abs = ScreenWidgetTuner.widgetAbsPos(SCREEN_KEY, wi);
            dragIndex = SLOT_COUNT + wi;
            dragOffsetX = mx - abs[0];
            dragOffsetY = my - abs[1];
            selected = SLOT_COUNT + wi;
            return true;
        }}
        // 3. Label hit-boxes (synthetic 60×10 around label origin).
        int li = ScreenWidgetTuner.tryStartDragLabel(SCREEN_KEY, mx, my, leftPos, topPos);
        if (li >= 0) {{
            int[] rel = ScreenWidgetTuner.labelPos(SCREEN_KEY, li);
            int absX = leftPos + rel[0];
            int absY = topPos + rel[1];
            int widgetCount = ScreenWidgetTuner.widgetCount(SCREEN_KEY);
            dragIndex = SLOT_COUNT + widgetCount + li;
            dragOffsetX = mx - absX;
            dragOffsetY = my - absY;
            selected = dragIndex;
            return true;
        }}
        // 4. Anchor hit-boxes (synthetic 24×24 around anchor origin).
        int ai = ScreenWidgetTuner.tryStartDragAnchor(SCREEN_KEY, mx, my, leftPos, topPos);
        if (ai >= 0) {{
            int[] rel = ScreenWidgetTuner.anchorPos(SCREEN_KEY, ai);
            int absX = leftPos + rel[0];
            int absY = topPos + rel[1];
            int widgetCount = ScreenWidgetTuner.widgetCount(SCREEN_KEY);
            dragIndex = SLOT_COUNT + widgetCount + ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY) + ai;
            dragOffsetX = mx - absX;
            dragOffsetY = my - absY;
            selected = dragIndex;
            return true;
        }}
        return false;
    }}

    public static boolean isDragging() {{ return dragIndex >= 0; }}

    public static void updateDrag(int mx, int my, int leftPos, int topPos) {{
        if (dragIndex < 0) return;
        int relX = mx - leftPos - dragOffsetX;
        int relY = my - topPos - dragOffsetY;
        switch (rangeOf(dragIndex)) {{
            case 0 -> {{ positions[dragIndex][0] = relX; positions[dragIndex][1] = relY; }}
            case 1 -> ScreenWidgetTuner.setWidgetRelPos(SCREEN_KEY, widgetIdx(dragIndex), relX, relY);
            case 2 -> ScreenWidgetTuner.setLabelRelPos(SCREEN_KEY, labelIdx(dragIndex), relX, relY);
            default -> ScreenWidgetTuner.setAnchorRelPos(SCREEN_KEY, anchorIdx(dragIndex), relX, relY);
        }}
    }}

    public static void endDrag() {{ dragIndex = -1; }}
    public static void resetAll() {{ positions = makeDefaults(); ScreenWidgetTuner.resetScreen(SCREEN_KEY); }}

    private static String slotName(int i) {{
        if (i < CONTAINER_COUNT) return CONTAINER_LABELS[i];
        if (i < CONTAINER_COUNT + 27) return "inv[" + (i - CONTAINER_COUNT) + "]";
        if (i < SLOT_COUNT) return "hotbar[" + (i - CONTAINER_COUNT - 27) + "]";
        int wc = ScreenWidgetTuner.widgetCount(SCREEN_KEY);
        if (i < SLOT_COUNT + wc) return ScreenWidgetTuner.widgetLabel(SCREEN_KEY, widgetIdx(i));
        if (i < SLOT_COUNT + wc + ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY)) return ScreenWidgetTuner.labelName(labelIdx(i));
        return ScreenWidgetTuner.anchorName(SCREEN_KEY, anchorIdx(i));
    }}

    public static String summary() {{
        int[] p = slotPos(selected);
        return String.format("[edit] {pascal_upper} %d (%s) -> (%d, %d)  [total=%d]",
                selected, slotName(selected), p[0], p[1], totalCount());
    }}

    public static String printJava() {{
        StringBuilder sb = new StringBuilder("=== Ironhold {key} layout ===\\n");
        for (int i = 0; i < SLOT_COUNT; i++) {{
            sb.append(String.format("/* %2d %-14s */ {{ %3d, %3d }},%n",
                    i, slotName(i), positions[i][0], positions[i][1]));
        }}
        sb.append(ScreenWidgetTuner.printJava(SCREEN_KEY));
        return sb.toString();
    }}
}}
'''

HANDLER_TMPL = '''package kingdom.smp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import {screen_pkg};
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * F4 / Tab / arrow / drag debug overlay for the Ironhold {pascal} reskin.
 * Generated by scripts/gen_gui_reskins.py.
 */
public final class {pascal}SlotDebugHandler {{
    private {pascal}SlotDebugHandler() {{}}

    private static boolean is{pascal}(Object screen) {{
        return screen instanceof {screen_simple};
    }}

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {{
        if (!is{pascal}(event.getScreen())) return;
        int key = event.getKeyEvent().key();
        boolean shift = (event.getKeyEvent().modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_F4) {{
            {pascal}SlotDebug.toggleEditMode();
            chat("§b[ironhold/{key}] edit mode " + ({pascal}SlotDebug.isEditMode() ? "ON" : "OFF"));
            event.setCanceled(true);
            return;
        }}
        if (!{pascal}SlotDebug.isEditMode()) return;
        int step = shift ? 5 : 1;
        switch (key) {{
            case GLFW.GLFW_KEY_TAB -> {{ {pascal}SlotDebug.cycleSelection(shift ? -1 : 1); chat({pascal}SlotDebug.summary()); }}
            case GLFW.GLFW_KEY_LEFT  -> {{ {pascal}SlotDebug.nudge(-step, 0); chat({pascal}SlotDebug.summary()); }}
            case GLFW.GLFW_KEY_RIGHT -> {{ {pascal}SlotDebug.nudge( step, 0); chat({pascal}SlotDebug.summary()); }}
            case GLFW.GLFW_KEY_UP    -> {{ {pascal}SlotDebug.nudge(0, -step); chat({pascal}SlotDebug.summary()); }}
            case GLFW.GLFW_KEY_DOWN  -> {{ {pascal}SlotDebug.nudge(0,  step); chat({pascal}SlotDebug.summary()); }}
            case GLFW.GLFW_KEY_P -> {{
                String dump = {pascal}SlotDebug.printJava();
                System.out.println(dump);
                for (String line : dump.split("\\n")) chat(line);
            }}
            case GLFW.GLFW_KEY_R -> {{ {pascal}SlotDebug.resetAll(); chat("§e[ironhold/{key}] reset to defaults"); }}
            default -> {{ return; }}
        }}
        event.setCanceled(true);
    }}

    @SubscribeEvent
    public static void onMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {{
        if (!is{pascal}(event.getScreen())) return;
        if (!{pascal}SlotDebug.isEditMode()) return;
        if (event.getMouseButtonEvent().button() != 0) return;
        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        if ({pascal}SlotDebug.tryStartDrag(mx, my, s.getGuiLeft(), s.getGuiTop())) {{
            event.setCanceled(true);
        }}
    }}

    @SubscribeEvent
    public static void onMouseDrag(ScreenEvent.MouseDragged.Pre event) {{
        if (!is{pascal}(event.getScreen())) return;
        if (!{pascal}SlotDebug.isDragging()) return;
        AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) event.getScreen();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        {pascal}SlotDebug.updateDrag(mx, my, s.getGuiLeft(), s.getGuiTop());
        event.setCanceled(true);
    }}

    @SubscribeEvent
    public static void onMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {{
        if (!is{pascal}(event.getScreen())) return;
        if (!{pascal}SlotDebug.isDragging()) return;
        {pascal}SlotDebug.endDrag();
        chat({pascal}SlotDebug.summary());
        event.setCanceled(true);
    }}

    private static void chat(String msg) {{
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA));
    }}
}}
'''

MIXIN_TMPL = '''package kingdom.smp.mixin;

import kingdom.smp.access.SlotAccessor;
import kingdom.smp.Ironhold;
import kingdom.smp.client.{pascal}SlotDebug;
import kingdom.smp.client.ScreenWidgetTuner;
import kingdom.smp.inventory.{pascal}Layout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import {screen_pkg};
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import {menu_pkg};
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reskin of the vanilla {pascal} screen.
 * Generated by scripts/gen_gui_reskins.py — slot positions are rough; drag-tune in-game with F4.
 */
@Mixin({screen_simple}.class)
public abstract class {pascal}ScreenMixin extends AbstractContainerScreen<{menu_simple}> {{

    private static final Identifier MAIN_TEXTURE =
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/{key}/{key}_main.png");

    /** Synthesized constructor — never invoked. */
    protected {pascal}ScreenMixin({menu_simple} menu, Inventory inv, net.minecraft.network.chat.Component title) {{
        super(menu, inv, title);
    }}

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ironhold$resizePanel({menu_simple} menu, Inventory inv,
                                      net.minecraft.network.chat.Component title, CallbackInfo ci) {{
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        acc.ironhold$setImageWidth({pascal}Layout.MAIN_W);
        acc.ironhold$setImageHeight({pascal}Layout.MAIN_H);
        // Labels are NOT force-hidden here — they're tunable via the F4 overlay.
        // To hide a label, drag it off-screen or set its tuned (x, y) to large negatives.
    }}

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ironhold$drawCustomBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {{
        ironhold$applyDebugPositions();
        // Lazy widget capture — only walks children() when the screen instance or widget count changes.
        // Avoids @Inject(method="init") which fails on subclasses that don't redeclare init().
        ScreenWidgetTuner.ensureCaptured({pascal}SlotDebug.SCREEN_KEY, this, this.leftPos, this.topPos);
        ScreenWidgetTuner.applyPositions({pascal}SlotDebug.SCREEN_KEY, this.leftPos, this.topPos);

        // Capture vanilla label positions on first sight; then apply tuned overrides every frame.
        ScreenWidgetTuner.ensureLabelsCaptured({pascal}SlotDebug.SCREEN_KEY, this,
                this.titleLabelX, this.titleLabelY,
                this.inventoryLabelX, this.inventoryLabelY);
        int[] titleXY = ScreenWidgetTuner.labelPos({pascal}SlotDebug.SCREEN_KEY, 0);
        int[] invXY   = ScreenWidgetTuner.labelPos({pascal}SlotDebug.SCREEN_KEY, 1);
        this.titleLabelX = titleXY[0];
        this.titleLabelY = titleXY[1];
        this.inventoryLabelX = invXY[0];
        this.inventoryLabelY = invXY[1];

        int left = this.leftPos;
        int top = this.topPos;

        gfx.fill(0, 0, this.width, this.height, 0xC0101010);

        // Stretch the hi-res source ({pascal}Layout.SRC_W×SRC_H) to in-game MAIN_W×MAIN_H.
        gfx.blit(RenderPipelines.GUI_TEXTURED, MAIN_TEXTURE, left, top, 0f, 0f,
                {pascal}Layout.MAIN_W, {pascal}Layout.MAIN_H,
                {pascal}Layout.SRC_W,  {pascal}Layout.SRC_H,
                {pascal}Layout.SRC_W,  {pascal}Layout.SRC_H);

        if ({pascal}SlotDebug.isEditMode()) {{
            ironhold$drawEditModeOverlay(gfx);
        }}
        ci.cancel();
    }}

    private void ironhold$applyDebugPositions() {{
        int n = Math.min({pascal}SlotDebug.SLOT_COUNT, this.menu.slots.size());
        for (int i = 0; i < n; i++) {{
            int[] xy = {pascal}SlotDebug.slotPos(i);
            Slot slot = this.menu.slots.get(i);
            SlotAccessor acc = (SlotAccessor) slot;
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }}
    }}

    private void ironhold$drawEditModeOverlay(GuiGraphicsExtractor gfx) {{
        gfx.fill(0, 0, this.width, 12, 0xCC000000);
        gfx.text(this.font, "[ironhold edit] " + {pascal}SlotDebug.summary()
                + "  |  F4=off  Tab=cycle  Arrows=nudge  Drag=move  P=print  R=reset",
                4, 2, 0xFF55FFFF, false);

        int left = this.leftPos;
        int top = this.topPos;
        int sel = {pascal}SlotDebug.selectedIndex();
        int[] xy = {pascal}SlotDebug.slotPos(sel);
        int widgetCount = ScreenWidgetTuner.widgetCount({pascal}SlotDebug.SCREEN_KEY);
        if (sel < {pascal}SlotDebug.SLOT_COUNT) {{
            // Slot — 16×16 cyan box.
            ironhold$drawOutline(gfx, left + xy[0] - 1, top + xy[1] - 1, 18, 18, 0xFF55FFFF);
        }} else if (sel < {pascal}SlotDebug.SLOT_COUNT + widgetCount) {{
            // Widget — orange box from live AbstractWidget bbox.
            int wi = sel - {pascal}SlotDebug.SLOT_COUNT;
            int[] box = ScreenWidgetTuner.widgetBox({pascal}SlotDebug.SCREEN_KEY, wi);
            ironhold$drawOutline(gfx, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, 0xFFFFAA00);
        }} else if (sel < {pascal}SlotDebug.SLOT_COUNT + widgetCount + ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY)) {{
            // Label — magenta box (synthetic 60×12 around label origin).
            int li = sel - {pascal}SlotDebug.SLOT_COUNT - widgetCount;
            int[] box = ScreenWidgetTuner.labelBox({pascal}SlotDebug.SCREEN_KEY, li, left, top);
            ironhold$drawOutline(gfx, box[0], box[1], box[2], box[3], 0xFFFF55FF);
        }} else {{
            // Anchor — yellow box (synthetic 24×24 around anchor origin).
            int ai = sel - {pascal}SlotDebug.SLOT_COUNT - widgetCount - ScreenWidgetTuner.labelCount({pascal}SlotDebug.SCREEN_KEY);
            int[] box = ScreenWidgetTuner.anchorBox({pascal}SlotDebug.SCREEN_KEY, ai, left, top);
            ironhold$drawOutline(gfx, box[0], box[1], box[2], box[3], 0xFFFFFF00);
        }}
    }}

    private static void ironhold$drawOutline(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {{
        gfx.fill(x,         y,         x + w, y + 1, color);
        gfx.fill(x,         y + h - 1, x + w, y + h, color);
        gfx.fill(x,         y,         x + 1, y + h, color);
        gfx.fill(x + w - 1, y,         x + w, y + h, color);
    }}
}}
'''

def render_container_slots(slots):
    lines = []
    for label, x, y in slots:
        lines.append(f"        {{ {x:>3}, {y:>3} }},  // {label}\n")
    return "".join(lines)

def render_widget_defaults(key, widget_defaults):
    if not widget_defaults:
        return "        // (no widget defaults baked in yet — tune in-game with F4 then bake)\n"
    lines = []
    for label, (x, y) in widget_defaults.items():
        lines.append(f'        ScreenWidgetTuner.setDefaultWidgetPos("{key}", "{label}", {x:>3}, {y:>3});\n')
    return "".join(lines)

def render_label_defaults(key, label_defaults):
    if not label_defaults:
        return ""
    lines = []
    name_to_idx = {"title_label": 0, "inv_label": 1}
    for name, (x, y) in label_defaults.items():
        idx = name_to_idx.get(name)
        if idx is None:
            raise ValueError(f"unknown label name {name!r} (must be 'title_label' or 'inv_label')")
        lines.append(f'        ScreenWidgetTuner.setDefaultLabelPos("{key}", {idx}, {x:>3}, {y:>3});  // {name}\n')
    return "".join(lines)

def render_disable_labels(key, disable):
    if not disable:
        return ""
    return f'        ScreenWidgetTuner.disableLabels("{key}");\n'

def render_anchors(key, anchors):
    """anchors is a list of (name, origX, origY) tuples — vanilla positions."""
    if not anchors:
        return ""
    lines = []
    for name, x, y in anchors:
        lines.append(f'        ScreenWidgetTuner.registerAnchor("{key}", "{name}", {x:>3}, {y:>3});\n')
    return "".join(lines)

def render_labels(slots):
    lines = []
    for label, _, _ in slots:
        lines.append(f"        \"{label}\",\n")
    return "".join(lines)

def class_simple(fqn):
    return fqn.rsplit(".", 1)[1]

def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    # BlastFurnaceScreenMixin is hand-edited to target AbstractFurnaceScreen
    # (extractBackground is declared on the parent, not BlastFurnaceScreen itself).
    # Do not regenerate it — manual edits would be lost.
    if path.name in ("BlastFurnaceScreenMixin.java", "StonecutterScreenMixin.java", "BeaconScreenMixin.java"):
        print(f"  SKIPPED (hand-edited) {path.relative_to(ROOT.parent.parent.parent.parent)}")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    print(f"  wrote {path.relative_to(ROOT.parent.parent.parent.parent)}")

def gen():
    for spec in SPECS:
        pascal = spec["pascal"]
        key = spec["key"]
        screen_simple = class_simple(spec["screen_pkg"])
        menu_simple = class_simple(spec["menu_pkg"])
        cslots = spec["container_slots"]
        ccount = len(cslots)
        total = ccount + 36
        inv_x, inv_y = spec["inv_origin"]
        ctx = dict(
            pascal=pascal, pascal_upper=pascal.upper(), key=key,
            main_w=spec["main_w"], main_h=spec["main_h"],
            src_w=spec["src_w"], src_h=spec["src_h"],
            inv_x=inv_x, inv_y=inv_y, hotbar_y=spec["hotbar_y"],
            inv_col_step=spec["inv_col_step"], inv_row_step=spec["inv_row_step"],
            container_slots_body=render_container_slots(cslots),
            widget_defaults_body=render_widget_defaults(key, spec.get("widget_defaults", {})),
            label_defaults_body=render_label_defaults(key, spec.get("label_defaults", {})),
            disable_labels_body=render_disable_labels(key, spec.get("disable_labels", False)),
            anchors_body=render_anchors(key, spec.get("anchors", [])),
            labels_body=render_labels(cslots),
            container_count=ccount, total=total,
            screen_pkg=spec["screen_pkg"], menu_pkg=spec["menu_pkg"],
            screen_simple=screen_simple, menu_simple=menu_simple,
        )
        print(f"=== {pascal} ===")
        write(ROOT / "inventory" / f"{pascal}Layout.java", LAYOUT_TMPL.format(**ctx))
        write(ROOT / "client"    / f"{pascal}SlotDebug.java", SLOTDEBUG_TMPL.format(**ctx))
        write(ROOT / "client"    / f"{pascal}SlotDebugHandler.java", HANDLER_TMPL.format(**ctx))
        write(ROOT / "mixin"     / f"{pascal}ScreenMixin.java", MIXIN_TMPL.format(**ctx))

if __name__ == "__main__":
    gen()
    print("done.")
