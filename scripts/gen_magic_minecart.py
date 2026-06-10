#!/usr/bin/env python3
"""Generate the Magic Minecart GeckoLib entity model + textures.

Single source of truth: the CUBES list defines geometry AND colouring, so the
per-face UVs in the .geo.json can never drift from the painted pixels.

Emits (under src/main/resources/assets/ironhold/):
  - geckolib/models/entity/magic_minecart.geo.json   per-face UVs, auto-packed
  - textures/entity/magic_minecart.png               base colour
  - textures/entity/magic_minecart_glow.png          emissive mask (AutoGlowingGeoLayer)

Design (pixels, y-up, wheels touch y=0): a dark-indigo trapezoidal tub with a
chunky silver rim + raised corner posts, glowing cyan diamond runes on all four
walls, a glowing cyan energy pool inside, and four wheels with emissive purple
hubs. Inspired by the user's reference art (purple cart, cyan runes, glow).
"""
import json
import math
import os
import random

from PIL import Image

random.seed(505)  # deterministic dither

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEO_PATH = os.path.join(
    ROOT, "src/main/resources/assets/ironhold/geckolib/models/entity/magic_minecart.geo.json")
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity")
TEX_BASE = os.path.join(TEX_DIR, "magic_minecart.png")
TEX_GLOW = os.path.join(TEX_DIR, "magic_minecart_glow.png")

T = (0, 0, 0, 0)  # transparent

# ── palette ───────────────────────────────────────────────────────────────
IND_TOP   = (58, 40, 96, 255)    # body indigo, lit top
IND_MID   = (46, 30, 80, 255)
IND_BOT   = (34, 22, 60, 255)    # body indigo, shadowed bottom
IND_EDGE  = (24, 16, 44, 255)    # panel outline
MAGENTA   = (150, 62, 200, 255)  # accent blotch
MAGENTA_HI= (196, 104, 236, 255)

FLOOR     = (28, 20, 52, 255)

SIL       = (190, 194, 208, 255) # silver rim
SIL_HI    = (226, 230, 240, 255)
SIL_LO    = (120, 124, 142, 255)
SIL_EDGE  = (86, 90, 108, 255)

RUBBER    = (30, 26, 48, 255)    # wheel tyre
RUBBER_LO = (18, 15, 32, 255)
SHADOW    = (12, 10, 20, 255)     # near-black: darkens wheel corners so the square reads round
BOLT      = (170, 176, 192, 255)

CYAN      = (74, 214, 246, 255)  # rune / energy
CYAN_HI   = (190, 244, 255, 255)
CYAN_LO   = (40, 132, 210, 255)
PURPLE    = (150, 70, 230, 255)  # hub
PURPLE_HI = (206, 140, 255, 255)

# Diamond rune template (6x6): outline + centre square.
RUNE = [
    "..##..",
    ".#..#.",
    "#.##.#",
    "#.##.#",
    ".#..#.",
    "..##..",
]


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


# ── geometry ────────────────────────────────────────────────────────────────
# Each cube: name, origin (min corner), size, style, optional rotation+pivot.
def box(name, x0, y0, z0, x1, y1, z1, style):
    return {"name": name, "origin": [x0, y0, z0],
            "size": [round(x1 - x0, 4), round(y1 - y0, 4), round(z1 - z0, 4)],
            "style": style}


