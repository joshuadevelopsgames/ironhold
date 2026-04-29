"""Paint piglin_villager.png — start from vanilla piglin.png and surgically
recolor the body/arms/legs into a villager-style dark brown robe with gold
trim, while leaving the head/ears/snout/tusks as authentic vanilla piglin
pixels. Adds gold hoop earrings to the ears.

Run: python3 scripts/paint_piglin_villager.py
Requires: /tmp/mc-vanilla/assets/minecraft/textures/entity/piglin/piglin.png
          (extracted earlier; re-extract with the same unzip command if missing)
"""
import os
import sys
from PIL import Image

VANILLA_SRC = "/tmp/mc-vanilla/assets/minecraft/textures/entity/piglin/piglin.png"
OUT = "/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/piglin_villager.png"

if not os.path.exists(VANILLA_SRC):
    print(f"Missing vanilla piglin.png at {VANILLA_SRC}", file=sys.stderr)
    print("Extract with: unzip <minecraft jar> 'assets/minecraft/textures/entity/piglin/*' -d /tmp/mc-vanilla")
    sys.exit(1)

# --- Load vanilla as starting canvas -----------------------------------------
img = Image.open(VANILLA_SRC).convert("RGBA")
px = img.load()
W, H = img.size
assert (W, H) == (64, 64), f"unexpected piglin.png size: {img.size}"

# --- Palette (vanilla cleric/illager-style 4-tone with hash dither) ----------
ROBE_DEEP   = (24, 14,  8, 255)   # #180e08
ROBE_BASE   = (58, 36, 26, 255)   # #3a241a
ROBE_MID    = (90, 54, 36, 255)   # #5a3624
ROBE_HIGH   = (122, 74, 50, 255)  # #7a4a32

GOLD_DEEP   = (110, 74, 20, 255)  # #6e4a14
GOLD_BASE   = (196, 156, 76, 255) # #c49c4c
GOLD_HIGH   = (232, 200, 120, 255)# #e8c878

POUCH_DEEP  = (16,  8,  4, 255)   # #100804
POUCH_BASE  = (42, 24, 14, 255)   # #2a180e
POUCH_HIGH  = (90, 54, 36, 255)   # #5a3624


