#!/usr/bin/env python3
"""
Ironholt Keep — a natural-stone fortified manor designed to apply the
catalog's block-truthed lessons (Onctaaf + Raaamseeel).

50×35×50 footprint. A peace-time noble seat:
  - Terrain podium INSIDE the litematic (dirt + grass + moss + moat)
  - Natural-stone palette (andesite + smooth_stone + stone_bricks + tuff)
  - Egalitarian 4-era mix (founding/curtain/refinement/modern at low %)
  - Slab-as-primary-trim (target slab:stair ratio ~5:1)
  - Stone-slab tier roof (no wooden A-frame)
  - Asymmetric 3-tower silhouette (NE tall watch, NW shorter, SW ruined stub)
  - Attached east chapel wing in refinement-era palette
  - Slab-top parapet (peace-time — NOT crenellated)
  - Torch + lantern lighting, NO soul fire
  - Cracked + moss together as weathering

Coordinate system (50 wide × 35 tall × 50 deep):
  y=0..2   dirt podium + moat floor
  y=3      grass / moss surface; moat is water at y=3..4
  y=4      ground floor of keep / courtyard
  y=4..7   ground floor walls
  y=8..12  great hall (5 blocks tall — refined ceiling)
  y=13..16 solar
  y=17..20 watch level
  y=21..24 stone-slab tier roof
  Towers extend above: NE to y=28, NW to y=20, SW ruined at y=12
"""
import random
from pathlib import Path

SX, SY, SZ = 50, 35, 50
SEED = 42
OUT = Path(__file__).resolve().parents[1] / "structures" / "ironholt_keep.isc"

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

# Stone family — natural stone medieval palette (Onctaaf-validated)
STONE       = P("minecraft:stone")
COBBLE      = P("minecraft:cobblestone")
MOSSY_COBBLE= P("minecraft:mossy_cobblestone")
SMOOTH_STONE= P("minecraft:smooth_stone")
ANDESITE    = P("minecraft:andesite")
P_ANDESITE  = P("minecraft:polished_andesite")
STONE_BRICK = P("minecraft:stone_bricks")
MOSSY_BRICK = P("minecraft:mossy_stone_bricks")
CRACK_BRICK = P("minecraft:cracked_stone_bricks")
CHIS_BRICK  = P("minecraft:chiseled_stone_bricks")
TUFF        = P("minecraft:tuff")
TUFF_BRICK  = P("minecraft:tuff_bricks")
P_TUFF      = P("minecraft:polished_tuff")
DIORITE     = P("minecraft:diorite")
P_DIORITE   = P("minecraft:polished_diorite")  # refinement-era / chapel
QUARTZ      = P("minecraft:quartz_block")      # chapel altar accent
CHIS_QUARTZ = P("minecraft:chiseled_quartz_block")

# Slabs (top-half is the primary trim — Onctaaf-validated)
SMOOTH_SLAB_T  = P("minecraft:smooth_stone_slab[type=top,waterlogged=false]")
SMOOTH_SLAB_B  = P("minecraft:smooth_stone_slab[type=bottom,waterlogged=false]")
SMOOTH_SLAB_D  = P("minecraft:smooth_stone_slab[type=double,waterlogged=false]")
BRICK_SLAB_T   = P("minecraft:stone_brick_slab[type=top,waterlogged=false]")
BRICK_SLAB_B   = P("minecraft:stone_brick_slab[type=bottom,waterlogged=false]")
TUFF_SLAB_T    = P("minecraft:tuff_brick_slab[type=top,waterlogged=false]")
TUFF_SLAB_B    = P("minecraft:tuff_brick_slab[type=bottom,waterlogged=false]")
ANDESITE_SLAB_T= P("minecraft:polished_andesite_slab[type=top,waterlogged=false]")
ANDESITE_SLAB_B= P("minecraft:polished_andesite_slab[type=bottom,waterlogged=false]")
DIORITE_SLAB_T = P("minecraft:polished_diorite_slab[type=top,waterlogged=false]")

# Stairs — used sparingly, only for explicit slope features
def brick_stair(facing, half="bottom"):
    return P(f"minecraft:stone_brick_stairs[facing={facing},half={half},shape=straight,waterlogged=false]")
def andesite_stair(facing, half="bottom"):
    return P(f"minecraft:polished_andesite_stairs[facing={facing},half={half},shape=straight,waterlogged=false]")
def tuff_stair(facing, half="bottom"):
    return P(f"minecraft:tuff_brick_stairs[facing={facing},half={half},shape=straight,waterlogged=false]")
def diorite_stair(facing, half="bottom"):
    return P(f"minecraft:polished_diorite_stairs[facing={facing},half={half},shape=straight,waterlogged=false]")

# Walls (railings + low parapet pieces)
STONE_BRICK_WALL = P("minecraft:stone_brick_wall[east=tall,north=tall,south=tall,up=true,waterlogged=false,west=tall]")
COBBLE_WALL      = P("minecraft:cobblestone_wall[east=tall,north=tall,south=tall,up=true,waterlogged=false,west=tall]")
MOSSY_COBBLE_WALL= P("minecraft:mossy_cobblestone_wall[east=tall,north=tall,south=tall,up=true,waterlogged=false,west=tall]")

# Wood — used sparingly for trim and structural accents
DARK_OAK_LOG_Y   = P("minecraft:dark_oak_log[axis=y]")
DARK_OAK_LOG_X   = P("minecraft:dark_oak_log[axis=x]")
DARK_OAK_LOG_Z   = P("minecraft:dark_oak_log[axis=z]")
STRIPPED_OAK_Y   = P("minecraft:stripped_oak_log[axis=y]")
DARK_OAK_PLANKS  = P("minecraft:dark_oak_planks")
SPRUCE_PLANKS    = P("minecraft:spruce_planks")

