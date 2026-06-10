#!/usr/bin/env python3
"""Generate Shroomling entity + item textures.

Entity UV layout (64x64) matches ShroomlingModel box-UV offsets:
  cap  w12 h5 d12  texOffs(0,0)
  stem w6  h4 d6   texOffs(0,18)
  leg  w2  h2 d2   texOffs(28,18)   (shared by both legs)

Item art (16x16): shroomcap, shroomling_spawn_egg.
"""
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENTITY_DIR = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity/shroomling")
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/item")

# ── palette (soft periwinkle cap, icy-blue stem) ──────────────────────────
CAP_TOP    = (96, 132, 196, 255)
CAP_SIDE   = (74, 108, 170, 255)
CAP_FRONT  = (84, 120, 184, 255)
CAP_BOTTOM = (52, 80, 130, 255)
SPOT       = (228, 238, 250, 255)
SPOT_HI    = (245, 250, 255, 255)
STEM       = (168, 198, 218, 255)
STEM_TOP   = (190, 216, 232, 255)
STEM_BOT   = (132, 162, 186, 255)
EYE        = (38, 52, 74, 255)
LEG        = (150, 180, 200, 255)
LEG_BOT    = (120, 150, 172, 255)
GLOW       = (150, 235, 230, 255)
GLOW_HI    = (210, 250, 246, 255)


def fill(px, x0, y0, w, h, color):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            px[x, y] = color


def shade(px, x0, y0, w, h, base, rng, amount=10):
    for x in range(x0, x0 + w):
        for y in range(y0, y0 + h):
            d = rng.randint(-amount, amount)
            px[x, y] = (
                max(0, min(255, base[0] + d)),
                max(0, min(255, base[1] + d)),
                max(0, min(255, base[2] + d)),
                255,
            )


def box_faces(u, v, w, h, d):
    """Return dict of face-name -> (x0, y0, fw, fh) for MC box-UV unwrap."""
    return {
        "top":    (u + d,         v,       w, d),
        "bottom": (u + d + w,     v,       w, d),
        "right":  (u,             v + d,   d, h),
        "front":  (u + d,         v + d,   w, h),
        "left":   (u + d + w,     v + d,   d, h),
        "back":   (u + 2 * d + w, v + d,   w, h),
    }


