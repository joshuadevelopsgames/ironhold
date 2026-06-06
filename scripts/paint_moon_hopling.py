"""
Generate the Moon Hopling textures (v3 — HD 32x rebuild).

The geometry's UV space stays at 64 (geo.json texture_width/height), but the painted
PNGs are 128x128, i.e. SCALE = 2 actual texels per model unit. Minecraft/GeckoLib
normalise UVs by the logical 64 and sample the 128px image, so every face renders at
2px/unit — twice the detail of the old 1px/unit vanilla model, with no geometry change.

Reads art/blockbench/moon_hopling/moon_hopling.geo.json for the authoritative UV layout
(logical 64-space), scales each face rect by SCALE, then paints:
  - moon_hopling.png        base albedo (luminous moon-white, smooth cool shading,
                            big rounded glowing eyes, crescent brow, snout nose)
  - moon_hopling_glow.png   emissive layer (crescent, ear-tips, eye glints, tail,
                            leg cuffs, body sparkles) — transparent elsewhere

Both go to src/main/resources/assets/ironhold/textures/entity/hopling/.
"""
import json
import os
import random

from PIL import Image

random.seed(27)

GEO = 'art/blockbench/moon_hopling/moon_hopling.geo.json'
OUT = 'src/main/resources/assets/ironhold/textures/entity/hopling'

SCALE = 2  # actual texels per model unit (HD)

with open(GEO) as f:
    desc = json.load(f)['minecraft:geometry'][0]
TEX_W = desc['description']['texture_width']     # logical UV space (64)
TEX_H = desc['description']['texture_height']
CW, CH = TEX_W * SCALE, TEX_H * SCALE            # actual canvas (128)
BONES = desc['bones']

# name -> (u, v, (w,h,d)) in logical 64-space
PARTS = {}
for b in BONES:
    for c in b.get('cubes', []):
        u, v = c['uv']
        PARTS[b['name']] = (int(u), int(v), tuple(int(round(s)) for s in c['size']))

# ── palette (luminous moon-white; whiter base, gentle cool shadows) ──────────
WHITE_HI = (255, 255, 255)
BASE     = (244, 247, 253)
MID      = (228, 233, 245)
SHADOW   = (206, 215, 234)
DEEP     = (184, 195, 221)
EYE_DARK = (22, 26, 48)
EYE_IRIS = (94, 140, 214)
EYE_IRIS2 = (146, 184, 240)
EYE_GLNT = (248, 252, 255)
NOSE     = (58, 54, 82)
NOSE_HI  = (120, 116, 150)
MOUTH    = (196, 204, 224)
CY_SOFT  = (176, 214, 255)
CY_BRT   = (232, 246, 255)
CY_CORE  = (150, 200, 250)
CY_DEEP  = (120, 176, 240)
BLUSH    = (208, 224, 250)
CLEAR    = (0, 0, 0, 0)

base = Image.new('RGBA', (CW, CH), CLEAR)
glow = Image.new('RGBA', (CW, CH), CLEAR)
bpx = base.load()
gpx = glow.load()


def rgba(c, a=255):
    return (c[0], c[1], c[2], a) if len(c) == 3 else c


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def fill(px, x0, y0, x1, y1, c):
    c = rgba(c)
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            if 0 <= x < CW and 0 <= y < CH:
                px[x, y] = c


def dot(px, x, y, c):
    if 0 <= x < CW and 0 <= y < CH:
        px[x, y] = rgba(c)


def soft(px, x, y, c, a):
    """Set a glow pixel only if currently empty (don't overwrite brighter marks)."""
    if 0 <= x < CW and 0 <= y < CH and px[x, y][3] == 0:
        px[x, y] = (c[0], c[1], c[2], a)


def face_rects(name):
    """Box-UV face rectangles in ACTUAL 128-space px. front = -Z (toward camera)."""
    u, v, (w, h, d) = PARTS[name]
    u *= SCALE; v *= SCALE
    w *= SCALE; h *= SCALE; d *= SCALE
    return {
        'top':    (u + d,         v,       u + d + w,         v + d),
        'bottom': (u + d + w,     v,       u + d + 2 * w,     v + d),
        'right':  (u,             v + d,   u + d,             v + d + h),
        'front':  (u + d,         v + d,   u + d + w,         v + d + h),
        'left':   (u + d + w,     v + d,   u + 2 * d + w,     v + d + h),
        'back':   (u + 2 * d + w, v + d,   u + 2 * d + 2 * w, v + d + h),
    }


def gradient_face(rect, ctop, cbot, grain=0.0):
    """Smooth vertical gradient ctop->cbot, with optional sparse fur grain."""
    x0, y0, x1, y1 = rect
    hh = y1 - y0
    for y in range(y0, y1):
        t = (y - y0) / max(1, hh - 1)
        col = rgba(lerp(ctop, cbot, t))
        for x in range(x0, x1):
            if 0 <= x < CW and 0 <= y < CH:
                bpx[x, y] = col
    if grain > 0:
        n = int((x1 - x0) * (y1 - y0) * grain)
        for _ in range(n):
            x = random.randint(x0, x1 - 1)
            y = random.randint(y0, y1 - 1)
            t = (y - y0) / max(1, hh - 1)
            cur = lerp(ctop, cbot, t)
            # nudge a touch lighter or cooler for a subtle fur grain
            nud = lerp(cur, WHITE_HI, 0.18) if random.random() < 0.5 else lerp(cur, DEEP, 0.18)
            dot(bpx, x, y, nud)