# Trapdoors as wall battens (Onctaaf signature — 280+ trapdoors)
def trapdoor(facing, half="top", open_="false"):
    return P(f"minecraft:dark_oak_trapdoor[facing={facing},half={half},open={open_},powered=false,waterlogged=false]")

# Doors
def door(part, hinge, facing):
    return P(f"minecraft:dark_oak_door[facing={facing},half={part},hinge={hinge},open=false,powered=false]")

# Glass — sparse, white-tinted, only chapel + solar
WHITE_GLASS = P("minecraft:white_stained_glass_pane[east=false,north=true,south=true,waterlogged=false,west=false]")
WHITE_GLASS_EW = P("minecraft:white_stained_glass_pane[east=true,north=false,south=false,waterlogged=false,west=true]")

# Lighting — torch primary, lantern accent (Onctaaf style — NO soul)
TORCH         = P("minecraft:torch")
def wall_torch(facing):
    return P(f"minecraft:wall_torch[facing={facing}]")
LANTERN_H     = P("minecraft:lantern[hanging=true,waterlogged=false]")
LANTERN_F     = P("minecraft:lantern[hanging=false,waterlogged=false]")
CAMPFIRE      = P("minecraft:campfire[facing=north,lit=true,signal_fire=false,waterlogged=false]")
WHITE_CANDLE3 = P("minecraft:white_candle[candles=3,lit=true,waterlogged=false]")
CHAIN_Y       = P("minecraft:chain[axis=y,waterlogged=false]")

# Terrain podium + moat
DIRT      = P("minecraft:dirt")
COARSE    = P("minecraft:coarse_dirt")
GRASS     = P("minecraft:grass_block[snowy=false]")
MOSS      = P("minecraft:moss_block")
GRAVEL    = P("minecraft:gravel")
WATER     = P("minecraft:water[level=0]")
LILY      = P("minecraft:lily_pad")
TALL_GR_L = P("minecraft:tall_grass[half=lower]")
TALL_GR_U = P("minecraft:tall_grass[half=upper]")
OAK_LEAF  = P("minecraft:oak_leaves[distance=7,persistent=true,waterlogged=false]")

# Decoration / furnishings
COBWEB        = P("minecraft:cobweb")
BARREL_Y      = P("minecraft:barrel[facing=up,open=false]")
ANVIL         = P("minecraft:anvil[facing=north]")
HAY_Y         = P("minecraft:hay_block[axis=y]")
LECTERN       = P("minecraft:lectern[facing=north,has_book=false,powered=false]")
FLOWER_POT    = P("minecraft:flower_pot")
BANNER_WALL_N = P("minecraft:red_wall_banner[facing=north]")
BANNER_WALL_S = P("minecraft:red_wall_banner[facing=south]")
BANNER_BLUE_N = P("minecraft:blue_wall_banner[facing=north]")

# Iron / decoration
IRON_BARS = P("minecraft:iron_bars[east=true,north=false,south=false,waterlogged=false,west=true]")

# Throne accents (refinement — solar)
RED_BED_FT_N = P("minecraft:red_bed[facing=north,occupied=false,part=foot]")
RED_BED_HD_N = P("minecraft:red_bed[facing=north,occupied=false,part=head]")

# ── 3D grid ────────────────────────────────────────────────────────────
g = [[[AIR for _ in range(SX)] for _ in range(SZ)] for _ in range(SY)]

def setb(x, y, z, b):
    if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
        g[y][z][x] = b

def getb(x, y, z):
    if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
        return g[y][z][x]
    return AIR

def fill_box(x0, y0, z0, x1, y1, z1, block):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, block)

def fill_box_fn(x0, y0, z0, x1, y1, z1, block_fn):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, block_fn())

def fill_wall_x(x0, x1, y, z, block):
    for x in range(x0, x1 + 1):
        setb(x, y, z, block)

# ── Material mixers (the EGALITARIAN 4-era mix — no block > 7%) ────────

def stone_natural():
    """The Onctaaf-validated natural-stone wall fabric.
    Egalitarian mix: no single block exceeds ~7% of placed blocks."""
    r = random.random()
    if r < 0.18: return STONE
    if r < 0.30: return ANDESITE
    if r < 0.40: return P_ANDESITE
    if r < 0.50: return STONE_BRICK
    if r < 0.60: return SMOOTH_STONE
    if r < 0.68: return TUFF_BRICK
    if r < 0.74: return TUFF
    if r < 0.79: return P_TUFF
    if r < 0.84: return MOSSY_BRICK
    if r < 0.88: return CRACK_BRICK
    if r < 0.92: return DIORITE
    if r < 0.96: return COBBLE
    return MOSSY_COBBLE

def stone_founding():
    """Older lower courses (the base of the keep)."""
    r = random.random()
    if r < 0.45: return COBBLE
    if r < 0.70: return MOSSY_COBBLE
    if r < 0.82: return STONE
    if r < 0.92: return CRACK_BRICK
    return ANDESITE

def stone_refinement_chapel():
    """The chapel wing — finer materials."""
    r = random.random()
    if r < 0.40: return P_DIORITE
    if r < 0.60: return DIORITE
    if r < 0.78: return SMOOTH_STONE
    if r < 0.90: return QUARTZ
    return CHIS_QUARTZ

def weathered(base_fn, moss_chance=0.04, crack_chance=0.03):
    """Wrap a material function with moss + cracked aging.
    Onctaaf rule: moss + cracked in roughly equal measure."""
    r = random.random()
    if r < moss_chance: return MOSS
    if r < moss_chance + crack_chance: return CRACK_BRICK
    return base_fn()

