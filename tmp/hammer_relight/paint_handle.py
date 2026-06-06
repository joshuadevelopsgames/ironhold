#!/usr/bin/env python3
"""Texture the handle as a dark-metal shaft matching the head: head gray palette,
per-face top-left lighting, subtle grip banding + pixel noise. Uses the verified
Blockbench box-UV net (confirmed against the bbmodel handle faces)."""
import math, random
from PIL import Image

random.seed(7)
TEX = "src/main/resources/assets/ironhold/textures/item/battle_hammer_geo.png"

# top-left light, same model as the head relight
L = (-0.5, 0.8, 0.35)
ln = math.sqrt(sum(c*c for c in L)); L = tuple(c/ln for c in L)
def lit(n):
    return max(0.4, min(1.7, 1.0 + 0.34 * sum(a*b for a, b in zip(n, L))))

# head metal palette ramp (dark -> light), sampled from the head grays
RAMP = [(28,26,29),(38,36,39),(46,44,47),(53,50,53),(62,59,62),(72,69,72)]

# box-UV net for handle 2(x) x 29(y) x 2(z) at origin (120,0). VERIFIED layout.
U, V, W, Hh, D = 120, 0, 2, 29, 2
SIDES = [  # name, normal, x0, y0, w, h   (the 4 long faces)
    ("east",  (1, 0, 0),  U,           V + D, D, Hh),
    ("north", (0, 0,-1),  U + D,       V + D, W, Hh),
    ("west",  (-1,0, 0),  U + D + W,   V + D, D, Hh),
    ("south", (0, 0, 1),  U + 2*D + W, V + D, W, Hh),
]
CAPS = [   # up/down small caps
    ("up",   (0, 1, 0),  U + D,     V, W, D),
    ("down", (0,-1, 0),  U + D + W, V, W, D),
]

im = Image.open(TEX).convert("RGBA")
px = im.load()

def shade(idx, f):
    idx = max(0, min(len(RAMP)-1, idx))
    r,g,b = RAMP[idx]
    return (max(0,min(255,round(r*f))), max(0,min(255,round(g*f))),
            max(0,min(255,round(b*f))), 255)

for name, n, x0, y0, w, h in SIDES:
    f = lit(n)
    for j in range(h):                      # j: 0 at head end -> h-1 at pommel
        # base sits mid-ramp; a slow brighten toward the head; grip bands darken
        base = 3
        base += 1 if j < 3 else 0           # collar just under the head a touch lighter
        if j % 6 == 0:        base -= 2      # wrap line (dark band)
        elif j % 6 == 1:      base -= 1
        if j >= h - 2:        base -= 1      # pommel slightly darker
        for i in range(w):
            idx = base + random.randint(-1, 1)   # metal noise
            px[x0 + i, y0 + j] = shade(idx, f)

for name, n, x0, y0, w, h in CAPS:
    f = lit(n)
    for i in range(w):
        for j in range(h):
            px[x0 + i, y0 + j] = shade(4 + random.randint(-1,0), f)

im.save(TEX)
print("handle metal-shaft painted ->", TEX)