def paint_generic(name):
    r = face_rects(name)
    gradient_face(r['top'], WHITE_HI, BASE)
    gradient_face(r['bottom'], SHADOW, DEEP)
    for f in ('front', 'back', 'left', 'right'):
        gradient_face(r[f], BASE, SHADOW, grain=0.05)


# ── 1) base shading for every part ───────────────────────────────────────────
for name in PARTS:
    paint_generic(name)


# ── 2) HEAD face: rounded glowing eyes + crescent + blush ────────────────────
def eye_mask(w, h):
    """Rounded tall-oval mask (set of (xx,yy) inside)."""
    cells = set()
    for yy in range(h):
        # taper the top two and bottom two rows for roundness
        if yy == 0:
            xs = range(2, w - 2)
        elif yy == 1 or yy == h - 1:
            xs = range(1, w - 1)
        else:
            xs = range(0, w)
        for xx in xs:
            cells.add((xx, yy))
    return cells


def draw_eye(ex0, ey0, w=6, h=10):
    cells = eye_mask(w, h)
    for (xx, yy) in cells:
        t = yy / (h - 1)
        if t < 0.6:
            col = EYE_DARK
        else:                                   # lower part: iris gradient
            tt = (t - 0.6) / 0.4
            col = lerp(EYE_IRIS, EYE_IRIS2, tt)
        dot(bpx, ex0 + xx, ey0 + yy, col)
    # big specular glint (upper-left) + small secondary (lower-right)
    fill(bpx, ex0 + 1, ey0 + 1, ex0 + 3, ey0 + 3, EYE_GLNT)
    dot(bpx, ex0 + w - 2, ey0 + h - 4, (200, 222, 252))
    # emissive: glints + a faint cyan iris bloom on the glow layer
    fill(gpx, ex0 + 1, ey0 + 1, ex0 + 3, ey0 + 3, EYE_GLNT)
    for (xx, yy) in cells:
        if yy / (h - 1) >= 0.6:
            soft(gpx, ex0 + xx, ey0 + yy, CY_SOFT, 110)


def draw_crescent(cx, cy):
    """Procedural crescent (C opening right): outer disc minus a right-shifted disc.
    cx,cy = top-left of an ~8x10 region in 128-space."""
    rx, ry = 4.2, 5.0
    ox, oy = cx + rx, cy + ry          # outer centre
    ix = ox + 2.6                      # inner (cut) centre, shifted right
    iy = oy
    irx, iry = 3.6, 4.4
    for yy in range(int(ry * 2) + 1):
        for xx in range(int(rx * 2) + 2):
            px, py = cx + xx, cy + yy
            do = ((px + 0.5 - ox) / rx) ** 2 + ((py + 0.5 - oy) / ry) ** 2
            di = ((px + 0.5 - ix) / irx) ** 2 + ((py + 0.5 - iy) / iry) ** 2
            if do <= 1.0 and di > 1.0:
                dot(bpx, px, py, CY_DEEP)
                dot(gpx, px, py, CY_BRT)
                # soft bloom halo on glow
                for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    soft(gpx, px + ax, py + ay, CY_SOFT, 95)


def paint_head():
    r = face_rects('head')
    fx0, fy0, fx1, fy1 = r['front']      # 20 wide x 18 tall (128-space)
    # crescent on the forehead, centred near the top
    draw_crescent(fx0 + 7, fy0 + 0)
    # two big rounded eyes set wide, below the brow and above the snout
    draw_eye(fx0 + 2, fy0 + 6)           # left  (cols 2..7)
    draw_eye(fx0 + 12, fy0 + 6)          # right (cols 12..17)
    # faint cool blush under each eye
    for bx in (fx0 + 3, fx0 + 13):
        for dx in range(3):
            soft_blush = lerp(BASE, BLUSH, 0.6)
            dot(bpx, bx + dx, fy0 + 16, soft_blush)


paint_head()


# ── 3) SNOUT: nose + faint mouth ─────────────────────────────────────────────
def paint_snout():
    r = face_rects('snout')
    fill(bpx, *r['front'], WHITE_HI)
    fill(bpx, *r['top'], WHITE_HI)
    fx0, fy0, fx1, fy1 = r['front']      # 8 wide x 6 tall
    cx = fx0 + (fx1 - fx0) // 2
    # small dark nose (2x2) with a tiny highlight pixel
    fill(bpx, cx - 1, fy0 + 0, cx + 1, fy0 + 2, NOSE)
    dot(bpx, cx - 1, fy0 + 0, NOSE_HI)
    # faint mouth: short downward tick under the nose
    dot(bpx, cx - 1, fy0 + 3, MOUTH)
    dot(bpx, cx, fy0 + 3, MOUTH)
    dot(bpx, cx - 2, fy0 + 4, MOUTH)
    dot(bpx, cx + 1, fy0 + 4, MOUTH)