# Minecart proportions: LOW + WIDE + a bit longer than tall, sitting on the rails.
# X = width (16), Z = length (18), total height only ~14px so it doesn't read as a
# lantern. Footprint ~1.25 blocks, height ~0.9 block.
CUBES = [
    box("floor", -8, 3, -9, 8, 5, 9, "floor"),
    # tub walls (hollow, only 6px tall)
    box("wall_n", -8, 5, -9, 8, 11, -7, "body"),
    box("wall_s", -8, 5, 7, 8, 11, 9, "body"),
    box("wall_w", -8, 5, -7, -6, 11, 7, "body"),
    box("wall_e", 6, 5, -7, 8, 11, 7, "body"),
    # interior energy pool — brims to the wall tops so it glows when seen from above
    box("pool", -6, 9, -7, 6, 11, 7, "pool"),
    # rim bars (overhang outward, sit on wall tops)
    box("rim_n", -7, 11, -10, 7, 13, -8, "rim"),
    box("rim_s", -7, 11, 8, 7, 13, 10, "rim"),
    box("rim_w", -9, 11, -8, -7, 13, 8, "rim"),
    box("rim_e", 7, 11, -8, 9, 13, 8, "rim"),
    # corner caps — small nubs just proud of the rim (not tall lantern posts)
    box("post_nw", -9, 11, -10, -7, 14, -8, "post"),
    box("post_ne", 7, 11, -10, 9, 14, -8, "post"),
    box("post_sw", -9, 11, 8, -7, 14, 10, "post"),
    box("post_se", 7, 11, 8, 9, 14, 10, "post"),
    # diamond rune emblems (protrude out, sink into wall to avoid z-fight)
    box("rune_n", -3, 5.5, -9.25, 3, 10.5, -8.75, "rune"),
    box("rune_s", -3, 5.5, 8.75, 3, 10.5, 9.25, "rune"),
    box("rune_w", -8.25, 5.5, -3, -7.75, 10.5, 3, "rune"),
    box("rune_e", 7.75, 5.5, -3, 8.25, 10.5, 3, "rune"),
    # wheels (front/back per side) — smaller round tyres + emissive hubs
    box("wheel_rf", 7.5, 0, -7.5, 9.5, 4, -3.5, "wheel"),
    box("wheel_rb", 7.5, 0, 3.5, 9.5, 4, 7.5, "wheel"),
    box("wheel_lf", -9.5, 0, -7.5, -7.5, 4, -3.5, "wheel"),
    box("wheel_lb", -9.5, 0, 3.5, -7.5, 4, 7.5, "wheel"),
    box("hub_rf", 9.25, 1, -6.5, 9.75, 3, -4.5, "hub"),
    box("hub_rb", 9.25, 1, 4.5, 9.75, 3, 6.5, "hub"),
    box("hub_lf", -9.75, 1, -6.5, -9.25, 3, -4.5, "hub"),
    box("hub_lb", -9.75, 1, 4.5, -9.25, 3, 6.5, "hub"),
]

# face -> (width_axis_size_index, height_axis_size_index) using size=[sx,sy,sz]
FACE_DIMS = {
    "north": (0, 1), "south": (0, 1),
    "east": (2, 1), "west": (2, 1),
    "up": (0, 2), "down": (0, 2),
}


def face_size(size, face):
    wi, hi = FACE_DIMS[face]
    return size[wi], size[hi]


# ── painters: fill an allocation box [x0,y0,aw,ah] on the base/glow images ────
def fill(px, x0, y0, w, h, c):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            px[x, y] = c


def vgrad(px, x0, y0, w, h, ctop, cbot):
    for y in range(h):
        t = y / max(1, h - 1)
        c = lerp(ctop, cbot, t)
        for x in range(w):
            px[x0 + x, y0 + y] = c


def outline(px, x0, y0, w, h, c):
    for x in range(w):
        px[x0 + x, y0] = c
        px[x0 + x, y0 + h - 1] = c
    for y in range(h):
        px[x0, y0 + y] = c
        px[x0 + w - 1, y0 + y] = c


