"""Paint the King Enderman 128x64 texture in vanilla-enderman style.

Layout follows KingEndermanModel.java texOffs values exactly. If you change UVs in
the model, update the regions table below to match.
"""
from PIL import Image

W, H = 128, 64

# --- Palettes (3-shade rule: base / shadow / highlight) ---
# Vanilla enderman skin is nearly-flat near-black with only a whisper of purple.
# The magenta you see in-game comes from eye particles + the EyesLayer, NOT the body.
# Keep the spread tight — too much variance reads as static, not skin.
SKIN_BASE      = (26, 26, 30, 255)    # #1a1a1e — body/limbs/head
SKIN_SHADOW    = (16, 16, 20, 255)    # #101014
SKIN_HIGHLIGHT = (40, 38, 46, 255)    # #28262e

# Crown — warm gold, distinctly different from skin so it reads at distance.
GOLD_BASE      = (212, 168, 64, 255)   # #d4a840
GOLD_SHADOW    = (154, 120, 32, 255)   # #9a7820
GOLD_HIGHLIGHT = (240, 208, 96, 255)   # #f0d060
JEWEL          = (204, 32, 68, 255)    # #cc2244 — sparingly, on front center spike

# Eyes are painted on the body texture as dim pixels; the EyesLayer pulls the
# bright glow color from king_enderman_glow.png at the same coords.
EYE_DIM        = (90, 40, 130, 255)    # #5a2882

img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()


def fill_rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = color


def paint_face(x0, y0, x1, y1, base, shadow, highlight,
               brightness="mid", border=False, dither_seed=0):
    """Paint one rectangular face with very subtle directional shading.

    Vanilla enderman is *nearly flat* — sparse pixel variation only.
    brightness:
      'top'    — base fill, ~3% highlight specks
      'mid'    — base fill, ~3% shadow specks
      'side'   — base fill, 1px shadow column on back edge only
      'back'   — base fill, no specks (slightly darker face read by lighting)
      'bottom' — solid shadow tone
    border: rarely needed for enderman; keep False unless explicitly called.
    """
    w = x1 - x0
    h = y1 - y0
    if w <= 0 or h <= 0:
        return

    # Step 1: flat base fill.
    fill_color = shadow if brightness == "bottom" else base
    fill_rect(x0, y0, x1, y1, fill_color)
    if brightness == "bottom":
        return  # bottom face stays solid; no further detail

    # Step 2: very sparse directional speckle.
    def hash_xy(x, y):
        return ((x * 73856093) ^ (y * 19349663) ^ dither_seed) & 0xFFFF

    # Tiny faces (≤3 in any dim) stay flat — speckle would dominate them.
    if w < 4 or h < 4:
        if brightness == "side" and w >= 2 and h >= 2:
            for y in range(y0, y1):
                px[x1 - 1, y] = shadow  # 1px back-edge shadow only
        return

    threshold_hi = 4 if brightness == "top" else 2   # 4% / 2% highlight chance
    threshold_lo = 6 if brightness == "mid" else 3   # 6% / 3% shadow chance
    for y in range(y0, y1):
        for x in range(x0, x1):
            ly = y - y0
            lx = x - x0
            r = hash_xy(x, y) % 100
            # Highlight: only near top edge.
            if ly < 2 and r < threshold_hi:
                px[x, y] = highlight
            # Shadow: prefer bottom edge + back column, sparse elsewhere.
            elif (ly >= h - 1 or lx >= w - 1) and r < 30:
                px[x, y] = shadow
            elif r < threshold_lo:
                px[x, y] = shadow

    # Step 3: silhouette border (off by default for enderman — flat fills look right).
    if border and w >= 3 and h >= 3:
        for x in range(x0, x1):
            px[x, y0]     = shadow
            px[x, y1 - 1] = shadow
        for y in range(y0, y1):
            px[x0, y]     = shadow
            px[x1 - 1, y] = shadow


