#!/usr/bin/env python3
"""
Castle of the Dread Crown — dark_castle_v2.isc.

60×50×60. A programmed, concentric fortress with five "construction eras"
worth of detail, four uniquely-designed corner towers, a multi-stage
gatehouse, courtyard buildings, and a keep with crypt → undercroft →
great hall → solar → chapel → spire vertical program.

Coordinate system:
  y=0..3   Crypt (underground when pasted on flat ground)
  y=4      Courtyard ground level
  y=4..15  Outer curtain wall and tower bases
  y=4..23  Corner towers
  y=10..18 Keep great hall (DOUBLE height)
  y=4..48  Total keep height including spire and flagpole
"""
import random
from pathlib import Path

SX, SY, SZ = 60, 50, 60
SEED = 13
OUT = Path(__file__).resolve().parents[1] / "structures" / "dark_castle_v2.isc"

random.seed(SEED)

# ── Palette ────────────────────────────────────────────────────────────
palette = []
palette_idx = {}

def P(bs):
    if bs not in palette_idx:
        palette_idx[bs] = len(palette)
        palette.append(bs)
    return palette_idx[bs]

AIR = P("minecraft:air")

# Stone family
DSB        = P("minecraft:deepslate_bricks")
DSB_CRACK  = P("minecraft:cracked_deepslate_bricks")
DSB_TILE   = P("minecraft:deepslate_tiles")
PDS        = P("minecraft:polished_deepslate")
CDS        = P("minecraft:cobbled_deepslate")
CHIS_DS    = P("minecraft:chiseled_deepslate")
BS         = P("minecraft:blackstone")
PBS        = P("minecraft:polished_blackstone")
PBSB       = P("minecraft:polished_blackstone_bricks")
PBSB_CRACK = P("minecraft:cracked_polished_blackstone_bricks")
CHIS_PBSB  = P("minecraft:chiseled_polished_blackstone")
OBS        = P("minecraft:obsidian")
CRYING_OBS = P("minecraft:crying_obsidian")
MAGMA      = P("minecraft:magma_block")

# Slabs (top/bottom)
DSB_SLAB_TOP = P("minecraft:deepslate_brick_slab[type=top,waterlogged=false]")
DSB_SLAB_BOT = P("minecraft:deepslate_brick_slab[type=bottom,waterlogged=false]")
PBSB_SLAB_TOP = P("minecraft:polished_blackstone_brick_slab[type=top,waterlogged=false]")
PBSB_SLAB_BOT = P("minecraft:polished_blackstone_brick_slab[type=bottom,waterlogged=false]")
PDS_SLAB_TOP = P("minecraft:polished_deepslate_slab[type=top,waterlogged=false]")
PDS_SLAB_BOT = P("minecraft:polished_deepslate_slab[type=bottom,waterlogged=false]")
CDS_SLAB_TOP = P("minecraft:cobbled_deepslate_slab[type=top,waterlogged=false]")

# Stairs (4 facings, bottom)
def stair(block, facing, half="bottom"):
    return P(f"minecraft:{block}_stairs[facing={facing},half={half},shape=straight,waterlogged=false]")

DSB_STAIR_N  = stair("deepslate_brick", "north")
DSB_STAIR_S  = stair("deepslate_brick", "south")
DSB_STAIR_E  = stair("deepslate_brick", "east")
DSB_STAIR_W  = stair("deepslate_brick", "west")
DSB_STAIR_N_T = stair("deepslate_brick", "north", "top")
DSB_STAIR_S_T = stair("deepslate_brick", "south", "top")
DSB_STAIR_E_T = stair("deepslate_brick", "east",  "top")
DSB_STAIR_W_T = stair("deepslate_brick", "west",  "top")
PDS_STAIR_N  = stair("polished_deepslate", "north")
PDS_STAIR_S  = stair("polished_deepslate", "south")
PDS_STAIR_E  = stair("polished_deepslate", "east")
PDS_STAIR_W  = stair("polished_deepslate", "west")
PBSB_STAIR_N = stair("polished_blackstone_brick", "north")
PBSB_STAIR_S = stair("polished_blackstone_brick", "south")
PBSB_STAIR_E = stair("polished_blackstone_brick", "east")
PBSB_STAIR_W = stair("polished_blackstone_brick", "west")
CDS_STAIR_N  = stair("cobbled_deepslate", "north")
CDS_STAIR_S  = stair("cobbled_deepslate", "south")

# Walls
DSB_WALL_X = P("minecraft:deepslate_brick_wall[east=tall,north=none,south=none,up=true,waterlogged=false,west=tall]")
DSB_WALL_Z = P("minecraft:deepslate_brick_wall[east=none,north=tall,south=tall,up=true,waterlogged=false,west=none]")
DSB_WALL_ALL = P("minecraft:deepslate_brick_wall[east=tall,north=tall,south=tall,up=true,waterlogged=false,west=tall]")
DSB_WALL_POST = P("minecraft:deepslate_brick_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]")

# Dark oak
DARK_PLANK = P("minecraft:dark_oak_planks")
DARK_LOG_Y = P("minecraft:dark_oak_log[axis=y]")
DARK_LOG_X = P("minecraft:dark_oak_log[axis=x]")
DARK_LOG_Z = P("minecraft:dark_oak_log[axis=z]")
DARK_FENCE_EW = P("minecraft:dark_oak_fence[east=true,north=false,south=false,waterlogged=false,west=true]")
DARK_FENCE_NS = P("minecraft:dark_oak_fence[east=false,north=true,south=true,waterlogged=false,west=false]")
DARK_FENCE_POST = P("minecraft:dark_oak_fence[east=false,north=false,south=false,waterlogged=false,west=false]")
DARK_DOOR_S_L = P("minecraft:dark_oak_door[facing=south,half=lower,hinge=right,open=false,powered=false]")
DARK_DOOR_S_U = P("minecraft:dark_oak_door[facing=south,half=upper,hinge=right,open=false,powered=false]")
DARK_DOOR_N_L = P("minecraft:dark_oak_door[facing=north,half=lower,hinge=right,open=false,powered=false]")
DARK_DOOR_N_U = P("minecraft:dark_oak_door[facing=north,half=upper,hinge=right,open=false,powered=false]")
DARK_TRAP_TOP = P("minecraft:dark_oak_trapdoor[facing=south,half=top,open=false,powered=false,waterlogged=false]")
DARK_STAIR_N = stair("dark_oak", "north")
DARK_STAIR_S = stair("dark_oak", "south")
DARK_STAIR_E = stair("dark_oak", "east")
DARK_STAIR_W = stair("dark_oak", "west")

# Lighting / decor
SOUL_LANTERN_HANG = P("minecraft:soul_lantern[hanging=true,waterlogged=false]")
SOUL_LANTERN_GND = P("minecraft:soul_lantern[hanging=false,waterlogged=false]")
LANTERN_HANG = P("minecraft:lantern[hanging=true,waterlogged=false]")
SOUL_TORCH_N = P("minecraft:soul_wall_torch[facing=north]")
SOUL_TORCH_S = P("minecraft:soul_wall_torch[facing=south]")
SOUL_TORCH_E = P("minecraft:soul_wall_torch[facing=east]")
SOUL_TORCH_W = P("minecraft:soul_wall_torch[facing=west]")
SOUL_TORCH_FLOOR = P("minecraft:soul_torch")
TORCH_FLOOR = P("minecraft:torch")
RED_CANDLE = P("minecraft:red_candle[candles=3,lit=true,waterlogged=false]")
WHITE_CANDLE = P("minecraft:white_candle[candles=2,lit=true,waterlogged=false]")
BLACK_CANDLE = P("minecraft:black_candle[candles=4,lit=true,waterlogged=false]")
CHAIN_Y = P("minecraft:chain[axis=y,waterlogged=false]")
CHAIN_X = P("minecraft:chain[axis=x,waterlogged=false]")
BELL = P("minecraft:bell[attachment=ceiling,facing=north,powered=false]")

# Glass / iron / barriers
RED_GLASS = P("minecraft:red_stained_glass")
RED_GLASS_PANE_EW = P("minecraft:red_stained_glass_pane[east=true,north=false,south=false,waterlogged=false,west=true]")
RED_GLASS_PANE_NS = P("minecraft:red_stained_glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]")
BLACK_GLASS = P("minecraft:black_stained_glass")
BARS_EW = P("minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")
BARS_NS = P("minecraft:iron_bars[east=false,north=true,south=true,waterlogged=false,west=false]")
BARS_ALL = P("minecraft:iron_bars[east=true,north=true,south=true,waterlogged=false,west=true]")
BARS_POST = P("minecraft:iron_bars[east=false,north=false,south=false,waterlogged=false,west=false]")

