"""
Generate Blockbench-openable model files for the Moon Hopling (v2 rebuild).

This rewrite authors the model DIRECTLY in Bedrock coordinates (Y-up, feet at
y=0) so what you see in Blockbench is the source of truth. UVs are auto-packed
(shelf packer) into a 64x64 atlas; paint_moon_hopling.py reads the resulting
geo.json so the texture can never drift out of alignment with the boxes.

Outputs (under art/blockbench/moon_hopling/):
  - moon_hopling.geo.json   Bedrock geometry (1.12.0)
  - moon_hopling.bbmodel    Native Blockbench project, texture embedded
  - moon_hopling.png        copy of the painted base texture (if it exists)

Run order for a full regen:
  1. python3 scripts/gen_moon_hopling_geo.py     # writes geo.json (+ bbmodel)
  2. python3 scripts/paint_moon_hopling.py       # reads geo.json -> paints PNGs
  3. python3 scripts/gen_moon_hopling_geo.py     # re-embed fresh PNG in bbmodel

Design (matches the reference art):
  - Big chibi head, protruding snout box.
  - Two LONG ears, each 2 segments (base + tip) so they curve up-and-outward;
    tips glow cyan.
  - Compact crouched body, four stubby legs with glowing cuffs.
  - Raised fluffy tail tilted up at the rear.
"""
import base64
import json
import os
import uuid

TEX_W, TEX_H = 64, 64
OUTDIR = 'art/blockbench/moon_hopling'
RES_TEX = 'src/main/resources/assets/ironhold/textures/entity/hopling/moon_hopling.png'
# GeckoLib loads the model straight from resources; keep it in lock-step with the art copy.
RES_GEO = 'src/main/resources/assets/ironhold/geckolib/models/entity/moon_hopling.geo.json'

# name, parent, size(w,h,d), origin(min x,y,z) Bedrock, pivot(x,y,z), rot(deg x,y,z)
# Bedrock: +X right, +Y up, +Z back (front face = -Z, toward camera/north).
PARTS = [
    # ── core ────────────────────────────────────────────────────────────────
    ("body",       None,   (7, 6, 8),  (-3.5, 4, -2),   (0, 4, 2),    (0, 0, 0)),
    ("head",       None,   (10, 9, 9), (-5, 9, -6),     (0, 10, -2),  (0, 0, 0)),
    ("snout",      "head", (4, 3, 2),  (-2, 11, -8),    (0, 12, -6),  (0, 0, 0)),
    # ── ears: base tilts out, tip hooks further out + slightly back ──────────
    ("ear_l_base", "head", (3, 7, 3),  (-5, 18, -2.5),  (-3.5, 18, -1),  (-4, 0, 10)),
    ("ear_l_tip",  "ear_l_base", (3, 6, 3), (-5, 25, -2.5), (-3.5, 25, -1), (8, 0, 18)),
    ("ear_r_base", "head", (3, 7, 3),  (2, 18, -2.5),   (3.5, 18, -1),   (-4, 0, -10)),
    ("ear_r_tip",  "ear_r_base", (3, 6, 3), (2, 25, -2.5), (3.5, 25, -1), (8, 0, -18)),
    # ── tail: raised + tilted up at the rear ─────────────────────────────────
    ("tail",       "body", (5, 5, 5),  (-2.5, 7, 5),    (0, 8, 6),    (-38, 0, 0)),
    # ── legs (front pair under the head, back pair under the body rear) ──────
    ("leg_fl",     None,   (3, 4, 3),  (-4, 0, -2),     (-2.5, 4, -0.5), (0, 0, 0)),
    ("leg_fr",     None,   (3, 4, 3),  (1, 0, -2),      (2.5, 4, -0.5),  (0, 0, 0)),
    ("leg_bl",     None,   (3, 4, 3),  (-4, 0, 3),      (-2.5, 4, 4.5),  (0, 0, 0)),
    ("leg_br",     None,   (3, 4, 3),  (1, 0, 3),       (2.5, 4, 4.5),   (0, 0, 0)),
]


def footprint(size):
    """Box-UV unwrap rectangle: width 2*(w+d), height d+h."""
    w, h, d = size
    return (2 * (w + d), d + h)


def pack_uvs(parts):
    """Simple shelf packer (sort by height desc). Returns {name: (u, v)}."""
    items = [(name, footprint(size)) for (name, _p, size, *_rest) in parts]
    order = sorted(range(len(items)), key=lambda i: -items[i][1][1])
    uvs = {}
    sx = sy = sh = 0
    for i in order:
        name, (w, h) = items[i]
        if sx + w > TEX_W:
            sy += sh
            sx = 0
            sh = 0
        uvs[name] = (sx, sy)
        sx += w
        sh = max(sh, h)
    used_h = sy + sh
    assert used_h <= TEX_H, f"UV atlas overflow: needs {used_h}px tall, have {TEX_H}"
    return uvs


