#!/usr/bin/env python3
"""Generate cumulative crack-overlay textures for the stone golem, masked to the body so cracks
only appear on stone. low ⊂ medium ⊂ high so damage visibly accumulates. Transparent elsewhere."""
import random
import numpy as np
from PIL import Image

BASE = "src/main/resources/assets/ironhold/textures/entity/stone_golem.png"
OUT = "src/main/resources/assets/ironhold/textures/entity/"

base = Image.open(BASE).convert("RGBA")
W, H = base.size
mask = np.array(base)[:, :, 3] > 8        # body pixels
opaque = np.argwhere(mask)                  # (y,x) candidates for crack seeds

CRACK = (18, 16, 16)                         # dark crack
EDGE = (96, 92, 88)                          # subtle lighter chip beside the crack
rng = random.Random(20260604)

def draw_crack(px, alpha_buf, x, y, length, branch_p=0.18, depth=0):
    ang = rng.uniform(0, 2 * 3.14159)
    for _ in range(length):
        ang += rng.uniform(-0.6, 0.6)
        x += np.cos(ang); y += np.sin(ang)
        ix, iy = int(round(x)), int(round(y))
        if not (0 <= ix < W and 0 <= iy < H and mask[iy, ix]):
            break
        px[ix, iy] = (*CRACK, 235)
        alpha_buf[iy, ix] = 235
        # a faint chip highlight beside the crack for relief
        hx, hy = ix + (1 if rng.random() < 0.5 else -1), iy
        if 0 <= hx < W and mask[iy, hx] and px[hx, hy][3] == 0:
            px[hx, hy] = (*EDGE, 90)
        if depth < 2 and rng.random() < branch_p:
            draw_crack(px, alpha_buf, x, y, rng.randint(4, length // 2 + 2), branch_p * 0.6, depth + 1)

def make(n_cracks, len_range, onto):
    img = onto.copy()
    px = img.load()
    ab = np.zeros((H, W))
    for _ in range(n_cracks):
        sy, sx = opaque[rng.randrange(len(opaque))]
        draw_crack(px, ab, sx, sy, rng.randint(*len_range))
    return img

blank = Image.new("RGBA", (W, H), (0, 0, 0, 0))
low = make(7, (8, 16), blank)
medium = make(12, (10, 22), low)            # accumulate onto low
high = make(20, (14, 30), medium)           # accumulate onto medium
for name, im in (("low", low), ("medium", medium), ("high", high)):
    p = OUT + f"stone_golem_cracks_{name}.png"
    im.save(p)
    print("wrote", p, "crack px:", int((np.array(im)[:, :, 3] > 0).sum()))
