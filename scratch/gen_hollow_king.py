#!/usr/bin/env python3
"""Generate the Hollow King boss model: GeckoLib .geo.json + Blockbench .bbmodel + blank texture.

Geometry is authored once as a bone tree of cubes (Minecraft/Bedrock coords: +X right,
+Y up, +Z back; the mob faces -Z). Box UVs are shelf-packed so nothing overlaps, giving a
clean blank sheet to paint in Blockbench. Texture is left transparent (geometry-only pass).
"""
import json, math, uuid, os
from PIL import Image

# ---------------------------------------------------------------------------
# Model definition.  Each bone: name, parent, pivot, optional rotation.
# Each cube: from-corner origin + size; optional own pivot+rotation for tilt.
# ---------------------------------------------------------------------------
def cube(origin, size, pivot=None, rot=None):
    c = {"origin": [float(x) for x in origin], "size": [float(x) for x in size]}
    if pivot is not None: c["pivot"] = [float(x) for x in pivot]
    if rot is not None:   c["rot"]   = [float(x) for x in rot]
    return c

BONES = [
    # ---- waist / root --------------------------------------------------
    {"name": "body", "parent": None, "pivot": [0, 14, 0], "cubes": [
        cube([-6, 13, -3], [12, 4, 6]),        # belt / hips
        cube([-5.5, 16.5, -3], [11, 6, 6]),    # lower torso
        cube([-6.5, 22, -3.5], [13, 10, 7]),   # cuirass / chest plate
        cube([-4, 31, -3.5], [8, 2, 7]),       # gorget collar
        cube([-2, 24, -4.3], [4, 4, 1]),       # chest skull medallion (front)
        cube([-1.5, 22, 3], [3, 9, 1.4]),      # spine ridge (back)
    ]},

    # ---- skull ----------------------------------------------------------
    {"name": "head", "parent": "body", "pivot": [0, 32, 0], "cubes": [
        cube([-4, 32, -4], [8, 8, 8]),          # cranium
        cube([-4.3, 37, -4.6], [8.6, 2, 1.6]),  # brow ridge
        cube([-3, 35, -4.4], [2, 1.6, 1]),      # right eye socket (emissive)
        cube([1, 35, -4.4], [2, 1.6, 1]),       # left eye socket  (emissive)
        cube([-3.6, 33, -4.3], [1.4, 1.4, 1]),  # right cheekbone
        cube([2.2, 33, -4.3], [1.4, 1.4, 1]),   # left cheekbone
        cube([-1, 33.5, -4.7], [2, 2.2, 1]),    # nasal cavity
    ]},
    {"name": "jaw", "parent": "head", "pivot": [0, 33, -3], "cubes": [
        cube([-3.5, 30.4, -4.4], [7, 2.6, 4.6]),
    ]},

    # ---- crown ----------------------------------------------------------
    {"name": "crown", "parent": "head", "pivot": [0, 40, 0], "cubes": [
        cube([-4.3, 40, -4.3], [8.6, 2.2, 8.6]),     # band
        cube([-1.2, 40.4, -4.7], [2.4, 1.6, 0.6]),   # front gem
        # spikes around the ring
        cube([-0.7, 42, -4.3], [1.4, 5, 1.4]),       # front centre (tall)
        cube([-3.2, 42, -4.2], [1.2, 3.5, 1.2]),     # front-left
        cube([2, 42, -4.2], [1.2, 3.5, 1.2]),        # front-right
        cube([-4.3, 42, -1], [1.2, 4, 1.2]),         # left-front side
        cube([-4.3, 42, 2], [1.2, 3.5, 1.2]),        # left-back side
        cube([3.1, 42, -1], [1.2, 4, 1.2]),          # right-front side
        cube([3.1, 42, 2], [1.2, 3.5, 1.2]),         # right-back side
        cube([-0.6, 42, 3.1], [1.2, 3, 1.2]),        # back centre
    ]},

    # ---- fur mantle / cape ---------------------------------------------
    {"name": "mantle", "parent": "body", "pivot": [0, 31, 0], "cubes": [
        cube([-8, 30, -4.6], [16, 4.5, 9.2]),   # chunky fur collar around shoulders
    ]},
    {"name": "cape", "parent": "body", "pivot": [0, 32, 4], "cubes": [
        cube([-7, 14, 3.4], [14, 18, 1.5]),     # main cape sheet (behind torso)
        # tattered hanging strips
        cube([-7, 2, 3.5], [3, 12, 1.2]),
        cube([-3.5, 0, 3.5], [3, 14, 1.2]),
        cube([0.6, 1, 3.5], [3, 13, 1.2]),
        cube([4, 3, 3.5], [3, 11, 1.2]),
    ]},

    # ---- pauldrons ------------------------------------------------------
    {"name": "right_pauldron", "parent": "body", "pivot": [-7, 31, 0], "cubes": [
        cube([-10.4, 28, -4], [5, 5, 8]),
        cube([-10.6, 32.2, -3.4], [3, 3, 2.6], pivot=[-9, 33, -2], rot=[0, 0, 20]),  # spike
    ]},
    {"name": "left_pauldron", "parent": "body", "pivot": [7, 31, 0], "cubes": [
        cube([5.4, 28, -4], [5, 5, 8]),
        cube([7.6, 32.2, -3.4], [3, 3, 2.6], pivot=[9, 33, -2], rot=[0, 0, -20]),
    ]},

    # ---- arms -----------------------------------------------------------
    {"name": "right_arm", "parent": "body", "pivot": [-7.5, 31, 0], "rot": [0, 0, 4], "cubes": [
        cube([-10.6, 20, -2.6], [4.2, 11, 5]),     # upper arm armour
        cube([-10.8, 14.5, -2.8], [4.6, 6, 5.4]),  # forearm bracer
        cube([-10.6, 11, -2.6], [4.2, 3.5, 4.4]),  # gauntlet / hand
    ]},
    {"name": "left_arm", "parent": "body", "pivot": [7.5, 31, 0], "rot": [0, 0, -4], "cubes": [
        cube([6.4, 20, -2.6], [4.2, 11, 5]),
        cube([6.2, 14.5, -2.8], [4.6, 6, 5.4]),
        cube([6.4, 11, -2.6], [4.2, 3.5, 4.4]),
    ]},

    # ---- great sword (point-down in right hand) -------------------------
    {"name": "sword", "parent": "right_arm", "pivot": [-8.6, 12, 0], "cubes": [
        cube([-9.3, 9.5, -1], [1.6, 4, 1.6]),       # grip
        cube([-9.6, 8.2, -1.3], [2.2, 1.4, 2.2]),   # pommel
        cube([-11.4, 13.2, -1], [5.6, 1.6, 1.6]),   # crossguard
        cube([-9, -1, -0.7], [1.2, 14, 1.2]),       # blade (upper)
        cube([-8.85, -7, -0.55], [0.9, 6, 0.9]),    # blade (taper to tip)
    ]},

    # ---- legs -----------------------------------------------------------
    {"name": "right_leg", "parent": "body", "pivot": [-3, 14, 0], "cubes": [
        cube([-6, 4, -2.5], [5, 10, 5]),        # greave
        cube([-6.2, 9, -3.2], [5.4, 3, 1.5]),   # knee plate (front)
        cube([-6.5, 0, -3], [6, 4, 6]),         # sabaton / boot
    ]},
    {"name": "left_leg", "parent": "body", "pivot": [3, 14, 0], "cubes": [
        cube([1, 4, -2.5], [5, 10, 5]),
        cube([0.8, 9, -3.2], [5.4, 3, 1.5]),
        cube([0.5, 0, -3], [6, 4, 6]),
    ]},
]

