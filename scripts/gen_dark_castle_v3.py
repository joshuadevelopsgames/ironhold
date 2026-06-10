#!/usr/bin/env python3
"""
Castle of the Hollow Crown — dark_castle_v3.isc.

80×58×80 full concentric fortress (user-picked scale), interiors first-class.
Built against docs/build_recipes: dark_castle palette (austere blackstone,
restraint rules), throne_on_dais, banquet tables, decoration_density targets,
lighting dramaturgy (soul light everywhere; the great-hall hearth is the one
warm-fire exception).

Layout
  y=0..4    Crypt under the keep (below grade when pasted at ground level)
  y=5       Courtyard ground level
  y=5..18   Outer curtain wall (walkway y=19, crenels y=20)
  y=5..34   Four distinct corner towers (SW tower is the collapsed scar)
  y=5..39   Keep: great hall (y6..16, double height) → library+armory (y18..24)
            → lord's solar (y26..31) → chapel (y33..38)
  y=40..55  Keep roof, spire, flag

Paste with the in-game ISC struct function; sink 5 so the crypt sits below grade.
"""
import random
from pathlib import Path

SX, SY, SZ = 80, 58, 80
SEED = 47
OUT = Path(__file__).resolve().parents[1] / "structures" / "dark_castle_v3.isc"

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

# Stone family (dark_castle palette weights)
PBSB       = P("minecraft:polished_blackstone_bricks")
PBSB_CRACK = P("minecraft:cracked_polished_blackstone_bricks")
CDS        = P("minecraft:cobbled_deepslate")
DSB_CRACK  = P("minecraft:cracked_deepslate_bricks")
DSB        = P("minecraft:deepslate_bricks")
DS_TILE    = P("minecraft:deepslate_tiles")
PDS        = P("minecraft:polished_deepslate")
PBS        = P("minecraft:polished_blackstone")
CHIS_PBS   = P("minecraft:chiseled_polished_blackstone")
CHIS_DS    = P("minecraft:chiseled_deepslate")
GILDED     = P("minecraft:gilded_blackstone")     # throne room ONLY
OBSIDIAN   = P("minecraft:obsidian")
CRY_OBS    = P("minecraft:crying_obsidian")
BS         = P("minecraft:blackstone")

def stone():
    """Dark-castle bulk masonry mix (palettes/dark_castle.md)."""
    r = random.random()
    if r < 0.50: return PBSB
    if r < 0.68: return CDS
    if r < 0.80: return PBSB_CRACK
    if r < 0.90: return DSB_CRACK
    return DS_TILE

def floor_stone():
    r = random.random()
    if r < 0.55: return DS_TILE
    if r < 0.80: return PDS
    if r < 0.92: return PBSB
    return CDS

def courtyard_stone():
    r = random.random()
    if r < 0.45: return CDS
    if r < 0.70: return DS_TILE
    if r < 0.85: return PDS
    if r < 0.96: return PBSB
    return DSB_CRACK

def rubble():
    r = random.random()
    if r < 0.5: return CDS
    if r < 0.8: return PBSB_CRACK
    return DSB_CRACK

# Slabs / stairs
def slab(block, half="bottom"):
    return P(f"minecraft:{block}_slab[type={half},waterlogged=false]")

def stair(block, facing, half="bottom", shape="straight"):
    return P(f"minecraft:{block}_stairs[facing={facing},half={half},shape={shape},waterlogged=false]")

PBSB_SLAB_B = slab("polished_blackstone_brick")
PBSB_SLAB_T = slab("polished_blackstone_brick", "top")
PDS_SLAB_B  = slab("polished_deepslate")
DS_TILE_SLAB_B = slab("deepslate_tile")
DOAK_SLAB_B = slab("dark_oak")
DOAK_SLAB_T = slab("dark_oak", "top")

# Wood
DOAK_LOG_Y = P("minecraft:dark_oak_log[axis=y]")
DOAK_LOG_X = P("minecraft:dark_oak_log[axis=x]")
DOAK_LOG_Z = P("minecraft:dark_oak_log[axis=z]")
DOAK_PLANK = P("minecraft:dark_oak_planks")
DOAK_FENCE = P("minecraft:dark_oak_fence[east=false,north=false,south=false,up=true,waterlogged=false,west=false]")
BOOKSHELF  = P("minecraft:bookshelf")

# Lighting (soul throughout; warm fire ONLY in the great-hall hearth)
SOUL_TORCH = P("minecraft:soul_torch")
def soul_wall_torch(f): return P(f"minecraft:soul_wall_torch[facing={f}]")
SOUL_LANT_H = P("minecraft:soul_lantern[hanging=true,waterlogged=false]")
SOUL_LANT   = P("minecraft:soul_lantern[hanging=false,waterlogged=false]")
LANT_H      = P("minecraft:lantern[hanging=true,waterlogged=false]")
SOUL_FIRE   = P("minecraft:soul_fire")
FIRE        = P("minecraft:fire[age=0]")
SOUL_SAND   = P("minecraft:soul_sand")
NETHERRACK  = P("minecraft:netherrack")
def candle(color, n, lit=True):
    return P(f"minecraft:{color}_candle[candles={n},lit={'true' if lit else 'false'},waterlogged=false]")
CHAIN_Y = P("minecraft:chain[axis=y]")

# Banners (black with red accents — lord's colors)
def wall_banner(color, f): return P(f"minecraft:{color}_wall_banner[facing={f}]")
def floor_banner(color, rot): return P(f"minecraft:{color}_banner[rotation={rot}]")

# Doors / gates
def door(f, half, hinge="left"):
    return P(f"minecraft:dark_oak_door[facing={f},half={half},hinge={hinge},open=false,powered=false]")
def iron_bars(props): return P(f"minecraft:iron_bars[{props},waterlogged=false]")
BARS_NS = iron_bars("east=false,north=true,south=true,west=false")
BARS_EW = iron_bars("east=true,north=false,south=false,west=true")
BARS_POST = iron_bars("east=false,north=false,south=false,west=false")

