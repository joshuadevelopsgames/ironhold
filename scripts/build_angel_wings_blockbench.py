#!/usr/bin/env python3
"""Synthesize a Blockbench .bbmodel from AngelWingsLayer.java + the wing texture.

The angel wings are authored procedurally in Java (a vanilla RenderLayer built
with CubeListBuilder), so there is no native .bbmodel. This rebuilds one — the
same primary/covert feather fan — so the model + texture can be inspected and
edited in Blockbench.

Conventions (matching gen_shroomling_bbmodel.py):
  * modded_entity format with flip_y: cubes are stored in the y-up space where
    bb_y = 24 - java_y. X/Z are unchanged.
  * Each feather is one cube rotated about its attach point (the fan "roll").
    Under the y-flip a Java rotation about Z negates, so bb_roll = -java_roll.
  * The wings are laid out flat/spread (no in-game sweep/roll/pitch baked in) —
    the cleanest pose for viewing and editing the feather geometry. The runtime
    sweep lives in AngelWingsLayer.submit(), not in the mesh.
"""
import base64
import json
import math
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity/cosmetic/angel_wings.png")
OUT = os.path.join(ROOT, "art/blockbench/angel_wings/angel_wings.bbmodel")

# ── feather-fan constants (kept in sync with AngelWingsLayer.java) ───────────
ARM_RADIUS = 8.5
ARM_SWEEP = 1.40
ARM_RISE = 0.60
PRIMARIES = 9
PRIMARY_LEN_MIN, PRIMARY_LEN_MAX = 7.0, 14.0
PRIMARY_SPLAY = 0.50
COVERTS = 6
COVERT_LEN_MIN, COVERT_LEN_MAX = 4.0, 7.0
COVERT_SPLAY = 0.30
FEATHER_WIDTH = 2.0
# Distinct interleaved per-feather depths so overlapping broad faces never share a
# plane (no z-fighting); whole span < 1px feather thickness so no front/back coincide.
PRIMARY_BAND = 0.45
COVERT_FRONT = 0.15
COVERT_BAND = 0.30


def layer_depth(i, count, band):
    half = (count + 1) // 2
    rank = (i // 2) if i % 2 == 0 else half + i // 2
    return band * rank / (count - 1)

# Standalone anchor for the wing roots (Java space; only sets where it sits in
# the viewport since this is a wings-only model).
SHOULDER_X = 1.0
SHOULDER_Y_JAVA = 16.0
SHOULDER_Z_JAVA = 2.0


def feathers(side):
    """Yield (name, attach_xyz_java, length, roll_rad) for one wing.

    side = +1 right (fans toward +X), -1 left (mirrored)."""
    out = []
    for i in range(PRIMARIES):
        t = i / (PRIMARIES - 1)
        beta = t * ARM_SWEEP
        ax = side * ARM_RADIUS * math.sin(beta)
        ay = -ARM_RADIUS * (1.0 - math.cos(beta)) * ARM_RISE   # negative = up (y-down)
        az = -PRIMARY_BAND + layer_depth(i, PRIMARIES, PRIMARY_BAND)
        length = PRIMARY_LEN_MIN + (PRIMARY_LEN_MAX - PRIMARY_LEN_MIN) * math.sin(t * math.pi * 0.8)
        roll = -side * t * PRIMARY_SPLAY
        out.append((f"p{i}", (ax, ay, az), length, roll))
    for i in range(COVERTS):
        t = i / (COVERTS - 1)
        beta = t * ARM_SWEEP * 0.75
        ax = side * ARM_RADIUS * math.sin(beta)
        ay = -ARM_RADIUS * (1.0 - math.cos(beta)) * ARM_RISE
        az = COVERT_FRONT + layer_depth(i, COVERTS, COVERT_BAND)
        length = COVERT_LEN_MIN + (COVERT_LEN_MAX - COVERT_LEN_MIN) * t
        roll = -side * t * COVERT_SPLAY
        out.append((f"c{i}", (ax, ay, az), length, roll))
    return out


def box_uv_faces(length, tex_uuid):
    """Standard cube unwrap at uv_offset (0,0). Texture is a uniform feather
    sheet, so every face reads as feathers regardless of footprint."""
    w, h, d = int(round(FEATHER_WIDTH)), int(round(length)), 1
    rects = {
        "up":    (d,         0,     w, d),
        "down":  (d + w,     0,     w, d),
        "north": (d,         d,     w, h),
        "south": (2 * d + w, d,     w, h),
        "west":  (0,         d,     d, h),
        "east":  (d + w,     d,     d, h),
    }
    return {n: {"uv": [x0, y0, x0 + fw, y0 + fh], "texture": tex_uuid}
            for n, (x0, y0, fw, fh) in rects.items()}


def main():
    tex_uuid = str(uuid.uuid4())
    with open(TEX, "rb") as fh:
        b64 = base64.b64encode(fh.read()).decode("ascii")

    elements = []
    outliner = []
    for side, wing_name in ((+1, "wing_right"), (-1, "wing_left")):
        sx = side * SHOULDER_X
        members = []
        for name, (ax, ay, az), length, roll in feathers(side):
            px = sx + ax
            py = SHOULDER_Y_JAVA + ay
            pz = SHOULDER_Z_JAVA + az
            origin = [px, 24.0 - py, pz]                       # rotation pivot = attach point
            frm = [px - FEATHER_WIDTH / 2, 24.0 - (py + length), pz - 0.5]
            to = [px + FEATHER_WIDTH / 2, 24.0 - py, pz + 0.5]
            cube_uuid = str(uuid.uuid4())
            members.append(cube_uuid)
            elements.append({
                "name": f"{wing_name}_{name}",
                "box_uv": True,
                "rescale": False,
                "locked": False,
                "light_emission": 0,
                "render_order": "default",
                "from": frm,
                "to": to,
                "autouv": 0,
                "color": 0,
                "origin": origin,
                "rotation": [0.0, 0.0, -math.degrees(roll)],   # y-flip negates Z roll
                "uv_offset": [0, 0],
                "faces": box_uv_faces(length, tex_uuid),
                "type": "cube",
                "uuid": cube_uuid,
            })
        outliner.append({
            "name": wing_name,
            "origin": [sx, 24.0 - SHOULDER_Y_JAVA, SHOULDER_Z_JAVA],
            "rotation": [0, 0, 0],
            "color": 0,
            "uuid": str(uuid.uuid4()),
            "export": True,
            "isOpen": True,
            "locked": False,
            "visibility": True,
            "autouv": 0,
            "children": members,
        })

    model = {
        "meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
        "name": "angel_wings",
        "model_identifier": "angel_wings",
        "modded_entity_version": "1.21",
        "modded_entity_flip_y": True,
        "visible_box": [3, 3, 1],
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "path": TEX,
            "name": "angel_wings.png",
            "folder": "entity/cosmetic",
            "namespace": "ironhold",
            "id": "0",
            "width": 16,
            "height": 16,
            "uv_width": 16,
            "uv_height": 16,
            "particle": False,
            "render_mode": "default",
            "render_sides": "auto",
            "visible": True,
            "internal": True,
            "saved": False,
            "uuid": tex_uuid,
            "source": "data:image/png;base64," + b64,
        }],
    }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as fh:
        json.dump(model, fh, indent=2)
    print("wrote", OUT, "with", len(elements), "feathers")


if __name__ == "__main__":
    main()