# ---------------------------------------------------------------------------
# Box-UV shelf packer.  Footprint of a w*h*d cube on a box-uv atlas is
# (2*d + 2*w) wide by (d + h) tall.
# ---------------------------------------------------------------------------
SHEET_W = 128
PAD = 1

def footprint(size):
    w, h, d = size
    return math.ceil(2 * d + 2 * w), math.ceil(d + h)

# collect every cube in order
all_cubes = [(b, c) for b in BONES for c in b["cubes"]]

# widen sheet until everything packs in a sensible height
def pack(sheet_w):
    x = y = shelf_h = 0
    placed = []
    for _, c in all_cubes:
        fw, fh = footprint(c["size"])
        if x + fw + PAD > sheet_w:
            x = 0
            y += shelf_h + PAD
            shelf_h = 0
        placed.append((x, y))
        x += fw + PAD
        shelf_h = max(shelf_h, fh)
    return placed, y + shelf_h + PAD

for SHEET_W in (128, 256, 512):
    uvs, total_h = pack(SHEET_W)
    if total_h <= SHEET_W:
        break
SHEET_H = 1 << (total_h - 1).bit_length()      # round up to power of two
SHEET_H = max(SHEET_H, SHEET_W // 2)
print(f"atlas {SHEET_W}x{SHEET_H}, {len(all_cubes)} cubes")

for (_, c), uv in zip(all_cubes, uvs):
    c["uv"] = [uv[0], uv[1]]

# ---------------------------------------------------------------------------
# Emit GeckoLib .geo.json (Bedrock 1.12.0 box-uv geometry).
# ---------------------------------------------------------------------------
def geo_cube(c):
    g = {"origin": c["origin"], "size": c["size"], "uv": c["uv"]}
    if "pivot" in c: g["pivot"] = c["pivot"]
    if "rot" in c:   g["rotation"] = c["rot"]
    return g

geo_bones = []
for b in BONES:
    gb = {"name": b["name"], "pivot": [float(x) for x in b["pivot"]]}
    if b.get("parent"): gb["parent"] = b["parent"]
    if b.get("rot"):    gb["rotation"] = [float(x) for x in b["rot"]]
    gb["cubes"] = [geo_cube(c) for c in b["cubes"]]
    geo_bones.append(gb)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.hollow_king",
            "texture_width": SHEET_W,
            "texture_height": SHEET_H,
            "visible_bounds_width": 6,
            "visible_bounds_height": 7,
            "visible_bounds_offset": [0, 2.5, 0],
        },
        "bones": geo_bones,
    }],
}

