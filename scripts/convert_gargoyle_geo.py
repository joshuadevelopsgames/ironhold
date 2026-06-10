#!/usr/bin/env python3
"""Convert the updated gargoyle .bbmodel (bedrock, box-UV) into the GeckoLib
entity geo.json, reconstructing the named bone rig from element names.

Why a one-off: the bbmodel's groups are UNNAMED (uuid/isOpen/children only, no
name/origin/rotation), but the committed fly/idle/charge animations bind to bone
names (body, head, left/right_arm, left/right_leg, left/right_wing, tail). So we
re-derive the rig from element-name prefixes mapping to the existing hierarchy.

Convention (verified against the known-good existing gargoyle.geo.json):
  bedrock-native format -> origin = from (NO X-flip), size = to - from,
  cube uv = box uv_offset, bone pivots are ABSOLUTE model-space coords.

Reuses the existing geo's description + format_version verbatim so only the
geometry/texture change. Also extracts the embedded texture.
"""
import base64
import json
from pathlib import Path

SRC = Path("/Users/joshua/Downloads/gargoyle.bbmodel")
ROOT = Path("/Users/joshua/ironhold/src/main/resources/assets/ironhold")
GEO_OUT = ROOT / "geckolib/models/entity/gargoyle.geo.json"
TEX_OUT = ROOT / "textures/entity/gargoyle.png"

# bone -> parent, mirroring the existing rig (stone_details is dropped; the new
# model folds those decorative cubes into body/head and nothing animates it).
PARENT = {
    "body": "root", "head": "body",
    "left_horn": "head", "right_horn": "head",
    "left_wing": "body", "right_wing": "body",
    "left_arm": "body", "right_arm": "body",
    "left_leg": "root", "right_leg": "root",
    "tail": "body",
}
ORDER = ["root", "body", "head", "left_horn", "right_horn", "left_wing",
         "right_wing", "left_arm", "right_arm", "left_leg", "right_leg", "tail"]
# bone -> the element whose `origin` supplies the (absolute) bone pivot.
PRIMARY = {
    "body": "body_core", "head": "head_core",
    "left_horn": "left_horn_base", "right_horn": "right_horn_base",
    "left_wing": "left_wing_inner", "right_wing": "right_wing_inner",
    "left_leg": "left_leg_core", "right_leg": "right_leg_core",
    "tail": "tail_base",
}


def bone_for(e):
    n = e["name"]
    cx = (e["from"][0] + e["to"][0]) / 2.0  # mid-X disambiguates mirror-named arms
    if n in ("body_core", "shoulders", "tailbone"):
        return "body"
    if n in ("head_core", "brow", "muzzle", "left_eye"):
        return "head"
    if n.startswith("left_horn"):
        return "left_horn"
    if n.startswith("right_horn"):
        return "right_horn"
    if n.startswith("left_wing"):
        return "left_wing"
    if n.startswith("right_wing"):
        return "right_wing"
    if n.startswith("left_leg") or n.startswith("left_foot") or n.startswith("left_toe"):
        return "left_leg"
    if n.startswith("right_leg") or n.startswith("right_foot") or n.startswith("right_toe"):
        return "right_leg"
    if n.startswith("tail"):  # tailbone already routed to body above
        return "tail"
    # arm pieces are all named "left_*" (mirror modeling) -> split by X sign
    if n.startswith("left_forearm") or n.startswith("left_claw"):
        return "right_arm" if cx > 0 else "left_arm"
    raise SystemExit(f"unmapped element name: {n!r}")


def make_cube(e):
    fr, to = e["from"], e["to"]
    size = [to[i] - fr[i] for i in range(3)]
    cube = {"origin": list(fr), "size": size, "uv": list(e["uv_offset"])}
    rot = e.get("rotation")
    if rot and any(abs(r) > 1e-9 for r in rot):
        cube["rotation"] = list(rot)        # bedrock-native: keep sign
        cube["pivot"] = list(e["origin"])   # rotation pivot
    return cube


def main():
    m = json.loads(SRC.read_text())
    elems = [e for e in m["elements"] if e.get("type", "cube") == "cube"]

    # group elements by bone, preserving file order
    buckets = {b: [] for b in ORDER}
    for e in elems:
        buckets[bone_for(e)].append(e)

    # pivot per bone: from the primary element's origin; arms use their forearm
    by_name_side = {}
    for e in elems:
        by_name_side[(e["name"], (e["from"][0] + e["to"][0]) / 2.0 > 0)] = e

    def pivot_for(bone):
        if bone == "root":
            return [0, 0, 0]
        if bone in PRIMARY:
            prim = next(e for e in buckets[bone] if e["name"] == PRIMARY[bone])
            return list(prim["origin"])
        # arms: forearm element on the matching side
        side = bone == "right_arm"
        prim = next(e for e in buckets[bone]
                    if e["name"].startswith("left_forearm")
                    and ((e["from"][0] + e["to"][0]) / 2.0 > 0) == side)
        return list(prim["origin"])

    # reuse existing description + format_version verbatim
    existing = json.loads(GEO_OUT.read_text())
    desc = existing["minecraft:geometry"][0]["description"]
    fmt = existing.get("format_version", "1.12.0")

    bones = []
    for bone in ORDER:
        b = {"name": bone}
        if bone in PARENT:
            b["parent"] = PARENT[bone]
        b["pivot"] = pivot_for(bone)
        cubes = [make_cube(e) for e in buckets[bone]]
        if cubes:
            b["cubes"] = cubes
        bones.append(b)

    geo = {"format_version": fmt,
           "minecraft:geometry": [{"description": desc, "bones": bones}]}
    GEO_OUT.write_text(json.dumps(geo, indent=2) + "\n")

    b64 = m["textures"][0]["source"].split(",", 1)[1]
    TEX_OUT.write_bytes(base64.b64decode(b64))

    # ---- verification ----
    total = sum(len(b.get("cubes", [])) for b in bones)
    print(f"wrote {GEO_OUT}")
    print(f"  format_version={fmt} identifier={desc['identifier']} "
          f"tex={desc['texture_width']}x{desc['texture_height']}")
    for b in bones:
        print(f"  {b['name']:11s} parent={str(b.get('parent')):6s} "
              f"pivot={b['pivot']} cubes={len(b.get('cubes', []))}")
    print(f"  total cubes={total} / elements={len(elems)}")
    print(f"wrote {TEX_OUT} ({TEX_OUT.stat().st_size} bytes)")

    anim = json.loads((ROOT / "geckolib/animations/entity/gargoyle.animation.json").read_text())
    used = {bn for a in anim["animations"].values() for bn in a.get("bones", {})}
    have = {b["name"] for b in bones}
    missing = used - have
    print(f"animation bones used={sorted(used)}")
    print(f"  MISSING from new rig: {sorted(missing) if missing else 'none ✓'}")


if __name__ == "__main__":
    main()