# Furnishings
BLACK_BANNER_N = P("minecraft:black_banner[rotation=0]")
BLACK_BANNER_S = P("minecraft:black_banner[rotation=8]")
RED_BANNER_E = P("minecraft:red_banner[rotation=4]")
RED_BANNER_W = P("minecraft:red_banner[rotation=12]")
RED_CARPET = P("minecraft:red_carpet")
BLACK_CARPET = P("minecraft:black_carpet")
CAULDRON = P("minecraft:cauldron")
ANVIL = P("minecraft:anvil[facing=north]")
GRINDSTONE = P("minecraft:grindstone[face=floor,facing=north]")
SMITHING = P("minecraft:smithing_table")
LECTERN = P("minecraft:lectern[facing=south,has_book=false,powered=false]")
LECTERN_W = P("minecraft:lectern[facing=west,has_book=false,powered=false]")
CHEST_N = P("minecraft:chest[facing=north,type=single,waterlogged=false]")
CHEST_S = P("minecraft:chest[facing=south,type=single,waterlogged=false]")
CHEST_W = P("minecraft:chest[facing=west,type=single,waterlogged=false]")
BARREL_UP = P("minecraft:barrel[facing=up,open=false]")
BARREL_N = P("minecraft:barrel[facing=north,open=false]")
HAY = P("minecraft:hay_block[axis=z]")
HAY_X = P("minecraft:hay_block[axis=x]")
COBWEB = P("minecraft:cobweb")
SCULK = P("minecraft:sculk")
SOUL_FIRE = P("minecraft:soul_fire")
FIRE = P("minecraft:fire[age=0]")
SKULL_0 = P("minecraft:skeleton_skull[rotation=0]")
SKULL_4 = P("minecraft:skeleton_skull[rotation=4]")
SKULL_8 = P("minecraft:skeleton_skull[rotation=8]")
SKULL_12 = P("minecraft:skeleton_skull[rotation=12]")
WITHER_SKULL = P("minecraft:wither_skeleton_skull[rotation=0]")
LADDER_S = P("minecraft:ladder[facing=south,waterlogged=false]")
LADDER_N = P("minecraft:ladder[facing=north,waterlogged=false]")
LADDER_E = P("minecraft:ladder[facing=east,waterlogged=false]")
LADDER_W = P("minecraft:ladder[facing=west,waterlogged=false]")
WATER = P("minecraft:water[level=0]")
SCULK_VEIN_UP = P("minecraft:sculk_vein[down=false,east=false,north=false,south=false,up=true,waterlogged=false,west=false]")

# ── 3D grid ────────────────────────────────────────────────────────────
g = [[[AIR for _ in range(SX)] for _ in range(SZ)] for _ in range(SY)]

def setb(x, y, z, b):
    if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
        g[y][z][x] = b

def getb(x, y, z):
    if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
        return g[y][z][x]
    return None

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

# Material "eras"
def stone_dressed():
    """Newer dressed masonry — finer block mix."""
    r = random.random()
    if r < 0.55: return DSB
    if r < 0.70: return PDS
    if r < 0.80: return DSB_TILE
    if r < 0.88: return PBSB
    if r < 0.94: return BS
    if r < 0.97: return DSB_CRACK
    if r < 0.99: return PBSB_CRACK
    return CHIS_DS

def stone_old():
    """Old keep masonry — cruder, more cobble."""
    r = random.random()
    if r < 0.50: return CDS
    if r < 0.70: return DSB
    if r < 0.82: return DSB_CRACK
    if r < 0.92: return BS
    if r < 0.97: return CHIS_DS
    return OBS

def stone_curtain():
    """Curtain wall — middle era."""
    r = random.random()
    if r < 0.50: return DSB
    if r < 0.70: return CDS
    if r < 0.82: return PDS
    if r < 0.90: return BS
    if r < 0.95: return DSB_CRACK
    return DSB_TILE

def courtyard_floor():
    r = random.random()
    if r < 0.50: return CDS
    if r < 0.75: return PDS
    if r < 0.90: return DSB
    if r < 0.95: return BS
    return SCULK

# Geometry constants
OUT_WALL_Y0 = 4
OUT_WALL_Y1 = 13       # top of outer curtain
OUT_BAT_Y  = 15        # outer battlements top

IN_X0, IN_Z0 = 12, 12  # inner curtain top-left
IN_X1, IN_Z1 = 47, 47  # 36×36 inner curtain
IN_WALL_Y1 = 17        # inner curtain a bit higher than outer

KEEP_X0, KEEP_Z0 = 20, 20  # 20×20 keep
KEEP_X1, KEEP_Z1 = 39, 39
KEEP_WALL_TOP = 32

TOWER_SIZE = 8
TOWERS = {
    "NW": (0, 0),
    "NE": (SX - TOWER_SIZE, 0),
    "SW": (0, SZ - TOWER_SIZE),
    "SE": (SX - TOWER_SIZE, SZ - TOWER_SIZE),
}

# Helper: is (x,z) inside a corner-tower footprint?
def in_corner_tower(x, z):
    for ox, oz in TOWERS.values():
        if ox <= x < ox + TOWER_SIZE and oz <= z < oz + TOWER_SIZE:
            return True
    return False

def in_inner_curtain_footprint(x, z):
    return IN_X0 <= x <= IN_X1 and IN_Z0 <= z <= IN_Z1

def in_keep_footprint(x, z):
    return KEEP_X0 <= x <= KEEP_X1 and KEEP_Z0 <= z <= KEEP_Z1