# ---------------------------------------------------------------------------
# Emit Blockbench .bbmodel (format 5.0, bedrock, box_uv).
# ---------------------------------------------------------------------------
def uid():
    return str(uuid.uuid4())

def box_faces(c):
    u, v = c["uv"]; w, h, d = c["size"]
    R = lambda *a: [round(x, 2) for x in a]
    return {
        "north": {"uv": R(u + d, v + d, u + d + w, v + d + h), "texture": 0},
        "east":  {"uv": R(u, v + d, u + d, v + d + h), "texture": 0},
        "south": {"uv": R(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h), "texture": 0},
        "west":  {"uv": R(u + d + w, v + d, u + 2 * d + w, v + d + h), "texture": 0},
        "up":    {"uv": R(u + d + w, v + d, u + d, v), "texture": 0},
        "down":  {"uv": R(u + 2 * d + w, v, u + d + w, v + d), "texture": 0},
    }

elements = []
outliner = []
for ci, b in enumerate(BONES):
    child_uuids = []
    for c in b["cubes"]:
        eid = uid()
        frm = c["origin"]
        to = [frm[i] + c["size"][i] for i in range(3)]
        el = {
            "name": b["name"], "box_uv": True, "rescale": False, "locked": False,
            "render_order": "default", "allow_mirror_modeling": True,
            "from": frm, "to": to, "autouv": 0, "color": ci % 8,
            "origin": [float(x) for x in c.get("pivot", b["pivot"])],
            "uv_offset": [c["uv"][0], c["uv"][1]],
            "faces": box_faces(c), "type": "cube", "uuid": eid,
        }
        if "rot" in c: el["rotation"] = [float(x) for x in c["rot"]]
        elements.append(el)
        child_uuids.append(eid)
    grp = {
        "name": b["name"], "origin": [float(x) for x in b["pivot"]],
        "color": ci % 8, "uuid": uid(), "export": True, "mirror_uv": False,
        "isOpen": False, "locked": False, "visibility": True, "autouv": 0,
        "children": child_uuids,
    }
    if b.get("rot"): grp["rotation"] = [float(x) for x in b["rot"]]
    outliner.append(grp)

# build parent nesting: move child groups under their parent's children list
by_name = {g["name"]: g for g in outliner}
roots = []
for b in BONES:
    g = by_name[b["name"]]
    if b.get("parent") and b["parent"] in by_name:
        by_name[b["parent"]]["children"].append(g)
    else:
        roots.append(g)

blank = Image.new("RGBA", (SHEET_W, SHEET_H), (0, 0, 0, 0))
ART_DIR = "art/blockbench/hollow_king"
os.makedirs(ART_DIR, exist_ok=True)
png_path = os.path.join(ART_DIR, "hollow_king.png")
blank.save(png_path)

import base64
with open(png_path, "rb") as f:
    data_url = "data:image/png;base64," + base64.b64encode(f.read()).decode()

texture = {
    "name": "hollow_king.png", "relative_path": "hollow_king.png", "folder": "",
    "namespace": "", "id": "0", "width": SHEET_W, "height": SHEET_H,
    "uv_width": SHEET_W, "uv_height": SHEET_H, "particle": False,
    "use_as_default": False, "render_mode": "default", "render_sides": "auto",
    "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
    "frame_interpolate": False, "visible": True, "internal": True, "saved": False,
    "uuid": uid(), "source": data_url,
}

bb = {
    "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": True},
    "name": "hollow_king.geo", "model_identifier": "",
    "visible_box": [6, 7, 1], "variable_placeholders": "",
    "variable_placeholder_buttons": [], "bedrock_animation_mode": "entity",
    "resolution": {"width": SHEET_W, "height": SHEET_H},
    "elements": elements, "outliner": roots, "textures": [texture],
}

os.makedirs("src/main/resources/assets/ironhold/geckolib/models/entity", exist_ok=True)
os.makedirs("src/main/resources/assets/ironhold/textures/entity", exist_ok=True)

with open(os.path.join(ART_DIR, "hollow_king.geo.json"), "w") as f:
    json.dump(geo, f, indent="\t")
with open(os.path.join(ART_DIR, "hollow_king.geo.bbmodel"), "w") as f:
    json.dump(bb, f, indent="\t")
with open("src/main/resources/assets/ironhold/geckolib/models/entity/hollow_king.geo.json", "w") as f:
    json.dump(geo, f, indent="\t")
blank.save("src/main/resources/assets/ironhold/textures/entity/hollow_king.png")

print("wrote:")
print(" ", os.path.join(ART_DIR, "hollow_king.geo.bbmodel"))
print(" ", os.path.join(ART_DIR, "hollow_king.geo.json"))
print(" ", os.path.join(ART_DIR, "hollow_king.png"))
print("  src/.../geckolib/models/entity/hollow_king.geo.json")
print("  src/.../textures/entity/hollow_king.png")