# ── Geometry constants ────────────────────────────────────────────────
GROUND_Y = 3                    # top of dirt podium = grass surface
WATER_Y  = 3                    # moat water level
FLOOR_Y  = 4                    # ground floor of keep

# Keep core
KEEP_X0, KEEP_X1 = 15, 32       # 18 wide
KEEP_Z0, KEEP_Z1 = 16, 33       # 18 deep
KEEP_TOP_Y = 21

# Corner towers — ASYMMETRIC (per asymmetry principle)
# NE: tall watchtower (the dominant vertical)
NE_X0, NE_X1, NE_TOP = 32, 36, 28
NE_Z0, NE_Z1 = 12, 16
# NW: shorter tower (never rebuilt)
NW_X0, NW_X1, NW_TOP = 11, 15, 20
NW_Z0, NW_Z1 = 12, 16
# SW: ruined stub (the "story scar")
SW_X0, SW_X1, SW_TOP = 11, 15, 12
SW_Z0, SW_Z1 = 33, 37
# SE: chapel wing (instead of a tower — asymmetric)
CH_X0, CH_X1 = 33, 40
CH_Z0, CH_Z1 = 25, 34
CH_TOP = 13

def in_keep(x, z):
    return KEEP_X0 <= x <= KEEP_X1 and KEEP_Z0 <= z <= KEEP_Z1

def in_tower(x, z, T):
    return T[0] <= x <= T[1] and T[2] <= z <= T[3]

NE_T = (NE_X0, NE_X1, NE_Z0, NE_Z1)
NW_T = (NW_X0, NW_X1, NW_Z0, NW_Z1)
SW_T = (SW_X0, SW_X1, SW_Z0, SW_Z1)
CH_T = (CH_X0, CH_X1, CH_Z0, CH_Z1)

# ════════════════════════════════════════════════════════════════════════
# STAGE 1: Terrain podium + moat (the Onctaaf/Raaamseeel lesson)
# ════════════════════════════════════════════════════════════════════════
def build_terrain():
    # Solid dirt podium across the entire footprint y=0..2
    fill_box(0, 0, 0, SX-1, 2, SZ-1, DIRT)
    # Grass cap at y=3 across the whole footprint
    fill_box(0, 3, 0, SX-1, 3, SZ-1, GRASS)

    # Carve the moat — a ring around the island
    # Island is a 38×38 area centered at (25, 25)
    cx, cz = SX // 2, SZ // 2
    for z in range(SZ):
        for x in range(SX):
            # Distance from center (Chebyshev)
            d = max(abs(x - cx), abs(z - cz))
            if 20 <= d <= 23:
                # Moat: 4 cells wide, water at y=3..4
                setb(x, 0, z, DIRT)  # already dirt
                setb(x, 1, z, DIRT)
                setb(x, 2, z, GRAVEL)  # moat floor
                setb(x, 3, z, WATER)
                setb(x, 4, z, WATER)
            elif d >= 24:
                # Beyond the moat — outer grass
                pass  # already grass
            else:
                # Island interior — grass
                pass

    # Sprinkle moss patches on the island for weathering
    for _ in range(140):
        x = random.randint(0, SX - 1)
        z = random.randint(0, SZ - 1)
        # Only on island grass cells
        if getb(x, 3, z) == GRASS:
            setb(x, 3, z, MOSS)

    # A few coarse dirt patches near the keep base for "worn path" feel
    for _ in range(40):
        x = random.randint(8, SX - 9)
        z = random.randint(8, SZ - 9)
        if getb(x, 3, z) == GRASS:
            setb(x, 3, z, COARSE)

    # Tall grass tufts on the island (a few)
    for _ in range(28):
        x = random.randint(2, SX - 3)
        z = random.randint(2, SZ - 3)
        if getb(x, 3, z) in (GRASS, MOSS) and getb(x, 4, z) == AIR:
            setb(x, 4, z, TALL_GR_L)
            setb(x, 5, z, TALL_GR_U)

    # Two lily pads on the moat
    for _ in range(6):
        x = random.randint(1, SX - 2)
        z = random.randint(1, SZ - 2)
        if getb(x, 4, z) == WATER:
            setb(x, 5, z, LILY)

    # Three trees outside the keep (oak — provides backdrop)
    def small_oak(cx, cy, cz):
        # Trunk
        for dy in range(5):
            setb(cx, cy + dy, cz, DARK_OAK_LOG_Y)
        # Leaves: 5x5 at top, smaller above
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2:
                    continue
                setb(cx + dx, cy + 4, cz + dz, OAK_LEAF)
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                setb(cx + dx, cy + 5, cz + dz, OAK_LEAF)
        setb(cx, cy + 6, cz, OAK_LEAF)
    small_oak(5, 4, 5)
    small_oak(SX - 6, 4, 6)
    small_oak(6, 4, SZ - 6)


