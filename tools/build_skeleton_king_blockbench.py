"""Build the Skeleton King art assets from a single source of truth.

Emits THREE synced artifacts from one bone/cube table:

  art/blockbench/skeleton_king/skeleton_king.png        — 128x128 painted texture
  art/blockbench/skeleton_king/skeleton_king.geo.json   — Bedrock geometry
  art/blockbench/skeleton_king/skeleton_king.bbmodel    — Blockbench project (texture embedded)

and copies the texture into the mod resources:

  src/main/resources/assets/ironhold/textures/entity/skeleton_king.png

The bone/cube table mirrors SkeletonKingModel.createBodyLayer() in Bedrock world
coords (feet at y=0, head top up; Y-up). UVs are a hand-packed 128x128 atlas; the
Java model's texOffs(...) values must match the `uv` fields below.

Box-UV face unwrap (standard Minecraft layout, also what Blockbench box_uv mode
generates) for a cube at atlas (U,V) with size (w=x, h=y, dep=z):
    up:    (U+dep,        V)        size (w,   dep)
    down:  (U+dep+w,      V)        size (w,   dep)
    east:  (U,            V+dep)    size (dep, h)
    north: (U+dep,        V+dep)    size (w,   h)   <- the FRONT face
    west:  (U+dep+w,      V+dep)    size (dep, h)
    south: (U+dep+w+dep,  V+dep)    size (w,   h)
"""
import base64
import json
import os
import uuid

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(ROOT, "art", "blockbench", "skeleton_king")
RES_PNG = os.path.join(ROOT, "src", "main", "resources", "assets", "ironhold",
                       "textures", "entity", "skeleton_king.png")
os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(os.path.dirname(RES_PNG), exist_ok=True)

TEX_W = TEX_H = 128

# ── Palette ─────────────────────────────────────────────────────────────────
BONE        = (188, 182, 168, 255)
BONE_SH     = (150, 143, 128, 255)
EYE_SOCKET  = (28, 30, 36, 255)
EYE_GLOW    = (78, 206, 224, 255)
EYE_CORE    = (190, 250, 255, 255)
NOSE        = (40, 42, 48, 255)
TEETH       = (206, 201, 186, 255)
GAP         = (44, 40, 44, 255)
GUN         = (54, 58, 64, 255)
GUN_DK      = (38, 41, 46, 255)
GUN_HI      = (74, 80, 88, 255)
GOLD        = (198, 158, 60, 255)
GOLD_HI     = (232, 196, 96, 255)
GOLD_DK     = (146, 112, 40, 255)
RED         = (96, 32, 38, 255)
RED_DK      = (62, 20, 26, 255)
RED_HI      = (124, 44, 50, 255)

