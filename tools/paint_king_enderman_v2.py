"""
Paints a 128x128 diffuse + glow for the hand-built King Enderman.

Uses Meshy-derived purple palette. Paints region-aware:
  - Bulk of texture: obsidian noise
  - Chest-core UV (98-102 x 40-46): distinct rune-like plate pattern
  - Eye socket UVs (110-128 x 0-10): near-black (deep hollow)
  - Eye glow UVs (112-128 x 116-122): dark base (glow layer handles the glow)
  - Crown base UV (110-128 x 14-30): slightly brighter obsidian

Glow texture: transparent everywhere except the eye glow UV patches, which
get hot magenta that matches the concept reference.
"""
from __future__ import annotations

import random
from pathlib import Path

from PIL import Image

TEX_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

W, H = 128, 128

# Meshy-derived body palette (darkest first).
PAL = [
    (0x0D, 0x03, 0x14),  # pushed a shade darker than Meshy's darkest for shadow
    (0x14, 0x0A, 0x17),
    (0x1C, 0x0F, 0x23),
    (0x2A, 0x15, 0x32),
    (0x36, 0x1A, 0x42),
    (0x46, 0x18, 0x55),
    (0x51, 0x19, 0x64),
]

ACCENT_MAGENTA = (0x7A, 0x22, 0x9A)

# Eye glow hot colors.
EYE_CORE = (0xF2, 0x6B, 0xFF, 255)
EYE_MID  = (0xCC, 0x46, 0xEE, 255)
EYE_EDGE = (0x9A, 0x28, 0xC8, 255)


def pick(weight_low=0.55):
    r = random.random()
    if r < weight_low:
        return PAL[random.randint(0, 2)]
    if r < weight_low + 0.30:
        return PAL[random.randint(2, 4)]
    return PAL[random.randint(4, 6)]


def jitter(c, amt=5):
    return tuple(max(0, min(255, ch + random.randint(-amt, amt))) for ch in c)


def fill_rect(px, x0, y0, x1, y1, color_fn):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < W and 0 <= y < H:
                px[x, y] = (*color_fn(x, y), 255)


def main():
    random.seed(0xBADF00D)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    px = img.load()

    # Base layer: obsidian noise everywhere.
    for y in range(H):
        for x in range(W):
            shade = jitter(pick(0.55 + 0.15 * (y / H)), 5)
            if random.random() < 0.006:
                shade = jitter(ACCENT_MAGENTA, 8)
            if random.random() < 0.003:
                shade = jitter(PAL[6], 6)
            px[x, y] = (*shade, 255)

    # Eye socket regions: very dark (deep hollow) for the 4x4x2 socket cubes.
    # Socket cube UV [110, 0] / [120, 0], each unwraps to 2*(4+2) x (4+2) = 12x6
    def deep_dark(x, y):
        return jitter(PAL[0], 4)

    fill_rect(px, 110, 0, 122, 6, deep_dark)   # right socket face/box UV
    fill_rect(px, 120, 0, 132, 6, deep_dark)   # left socket face/box UV
    # The socket region technically extends to y=6 (D+H=2+4); kept above.

    # Eye glow cube diffuse region: dark base (glow provides the brightness).
    # Cube is 3x3x1: UV footprint 2*(3+1) x (3+1) = 8x4, so (112-120, 116-120)
    fill_rect(px, 112, 116, 120, 120, deep_dark)
    fill_rect(px, 120, 116, 128, 120, deep_dark)

    # Chest-core plate: subtle bright accent for the small 4x6x1 cube.
    # UV patch is 2*(4+1) x (6+1) = 10x7 starting at (98, 40).
    def chest_core(x, y):
        # Mix of mid-purple + bright magenta for a "glyph" feel.
        base = jitter(PAL[5], 4)
        if random.random() < 0.18:
            base = jitter(ACCENT_MAGENTA, 4)
        return base

    fill_rect(px, 98, 40, 108, 47, chest_core)

    # Crown base UV [110, 14] for the 16x2x14 cube: slightly lighter obsidian
    # (so the "helm band" reads as worked metal). Footprint = 2*(16+14) x (2+14) = 60x16.
    def helm_band(x, y):
        return jitter(PAL[4], 4)

    fill_rect(px, 110, 14, 128, 30, helm_band)

    img.save(TEX_OUT)
    print(f"Wrote {TEX_OUT}  (128x128, {W*H} px)")

    # ── Glow texture ───────────────────────────────────────────────────────
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gp = glow.load()

    def glow_rect(x0, y0, x1, y1):
        cx = (x0 + x1 - 1) / 2.0
        cy = (y0 + y1 - 1) / 2.0
        max_r = max(x1 - x0, y1 - y0) / 2.0
        for y in range(y0, y1):
            for x in range(x0, x1):
                if not (0 <= x < W and 0 <= y < H):
                    continue
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                t = d / max_r if max_r > 0 else 0
                if t < 0.35:
                    gp[x, y] = EYE_CORE
                elif t < 0.75:
                    gp[x, y] = EYE_MID
                else:
                    gp[x, y] = EYE_EDGE

    # Eye glow cube front faces.
    # Cube 3x3x1 with UV (112, 116):
    #   top:    (112+1, 116) to (115, 117)
    #   bottom: (115, 116) to (118, 117)
    #   right:  (112, 117) to (113, 120)
    #   front:  (113, 117) to (116, 120)   ← the visible glow
    #   left:   (116, 117) to (117, 120)
    #   back:   (117, 117) to (120, 120)
    # Paint entire UV footprint bright — all visible faces of a tiny cube
    # will glow; no wasted pixels since the cube is small.
    glow_rect(112, 116, 120, 120)
    glow_rect(120, 116, 128, 120)

    glow.save(GLOW_OUT)
    print(f"Wrote {GLOW_OUT}")


if __name__ == "__main__":
    main()
