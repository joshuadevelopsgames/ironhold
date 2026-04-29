"""Simple orthographic cube renderer using PIL.

Projects each cube in king_enderman.geo.json to 2D and draws as filled
polygons with depth sorting. Three views: front, 3/4, side.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from PIL import Image, ImageDraw


GEO = Path("/Users/joshua/Kingdom SMP/ironhold/src/main/resources/assets/ironhold/geo/king_enderman.geo.json")

BG = (10, 6, 18, 255)

BONE_COLORS = {
    "body":      (44, 21, 56),
    "head":      (54, 32, 58),
    "right_arm": (48, 26, 64),
    "left_arm":  (48, 26, 64),
    "right_leg": (34, 20, 56),
    "left_leg":  (34, 20, 56),
}
EYE_GLOW = (240, 106, 255)

# Face shading multipliers (top brighter, sides dimmer, bottom darker).
FACE_SHADING = {
    "top":    1.15,
    "bottom": 0.55,
    "front":  1.0,
    "back":   0.85,
    "right":  0.75,
    "left":   0.78,
}

W, H = 700, 1000
SCALE = 10  # pixels per model unit
PAD = 60


def tint(rgb, mult):
    return tuple(max(0, min(255, int(c * mult))) for c in rgb)


def cube_faces(o, s):
    """Return the 6 faces of a cube as (name, 4 world-space vertices)."""
    x, y, z = o
    dx, dy, dz = s
    # 8 corners
    c = [
        (x,      y,      z),       # 0: left bottom front
        (x + dx, y,      z),       # 1: right bottom front
        (x + dx, y + dy, z),       # 2: right top front
        (x,      y + dy, z),       # 3: left top front
        (x,      y,      z + dz),  # 4: left bottom back
        (x + dx, y,      z + dz),  # 5: right bottom back
        (x + dx, y + dy, z + dz),  # 6: right top back
        (x,      y + dy, z + dz),  # 7: left top back
    ]
    return [
        ("front",  [c[0], c[1], c[2], c[3]]),
        ("back",   [c[5], c[4], c[7], c[6]]),
        ("bottom", [c[0], c[4], c[5], c[1]]),
        ("top",    [c[3], c[2], c[6], c[7]]),
        ("left",   [c[1], c[5], c[6], c[2]]),
        ("right",  [c[4], c[0], c[3], c[7]]),
    ]


def project(v, view, ax, ay, center_y):
    """World (x,y,z) → screen (px, py), with y flipped for image coords."""
    x, y, z = v
    if view == "front":
        sx = x
        sy = y
        depth = -z
    elif view == "side":
        sx = z
        sy = y
        depth = x
    else:  # "3q"
        ca, sa = math.cos(ax), math.sin(ax)
        ce, se = math.cos(ay), math.sin(ay)
        rx = x * ca - z * sa
        rz = x * sa + z * ca
        sx = rx
        sy = y * ce - rz * se
        depth = y * se + rz * ce
    return sx, sy, depth


def visible_face(name, view, ax):
    """Return True if this face is front-facing for the view."""
    if view == "front":
        return name in {"front", "top"}
    if view == "side":
        # Right side view looks from +X toward -X
        return name in {"left", "top", "front", "back"}
    # 3q — render everything; depth sort handles occlusion
    return True


def render(view_name, angle_deg=30, elev_deg=5):
    data = json.loads(GEO.read_text())
    bones = data["minecraft:geometry"][0]["bones"]

    # Collect (depth, face_polygon, color) tuples.
    prims = []
    for bone in bones:
        color = BONE_COLORS.get(bone["name"], (120, 60, 150))
        for cube in bone.get("cubes", []):
            uv = cube.get("uv", [0, 0])
            sx_, sy_, sz_ = cube["size"]
            is_eye = uv[1] >= 116 and sx_ <= 4 and sz_ <= 2
            base_color = EYE_GLOW if is_eye else color

            for fname, verts in cube_faces(cube["origin"], cube["size"]):
                if not visible_face(fname, view_name, 0):
                    continue
                # Project all 4 verts
                ax_r = math.radians(angle_deg)
                ay_r = math.radians(elev_deg)
                projected = [project(v, view_name, ax_r, ay_r, 0) for v in verts]
                avg_depth = sum(p[2] for p in projected) / 4
                face_color = tint(base_color, FACE_SHADING[fname])
                prims.append((avg_depth, [(p[0], p[1]) for p in projected], face_color))

    # Bounds for framing
    all_xy = [pt for _, poly, _ in prims for pt in poly]
    xs = [p[0] for p in all_xy]
    ys = [p[1] for p in all_xy]
    min_x, max_x = min(xs), max(xs)
    min_y, max_y = min(ys), max(ys)

    span_x = max_x - min_x
    span_y = max_y - min_y
    scale = min((W - 2 * PAD) / span_x, (H - 2 * PAD) / span_y)
    cx = (min_x + max_x) / 2
    cy = (min_y + max_y) / 2

    def map_pt(p):
        px = (p[0] - cx) * scale + W / 2
        py = H / 2 - (p[1] - cy) * scale
        return (px, py)

    img = Image.new("RGBA", (W, H), BG)
    draw = ImageDraw.Draw(img, "RGBA")

    # Sort back-to-front so near faces draw on top.
    prims.sort(key=lambda x: x[0])
    for _, poly, color in prims:
        screen = [map_pt(p) for p in poly]
        draw.polygon(screen, fill=(*color, 255), outline=(4, 2, 8, 255))

    return img


if __name__ == "__main__":
    out = Path("/tmp")
    render("front").save(out / "king_front.png")
    render("3q", angle_deg=35, elev_deg=10).save(out / "king_3q.png")
    render("side").save(out / "king_side.png")
    print("wrote /tmp/king_{front,3q,side}.png")
