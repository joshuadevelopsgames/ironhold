"""
Textured PIL renderer — samples king_enderman.png per-cube-face and
projects to 2D with depth sorting. Same math as render_geo_pil.py but
pulls colors from the actual texture instead of flat bone colors.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")
TEX = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman.png")
GLOW = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/textures/entity/king_enderman_glow.png")

BG = (10, 6, 18, 255)
W, H = 700, 1000
PAD = 50


def cube_faces(o, s):
    x, y, z = o
    dx, dy, dz = s
    c = [
        (x, y, z), (x+dx, y, z), (x+dx, y+dy, z), (x, y+dy, z),
        (x, y, z+dz), (x+dx, y, z+dz), (x+dx, y+dy, z+dz), (x, y+dy, z+dz),
    ]
    return [
        ("front",  [c[0], c[1], c[2], c[3]]),
        ("back",   [c[5], c[4], c[7], c[6]]),
        ("bottom", [c[0], c[4], c[5], c[1]]),
        ("top",    [c[3], c[2], c[6], c[7]]),
        ("left",   [c[1], c[5], c[6], c[2]]),
        ("right",  [c[4], c[0], c[3], c[7]]),
    ]


def face_uv(u, v, w, h, d):
    """Bedrock box-UV face regions."""
    return {
        "top":    (u + d,            v,       u + d + w,       v + d),
        "bottom": (u + d + w,        v,       u + d + 2 * w,   v + d),
        "right":  (u,                 v + d,   u + d,           v + d + h),
        "front":  (u + d,            v + d,   u + d + w,       v + d + h),
        "left":   (u + d + w,        v + d,   u + d + w + d,   v + d + h),
        "back":   (u + 2*d + w,      v + d,   u + 2 * (d + w),  v + d + h),
    }


def project(v, angle_rad, elev_rad, view):
    x, y, z = v
    if view == "front":
        return x, y, -z
    if view == "side":
        return z, y, x
    ca, sa = math.cos(angle_rad), math.sin(angle_rad)
    ce, se = math.cos(elev_rad), math.sin(elev_rad)
    rx = x * ca - z * sa
    rz = x * sa + z * ca
    return rx, y * ce - rz * se, y * se + rz * ce


def avg_color_of_region(tex_arr, x0, y0, x1, y1):
    """Average color of a texture region (returns (r,g,b))."""
    if x1 <= x0 or y1 <= y0:
        return (60, 30, 90)
    x0, y0 = max(0, int(x0)), max(0, int(y0))
    x1, y1 = min(tex_arr.width, int(x1)), min(tex_arr.height, int(y1))
    if x1 <= x0 or y1 <= y0:
        return (60, 30, 90)
    region = tex_arr.crop((x0, y0, x1, y1))
    pixels = region.getdata()
    n = len(pixels)
    if n == 0:
        return (60, 30, 90)
    r = sum(p[0] for p in pixels) // n
    g = sum(p[1] for p in pixels) // n
    b = sum(p[2] for p in pixels) // n
    return (r, g, b)


def sample_tiled_for_face(tex_img, u, v, w, h, d, face_name, glow_img=None):
    """
    For a larger area, sample multiple points across the face and return
    a list of (local_u, local_v, color) to draw the face as a grid of
    colored sub-rects. This captures per-pixel variation on the face.
    """
    uv = face_uv(u, v, w, h, d)[face_name]
    ux0, uy0, ux1, uy1 = uv
    # Decide grid resolution: face dimensions.
    if face_name in ("top", "bottom"):
        cols, rows = int(w), int(d)
    elif face_name in ("right", "left"):
        cols, rows = int(d), int(h)
    else:  # front, back
        cols, rows = int(w), int(h)
    cols = max(1, cols)
    rows = max(1, rows)

    cells = []
    for iy in range(rows):
        for ix in range(cols):
            tx = ux0 + ix * (ux1 - ux0) / cols
            ty = uy0 + iy * (uy1 - uy0) / rows
            ix_clamped = max(0, min(tex_img.width - 1, int(tx)))
            iy_clamped = max(0, min(tex_img.height - 1, int(ty)))
            col = tex_img.getpixel((ix_clamped, iy_clamped))
            # Overlay glow pixels if present on this UV
            if glow_img is not None:
                gcol = glow_img.getpixel((ix_clamped, iy_clamped))
                if len(gcol) == 4 and gcol[3] > 0:
                    col = gcol
            cells.append((ix, iy, cols, rows, col))
    return cells


def render(view, angle_deg=30, elev_deg=10, out_path="/tmp/king_tex.png"):
    data = json.loads(GEO.read_text())
    tex = Image.open(TEX).convert("RGBA")
    glow = Image.open(GLOW).convert("RGBA") if GLOW.exists() else None

    # Neutral shading since baked shading is already in the texture.
    FACE_SHADE = {
        "top": 1.0, "bottom": 0.9, "front": 1.0, "back": 0.92, "right": 0.95, "left": 0.94,
    }

    prims = []
    for bone in data["minecraft:geometry"][0]["bones"]:
        for cube in bone.get("cubes", []):
            u, v_ = cube.get("uv", [0, 0])
            sx, sy, sz = cube["size"]
            for fname, verts in cube_faces(cube["origin"], cube["size"]):
                ax = math.radians(angle_deg)
                ey = math.radians(elev_deg)
                proj = [project(v, ax, ey, view) for v in verts]
                avg_depth = sum(p[2] for p in proj) / 4
                verts2d = [(p[0], p[1]) for p in proj]
                cells = sample_tiled_for_face(tex, u, v_, sx, sy, sz, fname, glow)
                prims.append((avg_depth, fname, verts2d, cells, FACE_SHADE[fname]))

    all_xy = [p for _, _, verts2d, _, _ in prims for p in verts2d]
    xs = [p[0] for p in all_xy]; ys = [p[1] for p in all_xy]
    min_x, max_x = min(xs), max(xs); min_y, max_y = min(ys), max(ys)
    scale = min((W - 2*PAD) / (max_x - min_x), (H - 2*PAD) / (max_y - min_y))
    cx = (min_x + max_x) / 2; cy = (min_y + max_y) / 2

    def P(pt): return ((pt[0] - cx) * scale + W / 2, H / 2 - (pt[1] - cy) * scale)

    img = Image.new("RGBA", (W, H), BG)
    draw = ImageDraw.Draw(img, "RGBA")

    prims.sort(key=lambda x: x[0])
    for _, fname, verts2d, cells, face_mult in prims:
        # Compute the screen-space polygon for the face
        screen = [P(v) for v in verts2d]
        # Subdivide face into a grid of sub-quads using bilinear interpolation,
        # each colored by the corresponding texel.
        # Vertex order in our cube_faces output for each face is consistent:
        # [bottom-left, bottom-right, top-right, top-left] relative to the face.
        v_bl, v_br, v_tr, v_tl = verts2d

        # For each cell (ix, iy) of resolution (cols, rows), compute the 4 corners
        # via bilinear interpolation and fill with the texel color.
        if not cells:
            continue
        cols = cells[0][2]
        rows = cells[0][3]

        def lerp(a, b, t):
            return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

        def bilerp(u, v):
            # u along bottom edge (bl → br), v along left edge (bl → tl)
            pb = lerp(v_bl, v_br, u)
            pt = lerp(v_tl, v_tr, u)
            return lerp(pb, pt, v)

        for ix, iy, _, _, col in cells:
            u0 = ix / cols
            u1 = (ix + 1) / cols
            v0 = iy / rows
            v1 = (iy + 1) / rows
            p0 = P(bilerp(u0, v0))
            p1 = P(bilerp(u1, v0))
            p2 = P(bilerp(u1, v1))
            p3 = P(bilerp(u0, v1))
            # Apply face direction shading
            r, g, b = col[:3]
            r = max(0, min(255, int(r * face_mult)))
            g = max(0, min(255, int(g * face_mult)))
            b = max(0, min(255, int(b * face_mult)))
            draw.polygon([p0, p1, p2, p3], fill=(r, g, b, 255))

    img.save(out_path)
    print(f"wrote {out_path}")


if __name__ == "__main__":
    render("front", out_path="/tmp/king_tex_front.png")
    render("3q", angle_deg=35, elev_deg=10, out_path="/tmp/king_tex_3q.png")
