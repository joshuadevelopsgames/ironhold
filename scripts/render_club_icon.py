#!/usr/bin/env python3
"""Render a GUI/inventory icon for the club from its geckolib geo + texture.

Per-face-UV aware (the club geo uses {north:{uv,uv_size}, ...}, not box UV).
Orthographic 3/4 view, transparent background, autocropped, NEAREST-scaled to 16x16.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path("/Users/joshua/ironhold/src/main/resources/assets/ironhold")
GEO = ROOT / "geckolib/models/item/club.geo.json"
TEX = ROOT / "textures/item/club.png"
OUT = ROOT / "textures/item/club_gui.png"

UV_SPACE = 64            # geo texture_width/height
SS = 8                   # supersample working resolution per model unit
ANGLE = math.radians(28) # yaw
ELEV = math.radians(12)  # pitch
OUT_SIZE = 16

FACE_SHADE = {"up": 1.0, "down": 0.78, "north": 0.88, "south": 0.97, "east": 0.90, "west": 0.83}


def corners(o, s):
    ox, oy, oz = o
    sx, sy, sz = s
    return {
        "000": (ox, oy, oz), "100": (ox + sx, oy, oz), "110": (ox + sx, oy + sy, oz), "010": (ox, oy + sy, oz),
        "001": (ox, oy, oz + sz), "101": (ox + sx, oy, oz + sz), "111": (ox + sx, oy + sy, oz + sz), "011": (ox, oy + sy, oz + sz),
    }


# face -> (screen corners as [bl, br, tr, tl])
FACES = {
    "north": ("000", "100", "110", "010"),
    "south": ("101", "001", "011", "111"),
    "west":  ("001", "000", "010", "011"),
    "east":  ("100", "101", "111", "110"),
    "up":    ("010", "110", "111", "011"),
    "down":  ("001", "101", "100", "000"),
}


def project(v):
    x, y, z = v
    ca, sa = math.cos(ANGLE), math.sin(ANGLE)
    rx = x * ca - z * sa
    rz = x * sa + z * ca
    ce, se = math.cos(ELEV), math.sin(ELEV)
    return (rx, y * ce - rz * se, y * se + rz * ce)  # (screenX, screenY, depth)


def main():
    geo = json.loads(GEO.read_text())
    tex = Image.open(TEX).convert("RGBA")
    scale_uv = tex.width / UV_SPACE  # 128/64 = 2

    prims = []  # (depth, screen_quad[4], cells, shade)
    for bone in geo["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            c = corners(cube["origin"], cube["size"])
            uvs = cube.get("uv", {})
            for fname, keys in FACES.items():
                f = uvs.get(fname)
                if not f:
                    continue
                verts = [c[k] for k in keys]
                proj = [project(v) for v in verts]
                depth = sum(p[2] for p in proj) / 4
                quad2d = [(p[0], p[1]) for p in proj]
                u, v = f["uv"]
                w, h = f["uv_size"]
                cells = sample_cells(tex, u, v, w, h, scale_uv)
                prims.append((depth, quad2d, cells, FACE_SHADE[fname]))

    # bounds
    pts = [p for _, q, _, _ in prims for p in q]
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    scale = SS * 16  # working px per model-unit-ish; cropped later anyway
    pad = 4
    W = int((maxx - minx) * scale) + pad * 2
    H = int((maxy - miny) * scale) + pad * 2

    def P(pt):
        return (pad + (pt[0] - minx) * scale, pad + (maxy - pt[1]) * scale)  # flip Y

    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    prims.sort(key=lambda t: t[0])  # back to front
    for _, quad, cells, shade in prims:
        bl, br, tr, tl = quad

        def lerp(a, b, t):
            return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

        def bilerp(uu, vv):
            pb = lerp(bl, br, uu)
            pt = lerp(tl, tr, uu)
            return lerp(pb, pt, vv)

        cols, rows = cells["cols"], cells["rows"]
        for (ix, iy, col) in cells["data"]:
            if col[3] == 0:
                continue
            u0, u1 = ix / cols, (ix + 1) / cols
            v0, v1 = iy / rows, (iy + 1) / rows
            p0 = P(bilerp(u0, v0))
            p1 = P(bilerp(u1, v0))
            p2 = P(bilerp(u1, v1))
            p3 = P(bilerp(u0, v1))
            r, g, b, a = col
            r = max(0, min(255, int(r * shade)))
            g = max(0, min(255, int(g * shade)))
            b = max(0, min(255, int(b * shade)))
            draw.polygon([p0, p1, p2, p3], fill=(r, g, b, a))

    # autocrop to content, pad to square, downscale
    bbox = img.getchannel("A").getbbox()
    img = img.crop(bbox)
    side = max(img.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    icon = sq.resize((OUT_SIZE, OUT_SIZE), Image.NEAREST)
    icon.save(OUT)
    print("wrote", OUT, icon.size)


def sample_cells(tex, u, v, w, h, scale_uv):
    """Grid of texels covering the face's UV rect (uv_size may be negative)."""
    aw, ah = abs(w), abs(h)
    cols = max(1, int(round(aw)))
    rows = max(1, int(round(ah)))
    x0 = u * scale_uv
    y0 = v * scale_uv
    dx = (w * scale_uv) / cols
    dy = (h * scale_uv) / rows
    data = []
    for iy in range(rows):
        for ix in range(cols):
            tx = x0 + (ix + 0.5) * dx
            ty = y0 + (iy + 0.5) * dy
            px = max(0, min(tex.width - 1, int(tx)))
            py = max(0, min(tex.height - 1, int(ty)))
            data.append((ix, iy, tex.getpixel((px, py))))
    return {"cols": cols, "rows": rows, "data": data}


if __name__ == "__main__":
    main()
