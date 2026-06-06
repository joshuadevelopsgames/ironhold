"""Generate a Blockbench project for the *vanilla* Minecraft iron golem.

The geometry is a faithful, mechanical conversion of Mojang's decompiled
``net.minecraft.client.model.IronGolemModel#createBodyLayer`` into the Bedrock
geometry convention this repo already uses (Y-up, feet at y=0, box UV).

Java entity space is Y-down; Bedrock is Y-up. The transform is:
    bedrock = (jx, 24 - jy, jz)
i.e. negate Y and shift so the feet (Java y=24) land on y=0. X and Z are
unchanged (verified against the vanilla humanoid pivots, where Java rightArm
offset -5 -> Bedrock pivot -5, leg offset y=12 -> Bedrock pivot 24-12=12).

The real 128x128 vanilla texture is pulled straight out of the game jar and
embedded, so the model opens fully textured.

Outputs:
  art/blockbench/iron_golem/iron_golem.bbmodel
  art/blockbench/iron_golem/iron_golem.geo.json
  art/blockbench/iron_golem/iron_golem.png
"""

import base64
import io
import json
import os
import zipfile

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT_DIR = os.path.join(ROOT, "art", "blockbench", "iron_golem")

# Any vanilla jar that ships the 128x128 iron golem texture works; prefer the
# version the mod targets, fall back to whatever is installed.
JAR_CANDIDATES = [
    "~/Library/Application Support/minecraft/versions/26.1.1/26.1.1.jar",
    "~/Library/Application Support/minecraft/versions/26.1/26.1.jar",
    "~/Library/Application Support/minecraft/versions/1.21.11/1.21.11.jar",
]
TEX_PATH = "assets/minecraft/textures/entity/iron_golem/iron_golem.png"

TEX_W = 128
TEX_H = 128
IDENTIFIER = "geometry.minecraft:iron_golem"


def load_vanilla_texture():
    for cand in JAR_CANDIDATES:
        path = os.path.expanduser(cand)
        if not os.path.exists(path):
            continue
        with zipfile.ZipFile(path) as jar:
            if TEX_PATH in jar.namelist():
                with jar.open(TEX_PATH) as src:
                    return Image.open(io.BytesIO(src.read())).convert("RGBA")
    raise RuntimeError("Could not find vanilla iron_golem.png in any known jar")


def uid(prefix, n):
    return f"00000000-0000-4000-8000-{prefix}{n:08d}"[-36:]


def cube(name, origin, size, uv, inflate=0.0, mirror=False):
    c = {"name": name, "origin": origin, "size": size, "uv": uv}
    if inflate:
        c["inflate"] = inflate
    if mirror:
        c["mirror"] = True
    return c


# Bones are flat children of root, exactly as the vanilla model retrieves them
# (root.getChild("head"/"right_arm"/...)), so vanilla animations map 1:1.
def iron_golem_bones():
    return [
        {"name": "root", "pivot": [0, 0, 0],
         "children": ["body", "head", "right_arm", "left_arm", "right_leg", "left_leg"],
         "cubes": []},
        {"name": "body", "parent": "root", "pivot": [0, 31, 0], "cubes": [
            cube("body_main", [-9, 21, -6], [18, 12, 11], [0, 40]),
            cube("body_skirt", [-4.5, 16, -3], [9, 5, 6], [0, 70], inflate=0.5),
        ]},
        {"name": "head", "parent": "root", "pivot": [0, 31, -2], "cubes": [
            cube("head_main", [-4, 33, -7.5], [8, 10, 8], [0, 0]),
            cube("nose", [-1, 32, -9.5], [2, 4, 2], [24, 0]),
        ]},
        {"name": "right_arm", "parent": "root", "pivot": [0, 31, 0], "cubes": [
            cube("right_arm", [-13, 3.5, -3], [4, 30, 6], [60, 21]),
        ]},
        {"name": "left_arm", "parent": "root", "pivot": [0, 31, 0], "cubes": [
            cube("left_arm", [9, 3.5, -3], [4, 30, 6], [60, 58]),
        ]},
        {"name": "right_leg", "parent": "root", "pivot": [-4, 13, 0], "cubes": [
            cube("right_leg", [-7.5, 0, -3], [6, 16, 5], [37, 0]),
        ]},
        {"name": "left_leg", "parent": "root", "pivot": [5, 13, 0], "cubes": [
            cube("left_leg", [1.5, 0, -3], [6, 16, 5], [60, 0], mirror=True),
        ]},
    ]


def flatten_cubes(bones):
    all_cubes = []
    idx = 1
    for bone in bones:
        for c in bone["cubes"]:
            c["uuid"] = uid("8000", idx)
            all_cubes.append(c)
            idx += 1
    return all_cubes


def geometry(bones):
    geo_bones = []
    for bone in bones:
        entry = {"name": bone["name"], "pivot": bone["pivot"]}
        if "parent" in bone:
            entry["parent"] = bone["parent"]
        cubes = []
        for c in bone["cubes"]:
            item = {"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
            if c.get("inflate"):
                item["inflate"] = c["inflate"]
            if c.get("mirror"):
                item["mirror"] = True
            cubes.append(item)
        if cubes:
            entry["cubes"] = cubes
        geo_bones.append(entry)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": IDENTIFIER,
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 5,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": geo_bones,
        }],
    }


def bb_group_tree(bones):
    bone_by_name = {b["name"]: b for b in bones}
    child_map = {}
    for bone in bones:
        if "parent" in bone:
            child_map.setdefault(bone["parent"], []).append(bone["name"])

    def make_group(name):
        bone = bone_by_name[name]
        children = [c["uuid"] for c in bone["cubes"]]
        for child_name in child_map.get(name, []):
            children.append(make_group(child_name))
        return {
            "name": name,
            "origin": bone["pivot"],
            "rotation": [0, 0, 0],
            "uuid": uid("9000", len(name) + sum(ord(ch) for ch in name)),
            "export": True,
            "isOpen": True,
            "visibility": True,
            "children": children,
        }

    return [make_group(b["name"]) for b in bones if "parent" not in b]


def bbmodel(texture_png, bones):
    elements = []
    for c in flatten_cubes(bones):
        el = {
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
        }
        if c.get("inflate"):
            el["inflate"] = c["inflate"]
        if c.get("mirror"):
            el["mirror_uv"] = True
        elements.append(el)

    buf = io.BytesIO()
    texture_png.save(buf, format="PNG")
    source = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

    return {
        "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
        "name": "iron_golem",
        "model_identifier": IDENTIFIER,
        "visible_box": [5, 4, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "resolution": {"width": TEX_W, "height": TEX_H},
        "elements": elements,
        "outliner": bb_group_tree(bones),
        "textures": [{
            "path": "iron_golem.png",
            "name": "iron_golem.png",
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


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    texture = load_vanilla_texture()
    bones = iron_golem_bones()
    texture.save(os.path.join(OUT_DIR, "iron_golem.png"))
    write_json(os.path.join(OUT_DIR, "iron_golem.geo.json"), geometry(bones))
    write_json(os.path.join(OUT_DIR, "iron_golem.bbmodel"), bbmodel(texture, bones))
    for name in ("iron_golem.png", "iron_golem.geo.json", "iron_golem.bbmodel"):
        print(os.path.join(OUT_DIR, name))


if __name__ == "__main__":
    main()
