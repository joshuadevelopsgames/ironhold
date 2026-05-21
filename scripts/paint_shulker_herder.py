"""
Generate the Shulker Herder texture (64x64) in vanilla pixel-art style.
Follows the minecraft-mob-texturing skill conventions:
  - 3-shade palettes (base / shadow / highlight)
  - silhouette edge = 1px interior border in material's own shadow tone
  - top-lit directional shading
  - clustered dither (15-25% shadow on bottom edges, 5-10% highlight on top)
  - asymmetry — break left/right symmetry on details

UV layout matches ShulkerHerderModel.createBodyLayer():

  Head        (0, 0)  8x8x8       — atlas (0,0)-(32,16)
  Hood wrap   (32,0)  8x8x8 +0.5  — atlas (32,0)-(64,16)
  Body        (16,16) 8x12x4      — atlas (16,16)-(40,32)
  Right arm   (40,16) 4x12x4      — atlas (40,16)-(56,32)
  Right leg   (0,16)  4x12x4      — atlas (0,16)-(16,32)
  Left leg    (0,32)  4x12x4      — atlas (0,32)-(16,48)
  Left arm    (40,32) 4x12x4      — atlas (40,32)-(56,48)
  Horn right  (32,32) 2x5x2       — atlas (32,32)-(44,40)
  Horn left   (44,32) 2x5x2       — atlas (44,32)-(56,40)
  Hood band   (24,40) 8x1x8 +0.6  — atlas (24,40)-(56,49)
  Veil        (52,32) 7x5 flat    — atlas (52,32)-(59,37)
  Cloak yoke  (16,52) 10x3x6      — atlas (16,52)-(48,61)
  Cloak back  (0,52)  8x9x1       — atlas (0,52)-(16,63)

Body layout (8x12x4 cube at UV 16,16):
  top    (24,16)-(32,20)   8x4
  bottom (32,16)-(40,20)   8x4
  right  (16,20)-(20,32)   4x12
  front  (20,20)-(28,32)   8x12   ← cream tunic shown here
  left   (28,20)-(32,32)   4x12
  back   (32,20)-(40,32)   8x12   ← purple cloak interior

Leg layout (4x12x4 cube):
  top    (off+4, off_v)-(off+8, off_v+4)
  bottom (off+8, off_v)-(off+12, off_v+4)
  right  (off+0, off_v+4)-(off+4, off_v+16)
  front  (off+4, off_v+4)-(off+8, off_v+16)   ← cream legging
  left   (off+8, off_v+4)-(off+12, off_v+16)
  back   (off+12, off_v+4)-(off+16, off_v+16)
"""
import random
from PIL import Image

random.seed(11)

W = H = 64
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
px = img.load()

# ── Palettes (base, shadow, highlight) ─────────────────────────────────────
PURPLE = ((95,  60, 130), (61,  38,  89),  (122,  90, 168))   # main robe / hood
CREAM  = ((218, 200, 160), (168, 157, 118), (233, 218, 181))  # tunic / leggings
BRASS  = ((168, 132,  44), (122,  94,  26), (208, 166,  72))  # band / cuffs / amulet
DARKP  = ((44,  26,  74), (24,  14,  46),  (61,  40, 104))    # veil / face shadow
BOOT   = ((42,  31,  58), (23,  14,  34),  (61,  48,  80))    # boots
GLOW   = (215, 175, 255)                                       # glowing eye slits
BLOOD  = (200,  60, 110)                                       # accent stitching (rare)