# ════════════════════════════════════════════════════════════════════════
# STAGE 2: Keep — main building
# ════════════════════════════════════════════════════════════════════════
def build_keep():
    # Floor 0 (ground): walls y=4..7, with thicker founding base
    # FOUNDING-era courses y=4..5 (the lowest 2 courses are crude)
    for y in (4, 5):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            for z in range(KEEP_Z0, KEEP_Z1 + 1):
                # Perimeter only
                if x in (KEEP_X0, KEEP_X1) or z in (KEEP_Z0, KEEP_Z1):
                    setb(x, y, z, stone_founding())

    # CURTAIN-era walls y=6..7
    for y in (6, 7):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            for z in range(KEEP_Z0, KEEP_Z1 + 1):
                if x in (KEEP_X0, KEEP_X1) or z in (KEEP_Z0, KEEP_Z1):
                    setb(x, y, z, weathered(stone_natural, 0.05, 0.05))

    # Ground floor interior — clear, stone floor
    for x in range(KEEP_X0 + 1, KEEP_X1):
        for z in range(KEEP_Z0 + 1, KEEP_Z1):
            setb(x, 4, z, weathered(stone_natural, 0.03, 0.02))  # floor

    # First-floor break — slab string course (Onctaaf-style horizontal accent)
    for x in range(KEEP_X0, KEEP_X1 + 1):
        setb(x, 8, KEEP_Z0, SMOOTH_SLAB_T)
        setb(x, 8, KEEP_Z1, SMOOTH_SLAB_T)
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        setb(KEEP_X0, 8, z, SMOOTH_SLAB_T)
        setb(KEEP_X1, 8, z, SMOOTH_SLAB_T)

    # GREAT HALL floor y=9..12 (5-block ceiling — refined scale)
    for y in (9, 10, 11, 12):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            for z in range(KEEP_Z0, KEEP_Z1 + 1):
                if x in (KEEP_X0, KEEP_X1) or z in (KEEP_Z0, KEEP_Z1):
                    setb(x, y, z, weathered(stone_natural, 0.05, 0.04))

    # Floor at y=8 (the great hall floor)
    for x in range(KEEP_X0 + 1, KEEP_X1):
        for z in range(KEEP_Z0 + 1, KEEP_Z1):
            setb(x, 8, z, SMOOTH_SLAB_D)

    # Solar floor + walls y=13..16 (refinement era starts)
    for x in range(KEEP_X0 + 1, KEEP_X1):
        for z in range(KEEP_Z0 + 1, KEEP_Z1):
            setb(x, 13, z, P_ANDESITE)  # refined floor

    # Slab string course at floor 2 break
    for x in range(KEEP_X0, KEEP_X1 + 1):
        setb(x, 13, KEEP_Z0, ANDESITE_SLAB_T)
        setb(x, 13, KEEP_Z1, ANDESITE_SLAB_T)
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        setb(KEEP_X0, 13, z, ANDESITE_SLAB_T)
        setb(KEEP_X1, 13, z, ANDESITE_SLAB_T)

    # Solar walls y=14..16 (refinement: more polished_andesite + stone_bricks)
    def solar_stone():
        r = random.random()
        if r < 0.30: return P_ANDESITE
        if r < 0.55: return ANDESITE
        if r < 0.75: return STONE_BRICK
        if r < 0.88: return TUFF_BRICK
        if r < 0.96: return SMOOTH_STONE
        return MOSSY_BRICK

    for y in (14, 15, 16):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            for z in range(KEEP_Z0, KEEP_Z1 + 1):
                if x in (KEEP_X0, KEEP_X1) or z in (KEEP_Z0, KEEP_Z1):
                    setb(x, y, z, solar_stone())

    # Slab parapet around solar (peace-time, NOT crenellated)
    for x in range(KEEP_X0, KEEP_X1 + 1):
        setb(x, 17, KEEP_Z0, BRICK_SLAB_B)
        setb(x, 17, KEEP_Z1, BRICK_SLAB_B)
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        setb(KEEP_X0, 17, z, BRICK_SLAB_B)
        setb(KEEP_X1, 17, z, BRICK_SLAB_B)

    # Stone-slab tier roof (NOT wood A-frame; Onctaaf-style)
    # Tier 1 (outer ring) y=17 — already placed as parapet base
    # Tier 2 — pull in 2 from each side, y=18
    for x in range(KEEP_X0 + 2, KEEP_X1 - 1):
        for z in range(KEEP_Z0 + 2, KEEP_Z1 - 1):
            if x in (KEEP_X0 + 2, KEEP_X1 - 2) or z in (KEEP_Z0 + 2, KEEP_Z1 - 2):
                setb(x, 18, z, BRICK_SLAB_T)
    # Tier 3 — pull in 4
    for x in range(KEEP_X0 + 4, KEEP_X1 - 3):
        for z in range(KEEP_Z0 + 4, KEEP_Z1 - 3):
            if x in (KEEP_X0 + 4, KEEP_X1 - 4) or z in (KEEP_Z0 + 4, KEEP_Z1 - 4):
                setb(x, 19, z, BRICK_SLAB_T)
    # Tier 4 — pull in 6
    for x in range(KEEP_X0 + 6, KEEP_X1 - 5):
        for z in range(KEEP_Z0 + 6, KEEP_Z1 - 5):
            if x in (KEEP_X0 + 6, KEEP_X1 - 6) or z in (KEEP_Z0 + 6, KEEP_Z1 - 6):
                setb(x, 20, z, BRICK_SLAB_T)
    # Tier 5 — pull in 8 (cap), small platform
    for x in range(KEEP_X0 + 7, KEEP_X1 - 6):
        for z in range(KEEP_Z0 + 7, KEEP_Z1 - 6):
            setb(x, 21, z, SMOOTH_SLAB_D)
    # Solid tier interior fill (so roof reads as solid mass)
    for y in (18, 19, 20):
        inset = (y - 17) * 2
        for x in range(KEEP_X0 + inset, KEEP_X1 - inset + 1):
            for z in range(KEEP_Z0 + inset, KEEP_Z1 - inset + 1):
                if KEEP_X0 + inset + 1 <= x <= KEEP_X1 - inset - 1 and KEEP_Z0 + inset + 1 <= z <= KEEP_Z1 - inset - 1:
                    # Inner cells of this tier
                    if getb(x, y, z) == AIR:
                        setb(x, y, z, stone_natural())


