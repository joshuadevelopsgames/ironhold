#!/usr/bin/env python3
"""Relight battle_hammer_geo.png with a consistent top-left light.

Uses the authoring .bbmodel's per-face UV rects (same 128x128 space as the geo)
to map every texel to a cube face. Each face's world normal comes from its name
(up/down/north/south/east/west). We shade each face by Lambert(normal, light) so
the whole model reads as lit from one direction. Hue/chroma preserved; only value
scaled. Gentle per-face denoise calms the original random noise.
"""
import json, sys, math
from collections import defaultdict
from PIL import Image

BBM = "scratch/hammer.bbmodel"
SRC = "tmp/hammer_relight/battle_hammer_geo.ORIGINAL.png"

lx      = float(sys.argv[1]) if len(sys.argv) > 1 else -1.0
ly      = float(sys.argv[2]) if len(sys.argv) > 2 else  1.6
lz      = float(sys.argv[3]) if len(sys.argv) > 3 else  0.7
AMBIENT = float(sys.argv[4]) if len(sys.argv) > 4 else  1.00   # BASE: average brightness (1.0 = preserve)
DIFFUSE = float(sys.argv[5]) if len(sys.argv) > 5 else  0.50   # AMP: per-face directional swing
DENOISE = float(sys.argv[6]) if len(sys.argv) > 6 else  0.40
POSAMP  = float(sys.argv[8]) if len(sys.argv) > 8 else  0.24   # per-cube form-light swing across the head
OUT     = sys.argv[7]        if len(sys.argv) > 7 else "tmp/hammer_relight/v.png"

L = (lx, ly, lz)
ln = math.sqrt(sum(c*c for c in L)); L = tuple(c/ln for c in L)

# Minecraft/Blockbench face name -> world normal
NORMAL = {
    "up":    (0, 1, 0),
    "down":  (0,-1, 0),
    "north": (0, 0,-1),
    "south": (0, 0, 1),
    "east":  (1, 0, 0),
    "west":  (-1,0, 0),
}

def lit(normal):
    # symmetric: up-facing brightens above 1.0, down-facing darkens below 1.0,
    # so overall brightness (and thus the palette) is preserved.
    d = sum(a*b for a, b in zip(normal, L))   # -1..1
    return max(0.35, min(1.7, AMBIENT + DIFFUSE * d))

im = Image.open(SRC).convert("RGBA")
W, H = im.size
px = im.load()

# collect faces: (area, normal, [texels])  -- smallest area wins per texel
bb = json.load(open(BBM))
faces = []  # (area, normal, x0,y0,x1,y1)
for e in bb["elements"]:
    fdict = e.get("faces") or {}
    for fname, fd in fdict.items():
        uv = fd.get("uv")
        if not uv or fname not in NORMAL:
            continue
        x0, y0, x1, y1 = uv
        x0, x1 = sorted((x0, x1)); y0, y1 = sorted((y0, y1))
        ix0, iy0 = int(math.floor(x0)), int(math.floor(y0))
        ix1, iy1 = int(math.ceil(x1)),  int(math.ceil(y1))
        area = max(1, (ix1-ix0)) * max(1, (iy1-iy0))
        faces.append((area, NORMAL[fname], ix0, iy0, ix1, iy1))

faces.sort(key=lambda f: -f[0])  # large first so small overwrite

owner_normal = {}
rect_id = {}
rid = 0
for area, normal, x0, y0, x1, y1 in faces:
    for x in range(x0, x1):
        for y in range(y0, y1):
            if 0 <= x < W and 0 <= y < H and px[x, y][3] > 0:
                owner_normal[(x, y)] = normal
                rect_id[(x, y)] = rid
    rid += 1

# per-rect mean for denoise
groups = defaultdict(list)
for xy, r in rect_id.items():
    groups[r].append(xy)
mean = {}
for r, pts in groups.items():
    rr = gg = bb_ = 0
    for x, y in pts:
        p = px[x, y]; rr += p[0]; gg += p[1]; bb_ += p[2]
    n = len(pts); mean[r] = (rr/n, gg/n, bb_/n)

out = im.copy(); opx = out.load()
for xy, normal in owner_normal.items():
    x, y = xy
    r, g, b, a = px[x, y]
    m = mean[rect_id[xy]]
    r = r*(1-DENOISE) + m[0]*DENOISE
    g = g*(1-DENOISE) + m[1]*DENOISE
    b = b*(1-DENOISE) + m[2]*DENOISE
    f = lit(normal)
    opx[x, y] = (
        max(0, min(255, int(round(r*f)))),
        max(0, min(255, int(round(g*f)))),
        max(0, min(255, int(round(b*f)))),
        a,
    )

out.save(OUT)
total = sum(1 for x in range(W) for y in range(H) if px[x, y][3] > 0)
print(f"L={tuple(round(c,2) for c in L)} amb={AMBIENT} diff={DIFFUSE} denoise={DENOISE}")
print(f"covered {len(owner_normal)}/{total} opaque texels -> {OUT}")