# ── Bone / cube table (Bedrock world coords) ─────────────────────────────────
# role drives how each cube's faces are painted.
bones = [
    {"name": "head", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0], "role": "skull"},
    ]},
    {"name": "crown", "parent": "head", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 31, -4.5], "size": [8, 2, 1], "uv": [32, 0], "role": "gold"},
        {"origin": [-4, 31, 3.5], "size": [8, 2, 1], "uv": [32, 4], "role": "gold"},
        {"origin": [-4.5, 31, -4], "size": [1, 2, 8], "uv": [32, 8], "role": "gold"},
        {"origin": [3.5, 31, -4], "size": [1, 2, 8], "uv": [52, 8], "role": "gold"},
        {"origin": [-1, 33, -4.5], "size": [2, 4, 1], "uv": [52, 0], "role": "gold"},
        {"origin": [-3.5, 33, -4.5], "size": [1, 3, 1], "uv": [60, 0], "role": "gold"},
        {"origin": [2.5, 33, -4.5], "size": [1, 3, 1], "uv": [66, 0], "role": "gold"},
        {"origin": [3.5, 33, -1], "size": [1, 4, 2], "uv": [72, 0], "role": "gold"},
        {"origin": [-4.5, 33, -1], "size": [1, 4, 2], "uv": [80, 0], "role": "gold"},
        {"origin": [-1, 33.5, 3.5], "size": [2, 3, 1], "uv": [72, 8], "role": "gold"},
        {"origin": [-3.5, 33, 3.5], "size": [1, 3, 1], "uv": [80, 8], "role": "gold"},
        {"origin": [2.5, 33, 3.5], "size": [1, 3, 1], "uv": [86, 8], "role": "gold"},
    ]},
    {"name": "body", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [0, 20], "role": "cuirass"},
    ]},
    {"name": "robe_front", "parent": "body", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 1, -2.6], "size": [8, 11, 1], "uv": [0, 54], "role": "robe_front"},
    ]},
    {"name": "robe_back", "parent": "body", "pivot": [0, 24, 0], "cubes": [
        {"origin": [-4, 1, 1.6], "size": [8, 11, 1], "uv": [20, 54], "role": "robe"},
    ]},
    {"name": "cape", "parent": "body", "pivot": [0, 23, 2.6], "cubes": [
        {"origin": [-5, -1, 2.6], "size": [10, 24, 1], "uv": [0, 70], "role": "cape"},
    ]},
    {"name": "right_arm", "pivot": [-5, 22, 0], "cubes": [
        {"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [24, 20], "role": "arm"},
    ]},
    {"name": "right_pauldron", "parent": "right_arm", "pivot": [-5, 22, 0], "cubes": [
        {"origin": [-9, 21.5, -3.5], "size": [5, 4, 7], "uv": [0, 40], "inflate": 0.25, "role": "pauldron"},
    ]},
    {"name": "left_arm", "pivot": [5, 22, 0], "cubes": [
        {"origin": [4, 12, -2], "size": [4, 12, 4], "uv": [40, 20], "role": "arm"},
    ]},
    {"name": "left_pauldron", "parent": "left_arm", "pivot": [5, 22, 0], "cubes": [
        {"origin": [4, 21.5, -3.5], "size": [5, 4, 7], "uv": [24, 40], "inflate": 0.25, "role": "pauldron"},
    ]},
    {"name": "right_leg", "pivot": [-2, 12, 0], "cubes": [
        {"origin": [-4, 0, -2], "size": [4, 12, 4], "uv": [56, 20], "role": "leg"},
    ]},
    {"name": "left_leg", "pivot": [2, 12, 0], "cubes": [
        {"origin": [0, 0, -2], "size": [4, 12, 4], "uv": [72, 20], "role": "leg"},
    ]},
]


def all_cubes():
    for b in bones:
        for c in b["cubes"]:
            yield c


# ── Texture painting ─────────────────────────────────────────────────────────

def face_rects(c):
    """Return {face: (x0, y0, w, h)} in the standard MC box-UV layout."""
    tx, ty = c["uv"]
    w, h, dep = (int(round(s)) for s in c["size"])
    return {
        "up":    (tx + dep,           ty,         w,   dep),
        "down":  (tx + dep + w,       ty,         w,   dep),
        "east":  (tx,                 ty + dep,   dep, h),
        "north": (tx + dep,           ty + dep,   w,   h),
        "west":  (tx + dep + w,       ty + dep,   dep, h),
        "south": (tx + dep + dep + w, ty + dep,   w,   h),
    }


def rect(d, x, y, w, h, color):
    if w <= 0 or h <= 0:
        return
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


SKULL = [
    "BBBBBBBB",
    "BBBBBBBB",
    "BCCBBCCB",
    "BCcBBcCB",
    "BBBnnBBB",
    "BBBBBBBB",
    "BTgTgTgB",
    "BbBBBBbB",
]
SKULL_COLORS = {"B": BONE, "b": BONE_SH, "C": EYE_GLOW, "c": EYE_CORE,
                "n": NOSE, "T": TEETH, "g": GAP}


def paint_skull(d, north):
    x0, y0, w, h = north
    for ry, row in enumerate(SKULL):
        for rx, ch in enumerate(row):
            d.rectangle([x0 + rx, y0 + ry, x0 + rx, y0 + ry], fill=SKULL_COLORS[ch])


def side_faces(rects):
    return [rects[k] for k in ("east", "north", "west", "south")]


def band(d, rects, from_bottom, thickness, color):
    """Paint a horizontal band across all four side faces."""
    for (x0, y0, w, h) in side_faces(rects):
        yy = y0 + h - from_bottom - thickness
        rect(d, x0, yy, w, thickness, color)


