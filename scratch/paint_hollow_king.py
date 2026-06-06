#!/usr/bin/env python3
"""Paint a texture for the Hollow King onto its packed box-UV atlas.

Reads the generated geo.json (each cube already carries its uv offset + size),
then fills every cube's 6 box-UV face rectangles with a per-material colour,
directional shading, edge darkening and a little noise. Emissive parts (eye
sockets, blade, crown gem) are painted flat-bright so they read as glowing.
"""
import json, random
from PIL import Image

random.seed(7)
geo = json.load(open("src/main/resources/assets/ironhold/geckolib/models/entity/hollow_king.geo.json"))
desc = geo["minecraft:geometry"][0]["description"]
W, H = desc["texture_width"], desc["texture_height"]
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

# ---- palette: name -> (base rgb, emissive?) -------------------------------
PAL = {
    "bone":      ((201, 194, 170), False),
    "boneshadow":((150, 142, 118), False),
    "gold":      ((196, 154, 58), False),
    "steel":     ((58, 60, 70), False),     # blackened plate
    "steeldark": ((40, 41, 48), False),
    "robe":      ((92, 26, 30), False),     # deep crimson
    "fur":       ((34, 31, 37), False),
    "glow":      ((128, 232, 246), True),   # cursed cyan
    "gem":       ((70, 196, 224), True),
}

# ---- material assignment, in authored cube order per bone -----------------
MAT = {
    "body":          ["steel", "robe", "steel", "steel", "gold", "steel"],
    "head":          ["bone", "bone", "glow", "glow", "bone", "bone", "boneshadow"],
    "jaw":           ["bone"],
    "crown":         ["gold", "gem"] + ["gold"] * 8,
    "mantle":        ["fur"],
    "cape":          ["robe", "robe", "robe", "robe", "robe"],
    "right_pauldron":["steel", "steel"],
    "left_pauldron": ["steel", "steel"],
    "right_arm":     ["steel", "steel", "steeldark"],
    "left_arm":      ["steel", "steel", "steeldark"],
    "sword":         ["steeldark", "gold", "gold", "glow", "glow"],
    "right_leg":     ["steel", "gold", "steeldark"],
    "left_leg":      ["steel", "gold", "steeldark"],
}

# directional brightness per face (sunlight from upper-front)
FACE_SHADE = {"up": 1.18, "north": 1.06, "east": 0.92, "west": 0.86, "south": 0.80, "down": 0.62}

def clamp(v): return max(0, min(255, int(v)))

def fill_face(uv, base, emissive, shade):
    x1, y1, x2, y2 = uv
    x0, x1_ = sorted((x1, x2)); y0, y1_ = sorted((y1, y2))
    ix0, iy0, ix1, iy1 = int(round(x0)), int(round(y0)), int(round(x1_)), int(round(y1_))
    if ix1 <= ix0 or iy1 <= iy0:
        return
    r, g, b = base
    for y in range(iy0, iy1):
        for x in range(ix0, ix1):
            if not (0 <= x < W and 0 <= y < H):
                continue
            if emissive:
                n = random.randint(-10, 6)
                px[x, y] = (clamp(r + n), clamp(g + n // 2), clamp(b + n // 3), 255)
            else:
                edge = (x == ix0 or x == ix1 - 1 or y == iy0 or y == iy1 - 1)
                f = shade * (0.80 if edge else 1.0)
                n = random.randint(-9, 9)
                px[x, y] = (clamp(r * f + n), clamp(g * f + n), clamp(b * f + n), 255)

def box_faces(u, v, w, h, d):
    return {
        "north": (u + d, v + d, u + d + w, v + d + h),
        "east":  (u, v + d, u + d, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        "west":  (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "up":    (u + d + w, v + d, u + d, v),
        "down":  (u + 2 * d + w, v, u + d + w, v + d),
    }

for bone in geo["minecraft:geometry"][0]["bones"]:
    mats = MAT.get(bone["name"], ["steel"] * len(bone.get("cubes", [])))
    for i, c in enumerate(bone.get("cubes", [])):
        mat = mats[i] if i < len(mats) else mats[-1]
        base, emissive = PAL[mat]
        u, v = c["uv"]; w, h, d = c["size"]
        for fname, uv in box_faces(u, v, w, h, d).items():
            fill_face(uv, base, emissive, FACE_SHADE[fname])

out_art = "art/blockbench/hollow_king/hollow_king.png"
out_res = "src/main/resources/assets/ironhold/textures/entity/hollow_king.png"
img.save(out_art)
img.save(out_res)
print(f"painted {W}x{H} -> {out_art}, {out_res}")
