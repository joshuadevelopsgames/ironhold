#!/usr/bin/env python3
"""Convert a Blockbench `java_block` .bbmodel into a vanilla Minecraft block model JSON.

Blockbench stores face UVs in *texture-resolution* space (0..resolution); vanilla
block models expect UVs in 0..16 space, so we scale by 16/resolution. Element
geometry (from/to/rotation) and the `display` transforms are already in the vanilla
coordinate system and pass through unchanged. All faces are remapped to a single
`#all` texture (these models use one texture).

Usage:
    python3 scripts/bbmodel_to_blockmodel.py <in.bbmodel> <texture_ref> <out_model.json>
        texture_ref e.g. "ironhold:block/butterfly_terrarium"
"""
import json
import sys


def convert(bbmodel_path, texture_ref, out_path):
    bb = json.load(open(bbmodel_path))
    if bb.get("meta", {}).get("model_format") != "java_block":
        raise SystemExit(f"expected java_block model, got {bb.get('meta', {}).get('model_format')}")
    res = bb.get("resolution", {})
    scale = 16.0 / res.get("width", 16)

    def s(v):
        # round to a clean value; keep ints where possible
        r = round(v * scale, 4)
        return int(r) if r == int(r) else r

    elements = []
    for e in bb.get("elements", []):
        if "from" not in e or "to" not in e:
            continue
        out = {"from": e["from"], "to": e["to"]}
        rot = e.get("rotation")
        # Blockbench element rotation -> vanilla single-axis {angle, axis, origin}
        if isinstance(rot, dict) and rot.get("angle"):
            out["rotation"] = {
                "origin": rot.get("origin", [8, 8, 8]),
                "axis": rot.get("axis", "y"),
                "angle": rot.get("angle", 0),
            }
        faces = {}
        for face, fd in e.get("faces", {}).items():
            if fd.get("texture") is None or "uv" not in fd:
                continue
            uv = fd["uv"]
            entry = {"uv": [s(uv[0]), s(uv[1]), s(uv[2]), s(uv[3])], "texture": "#all"}
            if fd.get("rotation"):
                entry["rotation"] = fd["rotation"]
            if fd.get("cullface"):
                entry["cullface"] = fd["cullface"]
            faces[face] = entry
        out["faces"] = faces
        elements.append(out)

    model = {
        # block/block supplies the standard gui/fixed/ground/hand display transforms so the
        # block item renders correctly in inventories; our own display (if any) overrides per-key.
        "parent": "minecraft:block/block",
        "textures": {"all": texture_ref, "particle": texture_ref},
        "elements": elements,
    }
    display = bb.get("display")
    if display:
        model["display"] = display

    with open(out_path, "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")
    # sanity: report UV range
    mx = max((max(face["uv"]) for el in elements for face in el["faces"].values()), default=0)
    print(f"wrote {out_path}  ({len(elements)} elements, max uv {mx}, scale {scale})")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    convert(sys.argv[1], sys.argv[2], sys.argv[3])
