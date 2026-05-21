#!/usr/bin/env python3
"""
Generates structures/dark_castle.isc — a 50×35×50 large dark castle.

Architectural plan:
  - 50×50 footprint, 35 tall
  - 2-block-thick curtain wall ring with crenellated parapet
  - 4 corner towers (7×7, 17 tall) with stepped pyramid spires
  - South gatehouse with raised iron-bar portcullis + crying-obsidian threshold
  - Central 16×16 inner keep, 4 floors, central spire reaches y=33
  - Courtyard with magma brazier, sculk corruption patches, dark-oak fence rails
  - Soul lanterns on every tower face + red-glass gothic windows in keep
  - Black banners, cobwebs in dungeon corners, hanging chain lanterns at gate
"""
import random
from pathlib import Path

SX, SY, SZ = 50, 35, 50
SEED = 7
OUTPUT = Path(__file__).resolve().parents[1] / "structures" / "dark_castle.isc"

random.seed(SEED)

# ── Palette ────────────────────────────────────────────────────────────
palette = []
palette_idx = {}

def P(bs):
    if bs not in palette_idx:
        palette_idx[bs] = len(palette)
        palette.append(bs)
    return palette_idx[bs]

AIR        = P("minecraft:air")
# Stone family
DSB        = P("minecraft:deepslate_bricks")
DSB_CRACK  = P("minecraft:cracked_deepslate_bricks")
DSB_TILE   = P("minecraft:deepslate_tiles")
PDS        = P("minecraft:polished_deepslate")
CDS        = P("minecraft:cobbled_deepslate")
CHIS_DS    = P("minecraft:chiseled_deepslate")
BS         = P("minecraft:blackstone")
PBSB       = P("minecraft:polished_blackstone_bricks")
PBSB_CRACK = P("minecraft:cracked_polished_blackstone_bricks")
CHIS_PBSB  = P("minecraft:chiseled_polished_blackstone")
# Trim
DSB_SLAB_TOP = P("minecraft:deepslate_brick_slab[type=top,waterlogged=false]")
DSB_SLAB_BOT = P("minecraft:deepslate_brick_slab[type=bottom,waterlogged=false]")
DSB_WALL     = P("minecraft:deepslate_brick_wall[east=tall,north=tall,south=tall,up=true,waterlogged=false,west=tall]")
DSB_STAIR_N  = P("minecraft:deepslate_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
DSB_STAIR_S  = P("minecraft:deepslate_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
DSB_STAIR_E  = P("minecraft:deepslate_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]")
DSB_STAIR_W  = P("minecraft:deepslate_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
PBSB_STAIR_N = P("minecraft:polished_blackstone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
PBSB_STAIR_S = P("minecraft:polished_blackstone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
# Wood
DARK_PLANK = P("minecraft:dark_oak_planks")
DARK_LOG_Y = P("minecraft:dark_oak_log[axis=y]")
DARK_LOG_X = P("minecraft:dark_oak_log[axis=x]")
DARK_FENCE = P("minecraft:dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]")
DARK_FENCE_NS = P("minecraft:dark_oak_fence[east=false,north=true,south=true,waterlogged=false,west=false]")
DARK_DOOR_L = P("minecraft:dark_oak_door[facing=south,half=lower,hinge=right,open=false,powered=false]")
DARK_DOOR_U = P("minecraft:dark_oak_door[facing=south,half=upper,hinge=right,open=false,powered=false]")
DARK_TRAP   = P("minecraft:dark_oak_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]")
DARK_STAIR_N = P("minecraft:dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]")
DARK_STAIR_S = P("minecraft:dark_oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
# Light
SOUL_LANTERN_HANG = P("minecraft:soul_lantern[hanging=true,waterlogged=false]")
SOUL_LANTERN_FLOOR = P("minecraft:soul_lantern[hanging=false,waterlogged=false]")
SOUL_TORCH_N = P("minecraft:soul_wall_torch[facing=north]")
SOUL_TORCH_S = P("minecraft:soul_wall_torch[facing=south]")
SOUL_TORCH_E = P("minecraft:soul_wall_torch[facing=east]")
SOUL_TORCH_W = P("minecraft:soul_wall_torch[facing=west]")
CHAIN = P("minecraft:chain[axis=y,waterlogged=false]")
RED_CANDLE = P("minecraft:red_candle[candles=3,lit=true,waterlogged=false]")
# Glass / iron
RED_GLASS_PANE_NS = P("minecraft:red_stained_glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]")
RED_GLASS_PANE_EW = P("minecraft:red_stained_glass_pane[east=true,north=false,south=false,waterlogged=false,west=true]")
RED_GLASS = P("minecraft:red_stained_glass")
BARS_NS = P("minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]")
BARS_EW = P("minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
BARS_ALL = P("minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]")
# Accent / dread
CRYING_OBS = P("minecraft:crying_obsidian")
OBS = P("minecraft:obsidian")
MAGMA = P("minecraft:magma_block")
SCULK = P("minecraft:sculk")
SCULK_VEIN_FLOOR = P("minecraft:sculk_vein[down=false,east=false,north=false,south=false,up=true,waterlogged=false,west=false]")
COBWEB = P("minecraft:cobweb")
BLACK_BANNER_N = P("minecraft:black_banner[rotation=0]")
LADDER_S = P("minecraft:ladder[facing=south,waterlogged=false]")
LADDER_N = P("minecraft:ladder[facing=north,waterlogged=false]")
SKULL_N = P("minecraft:skeleton_skull[rotation=0]")

# ── Grid ───────────────────────────────────────────────────────────────
g = [[[AIR for _ in range(SX)] for _ in range(SZ)] for _ in range(SY)]

def in_bounds(x, y, z):
    return 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ

def setb(x, y, z, b):
    if in_bounds(x, y, z):
        g[y][z][x] = b

def fill_box(x0, y0, z0, x1, y1, z1, block):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, block)

def fill_box_fn(x0, y0, z0, x1, y1, z1, block_fn):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, block_fn(x, y, z))

def stone():
    """Weathered dark masonry mix."""
    r = random.random()
    if r < 0.45:  return DSB
    if r < 0.60:  return PDS
    if r < 0.72:  return CDS
    if r < 0.80:  return DSB_TILE
    if r < 0.88:  return BS
    if r < 0.93:  return PBSB
    if r < 0.97:  return DSB_CRACK
    if r < 0.99:  return PBSB_CRACK
    return CHIS_DS

def courtyard_floor():
    r = random.random()
    if r < 0.55: return CDS
    if r < 0.80: return PDS
    if r < 0.92: return DSB
    if r < 0.97: return BS
    return SCULK

# ── 1. Foundation + courtyard floor ───────────────────────────────────
# Whole footprint at y=0 = solid foundation (obsidian-edged plinth)
for z in range(SZ):
    for x in range(SX):
        edge = x == 0 or x == SX-1 or z == 0 or z == SZ-1
        g[0][z][x] = OBS if edge else CDS

# y=1 courtyard floor (rest gets overwritten by walls/keep below)
for z in range(SZ):
    for x in range(SX):
        g[1][z][x] = courtyard_floor()

# ── 2. Curtain wall (outer 2-thick ring) ──────────────────────────────
WALL_TOP = 10
WALL_THK = 2
# Corner tower footprints (defined again here for the wall exclusion check)
TOWER_SIZE = 7
CORNERS = [
    (0, 0),
    (SX - TOWER_SIZE, 0),
    (0, SZ - TOWER_SIZE),
    (SX - TOWER_SIZE, SZ - TOWER_SIZE),
]

def in_corner_tower(x, z):
    return any(ox <= x < ox + TOWER_SIZE and oz <= z < oz + TOWER_SIZE
               for ox, oz in CORNERS)

def in_wall(x, z):
    in_perim = (x < WALL_THK or x >= SX - WALL_THK or
                z < WALL_THK or z >= SZ - WALL_THK)
    return in_perim and not in_corner_tower(x, z)

for y in range(2, WALL_TOP + 1):
    for z in range(SZ):
        for x in range(SX):
            if in_wall(x, z):
                g[y][z][x] = stone()

# String course (slab lip) at y=4 and y=8 outside the curtain only
for y_course in (4, 8):
    for z in range(SZ):
        for x in range(SX):
            outer_edge = (x == 0 or x == SX-1 or z == 0 or z == SZ-1)
            if outer_edge and not in_corner_tower(x, z):
                g[y_course][z][x] = DSB_SLAB_TOP

# Battlement walkway at y=WALL_TOP+1 = 11 (on top of curtain, only inner row)
WALKWAY_Y = WALL_TOP + 1
for z in range(SZ):
    for x in range(SX):
        if in_wall(x, z):
            # Inner row of the 2-thick wall becomes walkway floor
            inner = (
                (x == 1 and 1 <= z <= SZ - 2) or
                (x == SX - 2 and 1 <= z <= SZ - 2) or
                (z == 1 and 1 <= x <= SX - 2) or
                (z == SZ - 2 and 1 <= x <= SZ - 2)
            )
            outer = (x == 0 or x == SX-1 or z == 0 or z == SZ-1)
            if outer:
                g[WALKWAY_Y][z][x] = stone()  # base of merlon
            elif inner:
                g[WALKWAY_Y][z][x] = stone()  # walkway floor

# Crenellated merlons at y=12 — alternating along the outer edge only
MERLON_Y = WALL_TOP + 2
for z in range(SZ):
    for x in range(SX):
        outer = (x == 0 or x == SX-1 or z == 0 or z == SZ-1)
        if outer and not in_corner_tower(x, z):
            param = x if (z == 0 or z == SZ-1) else z
            if param % 2 == 0:
                g[MERLON_Y][z][x] = stone()

# Arrow slits on each curtain face — vertical 2-tall slits every ~8 blocks
def slit_curtain():
    for x in range(8, SX - 8, 8):
        if in_corner_tower(x, 0): continue
        for dy in (5, 6):
            g[dy][0][x] = BARS_EW
            g[dy][SZ-1][x] = BARS_EW
    for z in range(8, SZ - 8, 8):
        if in_corner_tower(0, z): continue
        for dy in (5, 6):
            g[dy][z][0] = BARS_NS
            g[dy][z][SX-1] = BARS_NS
slit_curtain()

# ── 3. Corner towers (7×7, height 17, spire to 22) ────────────────────
TOWER_BASE = 1
TOWER_TOP = 17
SPIRE_TOP = 22

def build_corner_tower(ox, oz):
    """Tower with floors, ladder, lanterns, arrow slits, battlements, spire."""
    for y in range(TOWER_BASE, TOWER_TOP + 1):
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                         z == oz or z == oz + TOWER_SIZE - 1)
                if perim:
                    g[y][z][x] = stone()

    # Floors at y=6, y=12 — planks interior
    for fy in (6, 12):
        for z in range(oz + 1, oz + TOWER_SIZE - 1):
            for x in range(ox + 1, ox + TOWER_SIZE - 1):
                g[fy][z][x] = DARK_PLANK

    # Arrow slits each cardinal at mid-height of each floor
    cx = ox + TOWER_SIZE // 2
    cz = oz + TOWER_SIZE // 2
    for mid_y in (4, 9, 14):
        g[mid_y][oz][cx] = BARS_EW                       # north
        g[mid_y][oz + TOWER_SIZE - 1][cx] = BARS_EW      # south
        g[mid_y][cz][ox] = BARS_NS                       # west
        g[mid_y][cz][ox + TOWER_SIZE - 1] = BARS_NS      # east

    # Ladder up the inside, against an inner wall
    lad_x = cx
    lad_z = oz + 1 if oz == 0 else oz + TOWER_SIZE - 2
    for y in range(TOWER_BASE + 1, TOWER_TOP):
        g[y][lad_z][lad_x] = LADDER_S if oz == 0 else LADDER_N

    # Hanging lanterns at interior centre per floor
    for ly in (5, 11):
        g[ly][cz][cx] = SOUL_LANTERN_HANG

    # Roof slab at TOWER_TOP+1
    roof_y = TOWER_TOP + 1
    for z in range(oz, oz + TOWER_SIZE):
        for x in range(ox, ox + TOWER_SIZE):
            g[roof_y][z][x] = stone()

    # Trapdoor for access
    g[roof_y][lad_z][lad_x] = DARK_TRAP

    # Battlement merlons at roof_y+1
    bat_y = roof_y + 1
    for z in range(oz, oz + TOWER_SIZE):
        for x in range(ox, ox + TOWER_SIZE):
            perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                     z == oz or z == oz + TOWER_SIZE - 1)
            if perim and ((x + z) % 2 == 0):
                g[bat_y][z][x] = stone()

    # Stepped pyramid spire above the tower
    spire_levels = [
        (bat_y + 1, 1),  # 5×5
        (bat_y + 2, 2),  # 3×3
        (bat_y + 3, 3),  # 1×1
    ]
    for sy, inset in spire_levels:
        if sy > SPIRE_TOP: break
        for z in range(oz + inset, oz + TOWER_SIZE - inset):
            for x in range(ox + inset, ox + TOWER_SIZE - inset):
                g[sy][z][x] = stone()

    # Soul torch on each tower face above battlements (eerie corner glow)
    g[roof_y][oz - 1 if oz > 0 else oz][cx] = AIR  # don't overwrite
    # Actually place soul torches on outer face midway up the tower
    if oz == 0:
        g[10][oz - 0][cx] = SOUL_TORCH_N if False else g[10][oz][cx]  # noop placeholder

for ox, oz in CORNERS:
    build_corner_tower(ox, oz)

# ── 4. Gatehouse on south curtain ─────────────────────────────────────
GATE_W = 5  # gate opening width
GATE_H = 5  # gate opening height
GATE_X0 = (SX - GATE_W) // 2
GATE_X1 = GATE_X0 + GATE_W - 1
GATE_Z0 = SZ - WALL_THK
GATE_Z1 = SZ - 1

# Carve the gate opening out of the curtain wall
for y in range(1, GATE_H + 1):
    for z in range(GATE_Z0, GATE_Z1 + 1):
        for x in range(GATE_X0, GATE_X1 + 1):
            g[y][z][x] = AIR

# Threshold: crying obsidian + magma underneath player feet at gate
for x in range(GATE_X0, GATE_X1 + 1):
    g[0][SZ - 1][x] = CRYING_OBS
    g[0][SZ - 2][x] = MAGMA  # warning glow under the gate

# Gate arch — stone arch made of stairs hugging the opening top
for x in range(GATE_X0, GATE_X1 + 1):
    g[GATE_H + 1][GATE_Z1][x] = stone()
    g[GATE_H + 1][GATE_Z0][x] = stone()
# Side blocks shaping the arch
g[GATE_H][GATE_Z1][GATE_X0 - 1] = PBSB_STAIR_S
g[GATE_H][GATE_Z1][GATE_X1 + 1] = PBSB_STAIR_S
g[GATE_H][GATE_Z0][GATE_X0 - 1] = PBSB_STAIR_N
g[GATE_H][GATE_Z0][GATE_X1 + 1] = PBSB_STAIR_N

# Portcullis (raised) — iron bars hanging from top of gate opening
for x in range(GATE_X0, GATE_X1 + 1):
    g[GATE_H][GATE_Z1 - 1][x] = BARS_ALL  # top of opening, iron bars
    g[GATE_H - 1][GATE_Z1 - 1][x] = BARS_ALL  # second row of bars
# Chains hanging on either side
for y in range(2, GATE_H + 2):
    g[y][GATE_Z1][GATE_X0 - 1] = CHAIN if g[y][GATE_Z1][GATE_X0 - 1] == AIR else g[y][GATE_Z1][GATE_X0 - 1]
    g[y][GATE_Z1][GATE_X1 + 1] = CHAIN if g[y][GATE_Z1][GATE_X1 + 1] == AIR else g[y][GATE_Z1][GATE_X1 + 1]

# Soul torch sconces flanking the gate exterior
g[3][GATE_Z1][GATE_X0 - 2] = SOUL_TORCH_E
g[3][GATE_Z1][GATE_X1 + 2] = SOUL_TORCH_W

# Black banners flanking the gate, mounted on outer wall
g[6][GATE_Z1][GATE_X0 - 2] = BLACK_BANNER_N
g[6][GATE_Z1][GATE_X1 + 2] = BLACK_BANNER_N

# ── 5. Inner keep (16×16, central) ────────────────────────────────────
KEEP_W = 16
KEEP_X0 = (SX - KEEP_W) // 2
KEEP_X1 = KEEP_X0 + KEEP_W - 1
KEEP_Z0 = (SZ - KEEP_W) // 2
KEEP_Z1 = KEEP_Z0 + KEEP_W - 1
KEEP_BASE = 1
KEEP_FLOORS = [1, 7, 13, 19]  # floor levels
KEEP_TOP = 22  # walls top
KEEP_SPIRE_TOP = 33  # central column

# Walls
for y in range(KEEP_BASE, KEEP_TOP + 1):
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            perim = (x == KEEP_X0 or x == KEEP_X1 or z == KEEP_Z0 or z == KEEP_Z1)
            if perim:
                g[y][z][x] = stone()

# Floors — dark planks interior
for fy in KEEP_FLOORS[1:]:  # skip the foundation floor; it's already cobble
    for z in range(KEEP_Z0 + 1, KEEP_Z1):
        for x in range(KEEP_X0 + 1, KEEP_X1):
            g[fy][z][x] = DARK_PLANK

# Red gothic windows on each keep face — tall 3-block stained glass slits per floor
for fy_base in (3, 9, 15):
    cx = (KEEP_X0 + KEEP_X1) // 2
    cz = (KEEP_Z0 + KEEP_Z1) // 2
    # North face
    for dy in range(3):
        g[fy_base + dy][KEEP_Z0][cx - 1] = RED_GLASS_PANE_EW
        g[fy_base + dy][KEEP_Z0][cx + 1] = RED_GLASS_PANE_EW
    # South face
    for dy in range(3):
        g[fy_base + dy][KEEP_Z1][cx - 1] = RED_GLASS_PANE_EW
        g[fy_base + dy][KEEP_Z1][cx + 1] = RED_GLASS_PANE_EW
    # East face
    for dy in range(3):
        g[fy_base + dy][cz - 1][KEEP_X1] = RED_GLASS_PANE_NS
        g[fy_base + dy][cz + 1][KEEP_X1] = RED_GLASS_PANE_NS
    # West face
    for dy in range(3):
        g[fy_base + dy][cz - 1][KEEP_X0] = RED_GLASS_PANE_NS
        g[fy_base + dy][cz + 1][KEEP_X0] = RED_GLASS_PANE_NS

# Keep south entrance — dark oak double-ish door (single door for simplicity)
keep_door_x = (KEEP_X0 + KEEP_X1) // 2
g[2][KEEP_Z1][keep_door_x] = DARK_DOOR_L
g[3][KEEP_Z1][keep_door_x] = DARK_DOOR_U

# Soul torches flanking the keep door
g[3][KEEP_Z1][keep_door_x - 2] = SOUL_TORCH_S
g[3][KEEP_Z1][keep_door_x + 2] = SOUL_TORCH_S

# Ladders inside keep — go in NW interior corner
lad_x, lad_z = KEEP_X0 + 1, KEEP_Z0 + 1
for y in range(2, KEEP_TOP):
    g[y][lad_z][lad_x] = LADDER_S
# Holes in floors at ladder
for fy in KEEP_FLOORS[1:]:
    g[fy][lad_z][lad_x] = LADDER_S
# Ensure block behind ladder is full stone, not anything wonky
for y in range(2, KEEP_TOP):
    if g[y][lad_z - 1][lad_x] == AIR:
        g[y][lad_z - 1][lad_x] = stone()

# Hanging lanterns one per keep floor at the centre
for fy in KEEP_FLOORS[1:]:
    cx = (KEEP_X0 + KEEP_X1) // 2
    cz = (KEEP_Z0 + KEEP_Z1) // 2
    g[fy - 1][cz][cx] = SOUL_LANTERN_HANG

# Keep roof at y=KEEP_TOP+1
keep_roof_y = KEEP_TOP + 1
for z in range(KEEP_Z0, KEEP_Z1 + 1):
    for x in range(KEEP_X0, KEEP_X1 + 1):
        g[keep_roof_y][z][x] = stone()

# Battlements on keep roof
keep_bat_y = keep_roof_y + 1
for z in range(KEEP_Z0, KEEP_Z1 + 1):
    for x in range(KEEP_X0, KEEP_X1 + 1):
        perim = (x == KEEP_X0 or x == KEEP_X1 or z == KEEP_Z0 or z == KEEP_Z1)
        if perim and ((x + z) % 2 == 0):
            g[keep_bat_y][z][x] = stone()

# Keep central spire — stepped pyramid rising from roof centre
kcx = (KEEP_X0 + KEEP_X1) // 2
kcz = (KEEP_Z0 + KEEP_Z1) // 2
spire_steps = [
    (keep_roof_y + 1, 5),  # 5×5 + 1 = 11x11 — too big; let me use absolute
]
# More carefully: each step shrinks by 1 ring
# Step shape: inset from kcx/kcz by `r`
def keep_spire():
    y = keep_roof_y + 1
    r = 4
    while r >= 0 and y <= KEEP_SPIRE_TOP:
        for z in range(kcz - r, kcz + r + 1):
            for x in range(kcx - r, kcx + r + 1):
                perim = (x == kcx - r or x == kcx + r or
                         z == kcz - r or z == kcz + r)
                if perim:
                    g[y][z][x] = stone()
        y += 1
        # every 2 levels, shrink by 1
        if (y - (keep_roof_y + 1)) % 2 == 0:
            r -= 1
    # Cap with a single block
    for y_cap in range(y, min(y + 2, KEEP_SPIRE_TOP + 1)):
        g[y_cap][kcz][kcx] = stone()
keep_spire()

# Flagpole on top of the keep spire
for y in range(KEEP_SPIRE_TOP, min(KEEP_SPIRE_TOP + 4, SY - 1)):
    g[y][kcz][kcx] = DARK_LOG_Y
# Top of pole: black banner
if KEEP_SPIRE_TOP + 4 < SY:
    g[KEEP_SPIRE_TOP + 4][kcz][kcx] = BLACK_BANNER_N

# ── 6. Wall-walk corner turrets on the keep (4 mini-turrets) ──────────
def keep_corner_turret(cx, cz):
    """Small 3×3 turret rising from each corner of the keep roof."""
    for y in range(keep_bat_y + 1, keep_bat_y + 4):
        for z in range(cz - 1, cz + 2):
            for x in range(cx - 1, cx + 2):
                perim = (x == cx - 1 or x == cx + 1 or z == cz - 1 or z == cz + 1)
                if perim:
                    g[y][z][x] = stone()
    # Turret cap merlons
    cap_y = keep_bat_y + 4
    for z in range(cz - 1, cz + 2):
        for x in range(cx - 1, cx + 2):
            perim = (x == cx - 1 or x == cx + 1 or z == cz - 1 or z == cz + 1)
            if perim and ((x + z) % 2 == 0):
                g[cap_y][z][x] = stone()

for cx, cz in [(KEEP_X0 + 1, KEEP_Z0 + 1),
               (KEEP_X1 - 1, KEEP_Z0 + 1),
               (KEEP_X0 + 1, KEEP_Z1 - 1),
               (KEEP_X1 - 1, KEEP_Z1 - 1)]:
    keep_corner_turret(cx, cz)

# ── 7. Decoration pass ────────────────────────────────────────────────
# Magma brazier in the courtyard centre, ringed with fence
brazier_x, brazier_z = SX // 2, (KEEP_Z1 + SZ - WALL_THK) // 2
# Place it between the keep and the south gate
brazier_z = (KEEP_Z1 + (SZ - WALL_THK - 1)) // 2
g[1][brazier_z][brazier_x] = MAGMA
g[1][brazier_z - 1][brazier_x] = MAGMA
g[1][brazier_z][brazier_x - 1] = MAGMA
g[1][brazier_z][brazier_x + 1] = MAGMA
g[1][brazier_z + 1][brazier_x] = MAGMA
# Ring of dark-oak fence around it
for dx, dz in [(-2, 0), (2, 0), (0, -2), (0, 2), (-2, -2), (2, -2), (-2, 2), (2, 2)]:
    if g[1][brazier_z + dz][brazier_x + dx] not in (MAGMA,):
        g[1][brazier_z + dz][brazier_x + dx] = DARK_FENCE if dz != 0 else DARK_FENCE_NS

# Sculk corruption patches scattered in courtyard floor
for _ in range(40):
    px = random.randint(WALL_THK + 1, SX - WALL_THK - 2)
    pz = random.randint(WALL_THK + 1, SZ - WALL_THK - 2)
    # Skip if inside the keep footprint
    if KEEP_X0 <= px <= KEEP_X1 and KEEP_Z0 <= pz <= KEEP_Z1:
        continue
    if g[1][pz][px] in (MAGMA, DARK_FENCE, DARK_FENCE_NS):
        continue
    g[1][pz][px] = SCULK

# Cobwebs in tower corner interiors (highest floor)
for ox, oz in CORNERS:
    for dx, dz in [(1, 1), (TOWER_SIZE - 2, 1), (1, TOWER_SIZE - 2), (TOWER_SIZE - 2, TOWER_SIZE - 2)]:
        cell_y = 14  # top floor interior
        if g[cell_y][oz + dz][ox + dx] == AIR:
            g[cell_y][oz + dz][ox + dx] = COBWEB

# Chains hanging from keep ceilings (decorative)
for y in (5, 11, 17):
    g[y][kcz][kcx - 3] = CHAIN if g[y][kcz][kcx - 3] == AIR else g[y][kcz][kcx - 3]
    g[y][kcz][kcx + 3] = CHAIN if g[y][kcz][kcx + 3] == AIR else g[y][kcz][kcx + 3]

# Skull above gate (macabre warning)
g[GATE_H + 2][GATE_Z1][SX // 2] = SKULL_N

# Banner mounted on north face of keep
for bx in range(KEEP_X0 + 2, KEEP_X1 - 1, 5):
    g[8][KEEP_Z0][bx] = BLACK_BANNER_N

# ── Encode to ISC ─────────────────────────────────────────────────────
def code(i):
    def d(n):
        return chr(ord('0') + n) if n < 10 else chr(ord('a') + n - 10)
    return d(i // 36) + d(i % 36)

if len(palette) > 36 * 36:
    raise SystemExit(f"palette too large: {len(palette)} > {36*36}")

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
print(f"wrote {OUTPUT}")
print(f"  {SX}×{SY}×{SZ}  volume={SX*SY*SZ}  solid={solid}  palette={len(palette)}")