# ════════════════════════════════════════════════════════════════════════
# STAGE 1: Crypt (y=0..3) + foundation slab (y=3)
# ════════════════════════════════════════════════════════════════════════
def build_crypt():
    # Full footprint solid obsidian bedrock at y=0
    fill_box(0, 0, 0, SX - 1, 0, SZ - 1, OBS)
    # y=1..3 solid stone foundation everywhere EXCEPT the crypt cavity
    fill_box_fn(0, 1, 0, SX - 1, 3, SZ - 1, lambda x, y, z: stone_old())

    # Carve crypt cavity directly under the keep
    cx0, cz0 = KEEP_X0 + 2, KEEP_Z0 + 2
    cx1, cz1 = KEEP_X1 - 2, KEEP_Z1 - 2
    fill_box(cx0, 1, cz0, cx1, 3, cz1, AIR)
    # Crypt walls (1 block in from cavity edge)
    for y in (1, 2, 3):
        for z in range(cz0 - 1, cz1 + 2):
            for x in range(cx0 - 1, cx1 + 2):
                if (x == cx0 - 1 or x == cx1 + 1 or
                    z == cz0 - 1 or z == cz1 + 1):
                    setb(x, y, z, CDS)
    # Crypt floor
    for z in range(cz0, cz1 + 1):
        for x in range(cx0, cx1 + 1):
            setb(x, 0, z, PDS if random.random() < 0.7 else CDS)

    # Three sarcophagi along centre line
    sarc_xs = [cx0 + 2, (cx0 + cx1) // 2, cx1 - 2]
    sarc_z = (cz0 + cz1) // 2
    for sx in sarc_xs:
        # Base: 3 wide, 1 deep
        for dx in range(-1, 2):
            setb(sx + dx, 1, sarc_z, PDS_SLAB_TOP)
        # Skull at head
        setb(sx, 2, sarc_z - 1, SKULL_0)
        # Candles at foot
        setb(sx, 2, sarc_z + 1, RED_CANDLE)

    # Eternal flame altar at the back wall
    altar_z = cz0
    altar_x = (cx0 + cx1) // 2
    setb(altar_x, 1, altar_z, MAGMA)
    setb(altar_x - 1, 1, altar_z, MAGMA)
    setb(altar_x + 1, 1, altar_z, MAGMA)
    setb(altar_x, 2, altar_z, WITHER_SKULL)
    setb(altar_x - 1, 2, altar_z, BLACK_CANDLE)
    setb(altar_x + 1, 2, altar_z, BLACK_CANDLE)

    # Skulls along walls
    for dz in range(2, cz1 - cz0, 3):
        setb(cx0, 2, cz0 + dz, SKULL_4)
        setb(cx1, 2, cz0 + dz, SKULL_12)

    # Hatch up to undercroft (north-east interior corner)
    hatch_x = cx1 - 1
    hatch_z = cz0 + 1
    setb(hatch_x, 3, hatch_z, AIR)
    setb(hatch_x, 4, hatch_z, AIR)
    # Ladder going up
    for y in (1, 2, 3):
        setb(hatch_x, y, hatch_z - 1, LADDER_S)

build_crypt()

# ════════════════════════════════════════════════════════════════════════
# STAGE 2: Courtyard floor (y=4)
# ════════════════════════════════════════════════════════════════════════
def build_courtyard_floor():
    for z in range(SZ):
        for x in range(SX):
            setb(x, 4, z, courtyard_floor())

build_courtyard_floor()

# ════════════════════════════════════════════════════════════════════════
# STAGE 3: Outer curtain wall (y=5..OUT_WALL_Y1)
# Battered plinth at y=5 (extra row of stone outside the wall line),
# 2-thick wall above, buttresses every 8 blocks, battlements on top.
# ════════════════════════════════════════════════════════════════════════
def build_outer_wall():
    WALL_THK = 2
    def in_perim_strict(x, z):
        return (x < WALL_THK or x >= SX - WALL_THK or
                z < WALL_THK or z >= SZ - WALL_THK)
    def in_wall(x, z):
        return in_perim_strict(x, z) and not in_corner_tower(x, z)

    # Solid 2-thick wall y=5..OUT_WALL_Y1
    for y in range(5, OUT_WALL_Y1 + 1):
        for z in range(SZ):
            for x in range(SX):
                if in_wall(x, z):
                    setb(x, y, z, stone_curtain())

    # Battered plinth at y=5: extra row of stone projecting outward 1 cell.
    # Walls are at x=[0..1] etc. Plinth is technically inside the same x=[0..1]
    # so we model it as a single chamfered course of stairs facing inward at y=5.
    # On the outer face — stairs facing outward, top half — make a beveled lip.
    for x in range(SX):
        # North face plinth
        if not in_corner_tower(x, 0):
            setb(x, 5, 0, DSB_STAIR_S_T)  # top-half stair facing south
        # South face plinth
        if not in_corner_tower(x, SZ - 1):
            setb(x, 5, SZ - 1, DSB_STAIR_N_T)
    for z in range(SZ):
        # West face plinth
        if not in_corner_tower(0, z):
            setb(0, 5, z, DSB_STAIR_E_T)
        # East face plinth
        if not in_corner_tower(SX - 1, z):
            setb(SX - 1, 5, z, DSB_STAIR_W_T)

    # Buttresses every 8 blocks along each face — 1-block protrusion column
    for x in range(8, SX - 8, 8):
        if in_corner_tower(x, 0): continue
        for y in range(6, OUT_WALL_Y1 + 1):
            # Buttress protrudes outward on north face — but we don't have
            # room outside the bbox. Instead, thicken inward as a pilaster.
            setb(x, y, 2, stone_curtain())
        # Same on south
        for y in range(6, OUT_WALL_Y1 + 1):
            setb(x, y, SZ - 3, stone_curtain())
    for z in range(8, SZ - 8, 8):
        if in_corner_tower(0, z): continue
        for y in range(6, OUT_WALL_Y1 + 1):
            setb(2, y, z, stone_curtain())
            setb(SX - 3, y, z, stone_curtain())

    # String course at y=8 — slab lip on outer face
    for x in range(SX):
        if not in_corner_tower(x, 0):
            setb(x, 8, 0, DSB_SLAB_TOP)
        if not in_corner_tower(x, SZ - 1):
            setb(x, 8, SZ - 1, DSB_SLAB_TOP)
    for z in range(SZ):
        if not in_corner_tower(0, z):
            setb(0, 8, z, DSB_SLAB_TOP)
        if not in_corner_tower(SX - 1, z):
            setb(SX - 1, 8, z, DSB_SLAB_TOP)

    # Walkway at y=OUT_WALL_Y1+1 — interior row of wall is the walkway floor
    walk_y = OUT_WALL_Y1 + 1
    for x in range(SX):
        if not in_corner_tower(x, 0):
            setb(x, walk_y, 0, stone_curtain())  # outer parapet base
            setb(x, walk_y, 1, stone_curtain())  # walkway floor
        if not in_corner_tower(x, SZ - 1):
            setb(x, walk_y, SZ - 1, stone_curtain())
            setb(x, walk_y, SZ - 2, stone_curtain())
    for z in range(SZ):
        if not in_corner_tower(0, z):
            setb(0, walk_y, z, stone_curtain())
            setb(1, walk_y, z, stone_curtain())
        if not in_corner_tower(SX - 1, z):
            setb(SX - 1, walk_y, z, stone_curtain())
            setb(SX - 2, walk_y, z, stone_curtain())

    # Crenellated parapet at y=walk_y+1 (= OUT_BAT_Y)
    bat_y = OUT_BAT_Y
    for x in range(SX):
        if in_corner_tower(x, 0): continue
        if x % 2 == 0:
            setb(x, bat_y, 0, stone_curtain())
        if not in_corner_tower(x, SZ - 1) and x % 2 == 0:
            setb(x, bat_y, SZ - 1, stone_curtain())
    for z in range(SZ):
        if in_corner_tower(0, z): continue
        if z % 2 == 0:
            setb(0, bat_y, z, stone_curtain())
        if not in_corner_tower(SX - 1, z) and z % 2 == 0:
            setb(SX - 1, bat_y, z, stone_curtain())

    # Arrow slits along the curtain — 2-tall vertical iron-bar slits every 8 blocks
    for x in range(8, SX - 8, 8):
        if in_corner_tower(x, 0): continue
        for dy in (7, 8):
            setb(x, dy, 0, BARS_EW)
            setb(x, dy, SZ - 1, BARS_EW)
    for z in range(8, SZ - 8, 8):
        if in_corner_tower(0, z): continue
        for dy in (7, 8):
            setb(0, dy, z, BARS_NS)
            setb(SX - 1, dy, z, BARS_NS)

build_outer_wall()

# ════════════════════════════════════════════════════════════════════════
# STAGE 4: Corner towers — four distinct designs
# ════════════════════════════════════════════════════════════════════════
def in_octagon(x, z, ox, oz, size):
    """For drum-tower feel: chamfer the corners of a square footprint."""
    lx = x - ox
    lz = z - oz
    # Inside square
    in_sq = 0 <= lx < size and 0 <= lz < size
    if not in_sq:
        return False
    # Cut 1×1 from each corner
    if (lx, lz) in [(0, 0), (size - 1, 0), (0, size - 1), (size - 1, size - 1)]:
        return False
    return True

def build_drum_tower(ox, oz, top_y, has_belvedere=True):
    """NW: chamfered (octagonal-feel) tower with belvedere & spiral stair access."""
    cx = ox + TOWER_SIZE // 2
    cz = oz + TOWER_SIZE // 2

    # Walls follow octagonal outline
    for y in range(5, top_y + 1):
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                if not in_octagon(x, z, ox, oz, TOWER_SIZE):
                    continue
                # outline only
                perim = (x == ox or x == ox + 1 or x == ox + TOWER_SIZE - 1 or x == ox + TOWER_SIZE - 2 or
                         z == oz or z == oz + 1 or z == oz + TOWER_SIZE - 1 or z == oz + TOWER_SIZE - 2)
                # Actually octagon perim — outermost cell of the octagon shape
                on_edge = (
                    (x == ox + 1 and (z == oz or z == oz + TOWER_SIZE - 1)) or
                    (x == ox + TOWER_SIZE - 2 and (z == oz or z == oz + TOWER_SIZE - 1)) or
                    (z == oz + 1 and (x == ox or x == ox + TOWER_SIZE - 1)) or
                    (z == oz + TOWER_SIZE - 2 and (x == ox or x == ox + TOWER_SIZE - 1)) or
                    (x == ox) or (x == ox + TOWER_SIZE - 1) or
                    (z == oz) or (z == oz + TOWER_SIZE - 1)
                )
                if on_edge and in_octagon(x, z, ox, oz, TOWER_SIZE):
                    setb(x, y, z, stone_dressed())

    # Place stair-block corner chamfers at every level — gives the round feel
    for y in range(5, top_y + 1):
        # NW corner: stair facing SE? In MC, stair facing direction is where the high side points.
        # For NW corner of tower we want the slope to face inward (south-east).
        setb(ox + 1, y, oz, PDS_STAIR_S) if False else None  # skip for now
        # Just leave corners empty (octagon already does the chamfer effect)

    # Floors at y=8, y=14
    for fy in (8, 14):
        for z in range(oz + 1, oz + TOWER_SIZE - 1):
            for x in range(ox + 1, ox + TOWER_SIZE - 1):
                if in_octagon(x, z, ox, oz, TOWER_SIZE):
                    setb(x, fy, z, DARK_PLANK)

    # Spiral staircase — center pillar + 4-step rotation
    spiral_pattern = [
        ((1, 0), PDS_STAIR_N),
        ((0, 1), PDS_STAIR_W),
        ((-1, 0), PDS_STAIR_S),
        ((0, -1), PDS_STAIR_E),
    ]
    # Center pillar
    for y in range(5, top_y + 1):
        setb(cx, y, cz, PDS)
    # Spiral
    for y in range(5, top_y):
        idx = (y - 5) % 4
        (dx, dz), block = spiral_pattern[idx]
        setb(cx + dx, y, cz + dz, block)
    # Clear floors at stair access points so player can traverse
    for fy in (8, 14):
        for (dx, dz), _ in spiral_pattern:
            setb(cx + dx, fy, cz + dz, AIR)

    # Arrow slits — at mid-height of each floor, on the 4 cardinal "edges"
    for mid_y in (7, 12):
        # north
        setb(cx, mid_y, oz, BARS_EW)
        # south
        setb(cx, mid_y, oz + TOWER_SIZE - 1, BARS_EW)
        # west
        setb(ox, mid_y, cz, BARS_NS)
        # east
        setb(ox + TOWER_SIZE - 1, mid_y, cz, BARS_NS)

    # Belvedere: open-air gazebo on top, only corner pillars + roof
    if has_belvedere:
        # Pillars at the 4 octagonal "corners"
        for (px, pz) in [(ox + 1, oz + 1), (ox + TOWER_SIZE - 2, oz + 1),
                          (ox + 1, oz + TOWER_SIZE - 2),
                          (ox + TOWER_SIZE - 2, oz + TOWER_SIZE - 2)]:
            for y in range(top_y + 1, top_y + 4):
                setb(px, y, pz, PDS)
        # Roof slab at top
        roof_y = top_y + 4
        for z in range(oz + 1, oz + TOWER_SIZE - 1):
            for x in range(ox + 1, ox + TOWER_SIZE - 1):
                if in_octagon(x, z, ox, oz, TOWER_SIZE):
                    setb(x, roof_y, z, stone_dressed())
        # Soul lantern hanging in centre
        setb(cx, top_y + 3, cz, SOUL_LANTERN_HANG)

def build_bell_tower(ox, oz, top_y):
    """NE: square tower with open-belfry on top, bell hanging from chains."""
    # Solid square walls
    for y in range(5, top_y + 1):
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                         z == oz or z == oz + TOWER_SIZE - 1)
                if perim:
                    setb(x, y, z, stone_dressed())

    # Floors
    for fy in (9, 15):
        for z in range(oz + 1, oz + TOWER_SIZE - 1):
            for x in range(ox + 1, ox + TOWER_SIZE - 1):
                setb(x, fy, z, DARK_PLANK)

    # Ladder NW interior
    for y in range(6, top_y):
        setb(ox + 1, y, oz + 1, LADDER_S)
    for fy in (9, 15):
        setb(ox + 1, fy, oz + 1, LADDER_S)

    # Arrow slits
    cx = ox + TOWER_SIZE // 2
    cz = oz + TOWER_SIZE // 2
    for mid_y in (8, 13):
        setb(cx, mid_y, oz, BARS_EW)
        setb(cx, mid_y, oz + TOWER_SIZE - 1, BARS_EW)
        setb(ox, mid_y, cz, BARS_NS)
        setb(ox + TOWER_SIZE - 1, mid_y, cz, BARS_NS)

    # OPEN BELFRY: top floor (last 4 blocks) is open arches
    belfry_y0 = top_y - 3
    for y in range(belfry_y0, top_y + 1):
        # Replace walls with column-only at corners + arches
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                         z == oz or z == oz + TOWER_SIZE - 1)
                if perim:
                    is_corner = ((x == ox or x == ox + TOWER_SIZE - 1) and
                                 (z == oz or z == oz + TOWER_SIZE - 1))
                    if is_corner:
                        setb(x, y, z, stone_dressed())
                    elif y == top_y:
                        setb(x, y, z, stone_dressed())  # cap
                    elif y == belfry_y0:
                        # Bottom of belfry — half-open with stairs to suggest arches
                        # Use stairs to chamfer the opening
                        if (x == ox and z != oz and z != oz + TOWER_SIZE - 1):
                            setb(x, y, z, DSB_STAIR_E)
                        elif (x == ox + TOWER_SIZE - 1 and z != oz and z != oz + TOWER_SIZE - 1):
                            setb(x, y, z, DSB_STAIR_W)
                        elif (z == oz and x != ox and x != ox + TOWER_SIZE - 1):
                            setb(x, y, z, DSB_STAIR_S)
                        elif (z == oz + TOWER_SIZE - 1 and x != ox and x != ox + TOWER_SIZE - 1):
                            setb(x, y, z, DSB_STAIR_N)
                    else:
                        setb(x, y, z, AIR)  # open belfry sides

    # Bell hanging in centre
    bell_y = top_y - 1
    setb(cx, bell_y, cz, BELL)
    # Chains up to the cap
    setb(cx, bell_y + 1, cz, CHAIN_Y)

    # Pyramidal cap above the belfry
    cap_levels = [(top_y + 1, 1), (top_y + 2, 2), (top_y + 3, 3)]
    for sy, inset in cap_levels:
        for z in range(oz + inset, oz + TOWER_SIZE - inset):
            for x in range(ox + inset, ox + TOWER_SIZE - inset):
                setb(x, sy, z, stone_dressed())