def gen_entity():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(0x5417)

    # cap: texOffs(0,0) 12x5x12
    cap = box_faces(0, 0, 12, 5, 12)
    shade(px, *cap["top"], CAP_TOP, rng, 8)
    shade(px, *cap["bottom"], CAP_BOTTOM, rng, 6)
    for name in ("right", "left", "back"):
        shade(px, *cap[name], CAP_SIDE, rng, 8)
    shade(px, *cap["front"], CAP_FRONT, rng, 8)

    # white cap spots: deliberate, roughly symmetric polkadots.
    # Each dot is a uniform 2x2 block with one highlight pixel so it reads
    # as a rounded bump instead of scattered noise.
    def dot(face, rx, ry, size=2):
        fx0, fy0, fw, fh = face
        for dx in range(size):
            for dy in range(size):
                xx, yy = fx0 + rx + dx, fy0 + ry + dy
                if fx0 <= xx < fx0 + fw and fy0 <= yy < fy0 + fh:
                    px[xx, yy] = SPOT
        hx, hy = fx0 + rx, fy0 + ry  # top-left highlight for a domed look
        if fx0 <= hx < fx0 + fw and fy0 <= hy < fy0 + fh:
            px[hx, hy] = SPOT_HI

    # top (12x12): quincunx of evenly spaced dots
    for (rx, ry) in ((2, 2), (8, 2), (5, 5), (2, 8), (8, 8)):
        dot(cap["top"], rx, ry)
    # front / back vertical faces (12x5): three dots evenly spaced near the rim
    for face in (cap["front"], cap["back"]):
        for rx in (1, 5, 9):
            dot(face, rx, 1)
    # side faces, mirrored so left/right read as an intentional pair
    for rx in (1, 5, 9):
        dot(cap["right"], rx, 1)
    for rx in (9, 5, 1):
        dot(cap["left"], rx, 1)

    # stem: texOffs(0,18) 6x4x6
    stem = box_faces(0, 18, 6, 4, 6)
    shade(px, *stem["top"], STEM_TOP, rng, 6)
    shade(px, *stem["bottom"], STEM_BOT, rng, 6)
    for name in ("right", "left", "back", "front"):
        shade(px, *stem[name], STEM, rng, 6)
    # eyes on stem front face (6 wide x 4 tall)
    fx0, fy0, _, _ = stem["front"]
    fill(px, fx0 + 1, fy0 + 1, 1, 2, EYE)
    fill(px, fx0 + 4, fy0 + 1, 1, 2, EYE)

    # leg: texOffs(28,18) 2x2x2 (shared)
    leg = box_faces(28, 18, 2, 2, 2)
    shade(px, *leg["top"], LEG, rng, 5)
    shade(px, *leg["bottom"], LEG_BOT, rng, 5)
    for name in ("right", "left", "back", "front"):
        shade(px, *leg[name], LEG, rng, 5)

    os.makedirs(ENTITY_DIR, exist_ok=True)
    img.save(os.path.join(ENTITY_DIR, "shroomling.png"))

    # brighter "rare" variant cap (optional reuse): glowing teal cap
    bright = img.copy()
    bpx = bright.load()
    for name in ("top", "front", "right", "left", "back"):
        x0, y0, fw, fh = cap[name]
        for x in range(x0, x0 + fw):
            for y in range(y0, y0 + fh):
                r, g, b, a = bpx[x, y]
                if a:
                    bpx[x, y] = (min(255, r + 30), min(255, g + 55), min(255, b + 40), 255)
    bright.save(os.path.join(ENTITY_DIR, "shroomling_bright.png"))


def gen_shroomcap():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # a single bright cap chunk (rare)
    fill(px, 2, 6, 12, 5, GLOW)
    fill(px, 3, 4, 10, 2, GLOW_HI)
    fill(px, 2, 10, 12, 1, CAP_SIDE)
    fill(px, 2, 6, 1, 5, (110, 200, 196, 255))
    fill(px, 13, 6, 1, 5, (110, 200, 196, 255))
    # spots
    for (x, y) in [(5, 6), (9, 5), (11, 7), (4, 8), (8, 8)]:
        px[x, y] = SPOT
    img.save(os.path.join(ITEM_DIR, "shroomcap.png"))


def gen_spawn_egg():
    # Front-facing mob silhouette based on ShroomlingModel: a stepped,
    # three-tier cap, six-wide stem, and two stubby feet.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    palette = {
        "O": EYE,
        "H": CAP_TOP,
        "M": CAP_FRONT,
        "D": CAP_BOTTOM,
        "U": (44, 68, 112, 255),
        "S": SPOT,
        "P": SPOT_HI,
        "B": STEM,
        "L": STEM_TOP,
        "Q": STEM_BOT,
        "E": EYE,
        "F": LEG,
        "X": LEG_BOT,
    }
    rows = [
        "................",
        "......OOOO......",
        ".....OHHHDO.....",
        "....OHHHHDDO....",
        "...OHHPSHHDDO...",
        "..OHHHMHHMDDDO..",
        ".OHHMHHHMHHDDDO.",
        ".OMMSSMMMSSDDDO.",
        "..OOOUUUUUUOOO..",
        "....OBLLLLBO....",
        "....OBEBBEBO....",
        "....OBBBBBBO....",
        "....OQQQQQQO....",
        "....OFO..OXO....",
        "....OOO..OOO....",
        "................",
    ]
    for y, row in enumerate(rows):
        for x, symbol in enumerate(row):
            if symbol != ".":
                px[x, y] = palette[symbol]

    img.save(os.path.join(ITEM_DIR, "shroomling_spawn_egg.png"))


if __name__ == "__main__":
    gen_entity()
    gen_shroomcap()
    gen_spawn_egg()
    print("wrote shroomling textures ->", ENTITY_DIR, "and", ITEM_DIR)