# Furnishings
def bed(color, f, part): return P(f"minecraft:{color}_bed[facing={f},occupied=false,part={part}]")
CHEST_N = P("minecraft:chest[facing=north,type=single,waterlogged=false]")
CHEST_S = P("minecraft:chest[facing=south,type=single,waterlogged=false]")
CHEST_E = P("minecraft:chest[facing=east,type=single,waterlogged=false]")
BARREL_UP = P("minecraft:barrel[facing=up,open=false]")
BARREL_S  = P("minecraft:barrel[facing=south,open=false]")
SMOKER_S  = P("minecraft:smoker[facing=south,lit=true]")
BLAST_S   = P("minecraft:blast_furnace[facing=south,lit=false]")
CAULDRON  = P("minecraft:water_cauldron[level=3]")
ANVIL_X   = P("minecraft:anvil[facing=east]")
ANVIL_CHIP= P("minecraft:chipped_anvil[facing=south]")
GRINDSTONE= P("minecraft:grindstone[face=floor,facing=north]")
SMITH_TBL = P("minecraft:smithing_table")
FLETCH_TBL= P("minecraft:fletching_table")
CART_TBL  = P("minecraft:cartography_table")
BREW_STAND= P("minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=false,has_bottle_2=true]")
ENCH_TABLE= P("minecraft:enchanting_table")
LECTERN_N = P("minecraft:lectern[facing=north,has_book=true,powered=false]")
LECTERN_S = P("minecraft:lectern[facing=south,has_book=false,powered=false]")
HAY_Y     = P("minecraft:hay_block[axis=y]")
HAY_X     = P("minecraft:hay_block[axis=x]")
def carpet(color): return P(f"minecraft:{color}_carpet")
RED_CARPET   = carpet("red")
BLACK_CARPET = carpet("black")
GRAY_CARPET  = carpet("gray")
def tripwire_hook(f): return P(f"minecraft:tripwire_hook[attached=false,facing={f},powered=false]")
IRON_BLOCK = P("minecraft:iron_block")
GOLD_BLOCK = P("minecraft:gold_block")
def skull(rot): return P(f"minecraft:skeleton_skull[rotation={rot},powered=false]")
def wither_skull(rot): return P(f"minecraft:wither_skeleton_skull[rotation={rot},powered=false]")
def wall_skull(f): return P(f"minecraft:skeleton_wall_skull[facing={f},powered=false]")
COBWEB    = P("minecraft:cobweb")
SCULK     = P("minecraft:sculk")
SCULK_VEIN_UP = P("minecraft:sculk_vein[down=false,east=false,north=false,south=false,up=true,waterlogged=false,west=false]")
DEAD_BUSH_POT = P("minecraft:potted_dead_bush")
WITHER_ROSE_POT = P("minecraft:potted_wither_rose")
WITHER_ROSE = P("minecraft:wither_rose")
COARSE_DIRT = P("minecraft:coarse_dirt")
def ladder(f): return P(f"minecraft:ladder[facing={f},waterlogged=false]")
WATER = P("minecraft:water[level=0]")
TINTED_GLASS = P("minecraft:tinted_glass")
def pane(props): return P(f"minecraft:gray_stained_glass_pane[{props},waterlogged=false]")
PANE_NS = pane("east=false,north=true,south=true,west=false")
PANE_EW = pane("east=true,north=false,south=false,west=true")
COMPOSTER = P("minecraft:composter[level=0]")
DOAK_TRAP_BOT = P("minecraft:dark_oak_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]")
DOAK_PRESSURE = P("minecraft:dark_oak_pressure_plate[powered=false]")

# Stairs commonly used
PBSB_ST = {f: stair("polished_blackstone_brick", f) for f in ("north", "south", "east", "west")}
PBSB_ST_TOP = {f: stair("polished_blackstone_brick", f, "top") for f in ("north", "south", "east", "west")}
DOAK_ST = {f: stair("dark_oak", f) for f in ("north", "south", "east", "west")}
DS_TILE_ST = {f: stair("deepslate_tile", f) for f in ("north", "south", "east", "west")}

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

def fill_box_fn(x0, y0, z0, x1, y1, z1, fn):
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                setb(x, y, z, fn())

# ── Geometry constants ─────────────────────────────────────────────────
GROUND = 5                  # courtyard ground level
WALL_TOP = 18               # outer curtain top of solid wall
WALK_Y = 19                 # walkway level (player stands here)
CREN_Y = 20                 # crenel tops
TOWER = 11                  # corner tower footprint
TOWER_TOP = 34
GATE_X0, GATE_X1 = 37, 42   # gate passage in south wall
KEEP_X0, KEEP_X1 = 25, 54   # keep 30 wide
KEEP_Z0, KEEP_Z1 = 13, 44   # keep 32 deep, set toward north
# Keep floors (floor slab at the named y; room air above it)
HALL_FLOOR = GROUND         # great hall floor y=5, room y6..16 (double height)
F2_FLOOR  = 17              # library + armory floor slab y=17, room y18..24
F3_FLOOR  = 25              # solar floor slab y=25, room y26..31
F4_FLOOR  = 32              # chapel floor slab y=32, room y33..38
KEEP_ROOF = 39              # roof slab y=39, battlements above

# ════════════════════════════════════════════════════════════════════════
# STAGE 0: Foundation slab + courtyard floor
# ════════════════════════════════════════════════════════════════════════
fill_box_fn(0, GROUND - 1, 0, SX - 1, GROUND - 1, SZ - 1, courtyard_stone)
# Solid plinth under the walls so the paste seats well on uneven ground
fill_box_fn(0, GROUND - 2, 0, SX - 1, GROUND - 2, SZ - 1, stone)

# ════════════════════════════════════════════════════════════════════════
# STAGE 1: Outer curtain wall — BARE exterior (restraint rule 7)
# ════════════════════════════════════════════════════════════════════════
def curtain():
    t = 2  # wall thickness
    for y in range(GROUND, WALL_TOP + 1):
        for x in range(SX):
            for d in range(t):
                setb(x, y, d, stone())
                setb(x, y, SZ - 1 - d, stone())
        for z in range(SZ):
            for d in range(t):
                setb(d, y, z, stone())
                setb(SX - 1 - d, y, z, stone())
    # Walkway floor (the wall top) + inner guard lip
    for x in range(SX):
        setb(x, WALK_Y - 1, 0, PDS); setb(x, WALK_Y - 1, 1, PDS)
        setb(x, WALK_Y - 1, SZ - 1, PDS); setb(x, WALK_Y - 1, SZ - 2, PDS)
    for z in range(SZ):
        setb(0, WALK_Y - 1, z, PDS); setb(1, WALK_Y - 1, z, PDS)
        setb(SX - 1, WALK_Y - 1, z, PDS); setb(SX - 2, WALK_Y - 1, z, PDS)
    # Crenellation: merlon-gap-merlon along the OUTER edge row
    for x in range(0, SX, 2):
        setb(x, WALK_Y, 0, stone()); setb(x, CREN_Y, 0, PBSB_SLAB_B)
        setb(x, WALK_Y, SZ - 1, stone()); setb(x, CREN_Y, SZ - 1, PBSB_SLAB_B)
    for z in range(0, SZ, 2):
        setb(0, WALK_Y, z, stone()); setb(0, CREN_Y, z, PBSB_SLAB_B)
        setb(SX - 1, WALK_Y, z, stone()); setb(SX - 1, CREN_Y, z, PBSB_SLAB_B)
    # Sparse soul torches on the walkway inner lip (dread density: gaps of dark)
    for x in range(8, SX - 8, 16):
        setb(x, WALK_Y, 2, SOUL_TORCH)
        setb(x, WALK_Y, SZ - 3, SOUL_TORCH)
    for z in range(8, SZ - 8, 16):
        setb(2, WALK_Y, z, SOUL_TORCH)
        setb(SX - 3, WALK_Y, z, SOUL_TORCH)

curtain()