paint_snout()


# ── 4) EARS: cyan glowing tips ───────────────────────────────────────────────
def paint_ear_tip(name):
    r = face_rects(name)
    for f in ('front', 'back', 'left', 'right'):
        x0, y0, x1, y1 = r[f]
        h = y1 - y0
        for y in range(y0, y1):
            t = (y - y0) / max(1, h - 1)            # 0 top .. 1 bottom
            cb = lerp(CY_BRT, CY_SOFT, min(1.0, t * 1.4))
            ga = int(255 - 110 * t)
            for x in range(x0, x1):
                dot(bpx, x, y, cb)
                dot(gpx, x, y, (CY_BRT[0], CY_BRT[1], CY_BRT[2], max(60, ga)))
    fill(bpx, *r['top'], CY_BRT)
    fill(gpx, *r['top'], CY_BRT)


def paint_ear_base(name):
    r = face_rects(name)
    for f in ('front', 'back', 'left', 'right'):
        x0, y0, x1, y1 = r[f]
        h = y1 - y0
        for y in range(y0, y1):
            t = (y - y0) / max(1, h - 1)
            if t < 0.35:                            # cyan creeps down from the tip seam
                blend = (0.35 - t) / 0.35
                col = lerp(BASE, CY_SOFT, blend)
                for x in range(x0, x1):
                    dot(bpx, x, y, col)
                    soft(gpx, x, y, CY_SOFT, int(120 * blend))


for nm in ('ear_l_tip', 'ear_r_tip'):
    paint_ear_tip(nm)
for nm in ('ear_l_base', 'ear_r_base'):
    paint_ear_base(nm)


# ── 5) TAIL: soft glowing fluff ──────────────────────────────────────────────
def paint_tail():
    r = face_rects('tail')
    fill(bpx, *r['top'], WHITE_HI)
    for f in ('front', 'back', 'left', 'right', 'bottom'):
        x0, y0, x1, y1 = r[f]
        h = y1 - y0
        for y in range(y0, y1):
            t = (y - y0) / max(1, h - 1)
            col = lerp((236, 246, 255), CY_SOFT, t * 0.6)
            for x in range(x0, x1):
                dot(bpx, x, y, col)
                soft(gpx, x, y, CY_SOFT, int(130 - 50 * t))
        # bright fluff sparkles
        for _ in range((x1 - x0)):
            sx = random.randint(x0, x1 - 1)
            sy = random.randint(y0, y1 - 1)
            dot(bpx, sx, sy, CY_BRT)
            dot(gpx, sx, sy, CY_BRT)


paint_tail()


# ── 6) LEGS: glowing cuff near the foot ──────────────────────────────────────
def paint_leg(name):
    r = face_rects(name)
    for f in ('front', 'back', 'left', 'right'):
        x0, y0, x1, y1 = r[f]
        cuff = y1 - 4                                # bottom 4px ring (HD)
        for y in range(cuff, y1):
            t = (y - cuff) / 3.0
            col = lerp(CY_SOFT, CY_DEEP, t)
            for x in range(x0, x1):
                dot(bpx, x, y, col)
                dot(gpx, x, y, (CY_SOFT[0], CY_SOFT[1], CY_SOFT[2], int(200 - 60 * t)))


for nm in ('leg_fl', 'leg_fr', 'leg_bl', 'leg_br'):
    paint_leg(nm)


# ── 7) BODY: scattered 4-point star sparkles ─────────────────────────────────
def star(px, cx, cy, core, arm, arm_a=160):
    dot(px, cx, cy, core)
    for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if px is gpx:
            soft(px, cx + ax, cy + ay, arm, arm_a)
        else:
            dot(px, cx + ax, cy + ay, arm)


def paint_body():
    r = face_rects('body')
    for f in ('front', 'back', 'top', 'left', 'right'):
        x0, y0, x1, y1 = r[f]
        for _ in range(3):
            sx = random.randint(x0 + 1, x1 - 2)
            sy = random.randint(y0 + 1, y1 - 2)
            star(bpx, sx, sy, WHITE_HI, lerp(BASE, CY_SOFT, 0.5))
            star(gpx, sx, sy, CY_BRT, CY_SOFT)


paint_body()


# ── write ────────────────────────────────────────────────────────────────────
os.makedirs(OUT, exist_ok=True)
base.save(os.path.join(OUT, 'moon_hopling.png'))
glow.save(os.path.join(OUT, 'moon_hopling_glow.png'))
print(f'wrote {CW}x{CH} (SCALE={SCALE}, UV space {TEX_W}x{TEX_H})')
print('wrote', os.path.join(OUT, 'moon_hopling.png'))
print('wrote', os.path.join(OUT, 'moon_hopling_glow.png'))