UVS = pack_uvs(PARTS)


# ── Bedrock .geo.json ────────────────────────────────────────────────────────
def build_bones():
    bones = []
    for name, parent, size, origin, pivot, rot in PARTS:
        bone = {"name": name, "pivot": list(pivot)}
        if parent:
            bone["parent"] = parent
        if any(rot):
            bone["rotation"] = list(rot)
        bone["cubes"] = [{
            "origin": list(origin),
            "size": list(size),
            "uv": list(UVS[name]),
        }]
        bones.append(bone)
    return bones


geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.moon_hopling",
            "texture_width": TEX_W,
            "texture_height": TEX_H,
            "visible_bounds_width": 3,
            "visible_bounds_height": 3,
            "visible_bounds_offset": [0, 1.25, 0],
        },
        "bones": build_bones(),
    }],
}

os.makedirs(OUTDIR, exist_ok=True)
geo_path = os.path.join(OUTDIR, 'moon_hopling.geo.json')
with open(geo_path, 'w') as f:
    json.dump(geo, f, indent=2)
print('wrote', geo_path)
os.makedirs(os.path.dirname(RES_GEO), exist_ok=True)
with open(RES_GEO, 'w') as f:
    json.dump(geo, f, indent=2)
print('wrote', RES_GEO)
print('UV atlas:', {k: UVS[k] for k in UVS})


# ── Native .bbmodel (texture embedded if the PNG already exists) ──────────────
def uid():
    return str(uuid.uuid4())


b64 = None
if os.path.exists(RES_TEX):
    with open(RES_TEX, 'rb') as f:
        b64 = base64.b64encode(f.read()).decode('ascii')
    # keep a convenience copy beside the model
    import shutil
    shutil.copyfile(RES_TEX, os.path.join(OUTDIR, 'moon_hopling.png'))
    print('embedded texture from', RES_TEX)
else:
    print('NOTE: no texture yet at', RES_TEX, '- bbmodel will be untextured until paint runs')

elements = []
group_by_name = {}
for name, parent, size, origin, pivot, rot in PARTS:
    w, h, d = size
    frm = list(origin)
    to = [origin[0] + w, origin[1] + h, origin[2] + d]
    eid = uid()
    elements.append({
        "name": name,
        "box_uv": True,
        "rescale": False,
        "locked": False,
        "render_order": "default",
        "from": frm,
        "to": to,
        "uv_offset": list(UVS[name]),
        "origin": list(pivot),
        "rotation": [0, 0, 0],  # rotation lives on the parent group
        "uuid": eid,
    })
    group_by_name[name] = {
        "name": name,
        "origin": list(pivot),
        "rotation": list(rot),
        "uuid": uid(),
        "export": True,
        "isOpen": True,
        "visibility": True,
        "children": [eid],
    }

roots = []
for name, parent, *_ in PARTS:
    grp = group_by_name[name]
    if parent:
        group_by_name[parent]["children"].append(grp)
    else:
        roots.append(grp)

textures = []
if b64 is not None:
    # HD texture: PNG is rendered at TEX_SCALE x the UV grid (e.g. 128px PNG over a
    # 64-unit UV space = 2px/unit). Blockbench needs width/height = pixel size but
    # uv_width/uv_height = the model's UV grid so box-UV coords still line up.
    import struct
    with open(RES_TEX, 'rb') as _pf:
        _hdr = _pf.read(24)
    png_w, png_h = struct.unpack('>II', _hdr[16:24]) if _hdr[:8] == b'\x89PNG\r\n\x1a\n' else (TEX_W, TEX_H)
    textures.append({
        "path": "", "name": "moon_hopling.png", "folder": "", "namespace": "",
        "id": "0", "width": png_w, "height": png_h, "uv_width": TEX_W, "uv_height": TEX_H,
        "particle": False, "render_mode": "default", "render_sides": "auto",
        "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
        "frame_interpolate": False, "visible": True, "internal": True, "saved": False,
        "uuid": uid(), "source": "data:image/png;base64," + b64,
    })

bbmodel = {
    "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
    "name": "moon_hopling",
    "model_identifier": "",
    "visible_box": [3, 3, 0.5],
    "variable_placeholders": "",
    "variable_placeholder_buttons": [],
    "timeline_setups": [],
    "resolution": {"width": TEX_W, "height": TEX_H},
    "elements": elements,
    "outliner": roots,
    "textures": textures,
}

bb_path = os.path.join(OUTDIR, 'moon_hopling.bbmodel')
with open(bb_path, 'w') as f:
    json.dump(bbmodel, f, indent=2)
print('wrote', bb_path)