def build_watch_tower(ox, oz, top_y):
    """SW: tall, slim, conical roof using stairs."""
    # Make this tower TALLER than the others — adjust internal top_y
    walls_top = top_y + 4  # extra 4 blocks
    for y in range(5, walls_top + 1):
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                         z == oz or z == oz + TOWER_SIZE - 1)
                if perim:
                    setb(x, y, z, stone_dressed())

    # Floors
    for fy in (9, 15, 21):
        if fy > walls_top: break
        for z in range(oz + 1, oz + TOWER_SIZE - 1):
            for x in range(ox + 1, ox + TOWER_SIZE - 1):
                setb(x, fy, z, DARK_PLANK)

    # Ladder
    for y in range(6, walls_top):
        setb(ox + TOWER_SIZE - 2, y, oz + TOWER_SIZE - 2, LADDER_N)
    for fy in (9, 15, 21):
        if fy > walls_top: continue
        setb(ox + TOWER_SIZE - 2, fy, oz + TOWER_SIZE - 2, LADDER_N)

    # Arrow slits — many, to suggest a watchtower
    cx = ox + TOWER_SIZE // 2
    cz = oz + TOWER_SIZE // 2
    for mid_y in (8, 13, 18, 23):
        if mid_y > walls_top: break
        setb(cx, mid_y, oz, BARS_EW)
        setb(cx, mid_y, oz + TOWER_SIZE - 1, BARS_EW)
        setb(ox, mid_y, cz, BARS_NS)
        setb(ox + TOWER_SIZE - 1, mid_y, cz, BARS_NS)

    # Conical roof using stairs blocks
    roof_y0 = walls_top + 1
    # Roof slab base
    for z in range(oz, oz + TOWER_SIZE):
        for x in range(ox, ox + TOWER_SIZE):
            setb(x, roof_y0, z, stone_dressed())

    # Stairs forming a 4-sided pyramid
    for layer in range(1, 5):
        y = roof_y0 + layer
        inset = layer
        for x in range(ox + inset, ox + TOWER_SIZE - inset):
            for z in range(oz + inset, oz + TOWER_SIZE - inset):
                # Use stairs at the perimeter pointing inward
                if x == ox + inset:
                    setb(x, y, z, DSB_STAIR_E)
                elif x == ox + TOWER_SIZE - 1 - inset:
                    setb(x, y, z, DSB_STAIR_W)
                elif z == oz + inset:
                    setb(x, y, z, DSB_STAIR_S)
                elif z == oz + TOWER_SIZE - 1 - inset:
                    setb(x, y, z, DSB_STAIR_N)
                else:
                    setb(x, y, z, stone_dressed())
    # Apex
    setb(cx, roof_y0 + 5, cz, OBS)

def build_prison_tower(ox, oz, top_y):
    """SE: squat, narrow slits, cell with skeleton inside."""
    short_top = top_y - 6
    for y in range(5, short_top + 1):
        for z in range(oz, oz + TOWER_SIZE):
            for x in range(ox, ox + TOWER_SIZE):
                perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                         z == oz or z == oz + TOWER_SIZE - 1)
                if perim:
                    # Heavy weathering — more cracked / cobbled blocks
                    r = random.random()
                    if r < 0.3:    setb(x, y, z, DSB_CRACK)
                    elif r < 0.5:  setb(x, y, z, CDS)
                    elif r < 0.65: setb(x, y, z, DSB)
                    elif r < 0.8:  setb(x, y, z, BS)
                    else:          setb(x, y, z, PBSB)

    # Single interior floor
    for z in range(oz + 1, oz + TOWER_SIZE - 1):
        for x in range(ox + 1, ox + TOWER_SIZE - 1):
            setb(x, 9, z, DARK_PLANK)

    # Iron bar cells — interior split by iron bars
    for x in range(ox + 2, ox + TOWER_SIZE - 2):
        for y in (6, 7):
            setb(x, y, oz + 3, BARS_EW)

    # Skeleton in the cell
    cx = ox + TOWER_SIZE // 2
    setb(cx, 5, oz + 1, SKULL_8)

    # Narrow slits only
    for mid_y in (7,):
        setb(cx, mid_y, oz, BARS_EW)
        setb(cx, mid_y, oz + TOWER_SIZE - 1, BARS_EW)

    # Trapdoor down (oubliette)
    setb(ox + 2, 5, oz + 5, DARK_TRAP_TOP)

    # Squat crenellated cap
    roof_y = short_top + 1
    for z in range(oz, oz + TOWER_SIZE):
        for x in range(ox, ox + TOWER_SIZE):
            setb(x, roof_y, z, stone_dressed())
    # Battlements
    bat_y = roof_y + 1
    for z in range(oz, oz + TOWER_SIZE):
        for x in range(ox, ox + TOWER_SIZE):
            perim = (x == ox or x == ox + TOWER_SIZE - 1 or
                     z == oz or z == oz + TOWER_SIZE - 1)
            if perim and ((x + z) % 2 == 0):
                setb(x, bat_y, z, DSB_CRACK)

