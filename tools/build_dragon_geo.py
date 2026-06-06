#!/usr/bin/env python3
"""Carve the full Naga-converted rig down to OUR dragon: keep ONLY the wings + tail subtrees
(the parts we're mimicking), drop the serpent head/neck/body-spikes, and anchor them on our own
placeholder torso. Output is the base we build our dragon's body/head onto.

In:  art/blockbench/dragon/dragon_from_naga.geo.json   (full converted reference)
Out: art/blockbench/dragon/dragon.geo.json             (ours — wings + tail only)
"""
import json, os

IN = "art/blockbench/dragon/dragon_from_naga.geo.json"
OUT = "art/blockbench/dragon/dragon.geo.json"
IDENT = "geometry.ironhold:dragon"

# Subtree roots to KEEP — everything beneath them comes along (wing chains + tail chain incl.
# tail fins, tail spikes, and the small back-wings that hang off the tail base).
KEEP_ROOTS = ["shoulderLJoint", "shoulderRJoint", "tail1"]
ALWAYS = {"root", "body"}  # root + our anchor torso

doc = json.load(open(IN))
bones = doc["minecraft:geometry"][0]["bones"]
by = {b["name"]: b for b in bones}
children = {}
for b in bones:
    children.setdefault(b.get("parent"), []).append(b["name"])

def subtree(name):
    out = {name}
    for c in children.get(name, []):
        out |= subtree(c)
    return out

keep = set(ALWAYS)
for r in KEEP_ROOTS:
    if r in by:
        keep |= subtree(r)

# Drop the legacy "...Reversed" twin planes. They are a back-face hack for llibrary's single-sided
# renderer (a mirrored plane offset -0.004 to show the membrane's far side). In Blockbench / a
# double-sided render type each plane is already two-sided, so the coincident twins just Z-FIGHT
# (the cross-hatching). They are all leaf bones, so removing them orphans nothing.
keep = {n for n in keep if not n.endswith("Reversed")}

kept = [b for b in bones if b["name"] in keep]

# Give the body OUR own dragon torso (the placeholder is gone). Pivot kept so the wing shoulders
# (x=±8) and the tail (z=+8) stay attached where they were.
for b in kept:
    if b["name"] == "body":
        b["cubes"] = [{"origin": [-6.5, 2.0, -14.0], "size": [13, 13, 24], "uv": [0, 200]}]
        b.pop("rotation", None)

# Raise the wings out of the flat Naga flight-pose into a spread-up dragon V (roll at the shoulder).
WING_LIFT = {"shoulderLJoint": [0, 0, 38], "shoulderRJoint": [0, 0, -38]}
for b in kept:
    if b["name"] in WING_LIFT:
        b["rotation"] = WING_LIFT[b["name"]]

# OUR dragon head + neck. Authored as a STRAIGHT forward (-Z) chain at body-top height (y~13), which
# the per-bone rotations then arc UP into the iconic raised dragon neck. Forward = -Z; +X pitches the
# forward end UP. Pivots sit at each joint along the straight chain; cubes sit around their pivot.
DRAGON_BONES = [
    {"name": "neck1", "parent": "body", "pivot": [0, 13, -13], "rotation": [48, 0, 0],
     "cubes": [{"origin": [-4, 9, -25], "size": [8, 8, 12], "uv": [120, 0]}]},
    {"name": "neck2", "parent": "neck1", "pivot": [0, 13, -25], "rotation": [22, 0, 0],
     "cubes": [{"origin": [-3.5, 9.5, -36], "size": [7, 7, 11], "uv": [120, 22]}]},
    {"name": "neck3", "parent": "neck2", "pivot": [0, 13, -36], "rotation": [-18, 0, 0],
     "cubes": [{"origin": [-3, 10, -46], "size": [6, 6, 10], "uv": [120, 42]}]},
    {"name": "head", "parent": "neck3", "pivot": [0, 13, -46], "rotation": [-42, 0, 0],
     "cubes": [
         {"origin": [-5.5, 8.5, -60], "size": [11, 9, 14], "uv": [150, 0]},   # cranium
         {"origin": [-4, 9.5, -67], "size": [8, 6, 7], "uv": [150, 24]},      # snout
     ]},
    {"name": "jaw", "parent": "head", "pivot": [0, 8, -60], "rotation": [7, 0, 0],
     "cubes": [{"origin": [-4, 5, -67], "size": [8, 3, 21], "uv": [150, 40]}]},
    {"name": "horn_L", "parent": "head", "pivot": [3.5, 17, -49], "rotation": [40, 0, -12],
     "cubes": [{"origin": [2, 17, -50.5], "size": [3, 13, 3], "uv": [185, 0]}]},
    {"name": "horn_R", "parent": "head", "pivot": [-3.5, 17, -49], "rotation": [40, 0, 12],
     "cubes": [{"origin": [-5, 17, -50.5], "size": [3, 13, 3], "uv": [185, 0], "mirror": True}]},
]
kept.extend(DRAGON_BONES)

doc["minecraft:geometry"][0]["bones"] = kept
doc["minecraft:geometry"][0]["description"]["identifier"] = IDENT

os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump(doc, open(OUT, "w"), indent="\t")

wings = sum(1 for b in kept if any(t in b["name"].lower() for t in ("wing", "shoulder", "arm", "hand", "claw")))
tail = sum(1 for b in kept if any(t in b["name"].lower() for t in ("tail", "backfin", "spike", "backwing")))
print(f"wrote {OUT}: {len(kept)} bones ({wings} wing, {tail} tail, + root + body anchor)")
print("dropped:", sorted(b["name"] for b in bones if b["name"] not in keep)[:30])
