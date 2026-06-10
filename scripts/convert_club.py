#!/usr/bin/env python3
"""One-off: convert the "Normal Club" GeckoLib bbmodel into Ironhold's club assets.

Reproduces the validated box-UV bbmodel -> geckolib .geo.json transform documented in
memory (reproduces arcane_scepter 0-diff) and decodes the embedded texture.
"""
import base64
import json
from pathlib import Path

SRC = Path("/Users/joshua/Downloads/Normal Club- COMPLETED (geko).bbmodel")
ROOT = Path("/Users/joshua/ironhold/src/main/resources/assets/ironhold")
GEO_OUT = ROOT / "geckolib/models/item/club.geo.json"
TEX_OUT = ROOT / "textures/item/club.png"
ANIM_OUT = ROOT / "geckolib/animations/item/club.animation.json"

m = json.load(open(SRC))
res = m.get("resolution", {})
tw, th = res.get("width", 64), res.get("height", 64)


def face_uv(face, name):
    x1, y1, x2, y2 = face["uv"]
    if name in ("north", "east", "south", "west"):
        return {"uv": [x1, y1], "uv_size": [x2 - x1, y2 - y1]}
    if name == "up":
        return {"uv": [min(x1, x2), min(y1, y2)], "uv_size": [abs(x2 - x1), abs(y2 - y1)]}
    # down
    return {"uv": [min(x1, x2), max(y1, y2)], "uv_size": [abs(x2 - x1), -abs(y2 - y1)]}


cubes = []
for e in m["elements"]:
    if e.get("type", "cube") != "cube":
        continue
    fr, to = e["from"], e["to"]
    size = [to[0] - fr[0], to[1] - fr[1], to[2] - fr[2]]
    # bedrock/geckolib flips X: origin is the +X corner mirrored to -X.
    origin = [-(fr[0] + size[0]), fr[1], fr[2]]
    cube = {"origin": origin, "size": size}

    rot = e.get("rotation")
    bbo = e.get("origin", [0, 0, 0])
    if rot and any(abs(r) > 1e-9 for r in rot):
        cube["rotation"] = [-rot[0], -rot[1], rot[2]]
        cube["pivot"] = [-bbo[0], bbo[1], bbo[2]]

    faces = e.get("faces", {})
    uv = {}
    for name in ("north", "east", "south", "west", "up", "down"):
        f = faces.get(name)
        if f and "uv" in f:
            uv[name] = face_uv(f, name)
    cube["uv"] = uv
    cubes.append(cube)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.club",
                "texture_width": tw,
                "texture_height": th,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": [
                {"name": "club", "pivot": [0, 0, 0], "cubes": cubes}
            ],
        }
    ],
}

GEO_OUT.parent.mkdir(parents=True, exist_ok=True)
GEO_OUT.write_text(json.dumps(geo, indent=2) + "\n")
print("wrote", GEO_OUT, "(", len(cubes), "cubes )")

# Texture: decode the embedded base64 PNG.
tex = m["textures"][0]
src = tex["source"]
b64 = src.split(",", 1)[1]
TEX_OUT.parent.mkdir(parents=True, exist_ok=True)
TEX_OUT.write_bytes(base64.b64decode(b64))
print("wrote", TEX_OUT, "(", TEX_OUT.stat().st_size, "bytes )")

# Idle animation placeholder (matches pitchfork: file present, no controllers reference it).
anim = {
    "format_version": "1.10.0",
    "animations": {
        "animation.club.idle": {"loop": True, "animation_length": 1, "bones": {}}
    },
}
ANIM_OUT.parent.mkdir(parents=True, exist_ok=True)
ANIM_OUT.write_text(json.dumps(anim, indent=2) + "\n")
print("wrote", ANIM_OUT)