def draw_rune(px, x0, y0, w, h, c):
    for y in range(h):
        for x in range(w):
            if RUNE[min(5, y * 6 // h)][min(5, x * 6 // w)] == "#":
                px[x0 + x, y0 + y] = c


def is_big(w, h):  # a "face-on" panel (not a thin rim/edge strip)
    return w >= 4 and h >= 4


# style -> base painter(px, x0, y0, aw, ah, face, fw, fh)
def paint_body(px, x0, y0, w, h, face, fw, fh):
    if face == "up":
        fill(px, x0, y0, w, h, IND_TOP)
    elif face == "down":
        fill(px, x0, y0, w, h, IND_BOT)
    else:
        vgrad(px, x0, y0, w, h, IND_TOP, IND_BOT)
        outline(px, x0, y0, w, h, IND_EDGE)
        # magenta accent blotch low on the side panels
        if is_big(w, h):
            bx, by = x0 + 1, y0 + h - 4
            for (dx, dy, col) in [(0, 1, MAGENTA), (1, 1, MAGENTA_HI), (1, 2, MAGENTA),
                                  (w - 3, 2, MAGENTA), (w - 2, 1, MAGENTA_HI), (w - 2, 2, MAGENTA)]:
                xx, yy = x0 + dx, y0 + h - 4 + dy
                if x0 < xx < x0 + w - 1 and y0 < yy < y0 + h - 1:
                    px[xx, yy] = col


def paint_floor(px, x0, y0, w, h, face, fw, fh):
    fill(px, x0, y0, w, h, FLOOR)


def paint_rim(px, x0, y0, w, h, face, fw, fh):
    if face == "up":
        vgrad(px, x0, y0, w, h, SIL_HI, SIL)
    elif face == "down":
        fill(px, x0, y0, w, h, SIL_EDGE)
    else:
        vgrad(px, x0, y0, w, h, SIL_HI, SIL_LO)
        for x in range(w):  # top highlight line
            px[x0 + x, y0] = SIL_HI
        outline(px, x0, y0, w, h, SIL_EDGE) if False else None


def paint_post(px, x0, y0, w, h, face, fw, fh):
    if face == "up":
        fill(px, x0, y0, w, h, SIL_HI)
    elif face == "down":
        fill(px, x0, y0, w, h, SIL_EDGE)
    else:
        vgrad(px, x0, y0, w, h, SIL_HI, SIL_LO)


def paint_rune(px, x0, y0, w, h, face, fw, fh):
    vgrad(px, x0, y0, w, h, IND_MID, IND_BOT)
    if is_big(w, h):
        draw_rune(px, x0, y0, w, h, CYAN)
    else:
        fill(px, x0, y0, w, h, IND_EDGE)


def paint_pool(px, x0, y0, w, h, face, fw, fh):
    if face == "up":
        cx, cy = (w - 1) / 2, (h - 1) / 2
        maxd = math.hypot(cx, cy)
        for y in range(h):
            for x in range(w):
                t = math.hypot(x - cx, y - cy) / max(0.01, maxd)
                c = lerp(CYAN_HI, CYAN_LO, min(1.0, t))
                if random.random() < 0.12:
                    c = CYAN_HI
                px[x0 + x, y0 + y] = c
    elif face == "down":
        fill(px, x0, y0, w, h, FLOOR)
    else:
        fill(px, x0, y0, w, h, CYAN_LO)


def paint_wheel(px, x0, y0, w, h, face, fw, fh):
    if is_big(w, h):  # round tyre face — darkened corners make the square read round
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
        r = min(cx, cy) + 0.5
        for y in range(h):
            for x in range(w):
                d = math.hypot(x - cx, y - cy)
                if d > r:
                    c = SHADOW          # corner → blends to shadow, rounds the silhouette
                elif d > r - 1.0:
                    c = SIL             # bright silver tyre rim
                elif d > r - 1.7:
                    c = SIL_LO          # rim shading
                else:
                    c = RUBBER          # hub seat
                px[x0 + x, y0 + y] = c
        # bolt specks just inside the rim (N/E/S/W)
        for ang in (0, 90, 180, 270):
            bx = int(round(cx + (r - 1.4) * math.cos(math.radians(ang))))
            by = int(round(cy + (r - 1.4) * math.sin(math.radians(ang))))
            if 0 <= bx < w and 0 <= by < h:
                px[x0 + bx, y0 + by] = BOLT
    else:
        fill(px, x0, y0, w, h, SHADOW)  # thin tread band, dark


def paint_hub(px, x0, y0, w, h, face, fw, fh):
    # Hub faces are tiny (2x2) — paint purple on every face so the glowing axle
    # cap always reads, regardless of which face is visible.
    fill(px, x0, y0, w, h, PURPLE)
    if w >= 2 and h >= 2:
        px[x0 + w // 2, y0 + h // 2] = PURPLE_HI


BASE_PAINTERS = {
    "body": paint_body, "floor": paint_floor, "rim": paint_rim, "post": paint_post,
    "rune": paint_rune, "pool": paint_pool, "wheel": paint_wheel, "hub": paint_hub,
}


# glow painter -> only emissive pixels, rest transparent (returns nothing painted)
def glow_body(px, x0, y0, w, h, face, fw, fh):
    if face in ("up", "down") or not is_big(w, h):
        return
    for (dx, dy, col) in [(1, h - 3, MAGENTA_HI), (2, h - 2, MAGENTA),
                          (w - 2, h - 3, MAGENTA), (w - 3, h - 2, MAGENTA_HI)]:
        if 0 <= dx < w and 0 <= dy < h:
            px[x0 + dx, y0 + dy] = col


def glow_rune(px, x0, y0, w, h, face, fw, fh):
    if is_big(w, h):
        draw_rune(px, x0, y0, w, h, CYAN_HI)
        # soft inner glow on the centre square
        for y in range(h):
            for x in range(w):
                if RUNE[min(5, y * 6 // h)][min(5, x * 6 // w)] == "#":
                    px[x0 + x, y0 + y] = CYAN_HI


def glow_pool(px, x0, y0, w, h, face, fw, fh):
    if face != "up":
        return
    cx, cy = (w - 1) / 2, (h - 1) / 2
    maxd = math.hypot(cx, cy)
    for y in range(h):
        for x in range(w):
            t = math.hypot(x - cx, y - cy) / max(0.01, maxd)
            px[x0 + x, y0 + y] = lerp(CYAN_HI, CYAN, min(1.0, t))


def glow_hub(px, x0, y0, w, h, face, fw, fh):
    fill(px, x0, y0, w, h, PURPLE_HI)


GLOW_PAINTERS = {
    "body": glow_body, "rune": glow_rune, "pool": glow_pool, "hub": glow_hub,
}


# ── pack faces into an atlas ─────────────────────────────────────────────────
def next_pow2(n):
    p = 1
    while p < n:
        p *= 2
    return p


def pack(width):
    """Shelf-pack every cube face. Returns (placements, used_height).
    placements[(ci, face)] = (ax, ay, aw, ah, fw, fh)."""
    faces = []
    for ci, cube in enumerate(CUBES):
        for face in ("north", "south", "east", "west", "up", "down"):
            fw, fh = face_size(cube["size"], face)
            if fw <= 0 or fh <= 0:
                continue
            faces.append((ci, face, fw, fh, max(1, math.ceil(fw)), max(1, math.ceil(fh))))
    faces.sort(key=lambda f: -f[5])  # tallest first
    placements = {}
    x = y = shelf_h = 0
    for ci, face, fw, fh, aw, ah in faces:
        if x + aw > width:
            x = 0
            y += shelf_h + 1
            shelf_h = 0
        placements[(ci, face)] = (x, y, aw, ah, fw, fh)
        x += aw + 1
        shelf_h = max(shelf_h, ah)
    return placements, y + shelf_h + 1


def build():
    width = 128
    placements, used_h = pack(width)
    if used_h > 256:
        width = 256
        placements, used_h = pack(width)
    height = next_pow2(used_h)
    tw, th = width, height

    base = Image.new("RGBA", (tw, th), T)
    glow = Image.new("RGBA", (tw, th), T)
    bpx, gpx = base.load(), glow.load()

    bones_cubes = []
    for ci, cube in enumerate(CUBES):
        bp = BASE_PAINTERS[cube["style"]]
        gp = GLOW_PAINTERS.get(cube["style"])
        uv = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            if (ci, face) not in placements:
                continue
            ax, ay, aw, ah, fw, fh = placements[(ci, face)]
            bp(bpx, ax, ay, aw, ah, face, fw, fh)
            if gp:
                gp(gpx, ax, ay, aw, ah, face, fw, fh)
            uv[face] = {"uv": [ax, ay], "uv_size": [round(fw, 4), round(fh, 4)]}
        bones_cubes.append({"origin": cube["origin"], "size": cube["size"], "uv": uv})

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.magic_minecart",
                "texture_width": tw,
                "texture_height": th,
                "visible_bounds_width": 3,
                "visible_bounds_height": 2,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": [{"name": "magic_minecart", "pivot": [0, 0, 0], "cubes": bones_cubes}],
        }],
    }

    os.makedirs(os.path.dirname(GEO_PATH), exist_ok=True)
    os.makedirs(TEX_DIR, exist_ok=True)
    with open(GEO_PATH, "w") as f:
        json.dump(geo, f, indent=2)
    base.save(TEX_BASE)
    glow.save(TEX_GLOW)
    print(f"atlas {tw}x{th}, {len(CUBES)} cubes")
    print("wrote", os.path.relpath(GEO_PATH, ROOT))
    print("wrote", os.path.relpath(TEX_BASE, ROOT))
    print("wrote", os.path.relpath(TEX_GLOW, ROOT))


if __name__ == "__main__":
    build()
