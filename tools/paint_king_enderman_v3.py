"""
Vanilla-faithful King Enderman texture painter.

Rules derived from vanilla obsidian, enderman, and purple_allay analysis:
  - Tight palette: 5 near-identical obsidian-purple shades + micro-jitter.
  - Dithered two-shade base (obs[1] + obs[2]), not random noise.
  - Rare medium/highlight pixels (<15% combined), with slight clustering.
  - Per-face directional shading (top lighter, bottom/back darker).
  - 1-pixel dark outline on each UV face's border for cube-edge definition.
  - Eye sockets filled with darkest shade; eye glow UV filled with dark base
    (glow layer handles the brightness, not diffuse).
  - Chest cross glyph gets purple-shifted variation (still in same hue).
"""
from __future__ import annotations

import json
import random
from pathlib import Path

from PIL import Image


GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")
TEX_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

W, H = 128, 128

# Obsidian palette: vanilla-tight, derived from vanilla obsidian + Meshy sample.
# Order: darkest → lightest. Use indices 1, 2 for dither base (dominant).
OBS = [
    (0x08, 0x03, 0x10),  # 0: crevice shadow (rare)
    (0x11, 0x07, 0x1A),  # 1: dark base A  — dominant
    (0x18, 0x0B, 0x22),  # 2: dark base B  — dominant (slight shift from A)
    (0x25, 0x14, 0x30),  # 3: medium (occasional)
    (0x35, 0x20, 0x42),  # 4: highlight (rare)
]

# Chest-glyph accent: shifted toward magenta, same tight cluster.
GLYPH = [
    (0x28, 0x10, 0x35),
    (0x36, 0x18, 0x44),
    (0x42, 0x20, 0x50),
]

# Eye glow (emissive layer, not diffuse).
EYE_CORE = (0xF0, 0x6A, 0xFF, 255)
EYE_MID  = (0xC8, 0x42, 0xEE, 255)
EYE_EDGE = (0x8E, 0x24, 0xB8, 255)

# Face-direction brightness multipliers (baked "lighting" without real lighting).
# Vanilla convention: top a touch brighter, bottom + back darker.
FACE_SHADE = {
    "top":    1.12,
    "front":  1.00,
    "right":  0.92,
    "left":   0.90,
    "back":   0.82,
    "bottom": 0.70,
}


def clamp(v): return max(0, min(255, v))


def apply_shade(rgb, mult):
    return (clamp(int(rgb[0] * mult)), clamp(int(rgb[1] * mult)), clamp(int(rgb[2] * mult)))


def jitter(rgb, amt=3):
    return (clamp(rgb[0] + random.randint(-amt, amt)),
            clamp(rgb[1] + random.randint(-amt, amt)),
            clamp(rgb[2] + random.randint(-amt, amt)))


def pick_obs(x, y, special=False):
    """Dithered pick with occasional accent pixels.
    Uses a deterministic checkerboard between OBS[1] and OBS[2] as the base,
    overridden probabilistically for texture variation."""
    r = random.random()
    # Rare bright cluster (highlight) - ~4%
    if r < 0.04:
        return OBS[4]
    # Medium accent - ~10%
    if r < 0.14:
        return OBS[3]
    # Rare crevice dark - ~5%
    if r < 0.19:
        return OBS[0]
    # Remaining 81% → dithered base
    return OBS[1] if (x + y) % 2 == 0 else OBS[2]


def pick_glyph():
    r = random.random()
    if r < 0.25:
        return GLYPH[0]
    if r < 0.80:
        return GLYPH[1]
    return GLYPH[2]


def face_rects_for_cube(u, v, w, h, d):
    """
    Return the 6 face regions for a bedrock box-UV cube as
    list of (face_name, x0, y0, x1, y1) rectangles.
    """
    iw, ih, id_ = int(w), int(h), int(d)
    iu, iv = int(u), int(v)
    return [
        ("top",    iu + id_,               iv,          iu + id_ + iw,             iv + id_),
        ("bottom", iu + id_ + iw,          iv,          iu + id_ + 2 * iw,         iv + id_),
        ("right",  iu,                      iv + id_,    iu + id_,                  iv + id_ + ih),
        ("front",  iu + id_,               iv + id_,    iu + id_ + iw,             iv + id_ + ih),
        ("left",   iu + id_ + iw,          iv + id_,    iu + id_ + iw + id_,       iv + id_ + ih),
        ("back",   iu + id_ + iw + id_,    iv + id_,    iu + 2 * id_ + 2 * iw,     iv + id_ + ih),
    ]


