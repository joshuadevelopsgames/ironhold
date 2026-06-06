#!/usr/bin/env python3
"""Convert ~/Desktop/mask.bbmodel (free-form MESH elements) into our pipeline's
cube-based Bedrock geo.json. Each mesh in this file is an axis-aligned box, so
it maps to a cube exactly (from = AABB min, size = AABB extent). Coordinates are
preserved verbatim in Blockbench world space (y-up)."""
import json, os
SRC = os.path.expanduser("~/Desktop/mask.bbmodel")
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "scratch", "mask.geo.json")

m = json.load(open(SRC))
res = m.get("resolution", {"width": 16, "height": 16})
tw, th = res["width"], res["height"]

# Descriptive names inferred from position (front = -Z, up = +Y).
NAMES = ["face_center","brow_bar","cheek_bar","edge_right","edge_left","nose_bridge",
         "beak_1","beak_2","beak_3","beak_tip_1","beak_tip_2"]

cubes = []
for i, e in enumerate(m["elements"]):
    assert e.get("type") == "mesh", f"element {i} not a mesh"
    o = e["origin"]
    pts = [(o[0]+v[0], o[1]+v[1], o[2]+v[2]) for v in e["vertices"].values()]
    mn = [min(p[k] for p in pts) for k in range(3)]
    mx = [max(p[k] for p in pts) for k in range(3)]
    size = [round(mx[k]-mn[k], 4) for k in range(3)]
    name = NAMES[i] if i < len(NAMES) else f"part_{i}"
    cubes.append({"origin":[round(v,4) for v in mn], "size":size, "uv":[0,0]})

geo = {
  "format_version":"1.16.0",
  "minecraft:geometry":[{
    "description":{
      "identifier":"geometry.ironhold:mask",
      "texture_width":tw,"texture_height":th,
      "visible_bounds_width":3,"visible_bounds_height":3,
      "visible_bounds_offset":[0,1.5,0]},
    "bones":[{"name":"mask","pivot":[0,0,0],"cubes":cubes}]
  }]
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(geo, open(OUT,"w"), indent=2)
print("converted",len(cubes),"meshes -> cubes")
print("out:",OUT)
for i,c in enumerate(cubes):
    print(f"  {NAMES[i] if i<len(NAMES) else i}: origin={c['origin']} size={c['size']}")
