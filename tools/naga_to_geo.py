#!/usr/bin/env python3
"""Convert an llibrary AdvancedModelRenderer Java model (Mowzie's-style, e.g. ModelNaga) into a
GeckoLib/Bedrock geo.json, for use as a *reference base* to build our own model from.

Parses the model constructor (new AdvancedModelRenderer / setRotationPoint / addBox / setRotateAngle /
mirror / addChild) and emits geo bones. Coordinate transform Java ModelRenderer -> Bedrock geo:
  * pivots accumulated by TRANSLATION only (Bedrock re-applies each bone's own rotation via hierarchy),
  * Y axis flipped (ModelRenderer is Y-down, Bedrock is Y-up),
  * each bone keeps only its OWN rotateAngle (degrees), parent rotations come from parenting.

NOTE: axis/rotation sign conventions between the two engines are finicky — treat the output as a
first-cut to open in Blockbench and visually verify/clean up, not a final asset.
"""
import re, math, json, sys

SRC = sys.argv[1] if len(sys.argv) > 1 else "/tmp/mowzie/src/ModelNaga.java"
OUT = sys.argv[2] if len(sys.argv) > 2 else "art/blockbench/dragon/dragon_from_naga.geo.json"
IDENT = "geometry.ironhold:dragon_from_naga"

txt = open(SRC).read()

def num(s):
    s = s.replace("(float)", "").replace("Math.PI", "math.pi")
    s = re.sub(r"(?<=[0-9.])[fFdD]\b", "", s)
    return float(eval(s, {"math": math, "__builtins__": {}}))

def nums(s):
    return [num(p) for p in s.split(",")]

bones = {}          # name -> dict(tex,pivot,rot,cubes,mirror,parent)
order = []

for name, u, v in re.findall(r"this\.(\w+)\s*=\s*new AdvancedModelRenderer\(this,\s*([^,]+),\s*([^)]+)\);", txt):
    if name not in bones:
        bones[name] = {"tex": [int(num(u)), int(num(v))], "pivot": [0.0, 0.0, 0.0],
                       "rot": [0.0, 0.0, 0.0], "cubes": [], "mirror": False, "parent": None}
        order.append(name)

for name, args in re.findall(r"this\.(\w+)\.setRotationPoint\(([^)]+)\);", txt):
    if name in bones:
        bones[name]["pivot"] = nums(args)

for name, args in re.findall(r"this\.(\w+)\.addBox\(([^)]+)\);", txt):
    if name in bones:
        a = nums(args)
        ox, oy, oz, w, h, d = a[:6]
        inflate = a[6] if len(a) > 6 else 0.0
        bones[name]["cubes"].append({"o": [ox, oy, oz], "s": [w, h, d], "inf": inflate})

for name, args in re.findall(r"setRotateAngle\(this\.(\w+),\s*([^)]+)\);", txt):
    if name in bones:
        bones[name]["rot"] = nums(args)

for name in re.findall(r"this\.(\w+)\.mirror\s*=\s*true;", txt):
    if name in bones:
        bones[name]["mirror"] = True

for parent, child in re.findall(r"this\.(\w+)\.addChild\(this\.(\w+)\);", txt):
    if child in bones and parent in bones:
        bones[child]["parent"] = parent

# Absolute pivots by translation-only accumulation (root-down).
def abs_pivot(name, seen=None):
    seen = seen or set()
    if name in seen:
        return [0.0, 0.0, 0.0]
    seen.add(name)
    b = bones[name]
    if b["parent"] is None:
        return list(b["pivot"])
    pp = abs_pivot(b["parent"], seen)
    return [pp[0] + b["pivot"][0], pp[1] + b["pivot"][1], pp[2] + b["pivot"][2]]

def deg(r):
    return round(math.degrees(r), 4)

geo_bones = []
for name in order:
    b = bones[name]
    ap = abs_pivot(name)
    gb = {"name": name}
    if b["parent"]:
        gb["parent"] = b["parent"]
    gb["pivot"] = [round(ap[0], 4), round(-ap[1], 4), round(ap[2], 4)]   # Y flip
    rx, ry, rz = b["rot"]
    if (rx, ry, rz) != (0.0, 0.0, 0.0):
        gb["rotation"] = [deg(-rx), deg(-ry), deg(rz)]                    # Y-flip sign convention (verify)
    cubes = []
    for c in b["cubes"]:
        ox, oy, oz = c["o"]; w, h, d = c["s"]
        # cube min-corner in absolute Y-down space, then flip Y to get Bedrock origin
        ax, ay, az = ap[0] + ox, ap[1] + oy, ap[2] + oz
        cube = {"origin": [round(ax, 4), round(-(ay + h), 4), round(az, 4)],
                "size": [w, h, d], "uv": b["tex"]}
        if c["inf"]:
            cube["inflate"] = c["inf"]
        if b["mirror"]:
            cube["mirror"] = True
        cubes.append(cube)
    if cubes:
        gb["cubes"] = cubes
    geo_bones.append(gb)

geo = {"format_version": "1.12.0",
       "minecraft:geometry": [{
           "description": {"identifier": IDENT, "texture_width": 256, "texture_height": 256,
                           "visible_bounds_width": 12, "visible_bounds_height": 8,
                           "visible_bounds_offset": [0, 2, 0]},
           "bones": geo_bones}]}

import os
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(geo, open(OUT, "w"), indent="\t")
print(f"wrote {OUT}: {len(geo_bones)} bones, "
      f"{sum(len(b.get('cubes', [])) for b in geo_bones)} cubes")
# quick category breakdown
for tag in ("wing", "tail", "backFin", "spike", "head", "body", "neck"):
    n = sum(1 for b in geo_bones if tag.lower() in b["name"].lower())
    print(f"  {tag:8s}: {n} bones")
