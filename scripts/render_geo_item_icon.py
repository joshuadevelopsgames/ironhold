#!/usr/bin/env python3
"""Render a GUI/inventory icon for a geckolib item from its geo + texture.

Per-face-UV aware (unlike tools/render_geo_textured.py which is box-UV-only).
Orthographic 3/4 view, transparent background, autocropped, NEAREST-scaled.

  python3 scripts/render_geo_item_icon.py --name spiked_club
"""
import argparse
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path("/Users/joshua/ironhold/src/main/resources/assets/ironhold")

UV_SPACE = 64
ANGLE = math.radians(28)
ELEV = math.radians(12)
OUT_SIZE = 16
FACE_SHADE = {"up": 1.0, "down": 0.78, "north": 0.88, "south": 0.97, "east": 0.90, "west": 0.83}

FACES = {
    "north": ("000", "100", "110", "010"),
    "south": ("101", "001", "011", "111"),
    "west":  ("001", "000", "010", "011"),
    "east":  ("100", "101", "111", "110"),
    "up":    ("010", "110", "111", "011"),
    "down":  ("001", "101", "100", "000"),
}


def corners(o, s):
    ox, oy, oz = o
    sx, sy, sz = s
    return {"000": (ox, oy, oz), "100": (ox + sx, oy, oz), "110": (ox + sx, oy + sy, oz), "010": (ox, oy + sy, oz),
            "001": (ox, oy, oz + sz), "101": (ox + sx, oy, oz + sz), "111": (ox + sx, oy + sy, oz + sz), "011": (ox, oy + sy, oz + sz)}


def rot_point(p, pivot, rot):
    """Rotate p about pivot by Euler degrees rot=[rx,ry,rz] (Z*Y*X), to match the geo cube rotation."""
    if not rot:
        return p
    x, y, z = p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2]
    rx, ry, rz = (math.radians(a) for a in rot)
    # X
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    # Y
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    # Z
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + pivot[0], y + pivot[1], z + pivot[2])


def project(v):
    x, y, z = v
    ca, sa = math.cos(ANGLE), math.sin(ANGLE)
    rx = x * ca - z * sa
    rz = x * sa + z * ca
    ce, se = math.cos(ELEV), math.sin(ELEV)
    return (rx, y * ce - rz * se, y * se + rz * ce)


def sample_cells(tex, u, v, w, h, scale_uv):
    aw, ah = abs(w), abs(h)
    cols = max(1, int(round(aw)))
    rows = max(1, int(round(ah)))
    x0, y0 = u * scale_uv, v * scale_uv
    dx, dy = (w * scale_uv) / cols, (h * scale_uv) / rows
    data = []
    for iy in range(rows):
        for ix in range(cols):
            px = max(0, min(tex.width - 1, int(x0 + (ix + 0.5) * dx)))
            py = max(0, min(tex.height - 1, int(y0 + (iy + 0.5) * dy)))
            data.append((ix, iy, tex.getpixel((px, py))))
    return {"cols": cols, "rows": rows, "data": data}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", required=True)
    ap.add_argument("--size", type=int, default=OUT_SIZE)
    args = ap.parse_args()
    name = args.name

    geo = json.loads((ROOT / f"geckolib/models/item/{name}.geo.json").read_text())
    tex = Image.open(ROOT / f"textures/item/{name}.png").convert("RGBA")
    scale_uv = tex.width / UV_SPACE

    prims = []
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            c = corners(cube["origin"], cube["size"])
            rot = cube.get("rotation")
            piv = cube.get("pivot", [0, 0, 0])
            if rot:
                c = {k: rot_point(p, piv, rot) for k, p in c.items()}
            uvs = cube.get("uv", {})
            for fname, keys in FACES.items():
                f = uvs.get(fname)
                if not f:
                    continue
                proj = [project(c[k]) for k in keys]
                depth = sum(p[2] for p in proj) / 4
                quad2d = [(p[0], p[1]) for p in proj]
                cells = sample_cells(tex, f["uv"][0], f["uv"][1], f["uv_size"][0], f["uv_size"][1], scale_uv)
                prims.append((depth, quad2d, cells, FACE_SHADE[fname]))

    pts = [p for _, q, _, _ in prims for p in q]
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    scale = 16 * 8
    pad = 4
    W = int((maxx - minx) * scale) + pad * 2
    H = int((maxy - miny) * scale) + pad * 2

    def P(pt):
        return (pad + (pt[0] - minx) * scale, pad + (maxy - pt[1]) * scale)

    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    prims.sort(key=lambda t: t[0])
    for _, quad, cells, shade in prims:
        bl, br, tr, tl = quad

        def lerp(a, b, t):
            return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

        def bilerp(uu, vv):
            return lerp(lerp(bl, br, uu), lerp(tl, tr, uu), vv)

        cols, rows = cells["cols"], cells["rows"]
        for (ix, iy, col) in cells["data"]:
            if col[3] == 0:
                continue
            p0 = P(bilerp(ix / cols, iy / rows))
            p1 = P(bilerp((ix + 1) / cols, iy / rows))
            p2 = P(bilerp((ix + 1) / cols, (iy + 1) / rows))
            p3 = P(bilerp(ix / cols, (iy + 1) / rows))
            r, g, b, a = col
            draw.polygon([p0, p1, p2, p3],
                         fill=(int(r * shade), int(g * shade), int(b * shade), a))

    bbox = img.getchannel("A").getbbox()
    img = img.crop(bbox)
    side = max(img.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    out = ROOT / f"textures/item/{name}_gui.png"
    sq.resize((args.size, args.size), Image.NEAREST).save(out)
    print(f"wrote {out} ({args.size}x{args.size})")


if __name__ == "__main__":
    main()