def unwrap(u, v, w, h, d):
    """Return dict of face rectangles for a Minecraft cube unwrap.

    Layout: top, bottom on row 0..d. Sides+front+back on row d..d+h.
    """
    return dict(
        top=    (u + d,         v,         u + d + w,         v + d),
        bottom= (u + d + w,     v,         u + d + w + w,     v + d),
        right=  (u,             v + d,     u + d,             v + d + h),
        front=  (u + d,         v + d,     u + d + w,         v + d + h),
        left=   (u + d + w,     v + d,     u + d + w + d,     v + d + h),
        back=   (u + d + w + d, v + d,     u + d + w + d + w, v + d + h),
    )


def paint_cube(u, v, w, h, d, base, shadow, highlight, seed=0, border=False):
    f = unwrap(u, v, w, h, d)
    paint_face(*f["top"],    base, shadow, highlight, "top",    border, seed + 1)
    paint_face(*f["bottom"], base, shadow, highlight, "bottom", border, seed + 2)
    paint_face(*f["front"],  base, shadow, highlight, "mid",    border, seed + 3)
    paint_face(*f["back"],   base, shadow, highlight, "back",   border, seed + 4)
    paint_face(*f["right"],  base, shadow, highlight, "side",   border, seed + 5)
    paint_face(*f["left"],   base, shadow, highlight, "side",   border, seed + 6)
    return f


# --- Body parts (skin palette) ---
head_faces = paint_cube(0,   0, 8, 6, 8, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1001)
paint_cube(0,  14, 8, 2, 8, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1002)  # jaw
paint_cube(32, 14, 3, 4, 3, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1003)  # neck
paint_cube(50, 22, 8, 10, 4, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1004) # abdomen
paint_cube(76,  0, 10, 16, 6, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1005) # chest
paint_cube(108, 0, 3, 22, 3, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1006) # upper arm
paint_cube(108, 25, 3, 18, 3, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1007) # forearm
paint_cube(108, 46, 4, 4, 4, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1008)  # hand
paint_cube(0, 34, 3, 24, 3, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1009)   # thigh
paint_cube(12, 34, 3, 24, 3, SKIN_BASE, SKIN_SHADOW, SKIN_HIGHLIGHT, seed=1010)  # shin

# --- Eyes (dim violet on the body texture; the glow layer brightens them) ---
# Head front face is at unwrap['front'] = (8, 8)..(16, 14) — 8 wide × 6 tall.
# Eyes: rows y=10 (one pixel tall, vanilla enderman style), 2x1 each, mildly asymmetric.
fx0, fy0, fx1, fy1 = head_faces["front"]
# Left eye
px[fx0 + 1, fy0 + 2] = EYE_DIM
px[fx0 + 2, fy0 + 2] = EYE_DIM
# Right eye (asymmetry: shifted 1px right vs mirror)
px[fx0 + 5, fy0 + 2] = EYE_DIM
px[fx0 + 6, fy0 + 2] = EYE_DIM

# --- Crown (gold palette) ---
# UVs must match KingEndermanModel.java crown texOffs values.
paint_cube(74, 36, 10, 3, 1, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2001)   # front strip
paint_cube(74, 40, 10, 3, 1, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2002)   # back strip
paint_cube(24, 34, 1, 3, 9, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2003)    # right side
paint_cube(24, 46, 1, 3, 9, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2004)    # left side
paint_cube(96, 36, 1, 3, 1, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2005)    # corner spikes (shared)
center_spike_faces = paint_cube(100, 36, 2, 4, 2, GOLD_BASE, GOLD_SHADOW, GOLD_HIGHLIGHT, seed=2006)
# Add a single jewel pixel on the front of the center spike for personality.
csx0, csy0, csx1, csy1 = center_spike_faces["front"]
px[csx0, csy0 + 1] = JEWEL

# --- Asymmetry pass: minimal — vanilla enderman skin is uniform.
# Just one subtle differing pixel so it doesn't read as perfectly mirrored.
px[111, 0] = SKIN_BASE

img.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")

# --- Glow texture: only the eye pixels glow bright purple. ---
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gpx = glow.load()
EYE_GLOW = (220, 110, 255, 255)  # #dc6eff
gpx[fx0 + 1, fy0 + 2] = EYE_GLOW
gpx[fx0 + 2, fy0 + 2] = EYE_GLOW
gpx[fx0 + 5, fy0 + 2] = EYE_GLOW
gpx[fx0 + 6, fy0 + 2] = EYE_GLOW
glow.save("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

print("OK")