def fill(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = (c[0], c[1], c[2], 255)

def edge(x0, y0, x1, y1, c):
    for x in range(x0, x1):
        px[x, y0]     = (c[0], c[1], c[2], 255)
        px[x, y1 - 1] = (c[0], c[1], c[2], 255)
    for y in range(y0, y1):
        px[x0,     y] = (c[0], c[1], c[2], 255)
        px[x1 - 1, y] = (c[0], c[1], c[2], 255)

def dither_bottom(x0, y0, x1, y1, palette, density=0.20):
    base, shadow, _ = palette
    h, w = y1 - y0, x1 - x0
    n = max(1, int(w * h * density))
    for _ in range(n):
        if random.random() < 0.6:
            y = y1 - 1 - random.randint(0, min(2, h - 1))
        else:
            y = random.randint(y0, y1 - 1)
        if random.random() < 0.4:
            x = x0 if random.random() < 0.5 else x1 - 1
        else:
            x = random.randint(x0, x1 - 1)
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = (shadow[0], shadow[1], shadow[2], 255)

def dither_top(x0, y0, x1, y1, palette, density=0.08):
    _, _, hi = palette
    h, w = y1 - y0, x1 - x0
    n = max(1, int(w * h * density))
    for _ in range(n):
        if random.random() < 0.7:
            y = y0 + random.randint(0, min(1, h - 1))
        else:
            y = random.randint(y0, y1 - 1)
        x = random.randint(x0 + 1, x1 - 2) if w > 2 else random.randint(x0, x1 - 1)
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = (hi[0], hi[1], hi[2], 255)

def shaded_face(x0, y0, x1, y1, palette, kind, dith_b=0.18, dith_t=0.07):
    base, shadow, hi = palette
    if kind == 'top':
        body, out = hi, base
    elif kind == 'bottom':
        body = shadow
        out  = (max(0, shadow[0] - 10), max(0, shadow[1] - 10), max(0, shadow[2] - 10))
    elif kind == 'back':
        body = (max(0, base[0] - 12), max(0, base[1] - 12), max(0, base[2] - 12))
        out  = shadow
    else:
        body, out = base, shadow
    fill(x0, y0, x1, y1, body)
    edge(x0, y0, x1, y1, out)
    if kind != 'bottom':
        dither_bottom(x0, y0, x1, y1, palette, density=dith_b)
        if kind in ('top', 'front', 'left', 'right'):
            dither_top(x0, y0, x1, y1, palette, density=dith_t)

# ── HEAD: bare head sits inside the hood, in deep shadow ──────────────────
shaded_face(8,  0, 16,  8, DARKP, 'top')
shaded_face(16, 0, 24,  8, DARKP, 'bottom')
shaded_face(0,  8,  8, 16, DARKP, 'right')
shaded_face(8,  8, 16, 16, DARKP, 'front', dith_b=0.05, dith_t=0.02)
shaded_face(16, 8, 24, 16, DARKP, 'left')
shaded_face(24, 8, 32, 16, DARKP, 'back')

# ── HOOD WRAP: 8x8x8 inflated, UV (32,0) — main purple cloth ──────────────
shaded_face(32+8,  0,  32+16,  8, PURPLE, 'top')
shaded_face(32+16, 0,  32+24,  8, PURPLE, 'bottom')
shaded_face(32+0,  8,  32+8,  16, PURPLE, 'right')
shaded_face(32+8,  8,  32+16, 16, PURPLE, 'front')
shaded_face(32+16, 8,  32+24, 16, PURPLE, 'left')
shaded_face(32+24, 8,  32+32, 16, PURPLE, 'back')

# Hood front: dark recessed face area where the veil sits behind
# (will be visually combined with the bare head front + veil overlay).
# Add a 6x4 darker rectangle on the hood front face to simulate the
# shadow cast over the face.
for y in range(10, 14):
    for x in range(41, 47):
        r, g, b, _ = px[x, y]
        px[x, y] = (max(0, r - 30), max(0, g - 25), max(0, b - 25), 255)

# Vertical fold streak (asymmetric) on hood front
for y in range(10, 16):
    px[42, y] = (*PURPLE[2], 255)
for y in range(11, 15):
    px[45, y] = (*PURPLE[1], 255)

# ── HORNS: 2x5x2 cubes at UV (32,32) and (44,32) ──────────────────────────
def paint_horn(off_u, off_v, palette, mirror=False):
    # 2x5x2 cube unfolded:
    #   top    (off+2, v)-(off+4, v+2)
    #   bottom (off+4, v)-(off+6, v+2)
    #   right  (off+0, v+2)-(off+2, v+7)
    #   front  (off+2, v+2)-(off+4, v+7)
    #   left   (off+4, v+2)-(off+6, v+7)
    #   back   (off+6, v+2)-(off+8, v+7)
    base, shadow, hi = palette
    shaded_face(off_u+2, off_v,    off_u+4,  off_v+2,  palette, 'top',    dith_b=0, dith_t=0.5)
    shaded_face(off_u+4, off_v,    off_u+6,  off_v+2,  palette, 'bottom', dith_b=0, dith_t=0)
    shaded_face(off_u+0, off_v+2,  off_u+2,  off_v+7,  palette, 'right',  dith_b=0.10, dith_t=0.05)
    shaded_face(off_u+2, off_v+2,  off_u+4,  off_v+7,  palette, 'front',  dith_b=0.10, dith_t=0.05)
    shaded_face(off_u+4, off_v+2,  off_u+6,  off_v+7,  palette, 'left',   dith_b=0.10, dith_t=0.05)
    shaded_face(off_u+6, off_v+2,  off_u+8,  off_v+7,  palette, 'back',   dith_b=0.05, dith_t=0)
    # Brass tip on each horn (top 1 row of front face)
    if not mirror:
        px[off_u+2, off_v+2] = (*BRASS[2], 255)
        px[off_u+3, off_v+2] = (*BRASS[0], 255)
    else:
        px[off_u+2, off_v+2] = (*BRASS[0], 255)
        px[off_u+3, off_v+2] = (*BRASS[2], 255)

paint_horn(32, 32, PURPLE, mirror=False)
paint_horn(44, 32, PURPLE, mirror=True)

# ── HOOD BAND: 8x1x8 inflated, UV (24,40) ─────────────────────────────────
# Layout for an 8x1x8 cube:
#   top    (32,40)-(40,48)
#   bottom (40,40)-(48,48)
#   right  (24,48)-(32,49)
#   front  (32,48)-(40,49)
#   left   (40,48)-(48,49)
#   back   (48,48)-(56,49)
shaded_face(32, 40, 40, 48, BRASS, 'top',    dith_b=0.10, dith_t=0.10)
shaded_face(40, 40, 48, 48, BRASS, 'bottom', dith_b=0.0,  dith_t=0.0)
fill(24, 48, 32, 49, BRASS[0]); px[24, 48] = (*BRASS[1], 255); px[31, 48] = (*BRASS[1], 255)
fill(32, 48, 40, 49, BRASS[0]); px[32, 48] = (*BRASS[1], 255); px[39, 48] = (*BRASS[1], 255)
px[35, 48] = (*BRASS[2], 255)
fill(40, 48, 48, 49, BRASS[0]); px[40, 48] = (*BRASS[1], 255); px[47, 48] = (*BRASS[1], 255)
fill(48, 48, 56, 49, BRASS[0]); px[48, 48] = (*BRASS[1], 255); px[55, 48] = (*BRASS[1], 255)

# Decorative gem dots on the band's top face (asymmetric)
px[34, 43] = (*GLOW, 255)
px[37, 44] = (*GLOW, 255)
px[34, 45] = (*PURPLE[2], 255)
px[42, 44] = (*BRASS[1], 255)

# ── VEIL: 7x5 plane at UV (52,32) — dark face cover with two glow eyes ────
# Fill solid dark
fill(52, 32, 59, 37, DARKP[1])
edge(52, 32, 59, 37, (10, 6, 24))
# Vertical weave streaks
for y in range(33, 36):
    px[53, y] = (*DARKP[0], 255)
    px[57, y] = (*DARKP[0], 255)
# Two GLOWING EYE SLITS — the trademark feature
# Right eye slit (player's left): cols 53-54, row 34
px[53, 34] = (*GLOW, 255)
px[54, 34] = (*GLOW, 255)
# Left eye slit (player's right): cols 56-57, row 34
px[56, 34] = (*GLOW, 255)
px[57, 34] = (*GLOW, 255)
# Glow softener pixels under each eye (slight bleed)
px[53, 35] = (180, 130, 220, 255)
px[57, 35] = (180, 130, 220, 255)

# ── BODY: 8x12x4 at UV (16,16) ─────────────────────────────────────────────
# FRONT FACE shows the cream tunic; sides + back show purple cloak interior
# Top (cloak shoulders viewed from above)
shaded_face(24, 16, 32, 20, PURPLE, 'top')
# Bottom (cream tunic seen from below)
shaded_face(32, 16, 40, 20, CREAM, 'bottom')
# Right side: cream tunic
shaded_face(16, 20, 20, 32, CREAM, 'right')
# Front: cream tunic with brass amulet detail
shaded_face(20, 20, 28, 32, CREAM, 'front')
# Left side: cream tunic
shaded_face(28, 20, 32, 32, CREAM, 'left')
# Back: purple cloak
shaded_face(32, 20, 40, 32, PURPLE, 'back')

# Tunic detail: brass-rimmed amulet with glowing core, top of chest
px[23, 22] = (*BRASS[0], 255); px[24, 22] = (*BRASS[0], 255)
px[23, 23] = (*BRASS[0], 255); px[24, 23] = (*GLOW, 255)
px[22, 23] = (*BRASS[1], 255); px[25, 23] = (*BRASS[1], 255)
px[23, 24] = (*BRASS[2], 255); px[24, 24] = (*BRASS[0], 255)

# Tunic central seam stitching
for y in range(25, 31, 2):
    px[24, y] = (*CREAM[1], 255)
# Asymmetric belt cinch — single dark line across mid-body front
for x in range(20, 28):
    px[x, 27] = (*BRASS[1], 255)
px[26, 27] = (*BRASS[2], 255)  # buckle highlight (asymmetric, off-centre)

# Vertical cloak fold streak on body back (asymmetric)
for y in range(22, 30):
    px[34, y] = (*PURPLE[2], 255)

# ── ARMS: 4x9x4 sleeves + 5x2x5 flare + 4x2x4 cuff (inflated) ─────────────
# The sleeve is shorter than before (9 tall, not 12) — the flare and cuff
# fill the remaining space with their own 3D geometry.

def paint_sleeve(off_u, off_v):
    """4x9x4 cube. UV layout for a 4x9x4 cube, starting at (off_u, off_v):
       top    (off+4, v) - (off+8, v+4)
       bottom (off+8, v) - (off+12, v+4)
       right  (off+0, v+4) - (off+4, v+13)
       front  (off+4, v+4) - (off+8, v+13)
       left   (off+8, v+4) - (off+12, v+13)
       back   (off+12, v+4) - (off+16, v+13)
    """
    shaded_face(off_u+4,  off_v,    off_u+8,  off_v+4,  PURPLE, 'top')
    shaded_face(off_u+8,  off_v,    off_u+12, off_v+4,  PURPLE, 'bottom')
    shaded_face(off_u+0,  off_v+4,  off_u+4,  off_v+13, PURPLE, 'right')
    shaded_face(off_u+4,  off_v+4,  off_u+8,  off_v+13, PURPLE, 'front')
    shaded_face(off_u+8,  off_v+4,  off_u+12, off_v+13, PURPLE, 'left')
    shaded_face(off_u+12, off_v+4,  off_u+16, off_v+13, PURPLE, 'back')

paint_sleeve(40, 16)  # right arm sleeve
paint_sleeve(40, 32)  # left  arm sleeve

def paint_sleeve_flare(off_u, off_v, mirror=False):
    """5x2x5 cube. UV layout:
       top    (off+5,  v)   - (off+10, v+5)
       bottom (off+10, v)   - (off+15, v+5)
       right  (off+0,  v+5) - (off+5,  v+7)
       front  (off+5,  v+5) - (off+10, v+7)
       left   (off+10, v+5) - (off+15, v+7)
       back   (off+15, v+5) - (off+20, v+7)
    """
    # Top face — slightly darker (inside of the cuff visible from above)
    shaded_face(off_u+5,  off_v,   off_u+10, off_v+5, PURPLE, 'back', dith_b=0.05, dith_t=0)
    shaded_face(off_u+10, off_v,   off_u+15, off_v+5, PURPLE, 'bottom')
    shaded_face(off_u+0,  off_v+5, off_u+5,  off_v+7, PURPLE, 'right',  dith_b=0.2, dith_t=0)
    shaded_face(off_u+5,  off_v+5, off_u+10, off_v+7, PURPLE, 'front',  dith_b=0.2, dith_t=0)
    shaded_face(off_u+10, off_v+5, off_u+15, off_v+7, PURPLE, 'left',   dith_b=0.2, dith_t=0)
    shaded_face(off_u+15, off_v+5, off_u+20, off_v+7, PURPLE, 'back',   dith_b=0.2, dith_t=0)

paint_sleeve_flare(16, 38)  # right
paint_sleeve_flare(36, 38, mirror=True)  # left

def paint_cuff(off_u, off_v, mirror=False):
    """4x2x4 cube. UV layout:
       top    (off+4,  v)   - (off+8,  v+4)
       bottom (off+8,  v)   - (off+12, v+4)
       right  (off+0,  v+4) - (off+4,  v+6)
       front  (off+4,  v+4) - (off+8,  v+6)
       left   (off+8,  v+4) - (off+12, v+6)
       back   (off+12, v+4) - (off+16, v+6)
    """
    shaded_face(off_u+4,  off_v,    off_u+8,  off_v+4, BRASS, 'top',    dith_b=0.10, dith_t=0.15)
    shaded_face(off_u+8,  off_v,    off_u+12, off_v+4, BRASS, 'bottom')
    shaded_face(off_u+0,  off_v+4,  off_u+4,  off_v+6, BRASS, 'right',  dith_b=0.10, dith_t=0.10)
    shaded_face(off_u+4,  off_v+4,  off_u+8,  off_v+6, BRASS, 'front',  dith_b=0.10, dith_t=0.10)
    shaded_face(off_u+8,  off_v+4,  off_u+12, off_v+6, BRASS, 'left',   dith_b=0.10, dith_t=0.10)
    shaded_face(off_u+12, off_v+4,  off_u+16, off_v+6, BRASS, 'back',   dith_b=0.05, dith_t=0)
    # Decorative gem stud on the front face — asymmetric (right cuff only)
    if not mirror:
        px[off_u+5, off_v+5] = (*GLOW, 255)
        px[off_u+6, off_v+5] = (*PURPLE[1], 255)
    else:
        # Left cuff: a small brass divot instead of a gem
        px[off_u+10, off_v+5] = (*BRASS[1], 255)

paint_cuff(48, 52, mirror=False)  # right cuff
paint_cuff(48, 58, mirror=True)   # left  cuff

# ── LEGS: 4x12x4 — cream legging top, dark boot bottom ────────────────────
def paint_leg(off_u, off_v, mirror=False):
    # Cream legging — covers top 9 rows of each side face
    shaded_face(off_u+4,  off_v,    off_u+8,  off_v+4,  CREAM, 'top')
    shaded_face(off_u+8,  off_v,    off_u+12, off_v+4,  BOOT,  'bottom')
    shaded_face(off_u+0,  off_v+4,  off_u+4,  off_v+13, CREAM, 'right')
    shaded_face(off_u+4,  off_v+4,  off_u+8,  off_v+13, CREAM, 'front')
    shaded_face(off_u+8,  off_v+4,  off_u+12, off_v+13, CREAM, 'left')
    shaded_face(off_u+12, off_v+4,  off_u+16, off_v+13, CREAM, 'back')
    # Boots — bottom 3 rows of each side face
    shaded_face(off_u+0,  off_v+13, off_u+4,  off_v+16, BOOT, 'right',  dith_b=0.12, dith_t=0.0)
    shaded_face(off_u+4,  off_v+13, off_u+8,  off_v+16, BOOT, 'front',  dith_b=0.12, dith_t=0.0)
    shaded_face(off_u+8,  off_v+13, off_u+12, off_v+16, BOOT, 'left',   dith_b=0.12, dith_t=0.0)
    shaded_face(off_u+12, off_v+13, off_u+16, off_v+16, BOOT, 'back',   dith_b=0.05, dith_t=0.0)
    # Boot/legging seam — 1px brass piping across top of boot
    for x in range(off_u+0, off_u+16):
        px[x, off_v+13] = (*BRASS[1], 255)
    # Asymmetric scuff/dust
    if not mirror:
        px[off_u+5, off_v+15] = (*BOOT[2], 255)
        px[off_u+6, off_v+10] = (*CREAM[1], 255)
    else:
        px[off_u+10, off_v+14] = (*BOOT[2], 255)

paint_leg(0, 16, mirror=False)
paint_leg(0, 32, mirror=True)

# ── CLOAK YOKE: 10x3x6 at UV (16,52) ──────────────────────────────────────
# Layout for a 10x3x6 cube:
#   top    (22, 52)-(32, 58)
#   bottom (32, 52)-(42, 58)
#   right  (16, 58)-(22, 61)
#   front  (22, 58)-(32, 61)
#   left   (32, 58)-(38, 61)
#   back   (38, 58)-(48, 61)
shaded_face(22, 52, 32, 58, PURPLE, 'top')
shaded_face(32, 52, 42, 58, PURPLE, 'bottom')
shaded_face(16, 58, 22, 61, PURPLE, 'right')
# Front of yoke: paint a V-shaped cream gap so the tunic shows through
shaded_face(22, 58, 32, 61, PURPLE, 'front')
# Carve cream V into the front (rows 60-61 centre cols 26-28)
for x in range(25, 29):
    px[x, 60] = (*CREAM[0], 255)
for x in range(26, 28):
    px[x, 59] = (*CREAM[0], 255)
shaded_face(32, 58, 38, 61, PURPLE, 'left')
shaded_face(38, 58, 48, 61, PURPLE, 'back')
# Brass clasp pixels at the V apex (asymmetric — only on right shoulder)
px[24, 58] = (*BRASS[0], 255)
px[25, 58] = (*BRASS[2], 255)

# ── CLOAK BACK PANEL: 8x9x1 flat sheet at UV (0,52) ───────────────────────
# This is a thin 1-deep panel; both wide faces are 8x9.
# Standard layout for an 8x9x1 cube:
#   top    (1, 52)-(9, 53)
#   bottom (9, 52)-(17, 53)
#   right  (0, 53)-(1, 62)
#   front  (1, 53)-(9, 62)
#   left   (9, 53)-(10, 62)
#   back   (10, 53)-(18, 62)
shaded_face(1,  52, 9,  53, PURPLE, 'top',    dith_b=0, dith_t=0.0)
shaded_face(9,  52, 17, 53, PURPLE, 'bottom', dith_b=0, dith_t=0.0)
fill(0, 53, 1, 62, PURPLE[1])
fill(9, 53, 10, 62, PURPLE[1])
shaded_face(1, 53, 9, 62, PURPLE, 'front')
shaded_face(10, 53, 18, 62, PURPLE, 'back')
# Vertical fold streaks on the cloak back
for y in range(54, 62):
    px[3, y] = (*PURPLE[2], 255)
for y in range(55, 62):
    px[6, y] = (*PURPLE[1], 255)
# Brass-trimmed hem (bottom row of front face)
for x in range(1, 9):
    px[x, 61] = (*BRASS[1], 255)

# ── Final output ──────────────────────────────────────────────────────────
out = '/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/shulker_herder.png'
img.save(out)
print('wrote', out, img.size)
preview = img.resize((W * 8, H * 8), Image.NEAREST)
preview.save('/Users/joshua/Kingdom SMP/ironhold/shulker_herder_preview.png')
print('wrote 8x preview')