def fill_face(px, face_name, x0, y0, x1, y1, shade_mult, is_glyph=False, is_eye_socket=False):
    if x1 <= x0 or y1 <= y0:
        return
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not (0 <= x < W and 0 <= y < H):
                continue
            if is_eye_socket:
                base = OBS[0]  # deepest shadow
                c = jitter(base, 2)
            elif is_glyph:
                base = pick_glyph()
                c = jitter(apply_shade(base, shade_mult), 3)
            else:
                base = pick_obs(x, y)
                c = jitter(apply_shade(base, shade_mult), 3)
            # 1-pixel dark outline on face edges for cube-edge readability.
            on_edge = (x == x0 or x == x1 - 1 or y == y0 or y == y1 - 1)
            if on_edge:
                c = apply_shade(c, 0.65)
            px[x, y] = (*c, 255)


def is_eye_socket_cube(c):
    """Eye-socket heuristic: tiny 4x4x2 head cubes with UV in the far-right head region."""
    sx, sy, sz = c["size"]
    u, v = c.get("uv", [0, 0])
    return sx == 5 and sy == 4 and sz == 2 and v == 0 and u >= 100


def is_eye_glow_cube(c):
    sx, sy, sz = c["size"]
    u, v = c.get("uv", [0, 0])
    return sx == 3 and sy == 2 and sz == 1 and v >= 116


def is_glyph_cube(c):
    """Chest cross/rune glyph cubes (tiny 3x3x1 or 4x3x1 cubes with UV in the chest accent band)."""
    sx, sy, sz = c["size"]
    u, v = c.get("uv", [0, 0])
    return sz == 1 and u >= 96 and 40 <= v <= 45 and sx <= 4


def main():
    random.seed(0xB055)  # deterministic

    data = json.loads(GEO.read_text())
    bones = data["minecraft:geometry"][0]["bones"]

    img = Image.new("RGBA", (W, H), OBS[1] + (255,))
    px = img.load()

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gpx = glow.load()

    for bone in bones:
        for cube in bone.get("cubes", []):
            u, v = cube.get("uv", [0, 0])
            sx, sy, sz = cube["size"]
            is_socket = is_eye_socket_cube(cube)
            is_glow   = is_eye_glow_cube(cube)
            is_glyph  = is_glyph_cube(cube)

            for fname, x0, y0, x1, y1 in face_rects_for_cube(u, v, sx, sy, sz):
                shade = FACE_SHADE[fname]
                fill_face(px, fname, x0, y0, x1, y1, shade,
                          is_glyph=is_glyph,
                          is_eye_socket=is_socket or is_glow)

            # Glow cubes: paint all face UVs with radial magenta gradient.
            if is_glow:
                for fname, x0, y0, x1, y1 in face_rects_for_cube(u, v, sx, sy, sz):
                    if fname != "front":
                        continue  # only the protruding front glows (others clip inside socket)
                    cx = (x0 + x1 - 1) / 2
                    cy = (y0 + y1 - 1) / 2
                    max_r = max(1, max(x1 - x0, y1 - y0) / 2)
                    for y in range(y0, y1):
                        for x in range(x0, x1):
                            if not (0 <= x < W and 0 <= y < H):
                                continue
                            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                            t = d / max_r
                            if t < 0.35:
                                gpx[x, y] = EYE_CORE
                            elif t < 0.75:
                                gpx[x, y] = EYE_MID
                            else:
                                gpx[x, y] = EYE_EDGE

    img.save(TEX_OUT)
    glow.save(GLOW_OUT)
    print(f"wrote {TEX_OUT.name}  ({W}x{H})")
    print(f"wrote {GLOW_OUT.name}")


if __name__ == "__main__":
    main()
