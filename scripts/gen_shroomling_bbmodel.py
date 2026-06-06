#!/usr/bin/env python3
"""Synthesize a Blockbench .bbmodel from ShroomlingModel.java + the entity texture.

The Shroomling model is authored procedurally in Java (vanilla EntityModel,
not a Blockbench export), so there is no native .bbmodel. This rebuilds one so
the model+texture can be inspected in Blockbench.

Conversion notes:
  * Minecraft entity models are mirrored about y=24 at render time. Blockbench's
    "Modded Entity" format stores cubes in that same y-up space, so:
        bb_origin_y      = 24 - java_pivot_y
        bb_from_y        = 24 - (java_pivot_y + box_oy + box_h)
        bb_to_y          = 24 - (java_pivot_y + box_oy)
    X/Z are unchanged (global = pivot + box offset).
  * `cap` is a child of `body`, so its absolute pivot = body pivot + cap offset.
  * Box-UV rectangles match gen_shroomling_textures.box_faces() exactly.
"""
import base64
import json
import os
import uuid

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/ironhold/textures/entity/shroomling/shroomling.png")
OUT = "/tmp/shroomling_run/shroomling.bbmodel"

# ── cubes straight from ShroomlingModel.java ───────────────────────────────
# name: (abs_pivot xyz, box (ox,oy,oz,w,h,d), uv_offset (u,v))
CUBES = {
    "body":      ((0.0, 22.0, 0.0), (-3.0, -4.0, -3.0, 6.0, 4.0, 6.0),  (0, 18)),
    "cap":       ((0.0, 18.0, 0.0), (-6.0, -5.0, -6.0, 12.0, 5.0, 12.0), (0, 0)),   # body(0,22,0)+cap(0,-4,0)
    "leg_left":  ((-2.0, 22.0, 0.0), (-1.0, 0.0, -1.0, 2.0, 2.0, 2.0),  (28, 18)),
    "leg_right": ((2.0, 22.0, 0.0), (-1.0, 0.0, -1.0, 2.0, 2.0, 2.0),  (28, 18)),
}


def to_bb(pivot, box):
    px, py, pz = pivot
    ox, oy, oz, w, h, d = box
    origin = [px, 24.0 - py, pz]
    frm = [px + ox, 24.0 - (py + oy + h), pz + oz]
    to = [px + ox + w, 24.0 - (py + oy), pz + oz + d]
    return origin, frm, to


def box_uv_faces(uv, dims, tex_uuid):
    u, v = uv
    w, h, d = (int(round(x)) for x in dims)
    # rectangles identical to gen_shroomling_textures.box_faces(); [x0,y0,x1,y1]
    rects = {
        "up":    (u + d,         v,       w, d),
        "down":  (u + d + w,     v,       w, d),
        "north": (u + d,         v + d,   w, h),  # front (eyes)
        "south": (u + 2 * d + w, v + d,   w, h),  # back
        "west":  (u,             v + d,   d, h),  # -X
        "east":  (u + d + w,     v + d,   d, h),  # +X
    }
    faces = {}
    for name, (x0, y0, fw, fh) in rects.items():
        faces[name] = {"uv": [x0, y0, x0 + fw, y0 + fh], "texture": tex_uuid}
    return faces


def main():
    tex_uuid = str(uuid.uuid4())
    with open(TEX, "rb") as fh:
        b64 = base64.b64encode(fh.read()).decode("ascii")

    elements = []
    nodes = {}
    for name, (pivot, box, uv) in CUBES.items():
        cube_uuid = str(uuid.uuid4())
        nodes[name] = cube_uuid
        origin, frm, to = to_bb(pivot, box)
        elements.append({
            "name": name,
            "box_uv": True,
            "rescale": False,
            "locked": False,
            "light_emission": 0,
            "render_order": "default",
            "allow_mirror_modeling": True,
            "from": frm,
            "to": to,
            "autouv": 0,
            "color": 0,
            "origin": origin,
            "uv_offset": list(uv),
            "faces": box_uv_faces(uv, box[3:], tex_uuid),
            "type": "cube",
            "uuid": cube_uuid,
        })

    def group(name, members):
        po, _, _ = to_bb(CUBES[name][0], CUBES[name][1])
        return {
            "name": name,
            "origin": po,
            "rotation": [0, 0, 0],
            "color": 0,
            "uuid": str(uuid.uuid4()),
            "export": True,
            "mirror_uv": False,
            "isOpen": True,
            "locked": False,
            "visibility": True,
            "autouv": 0,
            "children": members,
        }

    cap_group = group("cap", [nodes["cap"]])
    body_group = group("body", [nodes["body"], cap_group])
    legl_group = group("leg_left", [nodes["leg_left"]])
    legr_group = group("leg_right", [nodes["leg_right"]])
    outliner = [body_group, legl_group, legr_group]

    model = {
        "meta": {
            "format_version": "4.10",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": "shroomling",
        "model_identifier": "shroomling",
        "modded_entity_version": "1.21",
        "modded_entity_flip_y": True,
        "visible_box": [2, 2, 0.5],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "outliner": outliner,
        "textures": [{
            "path": TEX,
            "name": "shroomling.png",
            "folder": "entity/shroomling",
            "namespace": "ironhold",
            "id": "0",
            "width": 64,
            "height": 64,
            "uv_width": 64,
            "uv_height": 64,
            "particle": False,
            "use_as_default": False,
            "layers_enabled": False,
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
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
    print("wrote", OUT)
    for e in elements:
        print(f"  {e['name']:10} from {e['from']} to {e['to']} origin {e['origin']} uv {e['uv_offset']}")


if __name__ == "__main__":
    main()