def band_top(d, rects, offset, thickness, color):
    for (x0, y0, w, h) in side_faces(rects):
        rect(d, x0, y0 + offset, w, thickness, color)


def paint_cube(d, c):
    rects = face_rects(c)
    role = c["role"]

    if role == "skull":
        for r in rects.values():
            rect(d, *r, BONE)
        # darker jaw underside + back
        rect(d, *rects["down"], BONE_SH)
        paint_skull(d, rects["north"])
        return

    if role == "gold":
        for r in rects.values():
            rect(d, *r, GOLD)
        rect(d, *rects["up"], GOLD_HI)
        band(d, rects, 0, 1, GOLD_DK)
        return

    if role in ("arm",):
        for r in rects.values():
            rect(d, *r, GUN)
        rect(d, *rects["up"], GUN_HI)
        band_top(d, rects, 0, 1, GUN_HI)
        band(d, rects, 0, 2, GOLD)        # gold gauntlet cuff
        band(d, rects, 2, 1, GOLD_DK)
        return

    if role == "leg":
        for r in rects.values():
            rect(d, *r, GUN)
        band(d, rects, 0, 4, GUN_DK)       # dark boots
        band(d, rects, 4, 1, GOLD_DK)
        return

    if role == "pauldron":
        for r in rects.values():
            rect(d, *r, GUN)
        rect(d, *rects["up"], GUN_HI)
        band_top(d, rects, 0, 1, GOLD)     # gold rim along the top
        band_top(d, rects, 1, 1, GOLD_DK)
        # cyan ember on the outer (south/back) plate
        sx, sy, sw, sh = rects["south"]
        rect(d, sx + sw // 2, sy + 1, 1, 1, EYE_GLOW)
        return

    if role in ("robe", "robe_front"):
        for r in rects.values():
            rect(d, *r, RED)
        rect(d, *rects["up"], RED_HI)
        band(d, rects, 0, 2, GOLD)         # gold hem
        band(d, rects, 2, 1, GOLD_DK)
        if role == "robe_front":
            # central darker slit
            x0, y0, w, h = rects["north"]
            rect(d, x0 + w // 2, y0, 1, h - 2, RED_DK)
        return

    if role == "cape":
        for r in rects.values():
            rect(d, *r, RED)
        band_top(d, rects, 0, 2, GOLD)     # gold shoulder trim
        band_top(d, rects, 2, 1, GOLD_DK)
        band(d, rects, 0, 6, RED_DK)       # tattered, darker hem
        # a couple of ragged notches at the very bottom
        for (x0, y0, w, h) in side_faces(rects):
            for nx in range(2, w, 4):
                rect(d, x0 + nx, y0 + h - 2, 1, 2, GAP)
        return

    if role == "cuirass":
        for r in rects.values():
            rect(d, *r, GUN)
        rect(d, *rects["up"], GUN_HI)
        x0, y0, w, h = rects["north"]
        rect(d, x0, y0, w, 1, GOLD)                 # gold collar
        rect(d, x0 + 2, y0 + 2, w - 4, h - 3, RED)  # red tabard panel
        rect(d, x0 + 2, y0 + 2, w - 4, 1, GOLD_DK)
        rect(d, x0, y0 + h - 3, w, 1, GOLD)         # gold belt
        rect(d, x0 + w // 2, y0 + 3, 1, h - 6, RED_HI)
        return


def build_texture():
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for c in all_cubes():
        paint_cube(d, c)
    return img


# ── Bedrock geometry (.geo.json) ─────────────────────────────────────────────

def build_geo():
    geo_bones = []
    for b in bones:
        node = {"name": b["name"], "pivot": b["pivot"]}
        if "parent" in b:
            node["parent"] = b["parent"]
        cubes = []
        for c in b["cubes"]:
            cube = {"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
            if "inflate" in c:
                cube["inflate"] = c["inflate"]
            cubes.append(cube)
        node["cubes"] = cubes
        geo_bones.append(node)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.skeleton_king",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3.5,
                "visible_bounds_offset": [0, 1.0, 0],
            },
            "bones": geo_bones,
        }],
    }


# ── Blockbench project (.bbmodel) ─────────────────────────────────────────────

