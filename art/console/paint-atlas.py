#!/usr/bin/env python3
"""
Procedurally paint kings_console.png from the locked palette.

This is *not* hand-painted art — it's a code-drawn approximation of the
reference style. Use it as a baseline that ships now; replace individual
sprites with real pixel art over time.

Outputs:
    ../../src/main/resources/assets/ironhold/textures/gui/console/kings_console.png

Atlas layout matches atlas-spec.md exactly. If you change a slot here,
update atlas-spec.md AND ConsoleAtlas.java.
"""

import os
from PIL import Image, ImageDraw

# ── Palette ──────────────────────────────────────────────────────────────
SHADOW       = (42, 42, 42, 255)
DARK_MID     = (63, 63, 63, 255)
STONE_DARK   = (92, 92, 92, 255)
STONE_MID    = (142, 142, 142, 255)
STONE_LIGHT  = (191, 191, 191, 255)
HIGHLIGHT    = (230, 230, 230, 255)
GOLD_DARK    = (184, 134, 11, 255)
GOLD_MID     = (255, 204, 68, 255)
GOLD_LIGHT   = (255, 238, 153, 255)
PARCH        = (200, 176, 122, 255)
PARCH_EDGE   = (92, 74, 43, 255)
PARCH_TEXT   = (58, 45, 20, 255)
OK_GREEN     = (85, 221, 85, 255)
BAD_RED      = (204, 51, 51, 255)
WAX_RED      = (139, 31, 31, 255)
ALLY_BLUE    = (68, 119, 204, 255)
TRANSPARENT  = (0, 0, 0, 0)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def rect(img, x0, y0, x1, y1, c):
    """Filled rectangle, inclusive corners."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(img, x, y, c)


def outline_rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        px(img, x, y0, c)
        px(img, x, y1, c)
    for y in range(y0, y1 + 1):
        px(img, x0, y, c)
        px(img, x1, y, c)


# ── Sprite painters ──────────────────────────────────────────────────────

def paint_icon_coin(img, x, y):
    # 16x16 gold coin with crown imprint
    rect(img, x + 2, y,     x + 13, y,      SHADOW)
    rect(img, x,     y + 2, x,      y + 13, SHADOW)
    rect(img, x + 15, y + 2, x + 15, y + 13, SHADOW)
    rect(img, x + 2, y + 15, x + 13, y + 15, SHADOW)
    px(img, x + 1, y + 1, SHADOW); px(img, x + 14, y + 1, SHADOW)
    px(img, x + 1, y + 14, SHADOW); px(img, x + 14, y + 14, SHADOW)
    # body
    rect(img, x + 2, y + 1, x + 13, y + 1, GOLD_DARK)
    rect(img, x + 1, y + 2, x + 14, y + 13, GOLD_MID)
    rect(img, x + 2, y + 14, x + 13, y + 14, GOLD_DARK)
    # specular
    rect(img, x + 3, y + 3, x + 5, y + 4, GOLD_LIGHT)
    px(img, x + 6, y + 3, GOLD_LIGHT)
    # crown imprint (5x3 darker)
    rect(img, x + 5, y + 8, x + 10, y + 9, GOLD_DARK)
    px(img, x + 5, y + 7, GOLD_DARK)
    px(img, x + 7, y + 7, GOLD_DARK)
    px(img, x + 9, y + 7, GOLD_DARK)


def paint_icon_gold_ingot(img, x, y):
    # 16x16 gold ingot, parallelogram
    # Top face
    rect(img, x + 3, y + 4, x + 13, y + 5, GOLD_LIGHT)
    rect(img, x + 3, y + 6, x + 13, y + 8, GOLD_MID)
    # Bottom taper
    rect(img, x + 4, y + 9, x + 12, y + 10, GOLD_DARK)
    # Outline
    for xi in range(x + 3, x + 14):
        px(img, xi, y + 3, SHADOW)
        px(img, xi, y + 11, SHADOW)
    for yi in range(y + 4, y + 11):
        px(img, x + 2, yi, SHADOW)
    for yi in range(y + 4, y + 11):
        px(img, x + 14, yi, SHADOW)
    # specular
    px(img, x + 4, y + 4, HIGHLIGHT)
    px(img, x + 5, y + 4, HIGHLIGHT)


def paint_icon_emerald(img, x, y):
    # 16x16 hexagonal emerald
    poly = [
        (x + 7, y + 2), (x + 8, y + 2),
        (x + 6, y + 3), (x + 9, y + 3),
        (x + 5, y + 4), (x + 10, y + 4),
        (x + 4, y + 5), (x + 11, y + 5),
        (x + 4, y + 10), (x + 11, y + 10),
        (x + 5, y + 11), (x + 10, y + 11),
        (x + 6, y + 12), (x + 9, y + 12),
        (x + 7, y + 13), (x + 8, y + 13),
    ]
    # outline
    for cx, cy in poly:
        px(img, cx, cy, SHADOW)
    # fill
    for yi in range(y + 3, y + 13):
        for xi in range(x + 5, x + 11):
            px(img, xi, yi, OK_GREEN)
    # darken for facets
    rect(img, x + 5, y + 8, x + 10, y + 12, DARK_MID)
    rect(img, x + 5, y + 8, x + 7, y + 12, OK_GREEN)
    # highlight
    rect(img, x + 6, y + 4, x + 7, y + 6, HIGHLIGHT)


def paint_icon_land(img, x, y):
    # 16x16 grass-block face
    # Grass top
    rect(img, x + 1, y + 1, x + 14, y + 5, OK_GREEN)
    # grass tufts
    px(img, x + 3, y, OK_GREEN); px(img, x + 7, y, OK_GREEN); px(img, x + 11, y, OK_GREEN)
    # Dirt body
    rect(img, x + 1, y + 6, x + 14, y + 14, PARCH_EDGE)
    # dirt flecks
    px(img, x + 3, y + 8, PARCH_TEXT); px(img, x + 9, y + 9, PARCH_TEXT)
    px(img, x + 5, y + 11, PARCH_TEXT); px(img, x + 12, y + 12, PARCH_TEXT)
    # Outline
    outline_rect(img, x + 1, y, x + 14, y + 14, SHADOW)


def paint_icon_crown(img, x, y):
    # 16x16 royal crown
    # Three peaks
    px(img, x + 3, y + 3, GOLD_MID); px(img, x + 7, y + 3, GOLD_MID); px(img, x + 11, y + 3, GOLD_MID)
    rect(img, x + 2, y + 4, x + 4, y + 6, GOLD_MID)
    rect(img, x + 6, y + 4, x + 8, y + 6, GOLD_MID)
    rect(img, x + 10, y + 4, x + 12, y + 6, GOLD_MID)
    # peak gems
    px(img, x + 3, y + 4, GOLD_LIGHT)
    px(img, x + 7, y + 4, GOLD_LIGHT)
    px(img, x + 11, y + 4, GOLD_LIGHT)
    # band
    rect(img, x + 2, y + 7, x + 12, y + 10, GOLD_MID)
    rect(img, x + 2, y + 7, x + 12, y + 7, GOLD_LIGHT)
    rect(img, x + 2, y + 10, x + 12, y + 10, GOLD_DARK)
    # band gems
    px(img, x + 5, y + 9, WAX_RED); px(img, x + 9, y + 9, ALLY_BLUE)
    # Outline
    outline_rect(img, x + 2, y + 3, x + 12, y + 10, SHADOW)


def paint_icon_gate(img, x, y):
    # 16x16 portcullis
    # Arch top
    rect(img, x + 4, y + 1, x + 11, y + 2, STONE_DARK)
    rect(img, x + 3, y + 3, x + 12, y + 3, STONE_MID)
    # vertical bars (three)
    rect(img, x + 4, y + 4, x + 5, y + 13, STONE_MID)
    rect(img, x + 7, y + 4, x + 8, y + 13, STONE_MID)
    rect(img, x + 10, y + 4, x + 11, y + 13, STONE_MID)
    # bar shadows
    rect(img, x + 5, y + 4, x + 5, y + 13, STONE_DARK)
    rect(img, x + 8, y + 4, x + 8, y + 13, STONE_DARK)
    rect(img, x + 11, y + 4, x + 11, y + 13, STONE_DARK)
    # crossbars
    rect(img, x + 3, y + 7, x + 12, y + 7, STONE_DARK)
    rect(img, x + 3, y + 11, x + 12, y + 11, STONE_DARK)
    # spike tips
    px(img, x + 4, y + 14, STONE_DARK); px(img, x + 7, y + 14, STONE_DARK); px(img, x + 10, y + 14, STONE_DARK)
    # outline
    outline_rect(img, x + 3, y + 1, x + 12, y + 13, SHADOW)


def paint_icon_portal(img, x, y):
    # 16x16 ender ring
    # outer ring
    for xi, yi in [(7,2),(8,2),(5,3),(10,3),(3,4),(12,4),(2,5),(13,5),
                   (2,6),(13,6),(2,7),(13,7),(2,8),(13,8),(2,9),(13,9),
                   (2,10),(13,10),(3,11),(12,11),(5,12),(10,12),(7,13),(8,13)]:
        px(img, x + xi, y + yi, ALLY_BLUE)
    # inner ring (purple-ish — closest is wax_red? use ally_blue with darker)
    for xi, yi in [(6,3),(9,3),(4,4),(11,4),(3,5),(12,5),(3,6),(12,6),
                   (3,7),(12,7),(3,8),(12,8),(3,9),(12,9),(3,10),(12,10),
                   (4,11),(11,11),(6,12),(9,12)]:
        px(img, x + xi, y + yi, SHADOW)
    # void centre with starpoints
    rect(img, x + 4, y + 5, x + 11, y + 10, SHADOW)
    px(img, x + 6, y + 7, GOLD_LIGHT)
    px(img, x + 9, y + 8, GOLD_LIGHT)
    px(img, x + 5, y + 9, HIGHLIGHT)


def paint_icon_wax_seal(img, x, y):
    # 16x16 small wax seal w/ crown
    # disc
    for r_y in range(2, 14):
        for r_x in range(2, 14):
            dx = r_x - 7.5
            dy = r_y - 7.5
            if dx * dx + dy * dy <= 36:
                px(img, x + r_x, y + r_y, WAX_RED)
            if 36 < dx * dx + dy * dy <= 42:
                px(img, x + r_x, y + r_y, PARCH_EDGE)
    # crown imprint
    rect(img, x + 5, y + 8, x + 10, y + 9, PARCH_EDGE)
    px(img, x + 5, y + 7, PARCH_EDGE)
    px(img, x + 7, y + 7, PARCH_EDGE)
    px(img, x + 9, y + 7, PARCH_EDGE)
    # specular
    px(img, x + 4, y + 4, GOLD_MID); px(img, x + 5, y + 4, GOLD_MID)
    # drips
    px(img, x + 3, y + 13, WAX_RED)
    px(img, x + 12, y + 13, WAX_RED)


def paint_icon_decree_stamp(img, x, y):
    # 16x16 stamp
    # handle
    rect(img, x + 6, y + 1, x + 9, y + 7, PARCH_EDGE)
    rect(img, x + 6, y + 1, x + 6, y + 7, PARCH_TEXT)
    rect(img, x + 7, y + 2, x + 7, y + 6, PARCH)
    # brass band
    rect(img, x + 5, y + 8, x + 10, y + 9, GOLD_MID)
    rect(img, x + 5, y + 8, x + 10, y + 8, GOLD_LIGHT)
    # base
    rect(img, x + 3, y + 10, x + 12, y + 13, DARK_MID)
    rect(img, x + 3, y + 10, x + 12, y + 10, STONE_DARK)
    # wax drop
    px(img, x + 7, y + 14, WAX_RED); px(img, x + 8, y + 14, WAX_RED)
    # outline
    outline_rect(img, x + 3, y + 1, x + 12, y + 13, SHADOW)


# ── Stepper button (12x12) — gold-trimmed iron face ──────────────────────
def paint_stepper(img, x, y, state):
    """state: 'idle', 'hover', 'pressed', 'disabled'"""
    if state == 'idle':
        body, hi, lo = STONE_DARK, STONE_LIGHT, DARK_MID
        rim_top = STONE_MID
        spec = HIGHLIGHT
    elif state == 'hover':
        body, hi, lo = STONE_MID, HIGHLIGHT, STONE_DARK
        rim_top = STONE_LIGHT
        spec = HIGHLIGHT
    elif state == 'pressed':
        body, hi, lo = DARK_MID, DARK_MID, STONE_LIGHT
        rim_top = STONE_DARK
        spec = None
    else:  # disabled
        body, hi, lo = DARK_MID, STONE_DARK, SHADOW
        rim_top = STONE_DARK
        spec = None

    # outer black rim
    outline_rect(img, x, y, x + 11, y + 11, SHADOW)
    # iron sub-rim (1 px inset)
    outline_rect(img, x + 1, y + 1, x + 10, y + 10, rim_top)
    # body
    rect(img, x + 2, y + 2, x + 9, y + 9, body)
    # bevel
    if state == 'pressed':
        rect(img, x + 2, y + 2, x + 9, y + 2, lo)
        rect(img, x + 2, y + 2, x + 2, y + 9, lo)
        rect(img, x + 3, y + 9, x + 9, y + 9, hi)
        rect(img, x + 9, y + 3, x + 9, y + 9, hi)
    else:
        rect(img, x + 2, y + 2, x + 9, y + 2, hi)
        rect(img, x + 2, y + 2, x + 2, y + 9, hi)
        rect(img, x + 3, y + 9, x + 9, y + 9, lo)
        rect(img, x + 9, y + 3, x + 9, y + 9, lo)
    if spec is not None:
        px(img, x + 3, y + 3, spec)
    # gold corner studs
    if state != 'disabled':
        px(img, x + 1, y + 1, GOLD_DARK)
        px(img, x + 10, y + 1, GOLD_DARK)
        px(img, x + 1, y + 10, GOLD_DARK)
        px(img, x + 10, y + 10, GOLD_DARK)


# ── Toggle button frame (60x16) — heraldic shield strip ──────────────────
def paint_toggle(img, x, y, state):
    """state: 'off_idle', 'off_hover', 'on_idle', 'on_hover'"""
    on = 'on' in state
    hover = 'hover' in state
    if on:
        body = (45, 75, 35, 255) if not hover else (60, 95, 45, 255)
        top_hi = OK_GREEN
        bot_lo = (30, 50, 25, 255)
    else:
        body = STONE_DARK if not hover else STONE_MID
        top_hi = STONE_LIGHT if hover else STONE_MID
        bot_lo = SHADOW
    border = OK_GREEN if on else SHADOW

    # body (60x14, with chamfered top corners — heraldic shield top)
    rect(img, x + 1, y + 1, x + 58, y + 14, body)
    # chamfer top corners (peaked)
    px(img, x, y + 1, TRANSPARENT); px(img, x + 1, y, TRANSPARENT)
    px(img, x + 59, y + 1, TRANSPARENT); px(img, x + 58, y, TRANSPARENT)
    # bottom corners (slight chamfer)
    px(img, x, y + 14, TRANSPARENT)
    px(img, x + 59, y + 14, TRANSPARENT)

    # top gilt strap (gold rim along top)
    rect(img, x + 2, y + 1, x + 57, y + 1, GOLD_DARK if on else top_hi)
    rect(img, x + 2, y + 2, x + 57, y + 2, top_hi)
    # bottom shadow
    rect(img, x + 1, y + 13, x + 58, y + 13, bot_lo)
    # check zone (right 10 px) — sunken square
    check_bg = (30, 50, 25, 255) if on else (35, 35, 35, 255)
    rect(img, x + 48, y + 4, x + 57, y + 12, check_bg)
    outline_rect(img, x + 48, y + 4, x + 57, y + 12, SHADOW)
    px(img, x + 48, y + 4, OK_GREEN if on else STONE_DARK)
    px(img, x + 57, y + 4, OK_GREEN if on else STONE_DARK)
    # outer outline (chamfered silhouette)
    rect(img, x + 2, y, x + 57, y, border)
    rect(img, x + 1, y + 15, x + 58, y + 15, border)
    rect(img, x, y + 2, x, y + 13, border)
    rect(img, x + 59, y + 2, x + 59, y + 13, border)
    # inner step from chamfer
    px(img, x + 1, y + 1, border)
    px(img, x + 58, y + 1, border)
    # gold corner studs for on-state
    if on:
        px(img, x + 2, y + 2, GOLD_LIGHT)
        px(img, x + 57, y + 2, GOLD_LIGHT)


# ── 9-slice stone frame (24x24 in a 32x32 slot) ──────────────────────────
def paint_outer_frame(img, x, y):
    """Heavy iron-bound stone frame, 24x24 9-slice w/ 8px corners.
    The corner blocks are richly detailed; the edges are layered bevels
    that tile cleanly when stretched."""
    # ── Edge layers (drawn first, corners overwrite) ────────────────
    # Top
    rect(img, x, y,     x + 23, y,     SHADOW)
    rect(img, x, y + 1, x + 23, y + 1, STONE_DARK)
    rect(img, x, y + 2, x + 23, y + 2, STONE_MID)
    rect(img, x, y + 3, x + 23, y + 3, STONE_LIGHT)
    rect(img, x, y + 4, x + 23, y + 4, STONE_MID)
    rect(img, x, y + 5, x + 23, y + 5, STONE_DARK)
    rect(img, x, y + 6, x + 23, y + 6, SHADOW)
    rect(img, x, y + 7, x + 23, y + 7, DARK_MID)
    # Bottom (mirror)
    rect(img, x, y + 16, x + 23, y + 16, DARK_MID)
    rect(img, x, y + 17, x + 23, y + 17, SHADOW)
    rect(img, x, y + 18, x + 23, y + 18, STONE_DARK)
    rect(img, x, y + 19, x + 23, y + 19, STONE_MID)
    rect(img, x, y + 20, x + 23, y + 20, STONE_LIGHT)
    rect(img, x, y + 21, x + 23, y + 21, STONE_MID)
    rect(img, x, y + 22, x + 23, y + 22, STONE_DARK)
    rect(img, x, y + 23, x + 23, y + 23, SHADOW)
    # Left
    rect(img, x,     y, x,     y + 23, SHADOW)
    rect(img, x + 1, y, x + 1, y + 23, STONE_DARK)
    rect(img, x + 2, y, x + 2, y + 23, STONE_MID)
    rect(img, x + 3, y, x + 3, y + 23, STONE_LIGHT)
    rect(img, x + 4, y, x + 4, y + 23, STONE_MID)
    rect(img, x + 5, y, x + 5, y + 23, STONE_DARK)
    rect(img, x + 6, y, x + 6, y + 23, SHADOW)
    rect(img, x + 7, y, x + 7, y + 23, DARK_MID)
    # Right (mirror)
    rect(img, x + 16, y, x + 16, y + 23, DARK_MID)
    rect(img, x + 17, y, x + 17, y + 23, SHADOW)
    rect(img, x + 18, y, x + 18, y + 23, STONE_DARK)
    rect(img, x + 19, y, x + 19, y + 23, STONE_MID)
    rect(img, x + 20, y, x + 20, y + 23, STONE_LIGHT)
    rect(img, x + 21, y, x + 21, y + 23, STONE_MID)
    rect(img, x + 22, y, x + 22, y + 23, STONE_DARK)
    rect(img, x + 23, y, x + 23, y + 23, SHADOW)
    # Center fill (visible only at sub-panel gaps)
    rect(img, x + 8, y + 8, x + 15, y + 15, DARK_MID)

    # ── Corner brackets (overwrite edges) ───────────────────────────
    # Each corner is 8x8 with a single bold rivet on a dark iron bracket
    for cx, cy in [(x, y), (x + 16, y), (x, y + 16), (x + 16, y + 16)]:
        # Black outer rim (full 8x8 outline)
        outline_rect(img, cx, cy, cx + 7, cy + 7, SHADOW)
        # Dark iron bracket fill (1 px in)
        rect(img, cx + 1, cy + 1, cx + 6, cy + 6, DARK_MID)
        # Inner stone face (where the rivet sits)
        rect(img, cx + 2, cy + 2, cx + 5, cy + 5, STONE_DARK)
        # Big gold rivet — 3x3 with darker outline + bright center
        rect(img, cx + 2, cy + 2, cx + 4, cy + 4, GOLD_DARK)
        rect(img, cx + 3, cy + 3, cx + 3, cy + 3, GOLD_LIGHT)
        px(img, cx + 2, cy + 2, GOLD_MID)
        # Highlight pip on the bracket
        px(img, cx + 1, cy + 1, STONE_LIGHT)
        # Shadow pip on the opposite corner
        px(img, cx + 6, cy + 6, SHADOW)


def paint_inner_box(img, x, y):
    """Iron-rim inset panel, 24x24 9-slice w/ 8px corners.
    Black outer rim, steel inner rim, dark recessed body."""
    # Recessed body
    rect(img, x + 2, y + 2, x + 21, y + 21, DARK_MID)
    # Outer black rim
    outline_rect(img, x, y, x + 23, y + 23, SHADOW)
    # Steel inner rim with bevel (top+left lighter, bottom+right darker)
    rect(img, x + 1, y + 1, x + 22, y + 1, STONE_MID)
    rect(img, x + 1, y + 1, x + 1, y + 22, STONE_MID)
    rect(img, x + 1, y + 22, x + 22, y + 22, STONE_DARK)
    rect(img, x + 22, y + 1, x + 22, y + 22, STONE_DARK)
    # Top inner highlight
    rect(img, x + 2, y + 2, x + 21, y + 2, STONE_DARK)
    # Bottom inner highlight (subtle)
    rect(img, x + 2, y + 21, x + 21, y + 21, SHADOW)
    # Corner pips (light steel)
    px(img, x + 1, y + 1, STONE_LIGHT)
    px(img, x + 22, y + 1, STONE_LIGHT)
    px(img, x + 1, y + 22, STONE_DARK)
    px(img, x + 22, y + 22, STONE_DARK)


def paint_title_bar(img, x, y):
    """Title plaque, 24x24 9-slice w/ 8px corners.
    Mostly dark body with thin gold edges — the dark center has to dominate
    so the runtime title text reads clearly on top."""
    # Body
    rect(img, x, y, x + 23, y + 23, DARK_MID)
    # Outer black border
    outline_rect(img, x, y, x + 23, y + 23, SHADOW)
    # Top gilt edge (1px gold)
    rect(img, x + 1, y + 1, x + 22, y + 1, GOLD_MID)
    # Bottom gilt edge (1px gold)
    rect(img, x + 1, y + 22, x + 22, y + 22, GOLD_DARK)
    # Side rims (subtle steel)
    rect(img, x + 1, y + 2, x + 1, y + 21, STONE_DARK)
    rect(img, x + 22, y + 2, x + 22, y + 21, STONE_DARK)


def paint_parchment(img, x, y):
    """Aged-scroll parchment 9-slice, 24×24 with 8×8 corners.
    Detail (specks, stains) must live inside the fixed 8×8 corner
    regions only — anything in the middle band tiles into stripes
    when the 9-slice stretches.

    Edges (top, bottom, left, right) tile along ONE axis, so detail
    that runs perpendicular to the stretch axis is safe."""
    # Slightly aged base body
    parch_dim = (188, 162, 108, 255)   # warmer, less yellow
    parch_warm = PARCH                 # standard parchment
    rect(img, x, y, x + 23, y + 23, parch_warm)

    # ── Outer dark border ──────────────────────────────────────
    rect(img, x, y, x + 23, y, PARCH_EDGE)
    rect(img, x, y + 23, x + 23, y + 23, PARCH_EDGE)
    rect(img, x, y, x, y + 23, PARCH_EDGE)
    rect(img, x + 23, y, x + 23, y + 23, PARCH_EDGE)

    # ── Inner soft shadow (2-px gradient on every edge) ────────
    # row 1 (just inside the dark border)
    rect(img, x + 1, y + 1, x + 22, y + 1, parch_dim)
    rect(img, x + 1, y + 22, x + 22, y + 22, parch_dim)
    rect(img, x + 1, y + 1, x + 1, y + 22, parch_dim)
    rect(img, x + 22, y + 1, x + 22, y + 22, parch_dim)
    # row 2 (subtle fade) — only on top and bottom (vertical stretch tiles)
    # left/right vertical fades are OK because they parallel the vertical stretch
    rect(img, x + 2, y + 2, x + 21, y + 2, (196, 172, 118, 255))
    rect(img, x + 2, y + 21, x + 21, y + 21, (196, 172, 118, 255))

    # ── Corner aging — burned/stained nicks (inside fixed corners) ──
    # Top-left
    px(img, x + 2, y + 2, PARCH_EDGE)
    px(img, x + 3, y + 3, PARCH_EDGE)
    px(img, x + 4, y + 2, parch_dim)
    px(img, x + 2, y + 4, parch_dim)
    # Top-right
    px(img, x + 21, y + 2, PARCH_EDGE)
    px(img, x + 20, y + 3, PARCH_EDGE)
    px(img, x + 19, y + 2, parch_dim)
    px(img, x + 21, y + 4, parch_dim)
    # Bottom-left
    px(img, x + 2, y + 21, PARCH_EDGE)
    px(img, x + 3, y + 20, PARCH_EDGE)
    px(img, x + 4, y + 21, parch_dim)
    px(img, x + 2, y + 19, parch_dim)
    # Bottom-right
    px(img, x + 21, y + 21, PARCH_EDGE)
    px(img, x + 20, y + 20, PARCH_EDGE)
    px(img, x + 19, y + 21, parch_dim)
    px(img, x + 21, y + 19, parch_dim)

    # ── Aged stain blotches in each corner (won't stretch) ──────
    # Top-left small blotch
    px(img, x + 5, y + 4, parch_dim)
    px(img, x + 4, y + 5, parch_dim)
    px(img, x + 6, y + 6, parch_dim)
    # Top-right
    px(img, x + 18, y + 4, parch_dim)
    px(img, x + 17, y + 5, parch_dim)
    px(img, x + 19, y + 6, parch_dim)
    # Bottom-left
    px(img, x + 5, y + 19, parch_dim)
    px(img, x + 4, y + 18, parch_dim)
    px(img, x + 6, y + 17, parch_dim)
    # Bottom-right
    px(img, x + 18, y + 19, parch_dim)
    px(img, x + 17, y + 18, parch_dim)
    px(img, x + 19, y + 17, parch_dim)


# ── Banner ribbon (96x20) ─────────────────────────────────────────────────
def paint_banner_ribbon(img, x, y):
    """Heraldic banner: red body w/ deep top highlight, dark bottom shadow,
    gold-trimmed edges, V-cut ends, gold pip studs near each end."""
    # Main body
    rect(img, x + 4, y + 2, x + 91, y + 17, WAX_RED)
    # Top highlight (red + brighter red blend — use ally_blue for highlight? no, just lighter wax)
    # Lighter wax = approximate by mixing with parchment... we have wax_red (139,31,31).
    # Brighter red not in palette — use GOLD_DARK as warm highlight
    rect(img, x + 4, y + 3, x + 91, y + 3, (170, 50, 50, 255))  # almost-palette red highlight
    # Bottom shadow (deeper red) — use a near-palette darker tone
    rect(img, x + 4, y + 16, x + 91, y + 16, (90, 18, 18, 255))
    # Gold rim (top + bottom)
    rect(img, x + 4, y + 1, x + 91, y + 1, GOLD_MID)
    rect(img, x + 4, y + 2, x + 91, y + 2, GOLD_LIGHT)
    rect(img, x + 4, y + 17, x + 91, y + 17, GOLD_DARK)
    # Outer black outline
    rect(img, x + 4, y, x + 91, y, SHADOW)
    rect(img, x + 4, y + 18, x + 91, y + 18, SHADOW)
    # Left V notch (3 stepped triangles)
    for i in range(4):
        rect(img, x, y + 5 + i, x + 3 + i, y + 5 + i, WAX_RED)
        rect(img, x, y + 14 - i, x + 3 + i, y + 14 - i, WAX_RED)
        # outline on the notch edge
        px(img, x + i, y + 5 + i, SHADOW)
        px(img, x + i, y + 14 - i, SHADOW)
    # Right V notch (mirror)
    for i in range(4):
        rect(img, x + 92 - i, y + 5 + i, x + 95, y + 5 + i, WAX_RED)
        rect(img, x + 92 - i, y + 14 - i, x + 95, y + 14 - i, WAX_RED)
        px(img, x + 95 - i, y + 5 + i, SHADOW)
        px(img, x + 95 - i, y + 14 - i, SHADOW)
    # Gold pip studs at each end
    for sx in (x + 7, x + 87):
        rect(img, sx, y + 8, sx + 1, y + 11, GOLD_DARK)
        px(img, sx, y + 8, GOLD_LIGHT)
        px(img, sx + 1, y + 8, GOLD_MID)


def paint_burden_bar_bg(img, x, y):
    # Recessed slot 96x20 — darker rectangle inset 2px from a thin iron rim
    rect(img, x, y, x + 95, y + 19, DARK_MID)
    # inset
    rect(img, x + 2, y + 2, x + 93, y + 17, SHADOW)
    # bevel
    rect(img, x, y, x + 95, y, STONE_LIGHT)
    rect(img, x, y, x, y + 19, STONE_LIGHT)
    rect(img, x, y + 19, x + 95, y + 19, STONE_DARK)
    rect(img, x + 95, y, x + 95, y + 19, STONE_DARK)


# ── Wax seal large (32x32) ────────────────────────────────────────────────
def paint_wax_seal_decree(img, x, y):
    cx, cy = x + 15, y + 11
    # disc
    for ry in range(-10, 11):
        for rx in range(-10, 11):
            d = rx * rx + ry * ry
            if d <= 100:
                px(img, cx + rx, cy + ry, WAX_RED)
            elif d <= 121:
                px(img, cx + rx, cy + ry, PARCH_EDGE)
    # crown imprint
    rect(img, cx - 3, cy - 1, cx + 3, cy + 1, PARCH_EDGE)
    px(img, cx - 3, cy - 2, PARCH_EDGE)
    px(img, cx - 1, cy - 3, PARCH_EDGE)
    px(img, cx + 1, cy - 3, PARCH_EDGE)
    px(img, cx + 3, cy - 2, PARCH_EDGE)
    # specular
    px(img, cx - 5, cy - 5, GOLD_MID)
    px(img, cx - 4, cy - 5, GOLD_MID)
    px(img, cx - 5, cy - 4, GOLD_LIGHT)
    # blue ribbons hanging down
    rect(img, x + 9, y + 22, x + 11, y + 31, ALLY_BLUE)
    rect(img, x + 19, y + 22, x + 21, y + 31, ALLY_BLUE)
    # ribbon shadows
    rect(img, x + 11, y + 22, x + 11, y + 31, DARK_MID)
    rect(img, x + 21, y + 22, x + 21, y + 31, DARK_MID)
    # fork ends
    rect(img, x + 9, y + 30, x + 9, y + 31, TRANSPARENT)
    rect(img, x + 11, y + 30, x + 11, y + 31, TRANSPARENT)
    rect(img, x + 19, y + 30, x + 19, y + 31, TRANSPARENT)
    rect(img, x + 21, y + 30, x + 21, y + 31, TRANSPARENT)


# ── King's console plate (128x24) ─────────────────────────────────────────
def paint_king_console_plate(img, x, y):
    # Ornate gilded plaque
    # body
    rect(img, x + 4, y + 2, x + 123, y + 21, DARK_MID)
    # peaked end caps
    rect(img, x, y + 6, x + 3, y + 17, DARK_MID)
    rect(img, x + 124, y + 6, x + 127, y + 17, DARK_MID)
    # gold edge
    rect(img, x + 4, y + 2, x + 123, y + 3, GOLD_MID)
    rect(img, x + 4, y + 20, x + 123, y + 21, GOLD_DARK)
    rect(img, x + 4, y + 4, x + 4, y + 19, GOLD_MID)
    rect(img, x + 123, y + 4, x + 123, y + 19, GOLD_MID)
    # cap edges
    rect(img, x, y + 6, x + 3, y + 6, GOLD_MID)
    rect(img, x, y + 17, x + 3, y + 17, GOLD_DARK)
    rect(img, x, y + 6, x, y + 17, GOLD_MID)
    rect(img, x + 124, y + 6, x + 127, y + 6, GOLD_MID)
    rect(img, x + 124, y + 17, x + 127, y + 17, GOLD_DARK)
    rect(img, x + 127, y + 6, x + 127, y + 17, GOLD_MID)
    # heraldic shield flanks
    paint_small_shield(img, x + 8, y + 8)
    paint_small_shield(img, x + 116, y + 8)


def paint_small_shield(img, x, y):
    # 4x6 shield with crown silhouette
    rect(img, x, y, x + 3, y + 4, ALLY_BLUE)
    px(img, x, y + 5, ALLY_BLUE)
    px(img, x + 3, y + 5, ALLY_BLUE)
    px(img, x + 1, y + 5, ALLY_BLUE)
    px(img, x + 2, y + 5, ALLY_BLUE)
    # crown speck
    px(img, x + 1, y + 1, GOLD_LIGHT)
    px(img, x + 2, y + 1, GOLD_LIGHT)


def main():
    img = Image.new("RGBA", (256, 256), TRANSPARENT)

    # Row A — icons
    paint_icon_coin       (img,   0, 0)
    paint_icon_gold_ingot (img,  16, 0)
    paint_icon_emerald    (img,  32, 0)
    paint_icon_land       (img,  48, 0)
    paint_icon_crown      (img,  64, 0)
    paint_icon_gate       (img,  80, 0)
    paint_icon_portal     (img,  96, 0)
    # 112 is reserved (skull, removed)
    paint_icon_wax_seal     (img, 128, 0)
    paint_icon_decree_stamp (img, 144, 0)

    # Row B — stepper states
    paint_stepper(img,  0, 16, 'idle')
    paint_stepper(img, 12, 16, 'hover')
    paint_stepper(img, 24, 16, 'pressed')
    paint_stepper(img, 36, 16, 'disabled')

    # Row C — toggle states
    paint_toggle(img,   0, 28, 'off_idle')
    paint_toggle(img,  60, 28, 'off_hover')
    paint_toggle(img, 120, 28, 'on_idle')
    paint_toggle(img, 180, 28, 'on_hover')

    # Row D — chrome
    paint_outer_frame(img,   0, 44)
    paint_inner_box  (img,  32, 44)
    paint_title_bar  (img,  64, 44)
    paint_parchment  (img, 128, 44)

    # Row E — banners
    paint_banner_ribbon (img,  0, 76)
    paint_burden_bar_bg (img, 96, 76)

    # Row F — decorative
    paint_wax_seal_decree   (img,  0, 100)
    paint_king_console_plate(img, 32, 100)

    out = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "..", "..", "src", "main", "resources", "assets", "ironhold",
        "textures", "gui", "console", "kings_console.png",
    )
    out = os.path.abspath(out)
    img.save(out)
    print(f"Wrote {out}")


if __name__ == "__main__":
    main()
