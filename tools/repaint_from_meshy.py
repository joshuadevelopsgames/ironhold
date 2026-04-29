"""
Samples the Meshy diffuse texture to extract its dominant body-color palette
(ignoring gold crown/staff pixels), then repaints our King Enderman textures
using that palette — so the auto-voxelized model inherits Meshy's art direction.
"""
from __future__ import annotations

import random
from collections import Counter
from pathlib import Path

from PIL import Image


MESHY_DIFFUSE = Path("/Users/joshua/Kingdom SMP/ironhold/tools/meshy_reference/Meshy_AI_King_Enderman_0421191821_texture.png")
TEX_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW_OUT = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

OUT_W, OUT_H = 64, 64


def is_body_pixel(r: int, g: int, b: int) -> bool:
    """Filter out gold/yellow (crown/staff) and near-white pixels.
    Keep only purple-dominated pixels (B > R, body) and dark shadows."""
    # Gold / yellow: R >> B
    if r > 90 and g > 60 and b < r - 20:
        return False
    # Near-white
    if r > 200 and g > 200 and b > 200:
        return False
    # Require blue channel to be at least competitive with green (purple bias).
    # This filters out any remaining warm/neutral pixels.
    if r > 50 and g > b + 5:
        return False
    return True


def sample_palette(meshy_path: Path, k: int = 7) -> list[tuple[int, int, int]]:
    img = Image.open(meshy_path).convert("RGB")
    print(f"Meshy diffuse: {img.size}")

    # Downsample for speed.
    small = img.resize((256, 256))
    px = list(small.getdata())
    body = [p for p in px if is_body_pixel(*p)]
    print(f"Body pixels after filter: {len(body)}/{len(px)}")

    # Bucket to 3-bit per channel (0..7), then take top-k most common buckets.
    # We re-expand each bucket back to 8-bit by centroid averaging.
    def bucket(c):
        return tuple(c_ >> 5 for c_ in c)

    by_bucket: dict[tuple[int, int, int], list[tuple[int, int, int]]] = {}
    for p in body:
        b = bucket(p)
        by_bucket.setdefault(b, []).append(p)

    counts = Counter({b: len(v) for b, v in by_bucket.items()})
    top = counts.most_common(k)
    palette = []
    for b_key, _ in top:
        group = by_bucket[b_key]
        ar = sum(p[0] for p in group) // len(group)
        ag = sum(p[1] for p in group) // len(group)
        ab = sum(p[2] for p in group) // len(group)
        palette.append((ar, ag, ab))

    # Sort by luminance so pick(weight_low) still biases toward dark.
    palette.sort(key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    print("Extracted palette (dark → light):")
    for c in palette:
        print(f"  rgb{c}  hex#{c[0]:02X}{c[1]:02X}{c[2]:02X}")
    return palette


def paint(palette):
    random.seed(0xC0FFEE)

    def pick():
        r = random.random()
        if r < 0.60:
            return palette[random.randint(0, min(2, len(palette) - 1))]
        if r < 0.90:
            return palette[random.randint(min(2, len(palette) - 1), min(4, len(palette) - 1))]
        return palette[random.randint(min(4, len(palette) - 1), len(palette) - 1)]

    def jitter(c, amt=4):
        return tuple(max(0, min(255, ch + random.randint(-amt, amt))) for ch in c)

    img = Image.new("RGBA", (OUT_W, OUT_H), (0, 0, 0, 255))
    p = img.load()
    for y in range(OUT_H):
        for x in range(OUT_W):
            c = jitter(pick(), 5)
            # Sparse bright magenta speck for visual interest.
            if random.random() < 0.006:
                c = (max(c[0], 0x7A), c[1], max(c[2], 0xA0))
            p[x, y] = (*c, 255)
    img.save(TEX_OUT)
    print(f"Wrote {TEX_OUT}")

    # Blank glow texture (no emission detected in Meshy output).
    glow = Image.new("RGBA", (OUT_W, OUT_H), (0, 0, 0, 0))
    glow.save(GLOW_OUT)
    print(f"Wrote {GLOW_OUT}")


if __name__ == "__main__":
    pal = sample_palette(MESHY_DIFFUSE, k=8)
    paint(pal)