def box_uv_faces(c, tex_id=0):
    tx, ty = c["uv"]
    w, h, dep = c["size"]
    return {
        "north": {"uv": [tx + dep, ty + dep, tx + dep + w, ty + dep + h], "texture": tex_id},
        "east":  {"uv": [tx, ty + dep, tx + dep, ty + dep + h], "texture": tex_id},
        "south": {"uv": [tx + dep + w + dep, ty + dep, tx + dep + w + dep + w, ty + dep + h], "texture": tex_id},
        "west":  {"uv": [tx + dep + w, ty + dep, tx + dep + w + dep, ty + dep + h], "texture": tex_id},
        "up":    {"uv": [tx + dep, ty, tx + dep + w, ty + dep], "texture": tex_id},
        "down":  {"uv": [tx + dep + w, ty, tx + dep + w + w, ty + dep], "texture": tex_id},
    }


def build_bbmodel(png_bytes):
    elements = []
    cube_uuids = {}  # id(cube) -> uuid
    for b in bones:
        for c in b["cubes"]:
            cu = str(uuid.uuid4())
            cube_uuids[id(c)] = cu
            ox, oy, oz = c["origin"]
            sx, sy, sz = c["size"]
            el = {
                "name": b["name"],
                "box_uv": True,
                "rescale": False,
                "locked": False,
                "render_order": "default",
                "allow_mirror_modeling": True,
                "from": [ox, oy, oz],
                "to": [ox + sx, oy + sy, oz + sz],
                "autouv": 0,
                "color": 0,
                "origin": b["pivot"],
                "uv_offset": c["uv"],
                "faces": box_uv_faces(c),
                "type": "cube",
                "uuid": cu,
            }
            if "inflate" in c:
                el["inflate"] = c["inflate"]
            elements.append(el)

    by_name = {b["name"]: b for b in bones}

    def bone_node(b):
        children = [cube_uuids[id(c)] for c in b["cubes"]]
        for child in bones:
            if child.get("parent") == b["name"]:
                children.append(bone_node(child))
        return {
            "name": b["name"],
            "origin": b["pivot"],
            "color": 0,
            "uuid": str(uuid.uuid4()),
            "export": True,
            "mirror_uv": False,
            "isOpen": False,
            "locked": False,
            "visibility": True,
            "autouv": 0,
            "children": children,
        }

    outliner = [bone_node(b) for b in bones if "parent" not in b]

    src = "data:image/png;base64," + base64.b64encode(png_bytes).decode("ascii")
    texture = {
        "name": "skeleton_king.png", "relative_path": "skeleton_king.png",
        "folder": "", "namespace": "", "id": "0",
        "width": TEX_W, "height": TEX_H, "uv_width": TEX_W, "uv_height": TEX_H,
        "particle": False, "use_as_default": False, "render_mode": "default",
        "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop",
        "frame_order": "", "frame_interpolate": False, "visible": True,
        "internal": True, "saved": True, "uuid": str(uuid.uuid4()), "source": src,
    }

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": True},
        "name": "skeleton_king",
        "model_identifier": "geometry.skeleton_king",
        "visible_box": [3, 4, 0.5],
        "resolution": {"width": TEX_W, "height": TEX_H},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
    }


# ── Write everything ─────────────────────────────────────────────────────────

def main():
    img = build_texture()
    png_path = os.path.join(OUT_DIR, "skeleton_king.png")
    img.save(png_path)
    img.save(RES_PNG)
    print(f"OK  {png_path}")
    print(f"OK  {RES_PNG}")
    # 8x preview for eyeballing
    img.resize((TEX_W * 8, TEX_H * 8), Image.NEAREST).save(
        os.path.join(OUT_DIR, "skeleton_king_x8.png"))

    with open(png_path, "rb") as f:
        png_bytes = f.read()

    geo_path = os.path.join(OUT_DIR, "skeleton_king.geo.json")
    with open(geo_path, "w") as f:
        json.dump(build_geo(), f, indent=2)
    print(f"OK  {geo_path}")

    bb_path = os.path.join(OUT_DIR, "skeleton_king.bbmodel")
    with open(bb_path, "w") as f:
        json.dump(build_bbmodel(png_bytes), f, indent=1)
    print(f"OK  {bb_path}")


if __name__ == "__main__":
    main()
