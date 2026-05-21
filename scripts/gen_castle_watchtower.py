#!/usr/bin/env python3
"""
Generates structures/castle_watchtower.isc — a 7×22×7 medieval watchtower.

Vocabulary borrowed from vanilla pillager outposts + real medieval design:
  - 7×7 footprint, 1-block-thick walls, 5×5 interior
  - Three floors (3 blocks of headroom each) + walkable battlement roof
  - Stone-brick masonry (65/15/5/15 stone/mossy/cracked/cobble — matches castle.isc)
  - String-course slab marking each floor line
  - Centred arrow slits on each cardinal face per floor
  - Spruce door (south face), spruce-plank interior floors
  - Single ladder column rising on the north interior wall
  - Trapdoor through the roof for sentry access
  - Hanging lantern centred on each floor's ceiling
  - Crenellated parapet (alternating merlons)
  - Oak-log flagpole at NE corner with a red banner finial
"""
import random
import os
from pathlib import Path

SX, SY, SZ = 7, 22, 7
SEED = 42
OUTPUT = Path(__file__).resolve().parents[1] / "structures" / "castle_watchtower.isc"

random.seed(SEED)

# ── Palette ────────────────────────────────────────────────────────────
palette = []
palette_idx = {}

def P(bs):
    if bs not in palette_idx:
        palette_idx[bs] = len(palette)
        palette.append(bs)
    return palette_idx[bs]

AIR      = P("minecraft:air")
SB       = P("minecraft:stone_bricks")
MSB      = P("minecraft:mossy_stone_bricks")
CSB      = P("minecraft:cracked_stone_bricks")
COB      = P("minecraft:cobblestone")
PLANKS   = P("minecraft:spruce_planks")
LADDER   = P("minecraft:ladder[facing=south,waterlogged=false]")
LANTERN  = P("minecraft:lantern[hanging=true,waterlogged=false]")
DOOR_L   = P("minecraft:spruce_door[facing=south,half=lower,hinge=right,open=false,powered=false]")
DOOR_U   = P("minecraft:spruce_door[facing=south,half=upper,hinge=right,open=false,powered=false]")
BARS_EW  = P("minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
BARS_NS  = P("minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]")
SLAB_TOP = P("minecraft:stone_brick_slab[type=top,waterlogged=false]")
TRAPDOOR = P("minecraft:oak_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]")
OAK_LOG  = P("minecraft:oak_log[axis=y]")
BANNER   = P("minecraft:red_banner[rotation=8]")

# ── Grid ───────────────────────────────────────────────────────────────
g = [[[AIR for _ in range(SX)] for _ in range(SZ)] for _ in range(SY)]

def perim(x, z):
    return x == 0 or x == SX - 1 or z == 0 or z == SZ - 1

def stone():
    r = random.random()
    if r < 0.65: return SB
    if r < 0.80: return MSB
    if r < 0.87: return CSB
    return COB

# Foundation (y=0) — perimeter is masonry, interior is cobble floor
for z in range(SZ):
    for x in range(SX):
        g[0][z][x] = stone() if perim(x, z) else COB

# Three floors of walls. Floor headroom = 3 blocks each.
WALL_BANDS = [(1, 4), (5, 8), (9, 12)]
for y0, y1 in WALL_BANDS:
    for y in range(y0, y1):
        for z in range(SZ):
            for x in range(SX):
                if perim(x, z):
                    g[y][z][x] = stone()

# Inter-floor string courses at y=4, y=8 — slab on perimeter, planks inside
for fy in (4, 8):
    for z in range(SZ):
        for x in range(SX):
            g[fy][z][x] = SLAB_TOP if perim(x, z) else PLANKS

# Roof slab at y=12 — walkable solid floor
for z in range(SZ):
    for x in range(SX):
        g[12][z][x] = stone()

# Battlements at y=13 — alternating merlons on the perimeter only
for z in range(SZ):
    for x in range(SX):
        if perim(x, z) and (x + z) % 2 == 0:
            g[13][z][x] = SB

# ── Door (south face, middle column) ───────────────────────────────────
g[1][SZ - 1][3] = DOOR_L
g[2][SZ - 1][3] = DOOR_U

# ── Arrow slits — one per cardinal face per floor at mid-height ────────
# Floor 1: y=2 (but south is the door at y=2, so skip)
# Floor 2: y=6
# Floor 3: y=10
for mid_y, skip_south in [(2, True), (6, False), (10, False)]:
    g[mid_y][0][3] = BARS_EW                          # north
    g[mid_y][3][SX - 1] = BARS_NS                     # east
    g[mid_y][3][0] = BARS_NS                          # west
    if not skip_south:
        g[mid_y][SZ - 1][3] = BARS_EW                 # south

# ── Ladder column (interior, against the north wall) ──────────────────
# Ladder cells: y=1..11. Needs a solid full block immediately behind it (z=0).
LADDER_X = 3
LADDER_Z = 1
for y in range(1, 12):
    g[y][LADDER_Z][LADDER_X] = LADDER
# Force the north-wall cells behind ladder at slab levels to be full stone,
# not slabs — ladders need a full side face to attach to.
for y in (4, 8):
    g[y][0][LADDER_X] = SB

# ── Roof trapdoor (lid at top of ladder) ──────────────────────────────
g[12][LADDER_Z][LADDER_X] = TRAPDOOR

# ── Hanging lanterns, one per floor (interior centre) ─────────────────
for y in (3, 7, 11):
    g[y][3][3] = LANTERN

# ── Flagpole at NE corner (x=6, z=0), rising above battlements ────────
FLAG_X, FLAG_Z = SX - 1, 0
for y in range(14, 21):
    g[y][FLAG_Z][FLAG_X] = OAK_LOG
g[21][FLAG_Z][FLAG_X] = BANNER

# ── Encode to ISC ─────────────────────────────────────────────────────
def code(i):
    hi = i // 36
    lo = i % 36
    def d(n):
        return chr(ord('0') + n) if n < 10 else chr(ord('a') + n - 10)
    return d(hi) + d(lo)

lines = []
lines.append("# Ironhold Structure Code v1")
lines.append(f"size {SX} {SY} {SZ}")
lines.append("palette")
for i, bs in enumerate(palette):
    lines.append(f"  {code(i)} {bs}")
lines.append("body")
for y in range(SY):
    lines.append(f"y={y}")
    for z in range(SZ):
        lines.append("  " + " ".join(code(g[y][z][x]) for x in range(SX)))

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_text("\n".join(lines) + "\n")

solid = sum(1 for y in range(SY) for z in range(SZ) for x in range(SX) if g[y][z][x] != AIR)
print(f"wrote {OUTPUT} — {SX}×{SY}×{SZ}, {solid} solid blocks, {len(palette)} palette entries")
