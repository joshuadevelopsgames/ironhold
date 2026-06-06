#!/usr/bin/env python3
"""Generate a stone golem .bbmodel whose face faithfully reads as a vanilla
iron golem (heavy head, central vertical nose, two flanking glowing eyes,
stern brow ridge) on a chunky stone body. box_uv + a procedural cracked-stone
texture with two orange glow patches for the eyes."""
import json, base64, io, math
from PIL import Image

OUT = "stone_golem_iron_face"
TEX_W = TEX_H = 128

# ---------------------------------------------------------------- UUIDs
_ctr = [0]
def uid():
    _ctr[0] += 1
    h = "%012x" % _ctr[0]
    return "00000000-0000-4000-8000-%s" % h

# ---------------------------------------------------------------- texture
def seeded(x, y, s=0):
    # cheap deterministic hash noise in [0,1)
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0

def stone_pixel(x, y):
    # blotchy grey stone: low-freq base + high-freq grain
    base = 0.0
    base += seeded(x // 16, y // 16, 1) * 0.5
    base += seeded(x // 6,  y // 6,  2) * 0.3
    base += seeded(x,       y,       3) * 0.2
    v = 96 + base * 70                      # ~96..166 grey
    # dark cracks
    if seeded(x, y, 7) > 0.93:
        v -= 55
    # subtle moss (green tint) in low patches
    moss = seeded(x // 5, y // 5, 9)
    g_boost = 0
    if moss > 0.86 and seeded(x, y, 11) > 0.4:
        g_boost = 28
    v = max(40, min(200, v))
    r = int(v)
    g = int(min(210, v + g_boost))
    b = int(max(35, v - g_boost * 0.4))
    return (r, g, b, 255)

img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
px = img.load()
for y in range(TEX_H):
    for x in range(TEX_W):
        px[x, y] = stone_pixel(x, y)

# two orange "glowing eye" patches (bright amber with hot core)
def stamp_glow(ox, oy, w, h):
    cx, cy = ox + w / 2.0, oy + h / 2.0
    for y in range(oy, oy + h):
        for x in range(ox, ox + w):
            d = math.hypot(x - cx, y - cy) / (w / 2.0)
            t = max(0.0, 1.0 - d)
            r = int(180 + 75 * t)
            g = int(90 + 110 * t)
            b = int(10 + 30 * t)
            px[x, y] = (min(255, r), min(255, g), min(255, b), 255)

EYE_R_OFF = (90, 0)
EYE_L_OFF = (108, 0)
stamp_glow(EYE_R_OFF[0], EYE_R_OFF[1], 16, 16)
stamp_glow(EYE_L_OFF[0], EYE_L_OFF[1], 16, 16)

img.save("%s.png" % OUT)
buf = io.BytesIO(); img.save(buf, "PNG")
data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

# ---------------------------------------------------------------- geometry
# Blockbench world space, Y up, golem stands on y=0.  Proportions follow the
# vanilla IronGolemModel (long arms to the ground, thick legs, blocky head with
# protruding vertical nose), scaled up & chunkified into stone.
elements = []  # (name, from, to, uv_offset)
def cube(name, f, t, uvoff=(0, 0)):
    elements.append((name, f, t, uvoff))

# --- legs (thick stone columns + slab feet) ---
cube("right_leg",  [-7, 3, -3], [-1, 16, 3])
cube("right_foot", [-8, 0, -4], [0, 4, 5])
cube("left_leg",   [1, 3, -3],  [7, 16, 3])
cube("left_foot",  [0, 0, -4],  [8, 4, 5])

# --- body / torso ---
cube("pelvis",      [-6, 14, -4], [6, 18, 4])
cube("torso",       [-9, 16, -6], [9, 31, 5])
cube("chest_plate", [-7, 23, -7], [7, 31, -5])   # proud stone chest
cube("back_slab",   [-6, 19, 5],  [6, 30, 7])     # boulder on the back
# small rounded shoulder caps (NOT big shelves) so the arms hang naturally
cube("shoulder_r",  [-16, 29, -4.5], [-9, 33, 4.5])
cube("shoulder_l",  [9, 29, -4.5],   [16, 33, 4.5])

# --- arms (long, iron-golem style, reaching near the ground) ---
cube("right_arm",  [-15, 7, -3.5], [-9, 31, 3.5])
cube("right_fist", [-16, 3, -4.5], [-8, 9, 4.5])
cube("left_arm",   [9, 7, -3.5],   [15, 31, 3.5])
cube("left_fist",  [8, 3, -4.5],   [16, 9, 4.5])

# --- HEAD: the iron-golem face -----------------------------------------
cube("head", [-5, 31, -5], [5, 41, 4])            # main head 10x10x9
cube("jaw",  [-4, 29, -5], [4, 32, 2.5])          # heavy lower jaw
# heavy brow ridge across the forehead — protrudes 1.4 to overhang the eyes
cube("brow", [-5.2, 37.5, -6.4], [5.2, 40, -4.5])
# THE signature iron-golem nose: a long central bar from brow to chin that
# protrudes a full 3px past the face so it reads even head-on
cube("nose", [-1.5, 30, -8], [1.5, 38, -4.5])
# glowing orange eyes, set tight against the nose and shadowed under the brow
cube("eye_right", [-4, 35, -5.4], [-1.5, 37.5, -5], EYE_R_OFF)
cube("eye_left",  [1.5, 35, -5.4], [4, 37.5, -5],   EYE_L_OFF)

# ---------------------------------------------------------------- build json
def mk_element(name, f, t, uvoff):
    return {
        "name": name, "box_uv": True, "rescale": False, "locked": False,
        "render_order": "default", "allow_mirror_modeling": True,
        "from": f, "to": t, "autouv": 0, "color": 0,
        "origin": list(f), "rotation": [0, 0, 0], "type": "cube",
        "uuid": uid(), "uv_offset": list(uvoff),
    }

el_objs = {name: mk_element(name, f, t, uvoff) for (name, f, t, uvoff) in elements}

def group(name, origin, child_names):
    return {
        "name": name, "origin": origin, "rotation": [0, 0, 0], "uuid": uid(),
        "export": True, "isOpen": True, "visibility": True,
        "children": [el_objs[c]["uuid"] for c in child_names],
    }

g_body = group("body", [0, 16, 0],
               ["pelvis", "torso", "chest_plate", "back_slab", "shoulder_r", "shoulder_l"])
g_head = group("head", [0, 31, -1],
               ["head", "jaw", "brow", "nose", "eye_right", "eye_left"])
g_rarm = group("right_arm", [-11, 31, 0], ["right_arm", "right_fist"])
g_larm = group("left_arm",  [11, 31, 0],  ["left_arm", "left_fist"])
g_rleg = group("right_leg", [-4, 16, 0],  ["right_leg", "right_foot"])
g_lleg = group("left_leg",  [4, 16, 0],   ["left_leg", "left_foot"])

root = {
    "name": "root", "origin": [0, 0, 0], "rotation": [0, 0, 0], "uuid": uid(),
    "export": True, "isOpen": True, "visibility": True,
    "children": [g_body, g_head, g_rarm, g_larm, g_rleg, g_lleg],
}

model = {
    "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
    "name": OUT,
    "model_identifier": "geometry.ironhold:%s" % OUT,
    "visible_box": [6, 5.5, 1],
    "variable_placeholders": "",
    "variable_placeholder_buttons": [],
    "timeline_setups": [],
    "resolution": {"width": TEX_W, "height": TEX_H},
    "elements": list(el_objs.values()),
    "outliner": [root],
    "textures": [{
        "path": "%s.png" % OUT, "name": "%s.png" % OUT, "folder": "",
        "namespace": "", "id": "0", "width": TEX_W, "height": TEX_H,
        "uv_width": TEX_W, "uv_height": TEX_H, "particle": False,
        "render_mode": "default", "render_sides": "auto", "frame_time": 1,
        "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
        "visible": True, "internal": True, "saved": True,
        "uuid": uid(), "source": data_url,
    }],
}

with open("%s.bbmodel" % OUT, "w") as fh:
    json.dump(model, fh, indent=2)

print("wrote %s.bbmodel (%d cubes) and %s.png" % (OUT, len(elements), OUT))