# ════════════════════════════════════════════════════════════════════════
# STAGE 3: Three corner towers (asymmetric)
# ════════════════════════════════════════════════════════════════════════
def build_tower(T, base_y, top_y, ruined=False, refined=False):
    x0, x1, z0, z1 = T
    if refined:
        mat_fn = lambda: solar_refined_tower_stone()
    else:
        mat_fn = lambda: stone_natural()

    # Founding base (lowest 3 courses crude)
    for y in (base_y, base_y + 1, base_y + 2):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    setb(x, y, z, stone_founding())

    # Body
    for y in range(base_y + 3, top_y + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    if ruined and y > base_y + 5 and random.random() < 0.35:
                        # Skip — collapsed/ruined upper section
                        continue
                    setb(x, y, z, weathered(mat_fn, 0.06, 0.06))

    # Slab cap (if not ruined)
    if not ruined:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    setb(x, top_y + 1, z, BRICK_SLAB_B)
        # Stone slab apex cap
        cx = (x0 + x1) // 2
        cz = (z0 + z1) // 2
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                setb(cx + dx, top_y + 1, cz + dz, SMOOTH_SLAB_T)
        setb(cx, top_y + 2, cz, CHIS_BRICK)

    # Ruined tower: scatter cobble + cracks at base + rubble outside
    if ruined:
        for _ in range(15):
            rx = random.randint(x0 - 2, x1 + 2)
            rz = random.randint(z0 - 2, z1 + 2)
            if 0 <= rx < SX and 0 <= rz < SZ and getb(rx, 3, rz) in (GRASS, MOSS, COARSE):
                setb(rx, 4, rz, random.choice([COBBLE, MOSSY_COBBLE, CRACK_BRICK]))
        # Add cobwebs in the hollow top
        cx = (x0 + x1) // 2
        cz = (z0 + z1) // 2
        for dy in (-1, 0):
            setb(cx, top_y + dy, cz, COBWEB)

def solar_refined_tower_stone():
    """Material for the refined NE watch tower upper section."""
    r = random.random()
    if r < 0.30: return P_ANDESITE
    if r < 0.50: return ANDESITE
    if r < 0.65: return STONE_BRICK
    if r < 0.78: return TUFF_BRICK
    if r < 0.88: return SMOOTH_STONE
    return MOSSY_BRICK


def build_all_towers():
    build_tower(NE_T, 4, NE_TOP, ruined=False, refined=True)   # tall watchtower
    build_tower(NW_T, 4, NW_TOP, ruined=False, refined=False)  # standard tower
    build_tower(SW_T, 4, SW_TOP, ruined=True,  refined=False)  # ruined stub


# ════════════════════════════════════════════════════════════════════════
# STAGE 4: Chapel wing (refinement era)
# ════════════════════════════════════════════════════════════════════════
def build_chapel():
    x0, x1, z0, z1 = CH_T
    # Foundation y=4..5 mixed founding for transition (visible seam)
    for y in (4, 5):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    setb(x, y, z, stone_founding())

    # Chapel walls y=6..10
    for y in range(6, 11):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    setb(x, y, z, weathered(stone_refinement_chapel, 0.03, 0.02))

    # Stone-slab pitched roof (rising to peak at z = (z0+z1)/2)
    cz = (z0 + z1) // 2
    # West end face (full wall)
    for z in range(z0, z1 + 1):
        # Stairs descending from peak
        # The ridge is along x-axis at z = cz, y = 13
        pass

    # Pitched roof using slab stairs from each side toward the ridge
    for x in range(x0 + 1, x1):
        # North slope: from z0 climbing inward up to cz
        for z in range(z0, cz + 1):
            y_roof = 11 + (z - z0)
            if y_roof > 13:
                y_roof = 13
            if z == cz:
                # Ridge
                setb(x, 13, z, SMOOTH_SLAB_D)
            else:
                setb(x, y_roof, z, BRICK_SLAB_B)
                setb(x, y_roof - 1, z, BRICK_SLAB_T)
        # South slope
        for z in range(cz + 1, z1 + 1):
            y_roof = 11 + (z1 - z)
            if y_roof > 13:
                y_roof = 13
            setb(x, y_roof, z, BRICK_SLAB_B)
            setb(x, y_roof - 1, z, BRICK_SLAB_T)

    # Gable ends (the two short faces — fill with stone up to roof line)
    for z in range(z0, z1 + 1):
        max_y = 13 - abs(z - cz)
        for y in range(11, max_y + 1):
            setb(x0, y, z, stone_refinement_chapel())
            setb(x1, y, z, stone_refinement_chapel())

    # Cathedral window on the east gable
    setb(x1 - 1, 8, cz, AIR)
    setb(x1 - 1, 9, cz, AIR)
    setb(x1 - 1, 10, cz, AIR)
    setb(x1, 8, cz, WHITE_GLASS)
    setb(x1, 9, cz, WHITE_GLASS)
    setb(x1, 10, cz, WHITE_GLASS)

    # Chapel door on west side
    setb(x0, 5, cz, door("lower", "right", "west"))
    setb(x0, 6, cz, door("upper", "right", "west"))

    # Stone floor inside
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            setb(x, 5, z, SMOOTH_SLAB_D)

    # Altar at east end
    altar_x = x1 - 2
    altar_z = cz
    setb(altar_x, 6, altar_z, P_DIORITE)
    setb(altar_x, 6, altar_z - 1, P_DIORITE)
    setb(altar_x, 6, altar_z + 1, P_DIORITE)
    setb(altar_x, 7, altar_z, DIORITE_SLAB_T)
    setb(altar_x, 7, altar_z - 1, DIORITE_SLAB_T)
    setb(altar_x, 7, altar_z + 1, DIORITE_SLAB_T)
    setb(altar_x, 8, altar_z, WHITE_CANDLE3)
    setb(altar_x, 8, altar_z - 1, WHITE_CANDLE3)
    setb(altar_x, 8, altar_z + 1, WHITE_CANDLE3)

    # Hanging lantern over altar
    setb(altar_x, 10, altar_z, LANTERN_H)
    setb(altar_x, 9, altar_z, CHAIN_Y)

    # Lectern in front of altar
    setb(altar_x - 2, 6, altar_z, LECTERN)


# ════════════════════════════════════════════════════════════════════════
# STAGE 5: Windows, doors, openings in the keep
# ════════════════════════════════════════════════════════════════════════
def cut_openings():
    # Main entrance (south face — double door)
    cx = (KEEP_X0 + KEEP_X1) // 2
    # Door is at z = KEEP_Z1 (south face), centered in x
    setb(cx - 1, 5, KEEP_Z1, door("lower", "left", "north"))
    setb(cx - 1, 6, KEEP_Z1, door("upper", "left", "north"))
    setb(cx, 5, KEEP_Z1, door("lower", "right", "north"))
    setb(cx, 6, KEEP_Z1, door("upper", "right", "north"))
    # Stone slab hood over the door
    for dx in (-2, -1, 0, 1):
        setb(cx + dx, 7, KEEP_Z1, SMOOTH_SLAB_B)
    # Trim posts on either side
    for dy in (4, 5, 6):
        setb(cx - 2, dy, KEEP_Z1, CHIS_BRICK)
        setb(cx + 1, dy, KEEP_Z1, CHIS_BRICK)

    # Great hall windows — 3 on each long side at y=10..11
    def carve_window(x, y_lo, y_hi, z, hood_dir):
        for y in range(y_lo, y_hi + 1):
            setb(x, y, z, AIR)
        # Glass pane
        setb(x, y_lo, z, WHITE_GLASS)
        setb(x, y_hi, z, WHITE_GLASS)
        # Slab hood above
        setb(x, y_hi + 1, z, SMOOTH_SLAB_B)

    for x_off in (3, 8, 13):
        carve_window(KEEP_X0 + x_off, 10, 11, KEEP_Z0, "north")
        carve_window(KEEP_X0 + x_off, 10, 11, KEEP_Z1, "south")
    for z_off in (3, 8, 13):
        carve_window(KEEP_X0, 10, 11, KEEP_Z0 + z_off, "west")
        carve_window(KEEP_X1, 10, 11, KEEP_Z0 + z_off, "east")

    # Solar windows (smaller, single pane) at y=15
    for x_off in (4, 9):
        setb(KEEP_X0 + x_off, 15, KEEP_Z0, WHITE_GLASS)
        setb(KEEP_X0 + x_off, 15, KEEP_Z1, WHITE_GLASS)
    for z_off in (4, 9):
        setb(KEEP_X0, 15, KEEP_Z0 + z_off, WHITE_GLASS_EW)
        setb(KEEP_X1, 15, KEEP_Z0 + z_off, WHITE_GLASS_EW)

    # Arrow loops on the lowest course (y=5..6) — single 1-block iron_bars
    for x_off in (2, 7, 14):
        setb(KEEP_X0 + x_off, 5, KEEP_Z0, IRON_BARS)
        setb(KEEP_X0 + x_off, 5, KEEP_Z1, IRON_BARS)


# ════════════════════════════════════════════════════════════════════════
# STAGE 6: Interior — great hall hearth, banquet, throne
# ════════════════════════════════════════════════════════════════════════
def build_interiors():
    cx = (KEEP_X0 + KEEP_X1) // 2
    cz = (KEEP_Z0 + KEEP_Z1) // 2

    # ── Ground floor: storage / undercroft ─────────────────
    # Two barrels and a hay bale near the south door
    setb(cx + 2, 4, KEEP_Z1 - 1, BARREL_Y)
    setb(cx + 2, 5, KEEP_Z1 - 1, BARREL_Y)
    setb(cx + 3, 4, KEEP_Z1 - 1, BARREL_Y)
    setb(cx - 3, 4, KEEP_Z1 - 1, HAY_Y)
    setb(cx - 3, 5, KEEP_Z1 - 1, HAY_Y)

    # Internal stairs from ground (y=5) to great hall (y=8) — NE corner of keep
    stair_x = KEEP_X1 - 2
    stair_z = KEEP_Z0 + 2
    # Use stone_brick_stairs ascending
    setb(stair_x, 5, stair_z, brick_stair("north"))
    setb(stair_x, 5, stair_z + 1, SMOOTH_SLAB_D)
    setb(stair_x, 6, stair_z + 1, brick_stair("north"))
    setb(stair_x, 6, stair_z + 2, SMOOTH_SLAB_D)
    setb(stair_x, 7, stair_z + 2, brick_stair("north"))
    setb(stair_x, 7, stair_z + 3, SMOOTH_SLAB_D)
    # Carve through the great-hall floor at the stair top
    setb(stair_x, 8, stair_z + 3, AIR)

    # ── Great hall floor: hearth + banquet table + throne ───────
    # GREAT HALL HEARTH on north wall (the keep's interior north wall)
    hearth_x = cx
    hearth_z = KEEP_Z0 + 1
    # Hearth recess in the wall — carve through the wall to z=KEEP_Z0
    for dy in (9, 10, 11):
        setb(hearth_x - 1, dy, hearth_z, AIR)
        setb(hearth_x, dy, hearth_z, AIR)
        setb(hearth_x + 1, dy, hearth_z, AIR)
    # Back of hearth = netherrack (then a campfire smoke effect)
    setb(hearth_x - 1, 9, KEEP_Z0, P("minecraft:netherrack"))
    setb(hearth_x, 9, KEEP_Z0, P("minecraft:netherrack"))
    setb(hearth_x + 1, 9, KEEP_Z0, P("minecraft:netherrack"))
    setb(hearth_x, 9, hearth_z, CAMPFIRE)
    # Mantle
    for dx in (-2, -1, 0, 1, 2):
        setb(hearth_x + dx, 12, hearth_z, SMOOTH_SLAB_T)
    # Banner above mantle (south-facing — into the room)
    setb(hearth_x, 12, KEEP_Z0, BANNER_WALL_S)

    # ── Banquet table (long, down the long axis) ────
    table_z_start = KEEP_Z0 + 4
    table_z_end   = KEEP_Z1 - 4
    table_x_c     = cx
    for z in range(table_z_start, table_z_end + 1):
        # 2-wide table top using stripped log (rotated)
        setb(table_x_c - 1, 9, z, P("minecraft:stripped_oak_log[axis=x]"))
        setb(table_x_c, 9, z, P("minecraft:stripped_oak_log[axis=x]"))
        # Benches on outside
        setb(table_x_c - 2, 9, z, SMOOTH_SLAB_B)
        setb(table_x_c + 1, 9, z, SMOOTH_SLAB_B)
    # Candle centerpieces every 3 cells
    for i, z in enumerate(range(table_z_start, table_z_end + 1, 3)):
        setb(table_x_c, 10, z, WHITE_CANDLE3)

    # ── Throne at south end of great hall (facing north) ───
    throne_x = cx
    throne_z = table_z_end + 2
    # Dais (single course of polished andesite slab top)
    for dx in (-2, -1, 0, 1, 2):
        for dz in (0, 1):
            setb(throne_x + dx, 9, throne_z + dz, P_ANDESITE)
    # Throne block
    setb(throne_x, 10, throne_z, CHIS_BRICK)
    setb(throne_x, 11, throne_z, CHIS_BRICK)
    # Banner behind throne
    setb(throne_x, 11, throne_z + 1, BANNER_BLUE_N)

    # ── Hanging lanterns: one over throne, one mid-hall, one over hearth ───
    setb(throne_x, 12, throne_z, LANTERN_H)
    setb(cx, 12, cz, LANTERN_H)
    setb(hearth_x, 11, hearth_z + 1, LANTERN_H)

    # ── Solar interior (y=14..16) — bed + writing desk ────
    bed_x = KEEP_X0 + 3
    bed_z = KEEP_Z0 + 3
    setb(bed_x, 14, bed_z, RED_BED_FT_N)
    setb(bed_x, 14, bed_z - 1, RED_BED_HD_N)
    # Lord's desk (lectern)
    setb(KEEP_X1 - 3, 14, KEEP_Z1 - 3, LECTERN)
    setb(KEEP_X1 - 3, 15, KEEP_Z1 - 3, WHITE_CANDLE3)
    # Small hearth in solar (camp-fire style)
    setb(KEEP_X1 - 3, 14, KEEP_Z0 + 3, CAMPFIRE)
    # One hanging lantern over the bed
    setb(bed_x, 16, bed_z, LANTERN_H)


# ════════════════════════════════════════════════════════════════════════
# STAGE 7: Lighting pass (torches + wall sconces)
# ════════════════════════════════════════════════════════════════════════
def lighting_pass():
    # Wall torches on every exterior keep wall, 4-block spacing, at y=6
    # South face
    for x in range(KEEP_X0 + 2, KEEP_X1, 4):
        if getb(x, 6, KEEP_Z1) == AIR:
            setb(x, 6, KEEP_Z1, wall_torch("south"))
    # North face
    for x in range(KEEP_X0 + 2, KEEP_X1, 4):
        if getb(x, 6, KEEP_Z0) == AIR:
            setb(x, 6, KEEP_Z0, wall_torch("north"))
    # East / west
    for z in range(KEEP_Z0 + 2, KEEP_Z1, 4):
        if getb(KEEP_X1, 6, z) == AIR:
            setb(KEEP_X1, 6, z, wall_torch("east"))
        if getb(KEEP_X0, 6, z) == AIR:
            setb(KEEP_X0, 6, z, wall_torch("west"))

    # Tower corners (NE — the watchtower — gets more lights)
    for y in (6, 12, 18, 24):
        cx = (NE_X0 + NE_X1) // 2
        cz = (NE_Z0 + NE_Z1) // 2
        if 0 <= cx < SX and 0 <= y < SY and 0 <= cz < SZ:
            setb(cx, y, cz, TORCH)

    # NW tower single mid-light
    cx = (NW_X0 + NW_X1) // 2
    cz = (NW_Z0 + NW_Z1) // 2
    setb(cx, 10, cz, TORCH)
    setb(cx, 15, cz, TORCH)

    # Watchtower top — a beacon brazier (campfire on chiseled brick platform)
    cx = (NE_X0 + NE_X1) // 2
    cz = (NE_Z0 + NE_Z1) // 2
    setb(cx, NE_TOP + 3, cz, CHIS_BRICK)
    setb(cx, NE_TOP + 4, cz, CAMPFIRE)

    # Courtyard ground torches on tall posts (4 of them on the island)
    def torch_post(x, z):
        for dy in (4, 5, 6):
            setb(x, dy, z, STRIPPED_OAK_Y)
        setb(x, 7, z, TORCH)
    torch_post(10, 25)
    torch_post(40, 22)
    torch_post(10, 40)
    torch_post(40, 40)


# ════════════════════════════════════════════════════════════════════════
# STAGE 8: Trapdoor batten decoration (Onctaaf signature)
# ════════════════════════════════════════════════════════════════════════
def trapdoor_pass():
    # Trapdoors as wall battens on the keep south face (between windows)
    # Place mounted on the south face at y=8 (the floor-break level)
    for x_off in range(2, 16, 3):
        x = KEEP_X0 + x_off
        # Only if cell next to wall is air
        if 0 <= KEEP_Z1 + 1 < SZ and getb(x, 8, KEEP_Z1 + 1) == AIR:
            setb(x, 8, KEEP_Z1 + 1, trapdoor("south", half="bottom"))
    # And on the north face
    for x_off in range(2, 16, 3):
        x = KEEP_X0 + x_off
        if 0 <= KEEP_Z0 - 1 and getb(x, 8, KEEP_Z0 - 1) == AIR:
            setb(x, 8, KEEP_Z0 - 1, trapdoor("north", half="bottom"))

    # Trapdoors as shutters next to great-hall windows (the carved openings)
    # We carved at x_off in (3, 8, 13). Mount trapdoors at (x-1, y, z) for each.
    for x_off in (3, 8, 13):
        x = KEEP_X0 + x_off
        for y in (10, 11):
            # south wall
            if getb(x - 1, y, KEEP_Z1 - 1) == AIR:
                setb(x - 1, y, KEEP_Z1 - 1, trapdoor("east"))
            if getb(x + 1, y, KEEP_Z1 - 1) == AIR:
                setb(x + 1, y, KEEP_Z1 - 1, trapdoor("west"))


# ════════════════════════════════════════════════════════════════════════
# STAGE 9: Curtain wall + slab parapet between towers
# ════════════════════════════════════════════════════════════════════════
def build_curtain_wall():
    # Wall between NW and NE towers (north face)
    # Goes from (NW_X1+1, 4, ~14) to (NE_X0-1, 4, ~14)
    n_z = 14
    for x in range(NW_X1 + 1, NE_X0):
        for y in (4, 5, 6, 7):
            setb(x, y, n_z, weathered(stone_natural, 0.05, 0.05))
        # Slab top parapet (peace-time, NOT crenellated)
        setb(x, 8, n_z, BRICK_SLAB_B)
        # Inner walk-rail (wall block course, raamseeel-style)
        setb(x, 8, n_z + 1, COBBLE_WALL)

    # Wall between NW and SW (west face) — partial, since SW is ruined
    w_x = 13
    for z in range(NW_Z1 + 1, SW_Z0):
        for y in (4, 5, 6, 7):
            setb(w_x, y, z, weathered(stone_natural, 0.06, 0.06))
        setb(w_x, 8, z, BRICK_SLAB_B)
        setb(w_x + 1, 8, z, COBBLE_WALL)

    # Wall between SW (ruined) and chapel — visible BREAK in the wall
    # (the "story scar")
    s_z = 36
    for x in range(SW_X1 + 1, CH_X0):
        # Skip a gap in the middle — the collapsed section
        if 20 <= x <= 25:
            # Rubble at the base
            if random.random() < 0.5:
                setb(x, 4, s_z, random.choice([COBBLE, MOSSY_COBBLE, CRACK_BRICK]))
            continue
        for y in (4, 5, 6, 7):
            setb(x, y, s_z, weathered(stone_founding, 0.10, 0.10))
        setb(x, 8, s_z, BRICK_SLAB_B)

    # Wall from chapel to NE (east face)
    e_x = 38
    for z in range(CH_Z0, NE_Z1 + 1):
        for y in (4, 5, 6, 7):
            setb(e_x, y, z, weathered(stone_natural, 0.05, 0.04))
        setb(e_x, 8, z, BRICK_SLAB_B)
        setb(e_x - 1, 8, z, COBBLE_WALL)


# ════════════════════════════════════════════════════════════════════════
# STAGE 10: Flower pots, signs, courtyard detail
# ════════════════════════════════════════════════════════════════════════
def courtyard_detail():
    # Some flower pots near the main entrance
    cx = (KEEP_X0 + KEEP_X1) // 2
    setb(cx - 3, 4, KEEP_Z1 + 2, FLOWER_POT)
    setb(cx + 3, 4, KEEP_Z1 + 2, FLOWER_POT)
    # Hay bale at the SW corner (the ruined tower's leftover)
    setb(SW_X1 + 2, 4, SW_Z0 - 2, HAY_Y)
    setb(SW_X1 + 3, 4, SW_Z0 - 2, HAY_Y)
    # Anvil near keep entrance (smithy hint)
    setb(KEEP_X1 + 1, 4, KEEP_Z1 - 2, ANVIL)
    # Lanterns flanking the keep main door (on posts)
    setb(cx - 3, 4, KEEP_Z1 + 1, STRIPPED_OAK_Y)
    setb(cx - 3, 5, KEEP_Z1 + 1, STRIPPED_OAK_Y)
    setb(cx - 3, 6, KEEP_Z1 + 1, LANTERN_F)
    setb(cx + 3, 4, KEEP_Z1 + 1, STRIPPED_OAK_Y)
    setb(cx + 3, 5, KEEP_Z1 + 1, STRIPPED_OAK_Y)
    setb(cx + 3, 6, KEEP_Z1 + 1, LANTERN_F)


# ════════════════════════════════════════════════════════════════════════
# Execute build pipeline
# ════════════════════════════════════════════════════════════════════════
build_terrain()
build_keep()
build_all_towers()
build_chapel()
cut_openings()
build_interiors()
build_curtain_wall()
trapdoor_pass()
lighting_pass()
courtyard_detail()


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
