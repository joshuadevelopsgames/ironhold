"""
King Enderman painter v4 — uses ACTUAL vanilla obsidian texture as base.

Approach:
  1. Load vanilla obsidian.png (16x16).
  2. Remap its 5 shades to our purple palette (same luminance ordering).
  3. Tile this remapped pattern across 128x128.
  4. For each cube's UV face region, overlay the vanilla-style pattern
     with face-directional brightness (top brighter, bottom darker).
  5. Darken face outer edges 1px for cube readability.
  6. Override eye socket regions with deepest shadow shade.
  7. Override chest glyph cubes with magenta-shifted variant.

This is textbook vanilla-style material: structural clustering baked in
because we're using the actual vanilla source pattern.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image


VANILLA_OBSIDIAN = Path("/tmp/vanilla_ref/assets/minecraft/textures/block/obsidian.png")
GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")
TEX_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

W, H = 128, 128

# Target palette — maps to vanilla obsidian's 5 shades by luminance order.
# Vanilla:              Our:
#   #000001 (darkest) → #06031A  (deep)
#   #06030B           → #100720  (dark A)  — DOMINANT
#   #100C1C           → #170B28  (dark B)
#   #271E3D           → #2A1440  (medium)
#   #3B2754 (lightest)→ #3E2058  (highlight)
TARGET = [
    (0x06, 0x03, 0x1A),
    (0x10, 0x07, 0x20),
    (0x17, 0x0B, 0x28),
    (0x2A, 0x14, 0x40),
    (0x3E, 0x20, 0x58),
]

# Chest-glyph accent variant — shifted hue, same cluster tightness.
GLYPH = [
    (0x1A, 0x08, 0x28),
    (0x2A, 0x12, 0x3C),
    (0x36, 0x18, 0x48),
    (0x48, 0x22, 0x5C),
    (0x5A, 0x30, 0x70),
]

EYE_CORE = (0xF2, 0x70, 0xFF, 255)
EYE_MID  = (0xCA, 0x46, 0xEE, 255)
EYE_EDGE = (0x88, 0x22, 0xB0, 255)

FACE_SHADE = {
    "top":    1.08,
    "front":  1.00,
    "right":  0.96,
    "left":   0.95,
    "back":   0.90,
    "bottom": 0.82,
}


def clamp(v): return max(0, min(255, int(v)))
def shade(rgb, m): return (clamp(rgb[0] * m), clamp(rgb[1] * m), clamp(rgb[2] * m))


def build_obsidian_tile(palette):
    """Load vanilla obsidian and remap to target palette by luminance order."""
    vanilla = np.array(Image.open(VANILLA_OBSIDIAN).convert("RGB"))
    flat = vanilla.reshape(-1, 3)
    unique_rgb = np.unique(flat, axis=0)
    # Sort unique vanilla shades by luminance
    lum = unique_rgb[:, 0] * 0.3 + unique_rgb[:, 1] * 0.59 + unique_rgb[:, 2] * 0.11
    order = np.argsort(lum)
    sorted_rgb = unique_rgb[order]
    n = len(sorted_rgb)

    # Map each vanilla shade (by luminance rank) to target palette index.
    # If vanilla has fewer/more unique colors than palette, scale positions.
    remap = {}
    for i, vcol in enumerate(sorted_rgb):
        pal_idx = min(len(palette) - 1, int(round(i / max(1, n - 1) * (len(palette) - 1))))
        remap[tuple(vcol)] = palette[pal_idx]

    out = np.zeros_like(vanilla)
    for y in range(vanilla.shape[0]):
        for x in range(vanilla.shape[1]):
            out[y, x] = remap[tuple(vanilla[y, x])]
    return out  # 16x16 RGB numpy array


def tile_sample(tile, x, y):
    """Sample a tile with wraparound tiling at integer pixel coords."""
    th, tw, _ = tile.shape
    return tuple(int(c) for c in tile[y % th, x % tw])


def face_rects(u, v, w, h, d):
    iw, ih, id_ = int(w), int(h), int(d)
    iu, iv = int(u), int(v)
    return [
        ("top",    iu + id_,            iv,       iu + id_ + iw,       iv + id_),
        ("bottom", iu + id_ + iw,       iv,       iu + id_ + 2 * iw,   iv + id_),
        ("right",  iu,                   iv + id_, iu + id_,            iv + id_ + ih),
        ("front",  iu + id_,            iv + id_, iu + id_ + iw,       iv + id_ + ih),
        ("left",   iu + id_ + iw,       iv + id_, iu + id_ + iw + id_, iv + id_ + ih),
        ("back",   iu + 2*id_ + iw,     iv + id_, iu + 2 * (id_ + iw),  iv + id_ + ih),
    ]


def is_socket(c):
    sx, sy, sz = c["size"]; u, v = c.get("uv", [0, 0])
    return sx == 5 and sy == 4 and sz == 2 and v == 0 and u >= 100


def is_glow(c):
    sx, sy, sz = c["size"]; u, v = c.get("uv", [0, 0])
    return sx == 3 and sy == 2 and sz == 1 and v >= 116


def is_glyph(c):
    sx, sy, sz = c["size"]; u, v = c.get("uv", [0, 0])
    return sz == 1 and u >= 96 and 40 <= v <= 45 and sx <= 4


def paint_face_from_tile(px, x0, y0, x1, y1, tile, shade_mult, offset=(0, 0),
                         force_dark=False, darkest=None):
    """Copy tile pixels into the face region with brightness shading and
    1-pixel dark edge outline."""
    if x1 <= x0 or y1 <= y0:
        return
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not (0 <= x < W and 0 <= y < H):
                continue
            if force_dark and darkest is not None:
                col = darkest
            else:
                col = tile_sample(tile, x + offset[0], y + offset[1])
                col = shade(col, shade_mult)
            # No per-face edge darkening — vanilla mobs don't do this and it
            # creates visible stripes on tall narrow faces. The face-direction
            # shade difference provides enough cube-separation contrast.
            px[x, y] = (*col, 255)


def main():
    # Remapped vanilla obsidian tiles (main body, glyph variant).
    body_tile  = build_obsidian_tile(TARGET)
    glyph_tile = build_obsidian_tile(GLYPH)
    darkest = TARGET[0]

    img = Image.new("RGBA", (W, H), (*TARGET[1], 255))
    px = img.load()
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gpx = glow.load()

    data = json.loads(GEO.read_text())
    bones = data["minecraft:geometry"][0]["bones"]

    for bone in bones:
        for cube in bone.get("cubes", []):
            u, v = cube.get("uv", [0, 0])
            sx, sy, sz = cube["size"]
            socket = is_socket(cube)
            glow_c = is_glow(cube)
            glyph  = is_glyph(cube)
            tile = glyph_tile if glyph else body_tile
            # Offset tile per-cube so tiling seams don't align across faces.
            off = (int(u * 7 + v * 3) % 16, int(v * 11 + u * 5) % 16)

            for fname, x0, y0, x1, y1 in face_rects(u, v, sx, sy, sz):
                paint_face_from_tile(
                    px, x0, y0, x1, y1, tile,
                    FACE_SHADE[fname], offset=off,
                    force_dark=(socket or glow_c),
                    darkest=darkest,
                )

            if glow_c:
                # Only paint the front-face region of the tiny glow cube.
                # Front face UV = (u + d, v + d) → (u + d + w, v + d + h)
                gx0 = u + sz
                gy0 = v + sz
                gx1 = gx0 + sx
                gy1 = gy0 + sy
                cx = (gx0 + gx1 - 1) / 2
                cy = (gy0 + gy1 - 1) / 2
                rad = max(1, max(gx1 - gx0, gy1 - gy0) / 2)
                for y in range(gy0, gy1):
                    for x in range(gx0, gx1):
                        if not (0 <= x < W and 0 <= y < H):
                            continue
                        d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                        t = d / rad
                        gpx[x, y] = EYE_CORE if t < 0.4 else (EYE_MID if t < 0.8 else EYE_EDGE)

    img.save(TEX_OUT)
    glow.save(GLOW_OUT)
    print(f"wrote {TEX_OUT.name}  {W}x{H}")
    print(f"wrote {GLOW_OUT.name}")


if __name__ == "__main__":
    main()
