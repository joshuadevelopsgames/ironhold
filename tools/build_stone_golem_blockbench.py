"""Generate Blockbench projects for the stone golem and its separate hammer.

Outputs:
  art/blockbench/stone_golem/stone_golem.bbmodel
  art/blockbench/stone_golem/stone_golem.geo.json
  art/blockbench/stone_golem/stone_golem.png
  art/blockbench/stone_golem/stone_golem_hammer.bbmodel
  art/blockbench/stone_golem/stone_golem_hammer.geo.json
  art/blockbench/stone_golem/stone_golem_hammer.png
"""

import base64
import io
import json
import os
import random
import zipfile

from PIL import Image, ImageDraw


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(ROOT, "art", "blockbench", "stone_golem")
MC_JAR = os.path.expanduser(
    "~/Library/Application Support/minecraft/versions/1.21.11/1.21.11.jar"
)

TEX_W = 256
TEX_H = 256

STONE = "assets/minecraft/textures/block/stone.png"
MOSSY = "assets/minecraft/textures/block/mossy_cobblestone.png"

AMBER = (255, 174, 42, 255)
AMBER_HI = (255, 232, 114, 255)
DARK = (32, 29, 26, 255)
CRACK = (55, 53, 50, 255)
MOSS = (54, 86, 45, 255)
MOSS_DK = (34, 58, 34, 255)
WOOD = (95, 75, 56, 255)
WOOD_DK = (61, 48, 37, 255)
WOOD_HI = (125, 98, 70, 255)


def load_vanilla_texture(path):
    with zipfile.ZipFile(MC_JAR) as jar:
        with jar.open(path) as src:
            return Image.open(src).convert("RGBA")


STONE_TILE = load_vanilla_texture(STONE)
MOSSY_TILE = load_vanilla_texture(MOSSY)


def uid(prefix, n):
    return f"00000000-0000-4000-8000-{prefix}{n:08d}"[-36:]


def footprint(size):
    w, h, d = [int(round(v)) for v in size]
    return 2 * d + 2 * w, d + h


def pack_uvs(bones):
    x = 0
    y = 0
    row_h = 0
    margin = 2
    for bone in bones:
        for cube in bone["cubes"]:
            fw, fh = footprint(cube["size"])
            if x + fw > TEX_W:
                x = 0
                y += row_h + margin
                row_h = 0
            if y + fh > TEX_H:
                raise RuntimeError(f"Texture atlas overflow at {cube['name']}")
            cube["uv"] = [x, y]
            x += fw + margin
            row_h = max(row_h, fh)


def face_rects(cube):
    u, v = cube["uv"]
    w, h, d = [int(round(v)) for v in cube["size"]]
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + d + w, v + d, w, h),
    }


def tile_fill(img, rect, tile=STONE_TILE, shade=1.0):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            r, g, b, a = tile.getpixel((x % tile.width, y % tile.height))
            img.putpixel(
                (x0 + x, y0 + y),
                (min(255, int(r * shade)), min(255, int(g * shade)), min(255, int(b * shade)), a),
            )