# Build the four towers
build_drum_tower(*TOWERS["NW"], top_y=22)
build_bell_tower(*TOWERS["NE"], top_y=22)
build_watch_tower(*TOWERS["SW"], top_y=22)
build_prison_tower(*TOWERS["SE"], top_y=22)

# ════════════════════════════════════════════════════════════════════════
# STAGE 5: Barbican / south gatehouse (forward-extending complex)
# ════════════════════════════════════════════════════════════════════════
def build_barbican():
    # Main south gate cut through the outer wall
    GATE_W = 5
    GX0 = (SX - GATE_W) // 2
    GX1 = GX0 + GATE_W - 1
    SOUTH_Z = SZ - 1

    # Carve gate opening
    for y in range(5, 10):
        for z in (SZ - 2, SZ - 1):
            for x in range(GX0, GX1 + 1):
                setb(x, y, z, AIR)

    # Crying obsidian threshold + magma underneath (warm glow at the gate)
    for x in range(GX0, GX1 + 1):
        setb(x, 4, SOUTH_Z, CRYING_OBS)
        setb(x, 3, SOUTH_Z, MAGMA)  # eternal embers under threshold

    # Main gate arch — pointed using stairs
    for x in (GX0 - 1, GX1 + 1):
        # column above stayed solid; we add arch stairs into the opening
        pass
    # Pointed arch over the opening
    setb(GX0, 10, SOUTH_Z, PBSB_STAIR_W)
    setb(GX1, 10, SOUTH_Z, PBSB_STAIR_E)
    setb(GX0 + 1, 10, SOUTH_Z, PBSB)
    setb(GX1 - 1, 10, SOUTH_Z, PBSB)
    setb(GX0 + 2, 11, SOUTH_Z, PBSB)
    setb(GX0 + 1, 11, SOUTH_Z, PBSB_STAIR_W)
    setb(GX1 - 1, 11, SOUTH_Z, PBSB_STAIR_E)
    # Apex
    setb(GX0 + 2, 12, SOUTH_Z, CHIS_PBSB)

    # Portcullis (raised) — iron bars hanging from arch
    for x in range(GX0, GX1 + 1):
        setb(x, 9, SOUTH_Z - 1, BARS_ALL)
        setb(x, 8, SOUTH_Z - 1, BARS_ALL)

    # Inner gate doors (dark oak double-ish — two single doors side by side)
    inner_z = SZ - 2
    setb(GX0 + 1, 5, inner_z, DARK_DOOR_N_L)
    setb(GX0 + 1, 6, inner_z, DARK_DOOR_N_U)
    setb(GX0 + 3, 5, inner_z, DARK_DOOR_N_L)
    setb(GX0 + 3, 6, inner_z, DARK_DOOR_N_U)

    # Soul-torch sconces flanking the gate exterior
    setb(GX0 - 1, 7, SOUTH_Z, SOUL_TORCH_N)
    setb(GX1 + 1, 7, SOUTH_Z, SOUL_TORCH_N)
    setb(GX0 - 2, 9, SOUTH_Z, BLACK_BANNER_N)
    setb(GX1 + 2, 9, SOUTH_Z, BLACK_BANNER_N)

    # Wither skull above the arch — macabre warning
    setb(GX0 + 2, 13, SOUTH_Z, WITHER_SKULL)

build_barbican()

# ════════════════════════════════════════════════════════════════════════
# STAGE 6: Inner curtain wall (40×40, raised 1 block plinth, 14 tall)
# ════════════════════════════════════════════════════════════════════════
def build_inner_curtain():
    # Walls on the rectangle perimeter
    for y in range(5, IN_WALL_Y1 + 1):
        for z in range(IN_Z0, IN_Z1 + 1):
            for x in range(IN_X0, IN_X1 + 1):
                perim = (x == IN_X0 or x == IN_X1 or
                         z == IN_Z0 or z == IN_Z1)
                if perim:
                    setb(x, y, z, stone_dressed())

    # Battlements
    bat_y = IN_WALL_Y1 + 1
    for z in range(IN_Z0, IN_Z1 + 1):
        for x in range(IN_X0, IN_X1 + 1):
            perim = (x == IN_X0 or x == IN_X1 or
                     z == IN_Z0 or z == IN_Z1)
            if perim and ((x + z) % 2 == 0):
                setb(x, bat_y, z, stone_dressed())

    # Inner gate — on the NORTH face (perpendicular to outer gate to force a turn)
    cx = (IN_X0 + IN_X1) // 2
    for y in range(5, 9):
        for x in range(cx - 1, cx + 2):
            setb(x, y, IN_Z0, AIR)
    # Arch
    setb(cx - 1, 9, IN_Z0, DSB_STAIR_W)
    setb(cx + 1, 9, IN_Z0, DSB_STAIR_E)
    setb(cx, 9, IN_Z0, DSB)
    # Dark-oak doors flanking
    setb(cx, 5, IN_Z0, DARK_DOOR_S_L)
    setb(cx, 6, IN_Z0, DARK_DOOR_S_U)

    # Soul torches flanking inner gate
    setb(cx - 2, 7, IN_Z0, SOUL_TORCH_S)
    setb(cx + 2, 7, IN_Z0, SOUL_TORCH_S)

    # Arrow slits on each face
    for mid_y in (7, 10, 13):
        for x in range(IN_X0 + 5, IN_X1, 8):
            if abs(x - cx) <= 2: continue  # don't slit through the gate
            setb(x, mid_y, IN_Z0, BARS_EW)
            setb(x, mid_y, IN_Z1, BARS_EW)
        for z in range(IN_Z0 + 5, IN_Z1, 8):
            setb(IN_X0, mid_y, z, BARS_NS)
            setb(IN_X1, mid_y, z, BARS_NS)

build_inner_curtain()