# ════════════════════════════════════════════════════════════════════════
# STAGE 2: Four corner towers — each distinct
# ════════════════════════════════════════════════════════════════════════
def tower_shell(x0, z0, top):
    x1, z1 = x0 + TOWER - 1, z0 + TOWER - 1
    for y in range(GROUND, top + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                # cut the corners for an octagon-ish read
                corner = (x in (x0, x1)) and (z in (z0, z1))
                if corner:
                    setb(x, y, z, AIR)
                elif edge:
                    setb(x, y, z, stone())
    # interior floors every 7
    for fy in range(GROUND + 7, top - 2, 7):
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                if not ((x in (x0, x1)) and (z in (z0, z1))):
                    setb(x, fy, z, DOAK_PLANK)
    return x1, z1

def tower_crenels(x0, z0, top):
    x1, z1 = x0 + TOWER - 1, z0 + TOWER - 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = (x in (x0, x1)) and (z in (z0, z1))
            if edge and not corner:
                if (x + z) % 2 == 0:
                    setb(x, top + 1, z, stone())
                    setb(x, top + 2, z, PBSB_SLAB_B)
            elif not edge:
                setb(x, top, z, PDS)

def arrow_loops(x0, z0, top, axis):
    """Vertical 1×2 slits at two heights on the outward faces."""
    cx, cz = x0 + TOWER // 2, z0 + TOWER // 2
    for ly in (GROUND + 5, GROUND + 13, GROUND + 21):
        if ly + 1 > top - 1:
            continue
        if axis == "x":
            for face_x in (x0, x0 + TOWER - 1):
                setb(face_x, ly, cz, AIR)
                setb(face_x, ly + 1, cz, AIR)
        else:
            for face_z in (z0, z0 + TOWER - 1):
                setb(cx, ly, face_z, AIR)
                setb(cx, ly + 1, face_z, AIR)

# NW: drum tower with conical-ish stepped roof
x1, z1 = tower_shell(0, 0, TOWER_TOP)
arrow_loops(0, 0, TOWER_TOP, "x")
for i, (inset, blk) in enumerate([(0, PBSB_SLAB_B), (1, None), (2, None), (3, None)]):
    pass
def stepped_cap(x0, z0, base_y):
    """Stepped pyramid cap (roofs/ stepped pyramid recipe)."""
    half = TOWER // 2
    for step in range(half + 1):
        y = base_y + step
        xa, za = x0 + step, z0 + step
        xb, zb = x0 + TOWER - 1 - step, z0 + TOWER - 1 - step
        if xa > xb:
            break
        for x in range(xa, xb + 1):
            for z in range(za, zb + 1):
                edge = x in (xa, xb) or z in (za, zb)
                if edge:
                    setb(x, y, z, DS_TILE if random.random() < 0.8 else PBSB)
    setb(x0 + half, base_y + half + 1, z0 + half, SOUL_LANT)

stepped_cap(0, 0, TOWER_TOP + 1)

# NE: square watch tower, taller, open crenellated top with brazier
x0, z0 = SX - TOWER, 0
tower_shell(x0, z0, TOWER_TOP + 4)
arrow_loops(x0, z0, TOWER_TOP + 4, "z")
tower_crenels(x0, z0, TOWER_TOP + 4)
bx, bz = x0 + TOWER // 2, z0 + TOWER // 2
setb(bx, TOWER_TOP + 5, bz, NETHERRACK)
setb(bx, TOWER_TOP + 6, bz, SOUL_FIRE)   # signal brazier — soul, of course

# SE: chapel-top tower (tinted-glass lancets near the top)
x0, z0 = SX - TOWER, SZ - TOWER
tower_shell(x0, z0, TOWER_TOP)
tower_crenels(x0, z0, TOWER_TOP)
cx, cz = x0 + TOWER // 2, z0 + TOWER // 2
for ly in (TOWER_TOP - 6, TOWER_TOP - 5, TOWER_TOP - 4):
    setb(x0, ly, cz, TINTED_GLASS)
    setb(x0 + TOWER - 1, ly, cz, TINTED_GLASS)
    setb(cx, ly, z0 + TOWER - 1, TINTED_GLASS)

# SW: THE SCAR — collapsed tower (restraint rule 6: one signature ruin)
def ruined_tower():
    x0, z0 = 0, SZ - TOWER
    x1, z1 = x0 + TOWER - 1, z0 + TOWER - 1
    break_y = GROUND + 12  # tower shears off at this height, ragged
    for y in range(GROUND, TOWER_TOP + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = (x in (x0, x1)) and (z in (z0, z1))
                if corner or not edge:
                    continue
                # ragged break line: keep more on the courtyard-facing side
                local_max = break_y + (x - x0) // 2 + random.randint(-2, 1)
                if y <= local_max:
                    setb(x, y, z, rubble() if y > break_y - 4 else stone())
    # rubble apron spilling into the courtyard + over the wall
    for _ in range(90):
        rx = x0 + random.randint(2, TOWER + 7)
        rz = z1 - random.randint(2, TOWER + 5)
        ry = GROUND
        if getb(rx, ry, rz) == AIR or random.random() < 0.4:
            setb(rx, ry, rz, rubble())
            if random.random() < 0.3:
                setb(rx, ry + 1, rz, PBSB_SLAB_B)
    # sculk creep in the wound (the castle's sickness shows here)
    for _ in range(25):
        rx = x0 + random.randint(1, TOWER - 2)
        rz = z0 + random.randint(1, TOWER - 2)
        setb(rx, GROUND - 1, rz, SCULK)
    # a couple of cobwebs in the broken stump
    setb(x0 + 2, break_y - 1, z0 + 2, COBWEB)
    setb(x0 + 4, break_y - 3, z0 + 6, COBWEB)

ruined_tower()

# ════════════════════════════════════════════════════════════════════════
# STAGE 3: Gatehouse (south wall) — competent shape, wrong materials (rule 5)
# ════════════════════════════════════════════════════════════════════════
def gatehouse():
    gz = SZ - 1            # south face
    tw = 7                 # flanking turret width
    # Twin turrets flanking the gate, proud of the wall line
    for tx0 in (GATE_X0 - tw, GATE_X1 + 1):
        for y in range(GROUND, WALL_TOP + 6):
            for x in range(tx0, tx0 + tw):
                for z in (SZ - 4, SZ - 3, SZ - 2, SZ - 1):
                    edge = x in (tx0, tx0 + tw - 1) or z in (SZ - 4, SZ - 1)
                    if edge:
                        setb(x, y, z, stone())
                    elif y in (GROUND + 6, GROUND + 12):
                        setb(x, y, z, DOAK_PLANK)
                    else:
                        setb(x, y, z, AIR)
        # turret crenels
        for x in range(tx0, tx0 + tw):
            if x % 2 == 0:
                setb(x, WALL_TOP + 6, SZ - 1, stone())
                setb(x, WALL_TOP + 7, SZ - 1, PBSB_SLAB_B)
                setb(x, WALL_TOP + 6, SZ - 4, stone())
    # Gate passage: punch through the curtain, 4 wide × 6 high, arched top
    for x in range(GATE_X0, GATE_X1 + 1):
        for y in range(GROUND, GROUND + 6):
            for z in (SZ - 1, SZ - 2, SZ - 3, SZ - 4):
                setb(x, y, z, AIR)
    # Arch shoulders
    for z in (SZ - 1, SZ - 2, SZ - 3, SZ - 4):
        setb(GATE_X0, GROUND + 5, z, PBSB_ST_TOP["east"])
        setb(GATE_X1, GROUND + 5, z, PBSB_ST_TOP["west"])
        setb(GATE_X0, GROUND + 6, z, CHIS_PBS)
        setb(GATE_X1, GROUND + 6, z, CHIS_PBS)
        for x in range(GATE_X0 + 1, GATE_X1):
            setb(x, GROUND + 6, z, PBSB)
    # Portcullis (iron bars, dropped) in the outer arch plane
    for x in range(GATE_X0, GATE_X1 + 1):
        for y in range(GROUND, GROUND + 5):
            setb(x, y, SZ - 2, BARS_EW)
    # Murder holes in the passage ceiling (doors/murder_hole recipe)
    for x in (GATE_X0 + 1, GATE_X1 - 1):
        setb(x, GROUND + 6, SZ - 3, AIR)
    # Gallery room above the gate (guards over the murder holes)
    for x in range(GATE_X0 - 1, GATE_X1 + 2):
        for y in range(GROUND + 7, GROUND + 11):
            for z in (SZ - 2, SZ - 3):
                setb(x, y, z, AIR)
    for x in range(GATE_X0, GATE_X1 + 1):
        setb(x, GROUND + 7, SZ - 2, DOAK_PLANK)
        setb(x, GROUND + 7, SZ - 3, DOAK_PLANK)
    # re-open the murder holes through the gallery floor
    for x in (GATE_X0 + 1, GATE_X1 - 1):
        setb(x, GROUND + 7, SZ - 3, DOAK_TRAP_BOT)
    setb(GATE_X0 + 1, GROUND + 8, SZ - 3, soul_wall_torch("north"))
    setb(GATE_X1 - 1, GROUND + 8, SZ - 3, soul_wall_torch("north"))
    # ONE wither skull on a fence post beside the gate (rule 2: exactly one)
    setb(GATE_X1 + 3, GROUND, SZ - 6, DOAK_FENCE)
    setb(GATE_X1 + 3, GROUND + 1, SZ - 6, wither_skull(8))
    # Cobbled approach road through the gate into the courtyard
    for z in range(SZ - 12, SZ - 1):
        for x in range(GATE_X0 + 1, GATE_X1):
            setb(x, GROUND - 1, z, PDS if random.random() < 0.6 else DS_TILE)

gatehouse()

# ════════════════════════════════════════════════════════════════════════
# STAGE 4: Keep shell + floors
# ════════════════════════════════════════════════════════════════════════
def keep_shell():
    # Outer wall, 2 thick
    for y in range(GROUND, KEEP_ROOF + 1):
        for x in range(KEEP_X0, KEEP_X1 + 1):
            for z in range(KEEP_Z0, KEEP_Z1 + 1):
                edge1 = x in (KEEP_X0, KEEP_X1) or z in (KEEP_Z0, KEEP_Z1)
                edge2 = x in (KEEP_X0 + 1, KEEP_X1 - 1) or z in (KEEP_Z0 + 1, KEEP_Z1 - 1)
                if edge1 or edge2:
                    setb(x, y, z, stone())
                else:
                    setb(x, y, z, AIR)
    # Buttresses on the long faces every 7 blocks (silhouette interest)
    for x in range(KEEP_X0 + 3, KEEP_X1 - 2, 7):
        for y in range(GROUND, KEEP_ROOF - 6):
            setb(x, y, KEEP_Z0 - 1, stone())
            setb(x, y, KEEP_Z1 + 1, stone())
        setb(x, KEEP_ROOF - 6, KEEP_Z0 - 1, PBSB_ST["north"])
        setb(x, KEEP_ROOF - 6, KEEP_Z1 + 1, PBSB_ST["south"])
    # Floor slabs
    for (fy, blk_fn) in ((F2_FLOOR, floor_stone), (F3_FLOOR, floor_stone), (F4_FLOOR, floor_stone)):
        for x in range(KEEP_X0 + 2, KEEP_X1 - 1):
            for z in range(KEEP_Z0 + 2, KEEP_Z1 - 1):
                setb(x, fy, z, blk_fn())
    # Great hall floor
    for x in range(KEEP_X0 + 2, KEEP_X1 - 1):
        for z in range(KEEP_Z0 + 2, KEEP_Z1 - 1):
            setb(x, HALL_FLOOR, z, floor_stone())
    # Roof deck + crenels
    for x in range(KEEP_X0 + 2, KEEP_X1 - 1):
        for z in range(KEEP_Z0 + 2, KEEP_Z1 - 1):
            setb(x, KEEP_ROOF, z, PDS)
    for x in range(KEEP_X0, KEEP_X1 + 1):
        for z in (KEEP_Z0, KEEP_Z1):
            if x % 2 == 0:
                setb(x, KEEP_ROOF + 1, z, stone())
                setb(x, KEEP_ROOF + 2, z, PBSB_SLAB_B)
    for z in range(KEEP_Z0, KEEP_Z1 + 1):
        for x in (KEEP_X0, KEEP_X1):
            if z % 2 == 0:
                setb(x, KEEP_ROOF + 1, z, stone())
                setb(x, KEEP_ROOF + 2, z, PBSB_SLAB_B)

keep_shell()

# Keep entrance: double door centered on the south face, with stoop
KEEP_DOOR_X = (KEEP_X0 + KEEP_X1) // 2  # 39
def keep_entrance():
    z = KEEP_Z1  # south face of keep
    for x in (KEEP_DOOR_X, KEEP_DOOR_X + 1):
        for y in (GROUND + 1, GROUND + 2, GROUND + 3):
            setb(x, y, z, AIR)
            setb(x, y, z - 1, AIR)
    # frame
    for y in (GROUND + 1, GROUND + 2, GROUND + 3):
        setb(KEEP_DOOR_X - 1, y, z, DOAK_LOG_Y)
        setb(KEEP_DOOR_X + 2, y, z, DOAK_LOG_Y)
    setb(KEEP_DOOR_X - 1, GROUND + 4, z, CHIS_PBS)
    setb(KEEP_DOOR_X + 2, GROUND + 4, z, CHIS_PBS)
    setb(KEEP_DOOR_X, GROUND + 4, z, PBSB_ST_TOP["east"])
    setb(KEEP_DOOR_X + 1, GROUND + 4, z, PBSB_ST_TOP["west"])
    # doors (hinges mirrored)
    setb(KEEP_DOOR_X, GROUND + 1, z, door("north", "lower", "left"))
    setb(KEEP_DOOR_X, GROUND + 2, z, door("north", "upper", "left"))
    setb(KEEP_DOOR_X + 1, GROUND + 1, z, door("north", "lower", "right"))
    setb(KEEP_DOOR_X + 1, GROUND + 2, z, door("north", "upper", "right"))
    # soul sconces flanking
    setb(KEEP_DOOR_X - 1, GROUND + 3, z + 1, soul_wall_torch("south"))
    setb(KEEP_DOOR_X + 2, GROUND + 3, z + 1, soul_wall_torch("south"))
    # steps
    for x in range(KEEP_DOOR_X - 1, KEEP_DOOR_X + 3):
        setb(x, GROUND, z + 1, PBSB_ST["south"])

keep_entrance()

# Tall lancet windows for the great hall (east + west faces), tinted glass
def hall_windows():
    for z in range(KEEP_Z0 + 5, KEEP_Z1 - 4, 6):
        for y in range(GROUND + 4, GROUND + 9):
            for face_x, inner_x in ((KEEP_X0, KEEP_X0 + 1), (KEEP_X1, KEEP_X1 - 1)):
                setb(face_x, y, z, TINTED_GLASS)
                setb(inner_x, y, z, AIR)
        for face_x in (KEEP_X0, KEEP_X1):
            setb(face_x, GROUND + 9, z, CHIS_PBS)
    # smaller upper-floor windows (slits — keep the dread)
    for fy in (F2_FLOOR + 3, F3_FLOOR + 3, F4_FLOOR + 3):
        for z in range(KEEP_Z0 + 6, KEEP_Z1 - 5, 8):
            for face_x, inner_x in ((KEEP_X0, KEEP_X0 + 1), (KEEP_X1, KEEP_X1 - 1)):
                setb(face_x, fy, z, AIR)
                setb(face_x, fy + 1, z, AIR)
                setb(inner_x, fy, z, AIR)
                setb(inner_x, fy + 1, z, AIR)

hall_windows()

# ════════════════════════════════════════════════════════════════════════
# STAGE 5: GREAT HALL (y6..16) — throne, banquet, hearths, banners
# Ceremonial density target: 12–20 furnishing/atmospheric per 10 m².
# ════════════════════════════════════════════════════════════════════════
HX0, HX1 = KEEP_X0 + 2, KEEP_X1 - 2
HZ0, HZ1 = KEEP_Z0 + 2, KEEP_Z1 - 2
HY = GROUND + 1  # standing level in hall

def great_hall():
    # ── Throne on dais (furniture/throne_on_dais.md), north end, facing south ──
    dais_x0 = KEEP_DOOR_X - 2
    dais_z0 = KEEP_Z0 + 3
    for dx in range(5):
        for dz in range(5):
            for dy in range(2):
                setb(dais_x0 + dx, HY + dy - 1 + 1, dais_z0 + dz, GILDED if random.random() < 0.18 else PBS)
    # step lip on the south (approach) edge
    for dx in range(5):
        setb(dais_x0 + dx, HY + 1, dais_z0 + 4, PBSB_SLAB_B)
    tx, tz = dais_x0 + 2, dais_z0 + 2
    setb(tx, HY + 2, tz, OBSIDIAN)                 # seat
    setb(tx, HY + 3, tz - 1, CRY_OBS)              # high back
    setb(tx, HY + 4, tz - 1, CRY_OBS)
    setb(tx - 1, HY + 3, tz - 1, PBSB_ST_TOP["west"])  # armrest shoulders
    setb(tx + 1, HY + 3, tz - 1, PBSB_ST_TOP["east"])
    setb(tx - 1, HY + 2, tz, PBSB_ST["west"])      # armrests
    setb(tx + 1, HY + 2, tz, PBSB_ST["east"])
    # rule of one light: the single soul lantern over the throne
    setb(tx, HY + 9, tz, CHAIN_Y)
    setb(tx, HY + 8, tz, CHAIN_Y)
    setb(tx, HY + 7, tz, SOUL_LANT_H)
    # banner triptych on the wall behind
    setb(tx - 2, HY + 4, KEEP_Z0 + 2, wall_banner("black", "south"))
    setb(tx, HY + 5, KEEP_Z0 + 2, wall_banner("red", "south"))
    setb(tx + 2, HY + 4, KEEP_Z0 + 2, wall_banner("black", "south"))

    # ── Red carpet runner: door → dais ──
    for z in range(dais_z0 + 5, KEEP_Z1 - 1):
        setb(KEEP_DOOR_X, HY, z, RED_CARPET)
        setb(KEEP_DOOR_X + 1, HY, z, RED_CARPET)

    # ── Twin banquet tables flanking the runner (banquet_table_long) ──
    def banquet_table(x, z0_, z1_):
        for z in range(z0_, z1_ + 1):
            setb(x, HY, z, DOAK_FENCE if z in (z0_, z1_) else DOAK_FENCE)
            setb(x, HY + 1, z, DOAK_SLAB_T)
            # candles down the table line every 4
            if (z - z0_) % 4 == 2:
                setb(x, HY + 2, z, candle("black", 2))
        # stair chairs both sides, facing the table
        for z in range(z0_, z1_ + 1, 2):
            setb(x - 1, HY, z, DOAK_ST["east"])
            setb(x + 1, HY, z, DOAK_ST["west"])

    banquet_table(KEEP_DOOR_X - 5, dais_z0 + 8, KEEP_Z1 - 8)
    banquet_table(KEEP_DOOR_X + 6, dais_z0 + 8, KEEP_Z1 - 8)

    # ── Twin hearths on east + west walls — THE warm-fire exception (rule 4) ──
    def hearth(face_x, inward):
        hz = (HZ0 + HZ1) // 2
        # firebox carved into the wall thickness
        for z in range(hz - 1, hz + 2):
            for y in range(HY, HY + 3):
                setb(face_x, y, z, AIR)
        setb(face_x, HY - 1, hz, NETHERRACK)
        setb(face_x, HY, hz, FIRE)
        # frame in polished blackstone, chiseled mantle
        for z in (hz - 2, hz + 2):
            for y in range(HY, HY + 3):
                setb(face_x + inward, y, z, PBS)
        for z in range(hz - 2, hz + 3):
            setb(face_x + inward, HY + 3, z, CHIS_PBS)
        # andiron look: iron bars sides inside the box
        setb(face_x, HY, hz - 1, BARS_POST)
        setb(face_x, HY, hz + 1, BARS_POST)

    hearth(KEEP_X0 + 1, 1)
    hearth(KEEP_X1 - 1, -1)

    # ── Chandeliers: two, over the tables ──
    def chandelier(x, z):
        setb(x, HY + 10, z, CHAIN_Y)
        setb(x, HY + 9, z, CHAIN_Y)
        setb(x, HY + 8, z, DOAK_SLAB_T)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            setb(x + dx, HY + 8, z + dz, DOAK_SLAB_T)
            setb(x + dx, HY + 9, z + dz, candle("red", 3))
        setb(x, HY + 9, z, candle("red", 4))

    mid_z = (HZ0 + HZ1) // 2
    chandelier(KEEP_DOOR_X - 5, mid_z)
    chandelier(KEEP_DOOR_X + 6, mid_z)

    # ── Alternating wall banners down both long walls ──
    i = 0
    for z in range(HZ0 + 4, HZ1 - 3, 4):
        c = "black" if i % 2 == 0 else "red"
        setb(KEEP_X0 + 2, HY + 5, z, wall_banner(c, "east"))
        setb(KEEP_X1 - 2, HY + 5, z, wall_banner(c, "west"))
        i += 1

    # ── Side service door into the kitchen annex (west wall, south end) ──
    setb(KEEP_X0 + 1, HY, HZ1 - 4, AIR)
    setb(KEEP_X0 + 1, HY + 1, HZ1 - 4, AIR)
    setb(KEEP_X0, HY, HZ1 - 4, AIR)
    setb(KEEP_X0, HY + 1, HZ1 - 4, AIR)

great_hall()

# ════════════════════════════════════════════════════════════════════════
# STAGE 6: CRYPT (y0..4 under the keep) — sacred-dread density
# ════════════════════════════════════════════════════════════════════════
def crypt():
    CX0, CX1 = KEEP_X0 + 4, KEEP_X1 - 4
    CZ0, CZ1 = KEEP_Z0 + 4, KEEP_Z1 - 4
    # hollow
    fill_box(CX0, 1, CZ0, CX1, 4, CZ1, AIR)
    # floor / ceiling structure
    for x in range(CX0, CX1 + 1):
        for z in range(CZ0, CZ1 + 1):
            setb(x, 0, z, floor_stone())
    # columns every 6 in both axes, with arched caps
    for x in range(CX0 + 2, CX1 - 1, 6):
        for z in range(CZ0 + 2, CZ1 - 1, 6):
            for y in range(1, 5):
                setb(x, y, z, PBS)
            setb(x - 1, 4, z, PBSB_ST_TOP["west"])
            setb(x + 1, 4, z, PBSB_ST_TOP["east"])
            setb(x, 4, z - 1, PBSB_ST_TOP["north"])
            setb(x, 4, z + 1, PBSB_ST_TOP["south"])
    # sarcophagi rows: polished blackstone base, slab lid, skull at head
    sid = 0
    for z in range(CZ0 + 3, CZ1 - 2, 6):
        for x in range(CX0 + 5, CX1 - 4, 6):
            setb(x, 1, z, PBS); setb(x + 1, 1, z, PBS); setb(x + 2, 1, z, PBS)
            setb(x, 2, z, PBSB_SLAB_B)
            setb(x + 1, 2, z, CHIS_PBS)
            setb(x + 2, 2, z, PBSB_SLAB_B)
            if sid % 3 == 0:
                setb(x - 1, 1, z, skull(4))
            sid += 1
    # soul candle clusters (sacred: ONE light zone per area, gaps of dark)
    for x, z in ((CX0 + 1, CZ0 + 1), (CX1 - 1, CZ1 - 1), ((CX0 + CX1) // 2, (CZ0 + CZ1) // 2)):
        setb(x, 1, z, candle("black", 4))
    # sculk patches + webs (the castle's sickness pools down here)
    for _ in range(40):
        rx, rz = random.randint(CX0, CX1), random.randint(CZ0, CZ1)
        if getb(rx, 1, rz) == AIR and random.random() < 0.5:
            setb(rx, 0, rz, SCULK)
            if random.random() < 0.3:
                setb(rx, 1, rz, SCULK_VEIN_UP)
    for _ in range(10):
        rx, rz = random.randint(CX0 + 1, CX1 - 1), random.randint(CZ0 + 1, CZ1 - 1)
        setb(rx, 4, rz, COBWEB)
    # ONE wither rose in a pot at the central sarcophagus (someone still visits)
    setb((CX0 + CX1) // 2 + 1, 3, (CZ0 + CZ1) // 2, WITHER_ROSE_POT)

    # Stair down from the great hall (NE corner of hall floor)
    sx = KEEP_X1 - 5
    sz = KEEP_Z0 + 4
    for i, y in enumerate(range(GROUND, 0, -1)):
        zz = sz + i
        setb(sx, y, zz, DS_TILE_ST["south"])
        setb(sx, y + 1, zz, AIR)
        setb(sx, y + 2, zz, AIR)
        setb(sx, y + 3, zz, AIR)
        setb(sx - 1, y + 2, zz, soul_wall_torch("east") if i % 2 == 1 else AIR)

crypt()

# ════════════════════════════════════════════════════════════════════════
# STAGE 7: Keep spiral stair (SE corner turret of keep, all floors)
# ════════════════════════════════════════════════════════════════════════
STAIR_X, STAIR_Z = KEEP_X1 - 4, KEEP_Z1 - 4   # stair shaft center
def keep_stair():
    # 5×5 shaft carved through all floor slabs, walled in dressed stone
    for y in range(GROUND, KEEP_ROOF + 1):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                edge = abs(dx) == 2 or abs(dz) == 2
                x, z = STAIR_X + dx, STAIR_Z + dz
                if edge:
                    if getb(x, y, z) == AIR:
                        setb(x, y, z, stone())
                else:
                    setb(x, y, z, AIR)
    # central newel column
    for y in range(GROUND, KEEP_ROOF + 1):
        setb(STAIR_X, y, STAIR_Z, DOAK_LOG_Y)
    # winder treads circling the newel
    ring = [(1, 0, "south"), (1, 1, "south"), (0, 1, "west"), (-1, 1, "west"),
            (-1, 0, "north"), (-1, -1, "north"), (0, -1, "east"), (1, -1, "east")]
    y = GROUND
    i = 0
    while y < KEEP_ROOF:
        dx, dz, f = ring[i % 8]
        setb(STAIR_X + dx, y, STAIR_Z + dz, DS_TILE_ST[f])
        if i % 8 == 7:
            pass
        i += 1
        if i % 2 == 0:
            y += 1
    # doorways from the shaft onto each floor (west side openings)
    for fy in (GROUND + 1, F2_FLOOR + 1, F3_FLOOR + 1, F4_FLOOR + 1):
        setb(STAIR_X - 2, fy, STAIR_Z, AIR)
        setb(STAIR_X - 2, fy + 1, STAIR_Z, AIR)
    # roof hatch
    setb(STAIR_X + 1, KEEP_ROOF, STAIR_Z, DOAK_TRAP_BOT)
    # sconces in the shaft
    for fy in (GROUND + 3, F2_FLOOR + 3, F3_FLOOR + 3, F4_FLOOR + 3):
        setb(STAIR_X + 1, fy, STAIR_Z + 1, soul_wall_torch("north"))

keep_stair()

# ════════════════════════════════════════════════════════════════════════
# STAGE 8: FLOOR 2 — Library (west half) + Armory (east half)
# ════════════════════════════════════════════════════════════════════════
def floor2():
    fy = F2_FLOOR + 1          # standing level
    mid_x = KEEP_DOOR_X        # partition line
    # partition wall with a door
    for z in range(KEEP_Z0 + 2, KEEP_Z1 - 1):
        for y in range(fy, fy + 6):
            setb(mid_x, y, z, DOAK_PLANK if random.random() < 0.7 else DOAK_LOG_Y)
    pz = (KEEP_Z0 + KEEP_Z1) // 2
    setb(mid_x, fy, pz, door("east", "lower"))
    setb(mid_x, fy + 1, pz, door("east", "upper"))

    # ── LIBRARY (west) — living density, candle light ──
    LX0, LX1 = KEEP_X0 + 2, mid_x - 1
    LZ0, LZ1 = KEEP_Z0 + 2, KEEP_Z1 - 2
    # bookshelf stacks along the west wall, two tall with chiseled caps
    for z in range(LZ0 + 1, LZ1, 1):
        if (z - LZ0) % 5 == 4:
            continue  # aisle gaps
        setb(LX0, fy, z, BOOKSHELF)
        setb(LX0, fy + 1, z, BOOKSHELF)
        setb(LX0, fy + 2, z, DOAK_SLAB_B)
    # freestanding double-sided stacks
    for sx in (LX0 + 4, LX0 + 8):
        for z in range(LZ0 + 3, LZ1 - 3, 1):
            if (z - LZ0) % 6 in (4, 5):
                continue
            setb(sx, fy, z, BOOKSHELF)
            setb(sx, fy + 1, z, BOOKSHELF)
            setb(sx, fy + 2, z, DOAK_SLAB_B)
    # reading nook: table + lecterns + candles by a window slit
    nx = LX0 + 6
    nz = LZ1 - 3
    setb(nx, fy, nz, DOAK_FENCE)
    setb(nx, fy + 1, nz, DOAK_SLAB_T)
    setb(nx, fy + 2, nz, candle("black", 3))
    setb(nx - 1, fy, nz, LECTERN_N)
    setb(nx + 1, fy, nz, DOAK_ST["west"])
    setb(LX0 + 2, fy, LZ0 + 2, LECTERN_S)
    # cartography corner
    setb(LX0 + 1, fy, LZ1 - 1, CART_TBL)
    setb(LX0 + 2, fy, LZ1 - 1, CHEST_N)
    # gray runner carpet down the main aisle
    for z in range(LZ0 + 2, LZ1 - 1):
        setb(LX0 + 2, fy, z, GRAY_CARPET) if getb(LX0 + 2, fy, z) == AIR else None
    # hanging soul lanterns in the aisles (warm domestic would be wrong here)
    for z in range(LZ0 + 4, LZ1 - 2, 7):
        setb(LX0 + 2, fy + 5, z, CHAIN_Y)
        setb(LX0 + 2, fy + 4, z, SOUL_LANT_H)
    # a few cobwebs on the top shelves
    setb(LX0 + 4, fy + 2, LZ0 + 3, COBWEB)
    setb(LX0 + 8, fy + 2, LZ1 - 5, COBWEB)

    # ── ARMORY (east) — utility-living density, racks + anvils ──
    AX0, AX1 = mid_x + 2, KEEP_X1 - 2
    AZ0, AZ1 = KEEP_Z0 + 2, KEEP_Z1 - 2
    # weapon racks: tripwire hooks rows on the east wall (Ironhold rack rings)
    for z in range(AZ0 + 2, AZ1 - 6, 2):
        setb(AX1, fy + 1, z, tripwire_hook("west"))
        setb(AX1, fy + 2, z, tripwire_hook("west"))
    # armor alcoves: iron blocks with chiseled "helms" in niches on north wall
    for x in range(AX0 + 2, AX1 - 2, 3):
        setb(x, fy, AZ0, IRON_BLOCK)
        setb(x, fy + 1, AZ0, CHIS_PBS)
        setb(x, fy + 2, AZ0 + 1, soul_wall_torch("south"))
    # smithing corner
    setb(AX0 + 1, fy, AZ1 - 2, ANVIL_X)
    setb(AX0 + 2, fy, AZ1 - 2, SMITH_TBL)
    setb(AX0 + 3, fy, AZ1 - 2, GRINDSTONE)
    setb(AX0 + 1, fy, AZ1 - 3, BLAST_S)
    setb(AX0 + 1, fy, AZ1 - 1, CHEST_N)
    setb(AX0 + 2, fy, AZ1 - 1, CHEST_N)
    # fletching bench + arrow barrels
    setb(AX1 - 2, fy, AZ1 - 2, FLETCH_TBL)
    setb(AX1 - 1, fy, AZ1 - 2, BARREL_UP)
    setb(AX1 - 1, fy + 1, AZ1 - 2, BARREL_UP)
    setb(AX1 - 1, fy, AZ1 - 3, BARREL_UP)
    # central sparring strip: gray carpet rectangle
    for z in range(AZ0 + 5, AZ1 - 5):
        for x in range(AX0 + 4, AX0 + 7):
            setb(x, fy, z, GRAY_CARPET)
    # chain-hung soul lanterns
    for z in (AZ0 + 5, AZ1 - 5):
        setb((AX0 + AX1) // 2, fy + 5, z, CHAIN_Y)
        setb((AX0 + AX1) // 2, fy + 4, z, SOUL_LANT_H)

floor2()

# ════════════════════════════════════════════════════════════════════════
# STAGE 9: FLOOR 3 — Lord's solar (canopy bed, study, wardrobe)
# ════════════════════════════════════════════════════════════════════════
def solar():
    fy = F3_FLOOR + 1
    SX0, SX1_ = KEEP_X0 + 2, KEEP_X1 - 2
    SZ0_, SZ1_ = KEEP_Z0 + 2, KEEP_Z1 - 2
    # the lord keeps only the north half; south half is the antechamber
    aw_z = (SZ0_ + SZ1_) // 2
    for x in range(SX0, SX1_ + 1):
        for y in range(fy, fy + 6):
            setb(x, y, aw_z, DOAK_PLANK if random.random() < 0.7 else DOAK_LOG_Y)
    setb(KEEP_DOOR_X, fy, aw_z, door("north", "lower"))
    setb(KEEP_DOOR_X, fy + 1, aw_z, door("north", "upper"))

    # ── Canopy bed (furniture/canopy_bed_lord.md): posts, slab canopy ──
    bx, bz = SX0 + 4, SZ0_ + 3
    setb(bx, fy, bz, bed("black", "west", "head"))
    setb(bx + 1, fy, bz, bed("black", "west", "foot"))
    for px, pz in ((bx - 1, bz - 1), (bx + 2, bz - 1), (bx - 1, bz + 1), (bx + 2, bz + 1)):
        for y in range(fy, fy + 3):
            setb(px, y, pz, DOAK_FENCE)
    for cx in range(bx - 1, bx + 3):
        for cz in range(bz - 1, bz + 2):
            setb(cx, fy + 3, cz, DOAK_SLAB_B)
    setb(bx, fy + 2, bz, candle("black", 1))  # bedside reading light... of a sort
    # black carpet around the bed
    for cx in range(bx - 1, bx + 3):
        for cz in range(bz - 1, bz + 3):
            if getb(cx, fy, cz) == AIR:
                setb(cx, fy, cz, BLACK_CARPET)
    # bedside chest + brewing stand (the lord's tonics)
    setb(bx + 3, fy, bz, CHEST_S)
    setb(bx - 1, fy, bz, BREW_STAND) if getb(bx - 1, fy, bz) == AIR else None

    # study desk under the north window
    dx = SX1_ - 5
    for ddx in range(3):
        setb(dx + ddx, fy, SZ0_ + 1, DOAK_ST["south"] if ddx == 1 else DOAK_PLANK)
        setb(dx + ddx, fy + 1, SZ0_ + 1, DOAK_SLAB_B)
    setb(dx, fy + 2, SZ0_ + 1, candle("red", 2))
    setb(dx + 2, fy + 2, SZ0_ + 1, LECTERN_S)
    setb(dx + 1, fy, SZ0_ + 2, DOAK_ST["north"])
    # wardrobe wall: barrels + chests bank
    for z in range(SZ0_ + 5, aw_z - 2, 2):
        setb(SX1_, fy, z, BARREL_S)
        setb(SX1_, fy + 1, z, CHEST_S) if z % 4 == 1 else None
    # hearth nook in the west wall (soul fire — the lord likes it cold)
    hz = SZ0_ + 6
    setb(KEEP_X0 + 1, fy, hz, AIR)
    setb(KEEP_X0 + 1, fy + 1, hz, AIR)
    setb(KEEP_X0 + 1, fy - 1, hz, SOUL_SAND)
    setb(KEEP_X0 + 1, fy, hz, SOUL_FIRE)
    setb(KEEP_X0 + 2, fy + 2, hz, CHIS_PBS)
    # antechamber (south half): guard bench, banner, sparse
    setb(KEEP_DOOR_X - 3, fy, SZ1_ - 2, DOAK_ST["east"])
    setb(KEEP_DOOR_X - 2, fy, SZ1_ - 2, DOAK_SLAB_B)
    setb(KEEP_DOOR_X - 1, fy, SZ1_ - 2, DOAK_ST["west"])
    setb(KEEP_DOOR_X, fy + 4, SZ1_ - 1, wall_banner("red", "north"))
    setb(SX0 + 1, fy + 4, SZ1_ - 4, SOUL_LANT)
    # potted dead bush by the stair door — hospitality, dark-castle style
    setb(STAIR_X - 3, fy, STAIR_Z, DEAD_BUSH_POT)

solar()

# ════════════════════════════════════════════════════════════════════════
# STAGE 10: FLOOR 4 — Chapel of the Hollow Crown
# Sacred density: ONE hanging lantern + altar candles; pews; tinted rose light
# ════════════════════════════════════════════════════════════════════════
def chapel():
    fy = F4_FLOOR + 1
    CX0_, CX1_ = KEEP_X0 + 2, KEEP_X1 - 2
    CZ0_, CZ1_ = KEEP_Z0 + 2, KEEP_Z1 - 2
    # ── Altar (furniture/altar.md) on the north end ──
    ax = KEEP_DOOR_X
    az = CZ0_ + 2
    # raised chancel platform
    for x in range(ax - 4, ax + 6):
        for z in range(CZ0_, CZ0_ + 5):
            setb(x, fy - 1, z, PBS)
    for x in range(ax - 4, ax + 6):
        setb(x, fy, CZ0_ + 4, PBSB_SLAB_B)  # chancel step lip
    # altar table: chiseled core, enchanting table as the reliquary
    setb(ax, fy, az, CHIS_PBS)
    setb(ax + 1, fy, az, ENCH_TABLE)
    setb(ax + 2, fy, az, CHIS_PBS)
    setb(ax, fy + 1, az, candle("red", 3))
    setb(ax + 2, fy + 1, az, candle("red", 3))
    # gold offering bowl (the one gilded accent outside the throne room is
    # NOT allowed — keep it blackstone; gold stays downstairs)
    setb(ax + 1, fy - 1 + 1, az - 1, GOLD_BLOCK) if False else None
    # rose window: tinted glass cross in the north face
    wx = ax + 1
    for (dx, dy) in ((0, 3), (0, 4), (0, 5), (-1, 4), (1, 4)):
        setb(wx + dx, fy + dy, KEEP_Z0, TINTED_GLASS)
        setb(wx + dx, fy + dy, KEEP_Z0 + 1, AIR)
    # ── Pews: stair rows facing north, center aisle ──
    for z in range(CZ0_ + 8, CZ1_ - 4, 3):
        for x in list(range(ax - 6, ax - 1)) + list(range(ax + 3, ax + 8)):
            if CX0_ < x < CX1_:
                setb(x, fy, z, DOAK_ST["south"])
    # center aisle runner
    for z in range(CZ0_ + 5, CZ1_ - 2):
        setb(ax, fy, z, BLACK_CARPET)
        setb(ax + 1, fy, z, BLACK_CARPET)
    # the ONE hanging lantern over the chancel (sacred rule)
    setb(ax + 1, fy + 7, az + 2, CHAIN_Y)
    setb(ax + 1, fy + 6, az + 2, SOUL_LANT_H)
    # side wall sconce pair at the chapel door only
    setb(ax - 1, fy + 2, CZ1_ - 1, soul_wall_torch("north"))
    setb(ax + 3, fy + 2, CZ1_ - 1, soul_wall_torch("north"))
    # wall skulls watching from the chancel sides — two, no more
    setb(CX0_ + 1, fy + 2, CZ0_ + 3, wall_skull("east"))
    setb(CX1_ - 1, fy + 2, CZ0_ + 3, wall_skull("west"))

chapel()

# ════════════════════════════════════════════════════════════════════════
# STAGE 11: Keep roof spire + flag
# ════════════════════════════════════════════════════════════════════════
def spire():
    sx, sz = KEEP_DOOR_X, (KEEP_Z0 + KEEP_Z1) // 2
    # square lantern-tower 5×5 rising from the roof
    for y in range(KEEP_ROOF + 1, KEEP_ROOF + 8):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 or abs(dz) == 2:
                    if not (abs(dx) == 2 and abs(dz) == 2 and y > KEEP_ROOF + 5):
                        setb(sx + dx, y, sz + dz, stone())
    # open lancets near the top
    for y in (KEEP_ROOF + 5, KEEP_ROOF + 6):
        setb(sx, y, sz - 2, AIR)
        setb(sx, y, sz + 2, AIR)
        setb(sx - 2, y, sz, AIR)
        setb(sx + 2, y, sz, AIR)
    # pyramidal cap
    for i, y in enumerate(range(KEEP_ROOF + 8, KEEP_ROOF + 11)):
        r = 2 - i
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) == r or abs(dz) == r:
                    setb(sx + dx, y, sz + dz, DS_TILE)
    # flagpole + the lord's banner
    for y in range(KEEP_ROOF + 11, KEEP_ROOF + 15):
        setb(sx, y, sz, DOAK_FENCE)
    setb(sx, KEEP_ROOF + 15, sz, floor_banner("black", 8))
    setb(sx + 1, KEEP_ROOF + 13, sz, wall_banner("red", "east"))
    # the spire's soul-light beacon glow inside
    setb(sx, KEEP_ROOF + 6, sz, SOUL_LANT)

spire()

# ════════════════════════════════════════════════════════════════════════
# STAGE 12: Courtyard life — stable, smithy, well, dead garden
# ════════════════════════════════════════════════════════════════════════
def courtyard():
    # ── Well (center-south, on the approach road's side) ──
    wx, wz = 30, SZ - 20
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            if abs(dx) == 1 or abs(dz) == 1:
                setb(wx + dx, GROUND, wz + dz, PBS)
            else:
                setb(wx, GROUND, wz, WATER)
                setb(wx, GROUND - 1, wz, WATER)
    for px, pz in ((wx - 1, wz - 1), (wx + 1, wz + 1)):
        setb(px, GROUND + 1, pz, DOAK_FENCE)
        setb(px, GROUND + 2, pz, DOAK_FENCE)
    setb(wx - 1, GROUND + 3, wz - 1, DOAK_SLAB_B)
    setb(wx, GROUND + 3, wz, DOAK_SLAB_B)
    setb(wx + 1, GROUND + 3, wz + 1, DOAK_SLAB_B)
    setb(wx, GROUND + 2, wz, CHAIN_Y)

    # ── Stable lean-to against the east curtain (z mid) ──
    sx0, sz0 = SX - 9, 28
    for z in range(sz0, sz0 + 14):
        for y in range(GROUND, GROUND + 4):
            setb(sx0, y, z, DOAK_LOG_Y if z % 4 == 0 else AIR)
    for z in range(sz0, sz0 + 14):
        for x in range(sx0, SX - 2):
            setb(x, GROUND + 4 - (x - sx0) // 3, z, DOAK_SLAB_T)
    for z in range(sz0 + 1, sz0 + 13, 4):  # stall dividers + hay
        for x in range(sx0 + 1, SX - 2):
            setb(x, GROUND, z, DOAK_FENCE)
        setb(sx0 + 2, GROUND, z + 1, HAY_Y)
        setb(sx0 + 2, GROUND, z + 2, HAY_Y) if random.random() < 0.5 else None
    setb(sx0 + 1, GROUND, sz0, BARREL_UP)
    setb(sx0 + 1, GROUND, sz0 + 13, COMPOSTER)

    # ── Smithy lean-to against the west curtain ──
    fx1, fz0 = 8, 30
    for z in range(fz0, fz0 + 10):
        for x in range(2, fx1):
            setb(x, GROUND + 4 - (fx1 - x) // 3, z, DOAK_SLAB_T)
    for z in (fz0, fz0 + 9):
        for y in range(GROUND, GROUND + 4):
            setb(fx1 - 1, y, z, DOAK_LOG_Y)
    setb(3, GROUND, fz0 + 2, BLAST_S)
    setb(4, GROUND, fz0 + 2, ANVIL_CHIP)
    setb(3, GROUND, fz0 + 4, CAULDRON)       # quench tub
    setb(3, GROUND, fz0 + 6, SMITH_TBL)
    setb(4, GROUND, fz0 + 7, GRINDSTONE)
    setb(3, GROUND, fz0 + 8, CHEST_E)
    setb(5, GROUND + 3, fz0 + 5, SOUL_LANT_H)
    setb(5, GROUND + 4, fz0 + 5, CHAIN_Y)

    # ── Dead garden NE: coarse-dirt beds, wither roses, one dead tree ──
    gx0, gz0 = 60, 16
    for x in range(gx0, gx0 + 10):
        for z in range(gz0, gz0 + 12):
            if (x + z) % 7 == 0:
                setb(x, GROUND - 1, z, COARSE_DIRT)
                if random.random() < 0.4:
                    setb(x, GROUND, z, WITHER_ROSE)
    # dead tree: bare dark-oak trunk + a few stubs
    tx, tz = gx0 + 5, gz0 + 6
    for y in range(GROUND, GROUND + 6):
        setb(tx, y, tz, DOAK_LOG_Y)
    setb(tx + 1, GROUND + 4, tz, DOAK_LOG_X)
    setb(tx - 1, GROUND + 5, tz, DOAK_LOG_X)
    setb(tx, GROUND + 5, tz + 1, DOAK_LOG_Z)
    setb(tx, GROUND + 3, tz - 1, COBWEB)
    # low garden wall
    for x in range(gx0 - 1, gx0 + 11):
        setb(x, GROUND, gz0 - 1, PBSB_SLAB_B)
        setb(x, GROUND, gz0 + 12, PBSB_SLAB_B)

    # ── Walkway access stairs (courtyard → curtain walkway), NE + SW of gate ──
    def wall_stairs(x0, z, dirx):
        for i in range(14):
            y = GROUND + i
            x = x0 + i * dirx
            if y >= WALK_Y:
                break
            setb(x, y, z, DS_TILE_ST["east" if dirx > 0 else "west"])
            for cy in range(y + 1, y + 4):
                setb(x, cy, z, AIR)
            setb(x, y - 1, z, stone()) if i > 0 else None
            # solid under each tread
            for uy in range(GROUND, y):
                setb(x, uy, z, stone())

    wall_stairs(14, 3, 1)              # along north wall, climbs east
    wall_stairs(SX - 15, SZ - 6, -1)   # near gate, climbs west

courtyard()

# ════════════════════════════════════════════════════════════════════════
# Encode and write (ISC v1)
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