def fill_rect(draw, rect, color):
    x, y, w, h = rect
    if w > 0 and h > 0:
        draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def add_cracks(draw, rect, seed):
    rng = random.Random(seed)
    x0, y0, w, h = rect
    if w < 3 or h < 3:
        return
    for _ in range(max(1, (w * h) // 90)):
        x = x0 + rng.randrange(1, max(2, w - 1))
        y = y0 + rng.randrange(1, max(2, h - 1))
        steps = rng.randrange(2, 5)
        pts = [(x, y)]
        for _step in range(steps):
            x = max(x0, min(x0 + w - 1, x + rng.choice([-1, 0, 1])))
            y = max(y0, min(y0 + h - 1, y + rng.choice([0, 1])))
            pts.append((x, y))
        draw.line(pts, fill=CRACK, width=1)


def add_moss(draw, rect, seed, amount):
    rng = random.Random(seed)
    x0, y0, w, h = rect
    if w < 4 or h < 4:
        return
    for _ in range(amount):
        x = x0 + rng.randrange(0, w)
        y = y0 + rng.randrange(0, max(1, h // 4 + 1))
        length = rng.randrange(1, max(2, min(4, h - y + y0)))
        color = MOSS if rng.random() > 0.25 else MOSS_DK
        draw.line([(x, y), (x, min(y0 + h - 1, y + length))], fill=color, width=1)
        if rng.random() > 0.65 and x + 1 < x0 + w:
            draw.point((x + 1, min(y0 + h - 1, y + length // 2)), fill=color)


def paint_stone_cube(img, draw, cube, seed):
    for face, rect in face_rects(cube).items():
        shade = {"up": 1.12, "down": 0.74, "north": 0.98, "south": 0.86, "east": 0.93, "west": 0.9}[face]
        tile_fill(img, rect, STONE_TILE, shade)
        face_seed = {"up": 11, "down": 23, "north": 37, "south": 41, "east": 53, "west": 67}[face]
        add_cracks(draw, rect, seed + face_seed)
        if face in ("north", "west", "east", "south"):
            add_moss(draw, rect, seed + 100 + face_seed, cube.get("moss", 1))


def paint_wood_cube(draw, cube):
    for face, rect in face_rects(cube).items():
        fill_rect(draw, rect, WOOD)
        x0, y0, w, h = rect
        for yy in range(y0, y0 + h, 3):
            draw.line([(x0, yy), (x0 + w - 1, yy)], fill=WOOD_DK)
        if face == "up":
            fill_rect(draw, rect, WOOD_HI)


def paint_eye(draw, cube):
    for rect in face_rects(cube).values():
        fill_rect(draw, rect, AMBER)
    north = face_rects(cube)["north"]
    fill_rect(draw, north, AMBER_HI)


def paint_dark(draw, cube):
    for rect in face_rects(cube).values():
        fill_rect(draw, rect, DARK)


def cube(name, origin, size, role="stone", moss=1):
    return {"name": name, "origin": origin, "size": size, "role": role, "moss": moss}


def golem_bones():
    return [
        {"name": "root", "pivot": [0, 0, 0], "children": ["body", "left_leg", "right_leg"], "cubes": []},
        {"name": "body", "parent": "root", "pivot": [0, 21, 0], "rotation": [-3, 0, -2], "cubes": [
            cube("waist_block", [-6, 9, -4], [12, 6, 8], moss=0),
            cube("lower_torso_core", [-5, 15, -4], [10, 9, 8], moss=0),
            cube("upper_torso_core", [-7, 23, -5], [14, 10, 10], moss=1),
            cube("front_breastplate_slab", [-8, 25, -6.5], [16, 6, 2], moss=0),
            cube("left_capstone_shoulder", [-23, 30, -7], [16, 8, 14], moss=1),
            cube("right_capstone_shoulder", [7, 30, -7], [16, 8, 14], moss=1),
            cube("center_collar_bridge", [-9, 32, -6], [18, 5, 12], moss=0),
            cube("rear_counterweight", [-6, 21, 4], [12, 12, 3], moss=0),
            cube("neck_shadow_socket", [-4, 33, -4], [8, 3, 8], role="dark", moss=0),
        ], "children": ["head", "left_arm", "right_arm", "rubble_details"]},
        {"name": "head", "parent": "body", "pivot": [0, 36, 0], "rotation": [7, 0, 3], "cubes": [
            cube("head_core_recessed", [-5, 36, -6], [10, 8, 10], moss=1),
            cube("left_temple_plate", [-7, 35, -5], [2, 9, 9], moss=0),
            cube("right_temple_plate", [5, 35, -5], [2, 9, 9], moss=0),
            cube("top_brow_lintel", [-6, 41, -7], [12, 3, 4], moss=0),
            cube("left_cheek_drop", [-5, 35, -7], [3, 5, 2], moss=0),
            cube("right_cheek_drop", [2, 35, -7], [3, 5, 2], moss=0),
            cube("deep_face_void", [-2.5, 35.5, -7.2], [5, 4.5, 1], role="dark", moss=0),
            cube("hanging_center_nose", [-1, 33.5, -7], [2, 5, 2], moss=0),
            cube("left_eye_glow", [-3.4, 38, -8], [1.4, 1.4, 1], role="eye", moss=0),
            cube("right_eye_glow", [2, 38, -8], [1.4, 1.4, 1], role="eye", moss=0),
        ]},
        {"name": "left_arm", "parent": "body", "pivot": [-21, 31, 0], "rotation": [-4, 0, 24], "cubes": [
            cube("left_hanging_upper_arm", [-28, 18, -5], [8, 13, 10], moss=0),
            cube("left_square_elbow", [-30, 14, -6], [11, 6, 12], moss=0),
            cube("left_long_forearm", [-35, 4, -5], [10, 13, 10], moss=0),
            cube("left_massive_fist", [-37, -1, -7], [12, 8, 14], moss=0),
            cube("left_outer_knuckle", [-40, 2, -4], [4, 4, 8], moss=0),
        ]},
        {"name": "right_arm", "parent": "body", "pivot": [21, 31, 0], "rotation": [-13, 0, -22], "cubes": [
            cube("right_hanging_upper_arm", [20, 18, -5], [8, 13, 10], moss=0),
            cube("right_square_elbow", [19, 14, -6], [11, 6, 12], moss=0),
            cube("right_long_forearm", [25, 4, -5], [10, 13, 10], moss=0),
            cube("right_massive_fist", [25, -1, -7], [12, 8, 14], moss=0),
            cube("right_outer_knuckle", [36, 2, -4], [4, 4, 8], moss=0),
        ]},
        {"name": "left_leg", "parent": "root", "pivot": [-5, 9, 0], "rotation": [0, 0, 8], "cubes": [
            cube("left_thigh_slab", [-11, 5, -4], [8, 11, 8], moss=0),
            cube("left_knee_block", [-12, 3, -5], [10, 5, 10], moss=0),
            cube("left_wide_foot", [-15, 0, -8], [14, 4, 13], moss=0),
        ]},
        {"name": "right_leg", "parent": "root", "pivot": [5, 9, 0], "rotation": [0, 0, -8], "cubes": [
            cube("right_thigh_slab", [3, 5, -4], [8, 11, 8], moss=0),
            cube("right_knee_block", [2, 3, -5], [10, 5, 10], moss=0),
            cube("right_wide_foot", [1, 0, -8], [14, 4, 13], moss=0),
        ]},
        {"name": "rubble_details", "parent": "body", "pivot": [0, 20, 0], "cubes": [
            cube("left_chest_chip", [-10, 26, -7], [3, 3, 2], moss=0),
            cube("right_shoulder_chip", [14, 36, -4], [4, 3, 5], moss=0),
            cube("waist_chip", [3, 12, -6], [4, 3, 2], moss=0),
            cube("single_moss_patch_back", [-8, 29, 4], [4, 5, 2], moss=2),
            cube("collar_chip", [-2, 35, -7], [4, 2, 3], moss=0),
            cube("left_shoulder_top_chip", [-18, 38, -2], [5, 3, 5], moss=0),
            cube("right_shoulder_top_chip", [13, 38, -2], [5, 3, 5], moss=0),
        ]},
    ]


def hammer_bones():
    return [
        {"name": "root", "pivot": [0, 0, 0], "cubes": [
            cube("long_handle", [-28, -1, -1], [48, 2, 2], role="wood", moss=0),
            cube("handle_end_cap", [20, -2, -2], [5, 4, 4], moss=1),
            cube("head_socket", [-22, -4, -4], [8, 8, 8], moss=2),
            cube("hammer_head_core", [-36, -7, -6], [14, 14, 12], moss=4),
            cube("top_band", [-34, 7, -5], [10, 3, 10], moss=2),
            cube("bottom_band", [-34, -10, -5], [10, 3, 10], moss=1),
            cube("front_striker", [-40, -5, -5], [4, 10, 10], moss=3),
            cube("rear_striker", [-22, -5, -5], [4, 10, 10], moss=3),
            cube("chipped_corner_a", [-39, 4, 3], [3, 3, 3], moss=2),
            cube("chipped_corner_b", [-29, -10, -6], [4, 3, 3], moss=1),
        ]},
    ]


def golem_bones_iron_based():
    return [
        {"name": "root", "pivot": [0, 0, 0], "children": ["body", "left_leg", "right_leg"], "cubes": []},
        {"name": "body", "parent": "root", "pivot": [0, 27, 0], "rotation": [-2, 0, 0], "cubes": [
            cube("golem_pelvis", [-8, 14, -5], [16, 6, 10], moss=0),
            cube("golem_belly", [-7, 20, -5], [14, 10, 10], moss=0),
            cube("golem_chest_core", [-10, 29, -6], [20, 14, 12], moss=1),
            cube("front_chest_big_slab", [-11, 31, -8], [22, 9, 3], moss=0),
            cube("upper_chest_lintel", [-12, 40, -7], [24, 5, 13], moss=0),
            cube("left_shoulder_capstone", [-22, 38, -7], [12, 8, 14], moss=1),
            cube("right_shoulder_capstone", [10, 38, -7], [12, 8, 14], moss=1),
            cube("left_shoulder_lower_slab", [-20, 33, -6], [9, 6, 12], moss=0),
            cube("right_shoulder_lower_slab", [11, 33, -6], [9, 6, 12], moss=0),
            cube("back_counter_slab", [-8, 28, 5], [16, 15, 3], moss=0),
            cube("neck_recess_dark", [-4, 43, -4], [8, 3, 8], role="dark", moss=0),
        ], "children": ["head", "left_arm", "right_arm", "body_detail"]},
        {"name": "head", "parent": "body", "pivot": [0, 47, -1], "rotation": [3, 0, 0], "cubes": [
            cube("small_head_core", [-5, 46, -6], [10, 9, 10], moss=1),
            cube("head_left_side_slab", [-7, 46, -5], [2, 9, 9], moss=0),
            cube("head_right_side_slab", [5, 46, -5], [2, 9, 9], moss=0),
            cube("heavy_brow_lintel", [-6, 52, -7.5], [12, 3, 4], moss=0),
            cube("left_hanging_cheek", [-5, 46, -7.5], [3, 6, 2], moss=0),
            cube("right_hanging_cheek", [2, 46, -7.5], [3, 6, 2], moss=0),
            cube("deep_face_socket", [-2.5, 47, -8], [5, 4, 1], role="dark", moss=0),
            cube("center_nose_stone", [-1, 44, -7.5], [2, 5, 2], moss=0),
            cube("left_eye_glow", [-3.5, 50, -8.6], [1.5, 1.5, 1], role="eye", moss=0),
            cube("right_eye_glow", [2, 50, -8.6], [1.5, 1.5, 1], role="eye", moss=0),
        ]},
        {"name": "left_arm", "parent": "body", "pivot": [-18, 39, 0], "rotation": [0, 0, 4], "cubes": [
            cube("left_upper_arm_long", [-25, 25, -5], [8, 16, 10], moss=0),
            cube("left_upper_arm_outer_plate", [-27, 29, -6], [3, 11, 12], moss=0),
            cube("left_elbow_block", [-27, 20, -6], [11, 7, 12], moss=0),
            cube("left_forearm_huge", [-28, 7, -6], [10, 15, 12], moss=0),
            cube("left_forearm_front_plate", [-29, 10, -8], [10, 9, 3], moss=0),
            cube("left_heavy_fist", [-29, 1, -7], [12, 8, 14], moss=0),
            cube("left_knuckle_end", [-32, 4, -4], [4, 4, 8], moss=0),
        ]},
        {"name": "right_arm", "parent": "body", "pivot": [18, 39, 0], "rotation": [0, 0, -4], "cubes": [
            cube("right_upper_arm_long", [17, 25, -5], [8, 16, 10], moss=0),
            cube("right_upper_arm_outer_plate", [24, 29, -6], [3, 11, 12], moss=0),
            cube("right_elbow_block", [16, 20, -6], [11, 7, 12], moss=0),
            cube("right_forearm_huge", [18, 7, -6], [10, 15, 12], moss=0),
            cube("right_forearm_front_plate", [19, 10, -8], [10, 9, 3], moss=0),
            cube("right_heavy_fist", [17, 1, -7], [12, 8, 14], moss=0),
            cube("right_knuckle_end", [28, 4, -4], [4, 4, 8], moss=0),
        ]},
        {"name": "left_leg", "parent": "root", "pivot": [-5, 14, 0], "rotation": [0, 0, 1], "cubes": [
            cube("left_tall_leg", [-10, 4, -4], [8, 14, 8], moss=0),
            cube("left_knee_slab", [-11, 8, -5], [10, 5, 10], moss=0),
            cube("left_stone_foot", [-13, 0, -8], [12, 5, 14], moss=0),
        ]},
        {"name": "right_leg", "parent": "root", "pivot": [5, 14, 0], "rotation": [0, 0, -1], "cubes": [
            cube("right_tall_leg", [2, 4, -4], [8, 14, 8], moss=0),
            cube("right_knee_slab", [1, 8, -5], [10, 5, 10], moss=0),
            cube("right_stone_foot", [1, 0, -8], [12, 5, 14], moss=0),
        ]},
        {"name": "body_detail", "parent": "body", "pivot": [0, 30, 0], "cubes": [
            cube("left_chest_cracked_tile", [-9, 34, -8.5], [5, 5, 2], moss=0),
            cube("right_chest_cracked_tile", [4, 32, -8.5], [5, 5, 2], moss=0),
            cube("center_belly_hanging_slab", [-3, 20, -7], [6, 9, 2], moss=0),
            cube("left_waist_chip", [-10, 17, -6], [4, 4, 3], moss=0),
            cube("right_waist_chip", [6, 16, -6], [4, 4, 3], moss=0),
            cube("left_shoulder_top_chip", [-18, 46, -3], [5, 3, 6], moss=0),
            cube("right_shoulder_top_chip", [13, 46, -3], [5, 3, 6], moss=0),
            cube("small_back_moss_patch", [-6, 37, 7], [4, 5, 2], moss=2),
        ]},
    ]


def golem_bones_v4():
    bones = golem_bones_iron_based()
    for bone in bones:
        if bone["name"] == "head":
            bone["cubes"] = [
                cube("iron_golem_head_core", [-5, 46, -6], [10, 9, 10], moss=1),
                cube("left_temple_plate", [-7, 46, -5], [2, 9, 9], moss=0),
                cube("right_temple_plate", [5, 46, -5], [2, 9, 9], moss=0),
                cube("heavy_stone_brow", [-6, 52, -7.5], [12, 3, 4], moss=0),
                cube("left_eye_recess", [-4, 49, -7.8], [2, 2, 1], role="dark", moss=0),
                cube("right_eye_recess", [2, 49, -7.8], [2, 2, 1], role="dark", moss=0),
                cube("left_eye_glow", [-3.5, 49.4, -8.7], [1.2, 1.2, 1], role="eye", moss=0),
                cube("right_eye_glow", [2.3, 49.4, -8.7], [1.2, 1.2, 1], role="eye", moss=0),
                cube("long_golem_nose_bridge", [-1.5, 46, -8], [3, 6, 3], moss=0),
                cube("long_golem_nose_tip", [-1, 43, -7.5], [2, 4, 2], moss=0),
                cube("left_lower_face_plane", [-5, 44.5, -7], [3, 3, 1], moss=0),
                cube("right_lower_face_plane", [2, 44.5, -7], [3, 3, 1], moss=0),
                cube("thin_mouth_shadow", [-2, 44, -7.6], [4, 1, 1], role="dark", moss=0),
            ]
            return bones
    return bones


def flatten_cubes(bones):
    all_cubes = []
    idx = 1
    for bone in bones:
        for c in bone["cubes"]:
            c["uuid"] = uid("8000", idx)
            all_cubes.append(c)
            idx += 1
    return all_cubes


def paint_texture(bones):
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    seed = 100
    for bone in bones:
        for c in bone["cubes"]:
            role = c.get("role", "stone")
            if role == "eye":
                paint_eye(draw, c)
            elif role == "dark":
                paint_dark(draw, c)
            elif role == "wood":
                paint_wood_cube(draw, c)
            else:
                paint_stone_cube(img, draw, c, seed)
            seed += 17
    return img


def geometry(name, identifier, bones):
    geo_bones = []
    for bone in bones:
        entry = {"name": bone["name"], "pivot": bone["pivot"]}
        if "parent" in bone:
            entry["parent"] = bone["parent"]
        if bone.get("rotation"):
            entry["rotation"] = bone["rotation"]
        cubes = []
        for c in bone["cubes"]:
            item = {"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
            cubes.append(item)
        if cubes:
            entry["cubes"] = cubes
        geo_bones.append(entry)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": identifier,
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 5,
                "visible_bounds_height": 4.5 if "hammer" not in name else 2,
                "visible_bounds_offset": [0, 1.7 if "hammer" not in name else 0, 0],
            },
            "bones": geo_bones,
        }],
    }


def bb_group_tree(bones, child_map, bone_by_name):
    def make_group(name):
        bone = bone_by_name[name]
        children = [c["uuid"] for c in bone["cubes"]]
        for child_name in child_map.get(name, []):
            children.append(make_group(child_name))
        return {
            "name": name,
            "origin": bone["pivot"],
            "rotation": bone.get("rotation", [0, 0, 0]),
            "uuid": uid("9000", len(name) + sum(ord(ch) for ch in name)),
            "export": True,
            "isOpen": True,
            "visibility": True,
            "children": children,
        }

    roots = [b["name"] for b in bones if "parent" not in b]
    return [make_group(root) for root in roots]


def bbmodel(name, identifier, texture_name, texture_png, bones):
    elements = []
    for c in flatten_cubes(bones):
        elements.append({
            "name": c["name"],
            "box_uv": True,
            "rescale": False,
            "locked": False,
            "render_order": "default",
            "allow_mirror_modeling": True,
            "from": c["origin"],
            "to": [c["origin"][i] + c["size"][i] for i in range(3)],
            "autouv": 0,
            "color": 0,
            "origin": c["origin"],
            "rotation": [0, 0, 0],
            "type": "cube",
            "uuid": c["uuid"],
            "uv_offset": c["uv"],
        })

    child_map = {}
    bone_by_name = {}
    for bone in bones:
        bone_by_name[bone["name"]] = bone
        if "parent" in bone:
            child_map.setdefault(bone["parent"], []).append(bone["name"])

    buf = io.BytesIO()
    texture_png.save(buf, format="PNG")
    source = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

    return {
        "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
        "name": name,
        "model_identifier": identifier,
        "visible_box": [5, 4.5 if "hammer" not in name else 2, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "resolution": {"width": TEX_W, "height": TEX_H},
        "elements": elements,
        "outliner": bb_group_tree(bones, child_map, bone_by_name),
        "textures": [{
            "path": texture_name,
            "name": texture_name,
            "folder": "",
            "namespace": "",
            "id": "0",
            "width": TEX_W,
            "height": TEX_H,
            "uv_width": TEX_W,
            "uv_height": TEX_H,
            "particle": False,
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
            "visible": True,
            "internal": True,
            "saved": True,
            "uuid": uid("9999", 1),
            "source": source,
        }],
    }


def write_json(path, data):
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def emit(asset_name, identifier, bones):
    pack_uvs(bones)
    texture = paint_texture(bones)
    png_path = os.path.join(OUT_DIR, f"{asset_name}.png")
    geo_path = os.path.join(OUT_DIR, f"{asset_name}.geo.json")
    bb_path = os.path.join(OUT_DIR, f"{asset_name}.bbmodel")
    texture.save(png_path)
    write_json(geo_path, geometry(asset_name, identifier, bones))
    write_json(bb_path, bbmodel(asset_name, identifier, f"{asset_name}.png", texture, bones))
    return bb_path, geo_path, png_path


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    generated = [
        emit("stone_golem", "geometry.ironhold:stone_golem", golem_bones()),
        emit("stone_golem_hammer", "geometry.ironhold:stone_golem_hammer", hammer_bones()),
        emit("stone_golem_v2", "geometry.ironhold:stone_golem_v2", golem_bones()),
        emit("stone_golem_hammer_v2", "geometry.ironhold:stone_golem_hammer_v2", hammer_bones()),
        emit("stone_golem_v3", "geometry.ironhold:stone_golem_v3", golem_bones_iron_based()),
        emit("stone_golem_hammer_v3", "geometry.ironhold:stone_golem_hammer_v3", hammer_bones()),
        emit("stone_golem_v4", "geometry.ironhold:stone_golem_v4", golem_bones_v4()),
        emit("stone_golem_hammer_v4", "geometry.ironhold:stone_golem_hammer_v4", hammer_bones()),
    ]
    for paths in generated:
        for path in paths:
            print(path)


if __name__ == "__main__":
    main()
