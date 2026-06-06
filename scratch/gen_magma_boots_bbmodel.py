#!/usr/bin/env python3
"""Emit an editable Blockbench source (bedrock, box-UV) for the Magma Boots.

Cube sizes / UV offsets mirror the authoritative Java model in
MagmaBootsModelDef (shaft 5x8x5 @ uv[0,0]; sole 5x3x6 @ uv[0,14]).
Shown as an upright pair of boots (feet at y=0) so it opens cleanly in
Blockbench; the texture is embedded so it always loads.
"""
import base64
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORN = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity/equipment/humanoid/magma_boots.png")
OUT_DIR = os.path.join(ROOT, "art/blockbench/magma_boots")
os.makedirs(OUT_DIR, exist_ok=True)
OUT = os.path.join(OUT_DIR, "magma_boots.bbmodel")

with open(WORN, "rb") as f:
    tex_b64 = "data:image/png;base64," + base64.b64encode(f.read()).decode()

tex_uuid = str(uuid.uuid4())


def cube(name, frm, to, uv_offset, mirror):
    u = str(uuid.uuid4())
    return u, {
        "name": name,
        "box_uv": True,
        "rescale": False,
        "locked": False,
        "render_order": "default",
        "from": frm,
        "to": to,
        "uv_offset": uv_offset,
        "mirror_uv": mirror,
        "origin": [0, 0, 0],
        "rotation": [0, 0, 0],
        "uuid": u,
        "faces": {f: {"texture": 0} for f in ("north", "east", "south", "west", "up", "down")},
    }


elements = []
outliner = []


def boot(group_name, cx, mirror):
    """5-wide boot centered at x=cx; sole y0..3, shaft y3..11; toe juts -Z."""
    half = 2.5
    shaft_u, shaft = cube(group_name + "_shaft",
                          [cx - half, 3, -2.5], [cx + half, 11, 2.5], [0, 0], mirror)
    sole_u, sole = cube(group_name + "_sole",
                        [cx - half, 0, -4], [cx + half, 3, 2], [0, 14], mirror)
    elements.append(shaft)
    elements.append(sole)
    outliner.append({
        "name": group_name,
        "origin": [cx, 0, 0],
        "rotation": [0, 0, 0],
        "uuid": str(uuid.uuid4()),
        "export": True, "isOpen": True, "visibility": True,
        "children": [shaft_u, sole_u],
    })


boot("right_boot", -2.7, False)
boot("left_boot", 2.7, True)

model = {
    "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
    "name": "magma_boots",
    "model_identifier": "",
    "visible_box": [2, 2, 0.5],
    "resolution": {"width": 64, "height": 32},
    "elements": elements,
    "outliner": outliner,
    "textures": [{
        "path": WORN,
        "name": "magma_boots.png",
        "folder": "entity/equipment/humanoid",
        "namespace": "ironhold",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "visible": True,
        "mode": "bitmap",
        "uuid": tex_uuid,
        "relative_path": "../../../src/main/resources/assets/ironhold/textures/entity/equipment/humanoid/magma_boots.png",
        "source": tex_b64,
    }],
}

with open(OUT, "w") as f:
    json.dump(model, f, indent=2)
print("wrote", OUT)
