#!/usr/bin/env python3
"""Convert a GeckoLib box-UV .bbmodel into Ironhold item assets.

Reproduces the validated box-UV bbmodel -> geckolib .geo.json transform documented in
memory (reference_geckolib_geo_conversion) and decodes the embedded texture.

  python3 scripts/convert_geo_item.py --name spiked_club --src "/path/to/Spiked Club.bbmodel"

Writes:
  assets/ironhold/geckolib/models/item/<name>.geo.json
  assets/ironhold/textures/item/<name>.png
  assets/ironhold/geckolib/animations/item/<name>.animation.json
"""
import argparse
import base64
import json
from pathlib import Path

ROOT = Path("/Users/joshua/ironhold/src/main/resources/assets/ironhold")


def face_uv(face, name):
    x1, y1, x2, y2 = face["uv"]
    if name in ("north", "east", "south", "west"):
        return {"uv": [x1, y1], "uv_size": [x2 - x1, y2 - y1]}
    if name == "up":
        return {"uv": [min(x1, x2), min(y1, y2)], "uv_size": [abs(x2 - x1), abs(y2 - y1)]}
    return {"uv": [min(x1, x2), max(y1, y2)], "uv_size": [abs(x2 - x1), -abs(y2 - y1)]}  # down


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", required=True, help="item id, e.g. spiked_club")
    ap.add_argument("--src", required=True, help="path to the .bbmodel")
    args = ap.parse_args()
    name = args.name

    m = json.load(open(args.src))
    res = m.get("resolution", {})
    tw, th = res.get("width", 64), res.get("height", 64)

    cubes = []
    for e in m["elements"]:
        if e.get("type", "cube") != "cube":
            continue
        fr, to = e["from"], e["to"]
        size = [to[0] - fr[0], to[1] - fr[1], to[2] - fr[2]]
        origin = [-(fr[0] + size[0]), fr[1], fr[2]]  # bedrock/geckolib flips X
        cube = {"origin": origin, "size": size}

        rot = e.get("rotation")
        bbo = e.get("origin", [0, 0, 0])
        if rot and any(abs(r) > 1e-9 for r in rot):
            cube["rotation"] = [-rot[0], -rot[1], rot[2]]
            cube["pivot"] = [-bbo[0], bbo[1], bbo[2]]

        faces = e.get("faces", {})
        uv = {}
        for fn in ("north", "east", "south", "west", "up", "down"):
            f = faces.get(fn)
            if f and "uv" in f:
                uv[fn] = face_uv(f, fn)
        cube["uv"] = uv
        cubes.append(cube)

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.{name}",
                "texture_width": tw, "texture_height": th,
                "visible_bounds_width": 4, "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": [{"name": name, "pivot": [0, 0, 0], "cubes": cubes}],
        }],
    }
    geo_out = ROOT / f"geckolib/models/item/{name}.geo.json"
    geo_out.parent.mkdir(parents=True, exist_ok=True)
    geo_out.write_text(json.dumps(geo, indent=2) + "\n")
    print(f"wrote {geo_out} ({len(cubes)} cubes / {len(m['elements'])} elements)")

    tex_out = ROOT / f"textures/item/{name}.png"
    b64 = m["textures"][0]["source"].split(",", 1)[1]
    tex_out.parent.mkdir(parents=True, exist_ok=True)
    tex_out.write_bytes(base64.b64decode(b64))
    print(f"wrote {tex_out} ({tex_out.stat().st_size} bytes)")

    anim = {"format_version": "1.10.0",
            "animations": {f"animation.{name}.idle": {"loop": True, "animation_length": 1, "bones": {}}}}
    anim_out = ROOT / f"geckolib/animations/item/{name}.animation.json"
    anim_out.parent.mkdir(parents=True, exist_ok=True)
    anim_out.write_text(json.dumps(anim, indent=2) + "\n")
    print(f"wrote {anim_out}")


if __name__ == "__main__":
    main()