def hash_xy(x, y, seed):
    return (((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFFFF) % 100


def repaint_face(x0, y0, x1, y1, deep, base, mid, high, mode="mid", seed=1):
    """Replace pixels in [x0,y0)-(x1,y1) with vanilla-style 4-tone dither."""
    w, h = x1 - x0, y1 - y0
    for y in range(y0, y1):
        for x in range(x0, x1):
            ly, lx = y - y0, x - x0
            r  = hash_xy(x, y, seed)
            r2 = hash_xy(x + 31, y + 17, seed + 7)
            c = base
            if mode == "top":
                if   ly == 0 and r < 35: c = high
                elif ly < 2 and r < 25: c = high
                elif r < 18: c = mid
                elif r > 88: c = deep
                elif r2 < 14: c = high
            elif mode == "back":
                if   r < 30: c = deep
                elif r2 < 12: c = mid
            elif mode == "side":
                if   lx >= w - 1 and r < 70: c = deep
                elif lx == 0 and r < 30:    c = mid
                elif r < 22: c = mid
                elif r > 90: c = deep
                elif r2 < 10: c = high
            elif mode == "bottom":
                c = deep
            else:  # mid / front
                if   ly >= h - 2 and r < 40: c = deep
                elif ly < 2 and r < 20:     c = high
                elif lx == 0 or lx == w - 1:
                    if r < 55: c = deep
                elif r < 28: c = mid
                elif r > 92: c = deep
                elif r2 < 14: c = high
            px[x, y] = c


def fill_rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = color


def ribbon_h(x0, x1, y, color, shadow=None, highlight=None, phase=0):
    for x in range(x0, x1):
        px[x, y] = color
        if shadow is not None:
            px[x, y + 1] = shadow
    if highlight is not None and (x1 - x0) > 2:
        px[x0 + 1 + phase % 3, y] = highlight


# === Body cube faces (8w x 12h x 4d at texOffs 16,20 in piglin.png) ==========
# Top:   (20, 16)-(28, 20)   8x4
# Bottom:(28, 16)-(36, 20)   8x4
# Right: (16, 20)-(20, 32)   4x12
# Front: (20, 20)-(28, 32)   8x12
# Left:  (28, 20)-(32, 32)   4x12
# Back:  (32, 20)-(40, 32)   8x12

# Front + back + sides: dark brown robe.
repaint_face(20, 20, 28, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "mid",  300)
repaint_face(32, 20, 40, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "back", 301)
repaint_face(16, 20, 20, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 302)
repaint_face(28, 20, 32, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 303)
repaint_face(20, 16, 28, 20, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "top",  304)

# Body front V-collar (gold) at the top — concept clearly shows a V neckline.
for x in range(20, 28):
    px[x, 20] = GOLD_BASE
    px[x, 21] = GOLD_DEEP
px[21, 20] = GOLD_DEEP    # asym break
px[26, 21] = GOLD_HIGH
# V opening at neck
px[23, 21] = GOLD_BASE; px[24, 21] = GOLD_BASE
px[23, 22] = GOLD_BASE; px[24, 22] = GOLD_BASE
px[23, 23] = GOLD_DEEP; px[24, 23] = GOLD_DEEP

# Gold neck pendant — small medallion 1px below the V
px[23, 24] = GOLD_BASE
px[24, 24] = GOLD_HIGH
px[23, 25] = GOLD_DEEP
px[24, 25] = GOLD_BASE

# Vertical fold creases on the front
for fy in [25, 27, 29]:
    px[22, fy] = ROBE_DEEP
    px[26, fy] = ROBE_DEEP
for fy in [26, 28]:
    px[25, fy] = ROBE_HIGH

# Waist trim ribbon across body front (gold band)
ribbon_h(20, 28, 29, GOLD_BASE, GOLD_DEEP, GOLD_HIGH, phase=1)

# Gold piping on body sides
for y in [22, 26, 30]:
    px[19, y] = GOLD_BASE     # right side back-edge
    px[28, y] = GOLD_BASE     # left side front-edge
ribbon_h(32, 40, 29, GOLD_BASE, GOLD_DEEP, GOLD_HIGH, phase=2)

# === Right Leg (4x12x4 at texOffs 0,16) ======================================
# Front: (4, 20)-(8, 32)
# Back:  (12, 20)-(16, 32)
# Right: (0, 20)-(4, 32)
# Left:  (8, 20)-(12, 32)
# Top:   (4, 16)-(8, 20)
# Bottom:(8, 16)-(12, 20)
repaint_face(4, 20,  8, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "mid",  400)
repaint_face(12, 20, 16, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "back", 401)
repaint_face(0, 20,  4, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 402)
repaint_face(8, 20, 12, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 403)
repaint_face(4, 16,  8, 20, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "top",  404)

# Gold ankle band at bottom of leg front
for x in range(4, 8):
    px[x, 31] = GOLD_BASE
    px[x, 30] = GOLD_DEEP

# === Left Leg (mirrored layer at 16,48) ======================================
repaint_face(20, 52, 24, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "mid",  410)
repaint_face(28, 52, 32, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "back", 411)
repaint_face(16, 52, 20, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 412)
repaint_face(24, 52, 28, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 413)
repaint_face(20, 48, 24, 52, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "top",  414)
for x in range(20, 24):
    px[x, 63] = GOLD_BASE
    px[x, 62] = GOLD_DEEP

# === Right Arm (4x12x4 at texOffs 40,16) =====================================
# Front: (44, 20)-(48, 32)
# Back:  (52, 20)-(56, 32)
# Right: (40, 20)-(44, 32)
# Left:  (48, 20)-(52, 32)
# Top:   (44, 16)-(48, 20)
# Bottom:(48, 16)-(52, 20)
repaint_face(44, 20, 48, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "mid",  500)
repaint_face(52, 20, 56, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "back", 501)
repaint_face(40, 20, 44, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 502)
repaint_face(48, 20, 52, 32, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 503)
repaint_face(44, 16, 48, 20, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "top",  504)

# Gold cuff at wrist
for x in range(44, 48):
    px[x, 31] = GOLD_BASE
    px[x, 30] = GOLD_DEEP
# Shoulder pin
px[45, 20] = GOLD_BASE
px[46, 20] = GOLD_DEEP

# === Left Arm (mirrored layer at 32,48) ======================================
repaint_face(36, 52, 40, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "mid",  510)
repaint_face(44, 52, 48, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "back", 511)
repaint_face(32, 52, 36, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 512)
repaint_face(40, 52, 44, 64, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "side", 513)
repaint_face(36, 48, 40, 52, ROBE_DEEP, ROBE_BASE, ROBE_MID, ROBE_HIGH, "top",  514)
for x in range(36, 40):
    px[x, 63] = GOLD_BASE
    px[x, 62] = GOLD_DEEP
px[37, 52] = GOLD_BASE
px[38, 52] = GOLD_DEEP

# === Belt pouch on body right side (small dark pouch with gold clasp) ========
# Body right face is (16, 20)-(20, 32). Paint a 3x4 pouch in the lower portion.
fill_rect(17, 26, 20, 30, POUCH_BASE)
px[17, 26] = POUCH_HIGH
px[18, 26] = POUCH_HIGH
px[19, 26] = POUCH_HIGH
px[17, 29] = POUCH_DEEP
px[19, 29] = POUCH_DEEP
px[18, 27] = GOLD_BASE     # clasp
px[18, 28] = GOLD_DEEP

# === Gold hoop earrings on ear OUTER faces ===================================
# Right ear cube (1x5x4) at texOffs(39, 6).
# Outer face = right face, at (39, 10)-(43, 15).
# Hoop hangs from bottom edge: 2x2 gold square w/ shadow on bottom-right.
def earring(x, y):
    px[x,     y]     = GOLD_BASE
    px[x + 1, y]     = GOLD_BASE
    px[x,     y + 1] = GOLD_DEEP
    px[x + 1, y + 1] = GOLD_BASE

# Right ear (model's left ear visually): outer face spans (39,10)-(43,15).
earring(40, 13)
# Left ear cube at texOffs(51, 6). Outer = (51, 10)-(55, 15).
earring(53, 13)

# === Save ===================================================================
img.save(OUT)
print(f"OK → {OUT}")