# ════════════════════════════════════════════════════════════════════════
# STAGE 7: Outer bailey buildings (chapel, stable, smithy)
# ════════════════════════════════════════════════════════════════════════
def build_chapel():
    """Small chapel leaned against north outer wall, between NW tower and inner curtain."""
    cx0 = TOWER_SIZE + 1
    cz0 = 2
    cw = 9
    cd = 6  # depth (z)
    cx1 = cx0 + cw - 1
    cz1 = cz0 + cd - 1
    # Floor
    for x in range(cx0, cx1 + 1):
        for z in range(cz0, cz1 + 1):
            setb(x, 4, z, PBSB)
    # Walls y=5..9
    for y in range(5, 10):
        for z in range(cz0, cz1 + 1):
            for x in range(cx0, cx1 + 1):
                perim = (x == cx0 or x == cx1 or z == cz0 or z == cz1)
                if perim:
                    setb(x, y, z, stone_dressed())
    # South-facing door
    door_x = (cx0 + cx1) // 2
    setb(door_x, 5, cz1, DARK_DOOR_N_L)
    setb(door_x, 6, cz1, DARK_DOOR_N_U)
    # East pointed-arch window (red glass)
    for dy in (6, 7):
        setb(cx1, dy, (cz0 + cz1) // 2, RED_GLASS_PANE_NS)
    # Sloped roof
    for layer in range(0, 4):
        y = 10 + layer
        for z in range(cz0 + layer, cz1 + 1 - layer):
            setb(cx0 + layer, y, z, DSB_STAIR_E)
            setb(cx1 - layer, y, z, DSB_STAIR_W)
            if cx0 + layer < cx1 - layer:
                for fx in range(cx0 + layer + 1, cx1 - layer):
                    setb(fx, y, z, AIR)  # hollow interior under roof
    # Altar inside — north end
    altar_x = (cx0 + cx1) // 2
    altar_z = cz0 + 1
    setb(altar_x, 5, altar_z, PBSB_SLAB_TOP)
    setb(altar_x, 6, altar_z, RED_CANDLE)
    setb(altar_x - 1, 6, altar_z, WHITE_CANDLE)
    setb(altar_x + 1, 6, altar_z, WHITE_CANDLE)
    # Pew benches
    for z in range(altar_z + 2, cz1):
        if (z - altar_z) % 2 == 0:
            setb(altar_x - 2, 5, z, DARK_STAIR_S)
            setb(altar_x - 1, 5, z, DARK_STAIR_S)
            setb(altar_x + 1, 5, z, DARK_STAIR_S)
            setb(altar_x + 2, 5, z, DARK_STAIR_S)
    # Red carpet aisle
    for z in range(altar_z + 1, cz1):
        setb(altar_x, 5, z, RED_CARPET)
    # Lantern hanging from peak
    setb(altar_x, 9, (cz0 + cz1) // 2, SOUL_LANTERN_HANG)

def build_stable():
    """Stables against west wall."""
    sx0 = 2
    sz0 = TOWER_SIZE + 2
    sw = 5
    sd = 12
    sx1 = sx0 + sw - 1
    sz1 = sz0 + sd - 1
    # Floor: dirt (more rustic for stable)
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            setb(x, 4, z, DARK_PLANK)
    # Walls — only north, south, east (west is shared with outer wall)
    for y in range(5, 8):
        for x in range(sx0, sx1 + 1):
            setb(x, y, sz0, DARK_LOG_Y if x % 2 == 0 else DARK_PLANK)
            setb(x, y, sz1, DARK_LOG_Y if x % 2 == 0 else DARK_PLANK)
        for z in range(sz0, sz1 + 1):
            setb(sx1, y, z, DARK_LOG_Y if z % 2 == 0 else DARK_PLANK)
    # Stalls — fence dividers every 3 cells
    for stall_z in range(sz0 + 1, sz1, 3):
        for x in range(sx0 + 1, sx1):
            setb(x, 5, stall_z, DARK_FENCE_EW)
        # Hay bales in each stall
        setb(sx0 + 2, 5, stall_z + 1, HAY)
    # Sloped wood roof
    for layer in range(0, 3):
        y = 8 + layer
        for z in range(sz0, sz1 + 1):
            if sx0 + layer < sx1 - layer:
                setb(sx1 - layer, y, z, DARK_STAIR_W)
            for fx in range(sx0 + layer, sx1 - layer):
                setb(fx, y, z, DARK_PLANK)

def build_smithy():
    """Smithy against east wall — magma forge, anvil, smithing table."""
    sx1 = SX - 3
    sz0 = TOWER_SIZE + 2
    sw = 5
    sd = 8
    sx0 = sx1 - sw + 1
    sz1 = sz0 + sd - 1
    # Floor
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            setb(x, 4, z, BS)
    # Walls (only inward sides; west wall shared)
    for y in range(5, 8):
        for x in range(sx0, sx1 + 1):
            setb(x, y, sz0, stone_dressed())
            setb(x, y, sz1, stone_dressed())
        for z in range(sz0, sz1 + 1):
            setb(sx0, y, z, stone_dressed())
    # Forge — magma block with iron bars cage on top
    forge_x = sx0 + 2
    forge_z = sz0 + 2
    setb(forge_x, 5, forge_z, MAGMA)
    setb(forge_x, 6, forge_z, BARS_ALL)
    # Chimney
    for y in range(7, 12):
        setb(forge_x, y, forge_z, stone_dressed())
        setb(forge_x + 1, y, forge_z, stone_dressed())
        setb(forge_x - 1, y, forge_z, stone_dressed())
        setb(forge_x, y, forge_z + 1, stone_dressed())
        setb(forge_x, y, forge_z - 1, stone_dressed())
    # Chimney top — open with iron bars cap
    for y in range(12, 14):
        setb(forge_x, y, forge_z, BARS_ALL)
    # Anvil + smithing table
    setb(sx0 + 2, 5, sz0 + 4, ANVIL)
    setb(sx0 + 3, 5, sz0 + 4, SMITHING)
    setb(sx0 + 2, 5, sz0 + 5, GRINDSTONE)
    # Water trough (small cauldron)
    setb(sx0 + 3, 5, sz0 + 1, CAULDRON)
    # Roof — wooden
    for x in range(sx0, sx1 + 1):
        for z in range(sz0, sz1 + 1):
            setb(x, 8, z, DARK_PLANK)

def build_well():
    """Stone well-curb in the outer bailey courtyard."""
    wx = (TOWER_SIZE + IN_X0) // 2 + 2
    wz = (SZ // 2)
    # 3×3 stone curb
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                setb(wx, 4, wz, WATER)
                setb(wx, 3, wz, WATER)
                setb(wx, 2, wz, WATER)
            else:
                setb(wx + dx, 4, wz + dz, CDS)
                setb(wx + dx, 5, wz + dz, CDS)
    # Posts for a roof
    setb(wx - 1, 6, wz - 1, DARK_LOG_Y)
    setb(wx + 1, 6, wz - 1, DARK_LOG_Y)
    setb(wx - 1, 6, wz + 1, DARK_LOG_Y)
    setb(wx + 1, 6, wz + 1, DARK_LOG_Y)
    for y in (7, 8):
        for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
            setb(wx + dx, y, wz + dz, DARK_LOG_Y)
    # Pyramidal cap
    for layer in range(0, 2):
        y = 9 + layer
        inset = layer
        for dx in range(-1 + inset, 2 - inset):
            for dz in range(-1 + inset, 2 - inset):
                setb(wx + dx, y, wz + dz, DARK_PLANK)
    # Lantern hanging
    setb(wx, 7, wz, CHAIN_Y)
    setb(wx, 6, wz, LANTERN_HANG)

build_chapel()
build_stable()
build_smithy()
build_well()

# ════════════════════════════════════════════════════════════════════════
# STAGE 8: Keep walls (full height) + plinth
# ════════════════════════════════════════════════════════════════════════
def build_keep_walls():
    # Keep plinth: y=4..5 raised step around keep
    for z in range(KEEP_Z0 - 1, KEEP_Z1 + 2):
        for x in range(KEEP_X0 - 1, KEEP_X1 + 2):
            if (x == KEEP_X0 - 1 or x == KEEP_X1 + 1 or
                z == KEEP_Z0 - 1 or z == KEEP_Z1 + 1):
                setb(x, 5, z, PDS_SLAB_TOP)  # plinth lip

    # Walls y=5..KEEP_WALL_TOP — OLD masonry mix (cruder)
    for y in range(5, KEEP_WALL_TOP + 1):
        for z in range(KEEP_Z0, KEEP_Z1 + 1):
            for x in range(KEEP_X0, KEEP_X1 + 1):
                perim = (x == KEEP_X0 or x == KEEP_X1 or
                         z == KEEP_Z0 or z == KEEP_Z1)
                if perim:
                    setb(x, y, z, stone_old())

    # Buttresses on the keep — 4 vertical pilasters at the corners
    for cx, cz in [(KEEP_X0, KEEP_Z0), (KEEP_X1, KEEP_Z0),
                    (KEEP_X0, KEEP_Z1), (KEEP_X1, KEEP_Z1)]:
        # Step outward at the base for visual weight
        for y in range(5, 10):
            for dx, dz in [(0, 0)]:
                pass  # corner already has stone

    # String course at y=14 and y=23 (between floor levels)
    for y in (14, 23):
        for z in range(KEEP_Z0, KEEP_Z1 + 1):
            for x in range(KEEP_X0, KEEP_X1 + 1):
                perim = (x == KEEP_X0 or x == KEEP_X1 or
                         z == KEEP_Z0 or z == KEEP_Z1)
                if perim and (x == KEEP_X0 or x == KEEP_X1):
                    # west/east courses
                    pass

build_keep_walls()

# ════════════════════════════════════════════════════════════════════════
# STAGE 9: Keep interior — undercroft, great hall, solar, chapel
# ════════════════════════════════════════════════════════════════════════
def build_keep_interior():
    # Undercroft floor at y=4 (already courtyard floor); ceiling at y=9
    for z in range(KEEP_Z0 + 1, KEEP_Z1):
        for x in range(KEEP_X0 + 1, KEEP_X1):
            setb(x, 9, z, DARK_PLANK)  # great hall floor

    # Trapdoor in great hall floor for undercroft access
    setb(KEEP_X1 - 2, 9, KEEP_Z0 + 2, DARK_TRAP_TOP)
    # Ladder down to undercroft from this trapdoor
    for y in range(5, 9):
        setb(KEEP_X1 - 2, y, KEEP_Z0 + 3, LADDER_S)

    # Barrels in undercroft (storage)
    for dx in range(2, 7):
        setb(KEEP_X0 + dx, 5, KEEP_Z0 + 2, BARREL_UP)
    for dz in range(3, 7):
        setb(KEEP_X0 + 2, 5, KEEP_Z0 + dz, BARREL_UP)
    setb(KEEP_X0 + 3, 5, KEEP_Z0 + 3, CHEST_S)
    setb(KEEP_X0 + 4, 5, KEEP_Z0 + 3, CHEST_S)

    # ── Great hall (y=10..18, double height) ──────────────────────────
    cx = (KEEP_X0 + KEEP_X1) // 2
    cz = (KEEP_Z0 + KEEP_Z1) // 2

    # Keep door on south face, accessed via external stair (will build below)
    setb(cx, 10, KEEP_Z1, DARK_DOOR_N_L)
    setb(cx, 11, KEEP_Z1, DARK_DOOR_N_U)

    # External stair up to keep door (from courtyard at y=4 to door at y=10)
    for step in range(0, 6):
        sx = cx
        sz = KEEP_Z1 + 1 + step
        sy = 5 + step
        setb(sx - 1, sy, sz, PDS_STAIR_N)
        setb(sx,     sy, sz, PDS_STAIR_N)
        setb(sx + 1, sy, sz, PDS_STAIR_N)
        # Solid fill under stairs
        for fy in range(5, sy):
            setb(sx - 1, fy, sz, PDS)
            setb(sx,     fy, sz, PDS)
            setb(sx + 1, fy, sz, PDS)
    # Landing in front of door
    for dx in (-1, 0, 1):
        setb(cx + dx, 10, KEEP_Z1, AIR if dx == 0 else stone_old())
        setb(cx + dx, 9, KEEP_Z1 + 1, PDS)

    # Pointed-arch windows on EAST and WEST faces of great hall
    for window_z in (KEEP_Z0 + 4, KEEP_Z0 + 9, KEEP_Z0 + 14):
        # East face
        for dy in range(12, 17):
            setb(KEEP_X1, dy, window_z, RED_GLASS_PANE_NS)
        # Arch top: stairs forming pointed arch above the window
        setb(KEEP_X1, 17, window_z - 1, DSB_STAIR_E_T)
        setb(KEEP_X1, 17, window_z + 1, DSB_STAIR_W_T)
        # West face
        for dy in range(12, 17):
            setb(KEEP_X0, dy, window_z, RED_GLASS_PANE_NS)
        setb(KEEP_X0, 17, window_z - 1, DSB_STAIR_E_T)
        setb(KEEP_X0, 17, window_z + 1, DSB_STAIR_W_T)

    # Throne at the north end of great hall
    throne_x = cx
    throne_z = KEEP_Z0 + 2
    # Dais — 3-tall step
    for dx in range(-2, 3):
        for dz in range(0, 3):
            setb(throne_x + dx, 10, throne_z + dz, CHIS_DS)
    # Throne block — stairs facing south, back at the wall
    setb(throne_x, 11, throne_z, PBSB_STAIR_S_T := stair("polished_blackstone_brick", "south", "top"))
    setb(throne_x - 1, 11, throne_z, CHIS_PBSB)
    setb(throne_x + 1, 11, throne_z, CHIS_PBSB)
    # Banners flanking the throne
    setb(throne_x - 2, 13, throne_z, RED_BANNER_W)
    setb(throne_x + 2, 13, throne_z, RED_BANNER_E)
    setb(throne_x - 2, 14, throne_z, BLACK_BANNER_S)
    setb(throne_x + 2, 14, throne_z, BLACK_BANNER_S)

    # Banquet table running south from the throne
    table_z0 = throne_z + 4
    table_z1 = KEEP_Z1 - 4
    for tz in range(table_z0, table_z1 + 1):
        setb(throne_x - 1, 10, tz, DARK_PLANK)
        setb(throne_x, 10, tz, DARK_PLANK)
        setb(throne_x + 1, 10, tz, DARK_PLANK)
        # Set as red-carpet runner just to the south of the throne up to the door
        setb(throne_x, 11, tz, RED_CARPET if (tz % 2 == 0) else RED_CARPET)

    # Wait — the banquet table should be at floor LEVEL, not on the floor of next level.
    # Floor is at y=10. So tables at y=10 OK, but that overwrites floor. Reset:
    for tz in range(table_z0, table_z1 + 1):
        setb(throne_x - 1, 10, tz, DARK_PLANK)  # OK floor stays planks
        setb(throne_x, 10, tz, RED_CARPET)       # red carpet on planks
        setb(throne_x + 1, 10, tz, DARK_PLANK)
    # Actually I want the table to be at y=11 (one block above floor). Let me redo:
    for tz in range(table_z0, table_z1 + 1):
        setb(throne_x, 11, tz, CDS_SLAB_TOP)  # table top

    # Chairs flanking the banquet table
    for tz in range(table_z0, table_z1 + 1, 2):
        setb(throne_x - 2, 11, tz, DARK_STAIR_E)
        setb(throne_x + 2, 11, tz, DARK_STAIR_W)

    # Central fireplace — pit between throne and table
    fp_z = throne_z + 3
    setb(throne_x - 1, 10, fp_z, PBSB)
    setb(throne_x + 1, 10, fp_z, PBSB)
    setb(throne_x, 10, fp_z, MAGMA)
    setb(throne_x, 11, fp_z, FIRE)
    # Chimney stack going up through the upper floors and out the roof
    for y in range(12, KEEP_WALL_TOP):
        setb(throne_x, y, fp_z, AIR)  # chimney shaft
        for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            if y < 18 and dx != 0:
                setb(throne_x + dx, y, fp_z, stone_old())
            elif y >= 18:
                setb(throne_x + dx, y, fp_z + dz, stone_old())

    # Chain lanterns hanging from great hall ceiling
    for tz in (table_z0 + 1, (table_z0 + table_z1) // 2, table_z1 - 1):
        setb(throne_x - 4, 18, tz, CHAIN_Y)
        setb(throne_x - 4, 17, tz, SOUL_LANTERN_HANG)
        setb(throne_x + 4, 18, tz, CHAIN_Y)
        setb(throne_x + 4, 17, tz, SOUL_LANTERN_HANG)

    # ── Great hall ceiling at y=19 ────────────────────────────────────
    for z in range(KEEP_Z0 + 1, KEEP_Z1):
        for x in range(KEEP_X0 + 1, KEEP_X1):
            setb(x, 19, z, DARK_PLANK)
    # Chimney passes through (clear cell)
    setb(throne_x, 19, fp_z, AIR)

    # ── Solar (y=20..24) ──────────────────────────────────────────────
    solar_door_x = KEEP_X0 + 3
    # Spiral stair access — placeholder ladder for now
    for y in range(20, 24):
        setb(KEEP_X0 + 1, y, KEEP_Z0 + 1, LADDER_S)
    # Bed — dark-oak fence "four-poster"
    bed_x = (KEEP_X0 + KEEP_X1) // 2
    bed_z = KEEP_Z0 + 3
    setb(bed_x - 1, 20, bed_z, DARK_PLANK)
    setb(bed_x, 20, bed_z, DARK_PLANK)
    setb(bed_x + 1, 20, bed_z, DARK_PLANK)
    setb(bed_x - 1, 20, bed_z + 1, DARK_PLANK)
    setb(bed_x, 20, bed_z + 1, DARK_PLANK)
    setb(bed_x + 1, 20, bed_z + 1, DARK_PLANK)
    setb(bed_x - 1, 20, bed_z + 2, DARK_PLANK)
    setb(bed_x, 20, bed_z + 2, DARK_PLANK)
    setb(bed_x + 1, 20, bed_z + 2, DARK_PLANK)
    # Posters
    for py in (21, 22, 23):
        setb(bed_x - 1, py, bed_z, DARK_FENCE_POST)
        setb(bed_x + 1, py, bed_z, DARK_FENCE_POST)
        setb(bed_x - 1, py, bed_z + 2, DARK_FENCE_POST)
        setb(bed_x + 1, py, bed_z + 2, DARK_FENCE_POST)
    # Canopy
    for cx_ in (bed_x - 1, bed_x, bed_x + 1):
        for cz_ in (bed_z, bed_z + 1, bed_z + 2):
            setb(cx_, 24, cz_, BLACK_CARPET)
    # Bed coverings: black + red carpet on top
    for cx_ in (bed_x - 1, bed_x, bed_x + 1):
        for cz_ in (bed_z, bed_z + 1, bed_z + 2):
            if cz_ in (bed_z, bed_z + 1):
                setb(cx_, 21, cz_, RED_CARPET)

    # Garderobe protrusion — cantilevered east of the wall (toilet)
    gx = KEEP_X1 + 1
    gz = KEEP_Z1 - 2
    for y in (21, 22):
        setb(gx, y, gz, stone_old())
        setb(gx, y, gz + 1, stone_old())
        setb(gx + 1, y, gz, stone_old())
        setb(gx + 1, y, gz + 1, stone_old())
    setb(gx, 23, gz, stone_old())
    setb(gx, 23, gz + 1, stone_old())
    setb(gx + 1, 23, gz, stone_old())
    setb(gx + 1, 23, gz + 1, stone_old())
    setb(gx, 21, gz + 1, AIR)  # the seat hole — falls to courtyard ;)
    setb(KEEP_X1, 21, gz, AIR)  # door into garderobe from solar
    setb(KEEP_X1, 22, gz, AIR)
    setb(KEEP_X1, 21, gz + 1, AIR)
    setb(KEEP_X1, 22, gz + 1, AIR)

    # Solar windows — smaller (single panes)
    for dy in (22,):
        setb(KEEP_X0, dy, KEEP_Z0 + 8, RED_GLASS_PANE_NS)
        setb(KEEP_X1, dy, KEEP_Z0 + 8, RED_GLASS_PANE_NS)
        setb((KEEP_X0 + KEEP_X1) // 2, dy, KEEP_Z0, RED_GLASS_PANE_EW)

    # Hanging lantern
    setb((KEEP_X0 + KEEP_X1) // 2 + 3, 23, (KEEP_Z0 + KEEP_Z1) // 2, SOUL_LANTERN_HANG)

    # Solar ceiling at y=25
    for z in range(KEEP_Z0 + 1, KEEP_Z1):
        for x in range(KEEP_X0 + 1, KEEP_X1):
            setb(x, 25, z, DARK_PLANK)
    setb(throne_x, 25, fp_z, AIR)  # chimney through

    # ── Chapel (y=26..31) ─────────────────────────────────────────────
    chap_y0 = 26
    chap_y1 = 31
    # Ladder up
    for y in range(chap_y0, chap_y1):
        setb(KEEP_X0 + 1, y, KEEP_Z0 + 1, LADDER_S)
    # Big east-facing pointed-arch window — 3 wide, 4 tall, in red glass
    cx2 = (KEEP_X0 + KEEP_X1) // 2
    cz2 = (KEEP_Z0 + KEEP_Z1) // 2
    for dy in range(chap_y0 + 1, chap_y0 + 5):
        for dz in range(cz2 - 1, cz2 + 2):
            setb(KEEP_X1, dy, dz, RED_GLASS_PANE_NS)
    # Pointed arch above
    setb(KEEP_X1, chap_y0 + 5, cz2, RED_GLASS)
    setb(KEEP_X1, chap_y0 + 5, cz2 - 1, DSB_STAIR_W_T)
    setb(KEEP_X1, chap_y0 + 5, cz2 + 1, DSB_STAIR_E_T)
    # Altar at east end
    altar_x = KEEP_X1 - 2
    setb(altar_x, chap_y0, cz2, PBSB_SLAB_TOP)
    setb(altar_x, chap_y0 + 1, cz2, BLACK_CANDLE)
    setb(altar_x, chap_y0 + 1, cz2 - 1, WHITE_CANDLE)
    setb(altar_x, chap_y0 + 1, cz2 + 1, WHITE_CANDLE)
    # Lectern beside altar
    setb(altar_x - 1, chap_y0, cz2, LECTERN_W)
    # Red carpet aisle going west
    for dx in range(KEEP_X0 + 2, altar_x):
        setb(dx, chap_y0, cz2, RED_CARPET)
    # Pew benches (dark oak stairs facing east, toward altar)
    for dx in range(KEEP_X0 + 3, altar_x - 1, 2):
        setb(dx, chap_y0, cz2 - 2, DARK_STAIR_E)
        setb(dx, chap_y0, cz2 + 2, DARK_STAIR_E)

    # Lantern hanging
    setb(altar_x - 4, chap_y0 + 5, cz2, CHAIN_Y)
    setb(altar_x - 4, chap_y0 + 4, cz2, SOUL_LANTERN_HANG)

    # Chapel ceiling at y=32 (the keep wall top)
    for z in range(KEEP_Z0 + 1, KEEP_Z1):
        for x in range(KEEP_X0 + 1, KEEP_X1):
            setb(x, 32, z, DARK_PLANK)

build_keep_interior()

# ════════════════════════════════════════════════════════════════════════
# STAGE 10: Keep roof + corner turrets + central spire
# ════════════════════════════════════════════════════════════════════════
def build_keep_roof():
    roof_y = KEEP_WALL_TOP + 1  # y=33
    # Walkable roof
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            setb(x, roof_y, z, stone_dressed())
    # Battlements on perimeter
    bat_y = roof_y + 1
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            perim = (x == KEEP_X0 or x == KEEP_X1 or
                     z == KEEP_Z0 or z == KEEP_Z1)
            if perim and ((x + z) % 2 == 0):
                setb(x, bat_y, z, stone_dressed())

    # 4 corner turrets — 3×3 footprint each
    cx = (KEEP_X0 + KEEP_X1) // 2
    cz = (KEEP_Z0 + KEEP_Z1) // 2
    for tx, tz in [(KEEP_X0 + 1, KEEP_Z0 + 1),
                    (KEEP_X1 - 1, KEEP_Z0 + 1),
                    (KEEP_X0 + 1, KEEP_Z1 - 1),
                    (KEEP_X1 - 1, KEEP_Z1 - 1)]:
        for y in range(roof_y + 1, roof_y + 5):
            for dx, dz in [(-1, -1), (0, -1), (1, -1),
                           (-1, 0),           (1, 0),
                           (-1, 1),  (0, 1),  (1, 1)]:
                setb(tx + dx, y, tz + dz, stone_dressed())
        # Conical cap
        cap_y = roof_y + 5
        for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
            setb(tx + dx, cap_y, tz + dz, OBS)
        setb(tx, cap_y, tz, stone_dressed())
        setb(tx, cap_y + 1, tz, OBS)

    # Central spire — stepped pyramid from roof centre
    spire_base = 9
    y = roof_y + 1
    r = spire_base // 2
    while r > 0 and y <= 45:
        for z in range(cz - r, cz + r + 1):
            for x in range(cx - r, cx + r + 1):
                perim = (x == cx - r or x == cx + r or
                         z == cz - r or z == cz + r)
                if perim:
                    setb(x, y, z, stone_dressed())
        y += 1
        if (y - (roof_y + 1)) % 2 == 0:
            r -= 1
    # Cap and flagpole
    setb(cx, y, cz, OBS)
    setb(cx, y + 1, cz, DARK_LOG_Y)
    setb(cx, y + 2, cz, DARK_LOG_Y)
    setb(cx, y + 3, cz, DARK_LOG_Y)
    if y + 4 < SY:
        setb(cx, y + 4, cz, BLACK_BANNER_N)

build_keep_roof()

# ════════════════════════════════════════════════════════════════════════
# STAGE 11: Decoration pass — sculk, cobwebs, banners
# ════════════════════════════════════════════════════════════════════════
def decorate():
    # Sculk corruption clusters — concentrate around the prison tower
    ox_p, oz_p = TOWERS["SE"]
    for _ in range(30):
        dx = random.randint(-6, 4)
        dz = random.randint(-6, 4)
        px = ox_p + dx
        pz = oz_p + dz
        if not (0 < px < SX - 1 and 0 < pz < SZ - 1): continue
        if in_corner_tower(px, pz): continue
        if in_inner_curtain_footprint(px, pz): continue
        cur = getb(px, 4, pz)
        if cur in (CDS, PDS, DSB, BS):
            setb(px, 4, pz, SCULK)

    # Cobwebs in the prison tower (heavy)
    for _ in range(20):
        x = random.randint(ox_p + 1, ox_p + TOWER_SIZE - 2)
        z = random.randint(oz_p + 1, oz_p + TOWER_SIZE - 2)
        y = random.randint(6, 9)
        if getb(x, y, z) == AIR:
            setb(x, y, z, COBWEB)

    # Cobwebs in keep undercroft corners
    for x, z in [(KEEP_X0 + 1, KEEP_Z0 + 1), (KEEP_X1 - 1, KEEP_Z0 + 1),
                  (KEEP_X0 + 1, KEEP_Z1 - 1), (KEEP_X1 - 1, KEEP_Z1 - 1)]:
        for y in (7, 8):
            if getb(x, y, z) == AIR:
                setb(x, y, z, COBWEB)

    # Black banners on keep north face
    cx = (KEEP_X0 + KEEP_X1) // 2
    for bx in (cx - 3, cx, cx + 3):
        setb(bx, 16, KEEP_Z0 - 1, BLACK_BANNER_S)

    # Soul torches lining the path from inner gate to keep door
    in_gate_x = (IN_X0 + IN_X1) // 2
    keep_door_x = (KEEP_X0 + KEEP_X1) // 2
    for tz in range(IN_Z0 + 2, KEEP_Z0, 3):
        setb(in_gate_x - 3, 5, tz, SOUL_LANTERN_GND)
        setb(in_gate_x + 3, 5, tz, SOUL_LANTERN_GND)

    # Wither skull above keep door
    setb(keep_door_x, 12, KEEP_Z1, WITHER_SKULL)

    # Gallows just outside the inner gate (in the outer bailey)
    gal_x = in_gate_x + 4
    gal_z = IN_Z0 - 2
    for y in range(5, 9):
        setb(gal_x, y, gal_z, DARK_LOG_Y)
    setb(gal_x, 9, gal_z, DARK_LOG_X)
    setb(gal_x - 1, 9, gal_z, DARK_LOG_X)
    setb(gal_x - 2, 9, gal_z, DARK_LOG_X)
    setb(gal_x - 1, 8, gal_z, CHAIN_Y)
    setb(gal_x - 1, 7, gal_z, SKULL_0)

decorate()

# ════════════════════════════════════════════════════════════════════════
# Encode and write
# ════════════════════════════════════════════════════════════════════════
def code(i):
    def d(n):
        return chr(ord('0') + n) if n < 10 else chr(ord('a') + n - 10)
    return d(i // 36) + d(i % 36)

if len(palette) > 36 * 36:
    raise SystemExit(f"palette overflow: {len(palette)} > 1296")

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

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines) + "\n")
solid = sum(1 for y in range(SY) for z in range(SZ) for x in range(SX) if g[y][z][x] != AIR)
print(f"wrote {OUT}")
print(f"  {SX}×{SY}×{SZ}  volume={SX*SY*SZ}  solid={solid}  palette={len(palette)}")
